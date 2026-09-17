export interface AppConfig {
  port: number;
  allowedOrigins: string;
  postgresUrl: string;
  externalSystemMode: string;
  erpUrl: string;
  clockUrl: string;
}

export const envOrDefault = (name: string, defaultValue: string): string => {
  const value = process.env[name];
  if (value) return value;
  return defaultValue;
};

const buildPostgresUrl = (): string => {
  if (process.env.POSTGRES_URL) return process.env.POSTGRES_URL;
  const host = envOrDefault('POSTGRES_DB_HOST', 'localhost');
  const port = envOrDefault('POSTGRES_DB_PORT', '5432');
  const name = envOrDefault('POSTGRES_DB_NAME', 'app');
  const user = envOrDefault('POSTGRES_DB_USER', 'app');
  const pass = envOrDefault('POSTGRES_DB_PASSWORD', 'app');
  return `postgresql://${user}:${pass}@${host}:${port}/${name}`;
};

export const getAppConfig = (): AppConfig => ({
  port: Number.parseInt(envOrDefault('PORT', '8081'), 10),
  allowedOrigins: envOrDefault('ALLOWED_ORIGINS', 'http://localhost:8080'),
  postgresUrl: buildPostgresUrl(),
  externalSystemMode: envOrDefault('EXTERNAL_SYSTEM_MODE', 'real'),
  erpUrl: envOrDefault('ERP_API_URL', 'http://localhost:9001/erp'),
  clockUrl: envOrDefault('CLOCK_API_URL', 'http://localhost:9001/clock'),
});
