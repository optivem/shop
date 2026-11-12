import fs from 'node:fs';
import path from 'node:path';
import { API_OPERATIONS, buildOpenApiDocument } from '@/lib/openapi';
import { decide } from '@/lib/auth/policy';

const API_DIR = path.join(__dirname, '..', 'app', 'api');

// Every exported HTTP handler under src/app/api, as "METHOD /api/path/{param}".
function discoverRoutes(dir: string): string[] {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) return discoverRoutes(full);
    if (entry.name !== 'route.ts') return [];
    const urlPath = `/api/${path.relative(API_DIR, path.dirname(full)).split(path.sep).join('/')}`
      .replace(/\[(\w+)\]/g, '{$1}')
      .replace(/\/$/, '');
    const source = fs.readFileSync(full, 'utf8');
    return [...source.matchAll(/export (?:async )?function (GET|POST|PUT|DELETE|PATCH)\b/g)].map(
      (match) => `${match[1]} ${urlPath}`
    );
  });
}

describe('OpenAPI document', () => {
  it('describes exactly the routes under src/app/api', () => {
    const documented = Object.entries(API_OPERATIONS).flatMap(([urlPath, operations]) =>
      Object.keys(operations).map((method) => `${method.toUpperCase()} ${urlPath}`)
    );

    expect(documented.sort()).toEqual(discoverRoutes(API_DIR).sort());
  });

  it('builds a document with schemas generated from the zod schemas', () => {
    const document = buildOpenApiDocument() as { info: { title: string }; paths: Record<string, Record<string, unknown>> };

    expect(document.info.title).toBe('MyShop API');
    expect(JSON.stringify(document.paths['/api/orders']?.post)).toContain('"sku"');
    expect(JSON.stringify(document.paths['/api/products/{sku}']?.get)).toContain('"price"');
  });

  it('marks as public exactly the operations the auth policy lets anonymous callers use', () => {
    for (const [urlPath, operations] of Object.entries(API_OPERATIONS)) {
      for (const [method, operation] of Object.entries(operations)) {
        const concrete = urlPath.replace(/\{\w+\}/g, 'X');
        expect([`${method} ${urlPath}`, decide(method.toUpperCase(), concrete, null) === 'allow']).toEqual([
          `${method} ${urlPath}`,
          operation.public === true,
        ]);
      }
    }
  });

  it('is served only to a signed-in caller, like springdoc on the Java side', () => {
    expect(decide('GET', '/v3/api-docs', null)).toBe('unauthenticated');
  });
});
