export interface AppConfig {
  port: number;
  todosApiBaseUrl: string;
}

export const getAppConfig = (): AppConfig => ({
  port: parseInt(process.env.PORT || '8081', 10),
  todosApiBaseUrl:
    process.env.TODOS_API_BASE_URL || 'https://jsonplaceholder.typicode.com',
});
