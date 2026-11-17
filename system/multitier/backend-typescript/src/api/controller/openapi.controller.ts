import { Controller, Get, NotFoundException } from '@nestjs/common';
import { ApiExcludeController, type OpenAPIObject } from '@nestjs/swagger';
import {
  OPENAPI_DOCUMENT_PATH,
  openApiDocument,
} from '../openapi/openapi-document';
import { isProdProfile } from '../../config/production-config.guard';

/** Serves the generated OpenAPI document at springdoc's URL; like the Java side it needs a signed-in caller. */
@ApiExcludeController()
@Controller(OPENAPI_DOCUMENT_PATH)
export class OpenApiController {
  @Get()
  document(): OpenAPIObject {
    // Not set up under the prod profile (B-PRG 4.4, see configureApp).
    if (isProdProfile()) {
      throw new NotFoundException();
    }
    return openApiDocument();
  }
}
