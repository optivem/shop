export class JsonWireMockClient {
  private adminUrl: string;
  private stubIds: string[] = [];

  constructor(baseUrl: string) {
    const url = new URL(baseUrl);
    this.adminUrl = `${url.protocol}//${url.host}/__admin/mappings`;
  }

  async stubGet<T>(urlPath: string, responseBody: T, statusCode = 200): Promise<void> {
    await this.createStub('GET', urlPath, responseBody, statusCode);
  }

  async stubPost<T>(urlPath: string, responseBody: T, statusCode = 200): Promise<void> {
    await this.createStub('POST', urlPath, responseBody, statusCode);
  }

  async stubPut<T>(urlPath: string, responseBody: T, statusCode = 200): Promise<void> {
    await this.createStub('PUT', urlPath, responseBody, statusCode);
  }

  async stubDelete<T>(urlPath: string, responseBody: T, statusCode = 200): Promise<void> {
    await this.createStub('DELETE', urlPath, responseBody, statusCode);
  }

  async removeStubs(): Promise<void> {
    for (const id of this.stubIds) {
      await fetch(`${this.adminUrl}/${id}`, { method: 'DELETE' });
    }
    this.stubIds = [];
  }

  private async createStub<T>(method: string, urlPath: string, responseBody: T, statusCode: number): Promise<void> {
    const response = await fetch(this.adminUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        request: {
          method,
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

    const body = await response.json() as { id?: string };
    if (body.id) {
      this.stubIds.push(body.id);
    }
  }
}
