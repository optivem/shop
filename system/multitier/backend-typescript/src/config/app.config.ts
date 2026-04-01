export interface AppConfig {
  port: number;
  allowedOrigins: string;
  postgresUrl: string;
  externalSystemMode: string;
  erpUrl: string;
  clockUrl: string;
}

export const getAppConfig = (): AppConfig => ({
  port: parseInt(process.env.PORT || '8081', 10),
  allowedOrigins: process.env.ALLOWED_ORIGINS || 'http://localhost:8080',
  postgresUrl:
    process.env.POSTGRES_URL ||
    'postgresql://starter:starter@localhost:5432/starter',
  externalSystemMode: process.env.EXTERNAL_SYSTEM_MODE || 'real',
  erpUrl: process.env.ERP_API_URL || 'http://localhost:9001/erp',
  clockUrl: process.env.CLOCK_API_URL || 'http://localhost:9001/clock',
});
