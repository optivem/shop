export class JsonWireMockClient {
  private adminUrl: string;

  constructor(baseUrl: string) {
    const url = new URL(baseUrl);
    this.adminUrl = `${url.protocol}//${url.host}/__admin/mappings`;
  }

  async stubGet<T>(urlPath: string, responseBody: T, statusCode = 200): Promise<void> {
    const response = await fetch(this.adminUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        request: {
          method: 'GET',
          urlPath,
        },
        response: {
          status: statusCode,
          headers: { 'Content-Type': 'application/json' },
          jsonBody: responseBody,
        },
      }),
    });

    if (!response.ok) {
      throw new Error(`Failed to create WireMock stub: ${response.status} ${response.statusText}`);
    }
  }
}
