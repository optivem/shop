export interface AppConfig {
  port: number;
  allowedOrigins: string;
  postgresUrl: string;
  externalSystemMode: string;
  erpUrl: string;
  clockUrl: string;
}

const buildPostgresUrl = (): string => {
  if (process.env.POSTGRES_URL) return process.env.POSTGRES_URL;
  const host = process.env.POSTGRES_DB_HOST || 'localhost';
  const port = process.env.POSTGRES_DB_PORT || '5432';
  const name = process.env.POSTGRES_DB_NAME || 'starter';
  const user = process.env.POSTGRES_DB_USER || 'starter';
  const pass = process.env.POSTGRES_DB_PASSWORD || 'starter';
  return `postgresql://${user}:${pass}@${host}:${port}/${name}`;
};

export const getAppConfig = (): AppConfig => ({
  port: parseInt(process.env.PORT || '8081', 10),
  allowedOrigins: process.env.ALLOWED_ORIGINS || 'http://localhost:8080',
  postgresUrl: buildPostgresUrl(),
  externalSystemMode: process.env.EXTERNAL_SYSTEM_MODE || 'real',
  erpUrl: process.env.ERP_API_URL || 'http://localhost:9001/erp',
  clockUrl: process.env.CLOCK_API_URL || 'http://localhost:9001/clock',
});
