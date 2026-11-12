import { z } from 'zod';
import {
  couponsResponseSchema,
  orderDetailSchema,
  ordersResponseSchema,
  placeOrderResponseSchema,
  productSchema,
  productsResponseSchema,
  profileResponseSchema,
} from './api-types';
import { placeOrderSchema, publishCouponSchema, updateProfileSchema } from './validation';
import { IDEMPOTENCY_KEY_HEADER } from './idempotency';

// Code-first OpenAPI, the Next.js twin of the Java backends' springdoc setup (OpenApiConfig). The
// schemas are generated from the same zod schemas the routes validate requests with and the pages
// parse responses with, so they cannot drift from the code; openapi.spec.ts fails when a route under
// src/app/api is missing here (or listed here but gone). Served at springdoc's URL, /v3/api-docs, and,
// like the Java side, only to a signed-in caller (the middleware's secure-by-default policy).

type Schema = Record<string, unknown>;

// Request schemas preprocess/transform their input (trim, numeric strings); describe what a client
// sends, and let any step JSON Schema cannot express fall back to "anything".
function requestSchema(schema: z.ZodType): Schema {
  return z.toJSONSchema(schema, { io: 'input', unrepresentable: 'any' });
}

function responseSchema(schema: z.ZodType): Schema {
  return z.toJSONSchema(schema, { io: 'output', unrepresentable: 'any' });
}

const problemDetail: Schema = {
  type: 'object',
  properties: {
    type: { type: 'string' },
    title: { type: 'string' },
    status: { type: 'integer' },
    detail: { type: 'string' },
    instance: { type: 'string' },
    timestamp: { type: 'string' },
    errors: {
      type: 'array',
      items: {
        type: 'object',
        properties: { field: { type: 'string' }, message: { type: 'string' }, code: { type: 'string' } },
      },
    },
  },
};

const json = (schema: Schema) => ({ content: { 'application/json': { schema } } });
const problem = (description: string) => ({
  description,
  content: { 'application/problem+json': { schema: { $ref: '#/components/schemas/ProblemDetail' } } },
});
const pathParam = (name: string) => ({ name, in: 'path', required: true, schema: { type: 'string' } });

const ERP_UNAVAILABLE = {
  ...problem('The ERP is unavailable; retry after the Retry-After header (seconds)'),
  headers: { 'Retry-After': { schema: { type: 'integer' } } },
};

interface Operation {
  summary: string;
  public?: boolean;
  parameters?: unknown[];
  requestBody?: Schema;
  responses: Record<string, unknown>;
}

/** Every API route and method; keep in step with src/app/api (openapi.spec.ts checks it). */
export const API_OPERATIONS: Record<string, Partial<Record<'get' | 'post' | 'put', Operation>>> = {
  '/api/products': {
    get: {
      summary: 'Browse the product catalog',
      public: true,
      responses: { '200': { description: 'The catalog', ...json(responseSchema(productsResponseSchema)) }, '503': ERP_UNAVAILABLE },
    },
  },
  '/api/products/{sku}': {
    get: {
      summary: 'View a product',
      public: true,
      parameters: [pathParam('sku')],
      responses: {
        '200': { description: 'The product', ...json(responseSchema(productSchema)) },
        '404': problem('No product has that SKU'),
        '503': ERP_UNAVAILABLE,
      },
    },
  },
  '/api/orders': {
    get: {
      summary: 'Browse order history',
      responses: { '200': { description: 'A page of orders', ...json(responseSchema(ordersResponseSchema)) } },
    },
    post: {
      summary: 'Place an order (customers only); idempotent with the Idempotency-Key header',
      parameters: [{ name: IDEMPOTENCY_KEY_HEADER, in: 'header', required: false, schema: { type: 'string' } }],
      requestBody: { required: true, ...json(requestSchema(placeOrderSchema)) },
      responses: {
        '201': { description: 'The order was placed', ...json(responseSchema(placeOrderResponseSchema)) },
        '400': problem('The request is invalid'),
        '503': ERP_UNAVAILABLE,
      },
    },
  },
  '/api/orders/{orderNumber}': {
    get: {
      summary: 'View an order',
      parameters: [pathParam('orderNumber')],
      responses: { '200': { description: 'The order', ...json(responseSchema(orderDetailSchema)) }, '404': problem('No such order') },
    },
  },
  '/api/orders/{orderNumber}/cancel': {
    post: {
      summary: 'Cancel an order',
      parameters: [pathParam('orderNumber')],
      responses: { '204': { description: 'The order was cancelled' }, '404': problem('No such order') },
    },
  },
  '/api/orders/{orderNumber}/deliver': {
    post: {
      summary: 'Deliver an order (admins only)',
      parameters: [pathParam('orderNumber')],
      responses: { '204': { description: 'The order was delivered' }, '404': problem('No such order') },
    },
  },
  '/api/coupons': {
    get: {
      summary: 'Browse coupons (admins only)',
      responses: { '200': { description: 'A page of coupons', ...json(responseSchema(couponsResponseSchema)) } },
    },
    post: {
      summary: 'Publish a coupon (admins only)',
      requestBody: { required: true, ...json(requestSchema(publishCouponSchema)) },
      responses: { '201': { description: 'The coupon was published' }, '400': problem('The request is invalid') },
    },
  },
  '/api/coupons/{code}': {
    get: {
      summary: 'View a coupon',
      parameters: [pathParam('code')],
      responses: { '200': { description: 'The coupon' }, '404': problem('No such coupon') },
    },
  },
  '/api/account/profile': {
    get: {
      summary: "View the signed-in user's profile",
      responses: { '200': { description: 'The profile', ...json(responseSchema(profileResponseSchema)) } },
    },
    put: {
      summary: "Update the signed-in user's profile",
      requestBody: { required: true, ...json(requestSchema(updateProfileSchema)) },
      responses: { '200': { description: 'The updated profile', ...json(responseSchema(profileResponseSchema)) }, '400': problem('The request is invalid') },
    },
  },
};

export function buildOpenApiDocument(): Schema {
  const paths = Object.fromEntries(
    Object.entries(API_OPERATIONS).map(([path, operations]) => [
      path,
      Object.fromEntries(
        Object.entries(operations).map(([method, { public: isPublic, ...operation }]) => [
          method,
          { ...operation, ...(isPublic ? { security: [] } : {}) },
        ])
      ),
    ])
  );
  return {
    openapi: '3.1.0',
    info: { title: 'MyShop API', description: 'REST API for the MyShop backend', version: '1.0.0' },
    security: [{ bearerAuth: [] }],
    paths,
    components: {
      securitySchemes: { bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' } },
      schemas: { ProblemDetail: problemDetail },
    },
  };
}
