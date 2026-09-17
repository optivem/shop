import { INestApplication } from '@nestjs/common';
import { GlobalExceptionFilter } from './api/exception/global-exception.filter';
import { RequestValidationPipe } from './api/exception/request-validation.pipe';
import { DecimalFormatInterceptor } from './api/interceptor/decimal-format.interceptor';

// Request/response pipeline shared by main.ts and the component harness, so tests run the same app.
export function configureApp(app: INestApplication): void {
  app.useGlobalPipes(new RequestValidationPipe());
  app.useGlobalFilters(new GlobalExceptionFilter());
  app.useGlobalInterceptors(new DecimalFormatInterceptor());
}
