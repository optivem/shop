import { describe, it, expect, vi } from 'vitest';
import { match } from '../../types/result.types';

describe('match', () => {
  it('calls success handler when result is successful', () => {
    const result = { success: true as const, data: 42 };
    const onSuccess = vi.fn((data: number) => `ok:${data}`);
    const onError = vi.fn<() => string>();

    const output = match(result, { success: onSuccess, error: onError });

    expect(onSuccess).toHaveBeenCalledWith(42);
    expect(onError).not.toHaveBeenCalled();
    expect(output).toBe('ok:42');
  });

  it('calls error handler when result is a failure', () => {
    const error = { message: 'Not found', status: 404 };
    const result = { success: false as const, error };
    const onSuccess = vi.fn<() => string>();
    const onError = vi.fn(() => 'err');

    const output = match(result, { success: onSuccess, error: onError });

    expect(onError).toHaveBeenCalledWith(error);
    expect(onSuccess).not.toHaveBeenCalled();
    expect(output).toBe('err');
  });
});
