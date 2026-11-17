# Edge rate limiting (opt-in, not deployed by default)

## What the default deploy protects

No shop backend ever sees a password. The monoliths send the browser to Keycloak to sign in, and the multitier backends only validate bearer tokens. So the login rate limit lives in Keycloak: **brute-force detection** is on in the production realm (`docker/keycloak/prod-realm.jq`; `deploy-keycloak` also sets it on the existing `shop-prod` realm, because a realm import never updates a realm that already exists). The test realms (local, acceptance, QA) leave it off: Keycloak 26 locks an account out when it signs in many times at once, which the parallel system tests do with their shared users.

| Setting | Value | Effect |
|---|---|---|
| `failureFactor` | 10 | After 10 failed logins for one user, the user is locked out for a while |
| `waitIncrementSeconds` / `maxFailureWaitSeconds` | 60 / 900 | The lockout starts at 1 minute and grows to at most 15 minutes |
| `maxDeltaTimeSeconds` | 43200 | The failure count resets after 12 hours without a failure |
| `quickLoginCheckMilliSeconds` / `minimumQuickLoginWaitSeconds` | 1000 / 60 | Two failures less than a second apart lock the user out for 1 minute |
| `permanentLockout` | false | Lockouts always expire; an admin never has to unlock a user |

Successful logins are never counted, so the system tests, which sign in as `customer1` and `admin1` many times in parallel, are not affected. None of them fails a login on purpose.

**What this does not cover (a deliberate shortcut):** the API endpoints themselves have no request limit. A client can call `GET /api/products` as often as it likes, and Cloud Run scales out to absorb it (up to the service's `--max-instances`). Limiting that belongs at the edge, in front of the app. The default deploy has no edge, to keep it at $0.

## Option A: Cloud Armor rate-based rule (GCP)

Cloud Armor attaches to an external Application Load Balancer, not to Cloud Run directly. So this means putting a global load balancer with a serverless NEG in front of each service you want to protect.

**Monthly cost (approximate, check current GCP pricing):** about $18 for the load balancer's forwarding rule, plus about $5 for the Cloud Armor policy and $1 per rule, plus per-request and data-processing charges that are cents at shop's traffic. **That is far above the project's billing kill-switch (about €1, see [terraform/README.md](../../terraform/README.md#cost)):** raise `BILLING_KILL_SWITCH_AMOUNT` before turning this on, or the kill-switch unlinks billing and every Cloud Run service stops.

Sketch, once the load balancer and its backend service (`shop-edge-backend`) exist:

```bash
gcloud compute security-policies create shop-edge --project "$PROJECT"
gcloud compute security-policies rules create 1000 --project "$PROJECT" \
  --security-policy shop-edge --expression "true" \
  --action rate-based-ban --rate-limit-threshold-count 300 --rate-limit-threshold-interval-sec 60 \
  --ban-duration-sec 600 --conform-action allow --exceed-action deny-429 --enforce-on-key IP
gcloud compute backend-services update shop-edge-backend --global --project "$PROJECT" \
  --security-policy shop-edge
```

That allows 300 requests a minute per client IP and answers `429` for 10 minutes once a client goes over. Then set the Cloud Run service's ingress to `internal-and-cloud-load-balancing`, so nobody can reach the `run.app` URL around the limit.

## Option B: a free fronting proxy

A CDN or proxy on a free plan in front of the custom domain (for example, Cloudflare's free plan includes a basic rate-limiting rule) costs $0. It needs the domain's DNS on that provider, and it protects only the custom domain: the `run.app` URLs stay reachable directly unless ingress is restricted, which the free proxy cannot do for you on Cloud Run.

**Monthly cost:** $0 on the free plan; paid plans are needed for finer rules.

## Recommendation

Keep the default (Keycloak brute-force detection only) for the course and for forks: it costs nothing and covers the one place a password is checked. Turn on Option A only for a real deployment that takes public traffic and has a budget for it. Option B is the cheap middle ground when the custom domain is what people use.
