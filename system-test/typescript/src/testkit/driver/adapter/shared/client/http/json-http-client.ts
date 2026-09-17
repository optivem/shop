import { type Result, success, failure } from '../../../../../common/result.js';

// Builds the client's error type from a failed response; body is undefined when it is not JSON.
export type ErrorMapper<E> = (status: number, body: unknown) => E;

export class JsonHttpClient<E> {
  constructor(
    private readonly baseUrl: string,
    private readonly toError: ErrorMapper<E>,
  ) {}

  async get<T>(path: string): Promise<Result<T, E>> {
    const response = await fetch(`${this.baseUrl}${path}`);
    return this.handleResponse<T>(response);
  }

  async post<T>(path: string, body?: unknown): Promise<Result<T, E>> {
    return this.handleResponse<T>(await this.doPost(path, body));
  }

  async getVoid(path: string): Promise<Result<void, E>> {
    const response = await fetch(`${this.baseUrl}${path}`);
    return response.ok ? success(undefined) : failure(await this.readError(response));
  }

  async postVoid(path: string, body?: unknown): Promise<Result<void, E>> {
    const response = await this.doPost(path, body);
    return response.ok ? success(undefined) : failure(await this.readError(response));
  }

  private doPost(path: string, body: unknown): Promise<Response> {
    return fetch(`${this.baseUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  }

  private async handleResponse<T>(response: Response): Promise<Result<T, E>> {
    if (response.ok) {
      const data = (await response.json()) as T;
      return success(data);
    }
    return failure(await this.readError(response));
  }

  private async readError(response: Response): Promise<E> {
    const body: unknown = await response.json().catch(() => undefined);
    return this.toError(response.status, body);
  }
}
