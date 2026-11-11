const OUTAGE_PRIORITY = 1;

export class JsonWireMockClient {
  private readonly adminUrl: string;
  private stubIds: string[] = [];
  /** The ids of the stubs registered with stubGetMatching, by their path pattern. */
  private matchingStubIds = new Map<string, string[]>();

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

  /** Stubs a GET of `urlPath` with a bodyless `statusCode`, at the default priority (the newest such stub wins). */
  async stubGetStatus(urlPath: string, statusCode: number): Promise<void> {
    await this.registerMapping(`GET ${urlPath}`, {
      request: { method: 'GET', urlPath },
      response: { status: statusCode },
    });
  }

  /**
   * Stubs every GET whose path matches `urlPathPattern` with a bodyless `statusCode`, at priority 1 so
   * it outranks the default-priority (5) stubs this client registers. Used to take a whole external
   * system down.
   */
  async stubGetMatching(urlPathPattern: string, statusCode: number): Promise<void> {
    await this.registerMapping(`GET ${urlPathPattern}`, {
      priority: OUTAGE_PRIORITY,
      request: { method: 'GET', urlPathPattern },
      response: { status: statusCode },
    });
    const id = this.stubIds[this.stubIds.length - 1];
    if (id !== undefined) {
      this.matchingStubIds.set(urlPathPattern, [...(this.matchingStubIds.get(urlPathPattern) ?? []), id]);
    }
  }

  /** Removes the stubs this client registered with stubGetMatching for `urlPathPattern`. */
  async removeStubsMatching(urlPathPattern: string): Promise<void> {
    const ids = this.matchingStubIds.get(urlPathPattern) ?? [];
    for (const id of ids) {
      const response = await fetch(`${this.adminUrl}/${id}`, { method: 'DELETE' });
      if (!response.ok) {
        throw new Error(`Failed to remove stub GET ${urlPathPattern}: ${response.status} ${response.statusText} ${await response.text()}`);
      }
    }
    this.stubIds = this.stubIds.filter((id) => !ids.includes(id));
    this.matchingStubIds.delete(urlPathPattern);
  }

  async removeStubs(): Promise<void> {
    for (const id of this.stubIds) {
      await fetch(`${this.adminUrl}/${id}`, { method: 'DELETE' });
    }
    this.stubIds = [];
    this.matchingStubIds.clear();
  }

  private async createStub<T>(method: string, urlPath: string, responseBody: T, statusCode: number): Promise<void> {
    await this.registerMapping(`${method} ${urlPath}`, {
      request: {
        method,
        urlPath,
      },
      response: {
        status: statusCode,
        headers: { 'Content-Type': 'application/json' },
        jsonBody: responseBody,
      },
    });
  }

  private async registerMapping(description: string, mapping: object): Promise<void> {
    const response = await fetch(this.adminUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(mapping),
    });

    if (!response.ok) {
      throw new Error(`Failed to register stub for ${description}: ${response.status} ${response.statusText} ${await response.text()}`);
    }

    const body = await response.json() as { id?: string };
    if (!body.id) {
      throw new Error(`Failed to register stub for ${description}: WireMock returned no mapping id`);
    }
    this.stubIds.push(body.id);

    await this.verifyRegistered(description, body.id);
  }

  /** Reads the new stub back, so a stub the server did not keep fails here, at registration. */
  private async verifyRegistered(description: string, id: string): Promise<void> {
    const readBack = await fetch(`${this.adminUrl}/${id}`);
    if (!readBack.ok) {
      throw new Error(
        `Failed to register stub for ${description}: stub ${id} is not visible on read-back (${readBack.status} ${readBack.statusText} ${await readBack.text()})`,
      );
    }
  }
}
