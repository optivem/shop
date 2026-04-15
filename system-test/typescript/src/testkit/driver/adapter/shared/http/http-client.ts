import { Result, success, failure } from '../../../../common/result.js';

export class JsonHttpClient<E> {
  constructor(private baseUrl: string) {}

  async get<T>(path: string): Promise<Result<T, E>> {
    const response = await fetch(`${this.baseUrl}${path}`);
    return this.handleResponse<T>(response);
  }

  async post<T>(path: string, body?: unknown): Promise<Result<T, E>> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    return this.handleResponse<T>(response);
  }

  async getVoid(path: string): Promise<Result<void, E>> {
    const response = await fetch(`${this.baseUrl}${path}`);
    if (response.ok) return success(undefined);
    try {
      const error = (await response.json()) as E;
      return failure(error);
    } catch {
      return failure({ message: `HTTP ${response.status}` } as unknown as E);
    }
  }

  async postVoid(path: string, body?: unknown): Promise<Result<void, E>> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (response.ok) return success(undefined);
    try {
      const error = (await response.json()) as E;
      return failure(error);
    } catch {
      return failure({ message: `HTTP ${response.status}` } as unknown as E);
    }
  }

  private async handleResponse<T>(response: Response): Promise<Result<T, E>> {
    if (response.ok) {
      const data = (await response.json()) as T;
      return success(data);
    }
    try {
      const error = (await response.json()) as E;
      return failure(error);
    } catch {
      return failure({ message: `HTTP ${response.status}` } as unknown as E);
    }
  }
}
