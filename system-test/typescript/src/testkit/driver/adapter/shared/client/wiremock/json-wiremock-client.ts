import { randomUUID } from 'node:crypto';

const OUTAGE_PRIORITY = 1;

// Admin calls to a Cloud Run stub service occasionally fail in Google's front end before they reach
// the container (a 502, or a request that reads out). Retry those a bounded number of times.
const MAX_ADMIN_ATTEMPTS = 4;
const INITIAL_BACKOFF_MS = 1000;
const ADMIN_REQUEST_TIMEOUT_MS = 30_000;
const TRANSIENT_STATUSES = new Set([502, 503, 504]);

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
   * system down, or with `fixedDelayMilliseconds` to make it slow, so the system's outbound timeout is
   * what ends the call.
   */
  async stubGetMatching(urlPathPattern: string, statusCode: number, fixedDelayMilliseconds = 0): Promise<void> {
    const id = await this.registerMapping(`GET ${urlPathPattern}`, {
      priority: OUTAGE_PRIORITY,
      request: { method: 'GET', urlPathPattern },
      response: fixedDelayMilliseconds > 0 ? { status: statusCode, fixedDelayMilliseconds } : { status: statusCode },
    });
    this.matchingStubIds.set(urlPathPattern, [...(this.matchingStubIds.get(urlPathPattern) ?? []), id]);
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

  /**
   * Registers the mapping under a client-chosen id, retrying transient admin failures. Before each retry
   * it checks whether the failed attempt reached the server after all, so a mapping is never added twice.
   */
  private async registerMapping(description: string, mapping: object): Promise<string> {
    const id = randomUUID();
    const mappingUrl = `${this.adminUrl}/${id}`;
    const body = JSON.stringify({ id, ...mapping });

    const response = await this.sendWithRetry(`register stub ${description} (id ${id})`, this.adminUrl, async (attempt) => {
      if (attempt > 1) {
        const check = await this.adminFetch(mappingUrl);
        if (check.ok || TRANSIENT_STATUSES.has(check.status)) {
          return check;
        }
        await check.body?.cancel();
      }
      return this.adminFetch(this.adminUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
    });

    if (!response.ok) {
      throw new Error(`Failed to register stub for ${description} at ${this.adminUrl}: ${response.status} ${response.statusText} ${await response.text()}`);
    }
    this.stubIds.push(id);

    await this.verifyRegistered(description, id);
    return id;
  }

  /** Reads the new stub back, so a stub the server did not keep fails here, at registration. */
  private async verifyRegistered(description: string, id: string): Promise<void> {
    const mappingUrl = `${this.adminUrl}/${id}`;
    const readBack = await this.sendWithRetry(`read back stub ${description} (id ${id})`, mappingUrl, () => this.adminFetch(mappingUrl));
    if (!readBack.ok) {
      throw new Error(
        `Failed to register stub for ${description}: stub ${id} is not visible on read-back at ${mappingUrl} (${readBack.status} ${readBack.statusText} ${await readBack.text()})`,
      );
    }
  }

  private adminFetch(url: string, init: RequestInit = {}): Promise<Response> {
    return fetch(url, { ...init, signal: AbortSignal.timeout(ADMIN_REQUEST_TIMEOUT_MS) });
  }

  /**
   * Sends an admin request, retrying with exponential backoff on a 502/503/504 or a transport failure
   * (connect/read timeout, reset connection). Returns the last response; throws once the retries run out
   * on a transport failure.
   */
  private async sendWithRetry(description: string, url: string, send: (attempt: number) => Promise<Response>): Promise<Response> {
    for (let attempt = 1; ; attempt++) {
      if (attempt > 1) {
        await new Promise((resolve) => setTimeout(resolve, INITIAL_BACKOFF_MS * 2 ** (attempt - 2)));
      }

      let failure: string;
      try {
        const response = await send(attempt);
        if (!TRANSIENT_STATUSES.has(response.status) || attempt === MAX_ADMIN_ATTEMPTS) {
          return response;
        }
        failure = `${response.status} ${response.statusText}`;
        await response.body?.cancel();
      } catch (error) {
        // fetch rejects with a TypeError on a network failure and a TimeoutError when the signal fires.
        if (attempt === MAX_ADMIN_ATTEMPTS) {
          throw new Error(`WireMock admin call failed after ${MAX_ADMIN_ATTEMPTS} attempts: ${description} at ${url}: ${String(error)}`, { cause: error });
        }
        failure = String(error);
      }

      console.warn(`WireMock admin call failed (attempt ${attempt}/${MAX_ADMIN_ATTEMPTS}): ${description} at ${url}: ${failure}`);
    }
  }
}
