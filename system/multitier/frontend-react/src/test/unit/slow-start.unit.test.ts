import { describe, expect, it } from 'vitest';
import { slowStartDelayMs } from '../../contexts/slow-start';

describe('slowStart dev switch', () => {
  it('delays the first catalog load by the given seconds in dev', () => {
    expect(slowStartDelayMs('?slowStart=5', true)).toBe(5000);
  });

  it('is off without the parameter or with a bad value', () => {
    expect(slowStartDelayMs('', true)).toBe(0);
    expect(slowStartDelayMs('?slowStart=abc', true)).toBe(0);
    expect(slowStartDelayMs('?slowStart=-3', true)).toBe(0);
  });

  it('is ignored in production builds', () => {
    expect(slowStartDelayMs('?slowStart=5', false)).toBe(0);
  });
});
