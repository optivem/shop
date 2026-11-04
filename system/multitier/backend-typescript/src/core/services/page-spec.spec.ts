import { PageSpec } from './page-spec';
import { ValidationException } from '../exceptions/validation.exception';

describe('PageSpec', () => {
  it('defaults to the first page of 50', () => {
    const spec = PageSpec.of(undefined, '');

    expect(spec.page).toBe(1);
    expect(spec.size).toBe(50);
    expect(spec.offset()).toBe(0);
  });

  it('computes the offset and the page count', () => {
    const spec = PageSpec.of('3', '10');

    expect(spec.offset()).toBe(20);
    expect(spec.totalPages(21)).toBe(3);
    expect(spec.totalPages(0)).toBe(0);
    expect(spec.isPastEnd(20)).toBe(true);
    expect(spec.isPastEnd(21)).toBe(false);
  });

  it('accepts the cap and rejects one above it', () => {
    expect(PageSpec.of('1', '100').size).toBe(100);
    expect(() => PageSpec.of('1', '101')).toThrow(ValidationException);
  });

  it('treats a huge page as past the end instead of overflowing', () => {
    expect(PageSpec.of('2000000000', '100').isPastEnd(10)).toBe(true);
  });
});
