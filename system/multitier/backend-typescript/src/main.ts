import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { GlobalExceptionFilter } from './api/exception/global-exception.filter';
import { CustomValidationPipe } from './api/exception/custom-validation.pipe';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    rawBody: true,
  });

  const allowedOrigins = (process.env.ALLOWED_ORIGINS || 'http://localhost:8080').split(',');

  app.enableCors({
    origin: allowedOrigins,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: '*',
    credentials: true,
    maxAge: 3600,
  });

  app.useGlobalPipes(new CustomValidationPipe());
  app.useGlobalFilters(new GlobalExceptionFilter());

  // Store raw body on request for validation error handling
  app.use((req: { rawBody?: Buffer; on: (event: string, cb: (chunk: Buffer) => void) => void; body?: unknown }, _res: unknown, next: () => void) => {
    if (req.rawBody) {
      // rawBody already available via NestFactory option
    }
    next();
  });

  await app.listen(process.env.PORT ?? 8081);
}
void bootstrap();
