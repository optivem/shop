// k6 load test: a customer browses the catalog, views a product and places an order.
//
// Runs against a local docker stack in stub mode (the WireMock ERP/Clock/Tax stubs ship with seeded
// products and tax rates, so nothing needs staging). Start one first, e.g. the Java monolith:
//
//   docker compose -f docker/java/monolith/docker-compose.local.stub.yml up -d --build --wait
//   k6 run perf/k6/browse-and-place-order.js
//
// Defaults target the Java monolith stub stack (API 8112, Keycloak 8192). For the others set
// BASE_URL / KEYCLOAK_URL: .NET 8212 / 8292, TypeScript 8312 / 8392 (see docker/<language>/monolith).
//
// Env vars (all optional):
//   BASE_URL      backend API base URL                 (default http://localhost:8112)
//   KEYCLOAK_URL  Keycloak base URL                    (default http://localhost:8192)
//   SKU           product to view and order            (default HP-15, a seeded stub product)
//   COUNTRY       Quick order tax country              (default DE, a seeded stub country)
//   VUS           concurrent virtual users             (default 5)
//   DURATION      how long to hold the load            (default 1m)
//   SUMMARY_DIR   where handleSummary writes its files (default the current directory)
//
// Thresholds are deliberately loose: this is a latency report (p95/p99 per request), not a gate.
// Only a broken system (errors) or a pathological slowdown fails the run.

import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8112').replace(/\/+$/, '');
const KEYCLOAK_URL = (__ENV.KEYCLOAK_URL || 'http://localhost:8192').replace(/\/+$/, '');
const SKU = __ENV.SKU || 'HP-15';
const COUNTRY = __ENV.COUNTRY || 'DE';
const SUMMARY_DIR = (__ENV.SUMMARY_DIR || '.').replace(/\/+$/, '');

// The test realm's password-grant client and customer (docker/keycloak/shop-realm.json). Test-only
// credentials, the same ones the system tests use; they never exist in a production realm.
const TOKEN_CLIENT_ID = 'shop-system-test';
const CUSTOMER = { username: 'customer1', password: 'customer1-test-password' };

const REQUESTS = ['browse_catalog', 'view_product', 'place_order'];

export const options = {
  scenarios: {
    shopper: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '1m',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // One sub-metric per request, so the summary reports p95/p99 for each of them.
    ...Object.fromEntries(REQUESTS.map((name) => [`http_req_duration{name:${name}}`, ['p(95)<2000', 'p(99)<5000']])),
  },
};

// Keycloak's default access-token lifetime (5 min) covers the default 1m run; a longer DURATION
// refreshes the token per VU once it is about to expire.
function fetchToken() {
  const response = http.post(
    `${KEYCLOAK_URL}/realms/shop/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id: TOKEN_CLIENT_ID,
      username: CUSTOMER.username,
      password: CUSTOMER.password,
    },
    { tags: { name: 'token' } },
  );
  if (response.status !== 200) {
    throw new Error(`Failed to get a token from ${KEYCLOAK_URL}: HTTP ${response.status} ${response.body}`);
  }
  const body = response.json();
  return { value: body.access_token, expiresAt: Date.now() + (body.expires_in - 30) * 1000 };
}

export function setup() {
  const health = http.get(`${BASE_URL}/api/products/${SKU}`, { tags: { name: 'preflight' } });
  if (health.status !== 200) {
    throw new Error(`Preflight failed: GET ${BASE_URL}/api/products/${SKU} answered HTTP ${health.status}; is the stub stack up?`);
  }
  return { token: fetchToken() };
}

let token = null;

export default function (data) {
  if (token === null) token = data.token;
  if (Date.now() >= token.expiresAt) token = fetchToken();
  const auth = { Authorization: `Bearer ${token.value}` };

  group('browse', () => {
    const catalog = http.get(`${BASE_URL}/api/products`, { headers: auth, tags: { name: 'browse_catalog' } });
    check(catalog, { 'catalog is 200': (r) => r.status === 200 });

    const product = http.get(`${BASE_URL}/api/products/${SKU}`, { headers: auth, tags: { name: 'view_product' } });
    check(product, { 'product is 200': (r) => r.status === 200 });
  });

  sleep(1);

  group('place order', () => {
    const order = http.post(
      `${BASE_URL}/api/orders`,
      JSON.stringify({ sku: SKU, quantity: 1, country: COUNTRY }),
      {
        headers: {
          ...auth,
          'Content-Type': 'application/json',
          // Unique per request, so a client-side retry could never place a duplicate order.
          'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}`,
        },
        tags: { name: 'place_order' },
      },
    );
    check(order, { 'order is 201': (r) => r.status === 201 });
  });

  sleep(1);
}

function metricValue(data, name, stat) {
  const metric = data.metrics[`http_req_duration{name:${name}}`];
  const value = metric && metric.values[stat];
  return value === undefined ? 'n/a' : value.toFixed(1);
}

// Writes a markdown latency table (for $GITHUB_STEP_SUMMARY) and the raw JSON next to the console summary.
export function handleSummary(data) {
  const rows = REQUESTS.map(
    (name) => `| ${name} | ${metricValue(data, name, 'med')} | ${metricValue(data, name, 'p(95)')} | ${metricValue(data, name, 'p(99)')} | ${metricValue(data, name, 'max')} |`,
  );
  const failed = data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : 'n/a';
  const requests = data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 'n/a';
  const markdown = [
    `## k6 latency (${BASE_URL})`,
    '',
    `${requests} requests, ${failed}% failed.`,
    '',
    '| Request | median (ms) | p95 (ms) | p99 (ms) | max (ms) |',
    '|---|---|---|---|---|',
    ...rows,
    '',
  ].join('\n');
  return {
    stdout: `${markdown}\n`,
    [`${SUMMARY_DIR}/k6-summary.md`]: markdown,
    [`${SUMMARY_DIR}/k6-summary.json`]: JSON.stringify(data, null, 2),
  };
}
