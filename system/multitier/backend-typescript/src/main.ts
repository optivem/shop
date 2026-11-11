// Must stay the first import: it patches http/express/pg/undici before they load (no-op unless OTel is on).
import './tracing';
import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { configureApp } from './configure-app';
import { envOrDefault } from './config/app.config';
import { JsonLogger } from './json-logger';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, { logger: new JsonLogger() });

  const allowedOrigins = envOrDefault(
    'ALLOWED_ORIGINS',
    'http://localhost:8080',
  ).split(',');

  app.enableCors({
    origin: allowedOrigins,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    // Left unset so the cors middleware reflects the preflight's requested headers (Authorization,
    // Content-Type, traceparent, tracestate, ...). A literal '*' is not a wildcard for credentialed requests.
    credentials: true,
    maxAge: 3600,
  });

  configureApp(app);

  await app.listen(process.env.PORT ?? 8081);
}

bootstrap().catch((error: unknown) => {
  new Logger('Bootstrap').error(
    'Application failed to start',
    error instanceof Error ? error.stack : String(error),
  );
  process.exit(1);
});
