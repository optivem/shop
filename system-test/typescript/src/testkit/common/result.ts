export type Result<T, E> =
  | { success: true; value: T }
  | { success: false; error: E };

export function success<T, E = never>(value: T): Result<T, E> {
  return { success: true, value };
}

export function failure<T = never, E = unknown>(error: E): Result<T, E> {
  return { success: false, error };
}
