export interface AppConfig {
  port: number;
}

export const getAppConfig = (): AppConfig => ({
  port: parseInt(process.env.PORT || '8081', 10),
});
