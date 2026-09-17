import { type ClassConstructor, plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';

export class HttpStatusError extends Error {
  constructor(
    readonly url: string,
    readonly status: number,
    readonly body: string,
  ) {
    super(`HTTP ${status} from ${url}. Response: ${body}`);
    this.name = 'HttpStatusError';
  }
}

export class MalformedResponseError extends Error {
  constructor(url: string, detail: string) {
    super(`Malformed response from ${url}: ${detail}`);
    this.name = 'MalformedResponseError';
  }
}

// GETs JSON and validates it against the response class's class-validator decorators, so a malformed
// response fails here, naming the URL, instead of as a TypeError further down.
export async function fetchJson<T extends object>(
  url: string,
  responseType: ClassConstructor<T>,
): Promise<T> {
  const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    throw new HttpStatusError(url, response.status, await response.text());
  }

  const body: unknown = await response.json();
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    throw new MalformedResponseError(url, 'expected a JSON object');
  }
  const instance = plainToInstance(responseType, body);
  const errors = await validate(instance);
  if (errors.length > 0) {
    const detail = errors
      .flatMap((error) => Object.values(error.constraints ?? {}))
      .join('; ');
    throw new MalformedResponseError(url, detail);
  }
  return instance;
}

export function isNotFound(error: unknown): boolean {
  return error instanceof HttpStatusError && error.status === 404;
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
