import { Logger } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { configureApp } from './configure-app';
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
