import type { INestApplication } from '@nestjs/common';
import {
  DocumentBuilder,
  SwaggerModule,
  type OpenAPIObject,
} from '@nestjs/swagger';

// Code-first OpenAPI, the TypeScript twin of the Java backends' springdoc setup (OpenApiConfig):
// the document is generated from the controllers (and, under `nest build`, the DTO classes via the
// @nestjs/swagger CLI plugin in nest-cli.json) and served at springdoc's URL, /v3/api-docs, by
// OpenApiController, so the global AuthGuard applies to it like any other route.
export const OPENAPI_DOCUMENT_PATH = 'v3/api-docs';
export const SWAGGER_UI_PATH = 'swagger-ui';

let document: OpenAPIObject | null = null;

export function setUpOpenApi(app: INestApplication): void {
  const config = new DocumentBuilder()
    .setTitle('MyShop API')
    .setDescription('REST API for the MyShop backend')
    .setVersion('1.0.0')
    .addBearerAuth()
    .build();
  document = SwaggerModule.createDocument(app, config);
  // Swagger UI only; raw: false stops it serving its own unguarded copy of the document, so the
  // UI reads the guarded /v3/api-docs like any other caller.
  SwaggerModule.setup(SWAGGER_UI_PATH, app, document, {
    raw: false,
    swaggerOptions: { url: `/${OPENAPI_DOCUMENT_PATH}` },
  });
}

export function openApiDocument(): OpenAPIObject {
  if (document === null) {
    throw new Error(
      'The OpenAPI document is built by setUpOpenApi() at start-up',
    );
  }
  return document;
}
