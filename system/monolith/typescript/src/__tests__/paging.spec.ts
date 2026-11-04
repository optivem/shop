import { isPastEnd, pageEnvelope, resolvePage } from '../lib/paging';

describe('resolvePage', () => {
  it('defaults to the first page of 50', () => {
    expect(resolvePage(null, '')).toEqual({ ok: true, value: { page: 1, size: 50, offset: 0 } });
  });

  it('computes the offset and the envelope', () => {
    const resolution = resolvePage('3', '10');
    if (!resolution.ok) throw new Error('expected a valid page');

    expect(resolution.value.offset).toBe(20);
    expect(pageEnvelope(resolution.value, 21)).toEqual({ page: 3, size: 10, totalElements: 21, totalPages: 3 });
    expect(pageEnvelope(resolution.value, 0).totalPages).toBe(0);
    expect(isPastEnd(resolution.value, 20)).toBe(true);
    expect(isPastEnd(resolution.value, 21)).toBe(false);
  });

  it('accepts the cap', () => {
    expect(resolvePage('1', '100').ok).toBe(true);
  });

  it.each([
    ['0', null, 'page', 'Page must be 1 or greater'],
    ['abc', null, 'page', 'Page must be 1 or greater'],
    [null, '0', 'size', 'Page size must be between 1 and 100'],
    [null, '101', 'size', 'Page size must be between 1 and 100'],
  ])('rejects page %p / size %p on field %s', (page, size, field, message) => {
    expect(resolvePage(page, size)).toEqual({ ok: false, errors: [{ field, message }] });
  });

  it('treats a huge page as past the end', () => {
    const resolution = resolvePage('2000000000', '100');
    if (!resolution.ok) throw new Error('expected a valid page');
    expect(isPastEnd(resolution.value, 10)).toBe(true);
  });
});
