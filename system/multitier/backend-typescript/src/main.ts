import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { GlobalExceptionFilter } from './api/exception/global-exception.filter';
import { CustomValidationPipe } from './api/exception/custom-validation.pipe';
import { DecimalFormatInterceptor } from './api/interceptor/decimal-format.interceptor';
import { envOrDefault } from './config/app.config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  const allowedOrigins = envOrDefault(
    'ALLOWED_ORIGINS',
    'http://localhost:8080',
  ).split(',');

  app.enableCors({
    origin: allowedOrigins,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: '*',
    credentials: true,
    maxAge: 3600,
  });

  app.useGlobalPipes(new CustomValidationPipe());
  app.useGlobalFilters(new GlobalExceptionFilter());
  app.useGlobalInterceptors(new DecimalFormatInterceptor());

  await app.listen(process.env.PORT ?? 8081);
}
void bootstrap();
