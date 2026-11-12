import { Controller, Get } from '@nestjs/common';
import { ApiExcludeController, type OpenAPIObject } from '@nestjs/swagger';
import {
  OPENAPI_DOCUMENT_PATH,
  openApiDocument,
} from '../openapi/openapi-document';

/** Serves the generated OpenAPI document at springdoc's URL; like the Java side it needs a signed-in caller. */
@ApiExcludeController()
@Controller(OPENAPI_DOCUMENT_PATH)
export class OpenApiController {
  @Get()
  document(): OpenAPIObject {
    return openApiDocument();
  }
}
