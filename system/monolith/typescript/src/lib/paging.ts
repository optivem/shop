import type { FieldError } from './errors';

// Which page of a list to read. Pages are numbered from one; a missing page is the first and a
// missing size is DEFAULT_SIZE. The cap exists because how many rows one request may cost is not the
// caller's decision.
export const FIRST_PAGE = 1;
export const DEFAULT_PAGE_SIZE = 50;
export const MAX_PAGE_SIZE = 100;

export interface PageSpec {
  page: number;
  size: number;
  /** Rows to skip. A page number is a stranger's number, so it can be huge: check isPastEnd first. */
  offset: number;
}

export type PageResolution = { ok: true; value: PageSpec } | { ok: false; errors: FieldError[] };

function parse(value: string | null | undefined): number | null {
  if (value === null || value === undefined || value.trim() === '') {
    return null;
  }
  return /^-?\d+$/.test(value.trim()) ? Number(value.trim()) : Number.NaN;
}

/** Validates the raw query values; out-of-range or non-integer values are a field validation error. */
export function resolvePage(rawPage: string | null | undefined, rawSize: string | null | undefined): PageResolution {
  const page = parse(rawPage);
  if (page !== null && (Number.isNaN(page) || page < FIRST_PAGE)) {
    return { ok: false, errors: [{ field: 'page', message: `Page must be ${FIRST_PAGE} or greater` }] };
  }
  const size = parse(rawSize);
  if (size !== null && (Number.isNaN(size) || size < 1 || size > MAX_PAGE_SIZE)) {
    return { ok: false, errors: [{ field: 'size', message: `Page size must be between 1 and ${MAX_PAGE_SIZE}` }] };
  }
  const effectivePage = page ?? FIRST_PAGE;
  const effectiveSize = size ?? DEFAULT_PAGE_SIZE;
  return {
    ok: true,
    value: { page: effectivePage, size: effectiveSize, offset: (effectivePage - FIRST_PAGE) * effectiveSize },
  };
}

/** True when the page starts at or past the last row, so the row query can be skipped. */
export function isPastEnd(spec: PageSpec, totalElements: number): boolean {
  return spec.offset >= totalElements;
}

export function totalPages(spec: PageSpec, totalElements: number): number {
  return Math.ceil(totalElements / spec.size);
}

/** The paging half of a list response; the numbers are the ones that were served. */
export function pageEnvelope(spec: PageSpec, totalElements: number) {
  return { page: spec.page, size: spec.size, totalElements, totalPages: totalPages(spec, totalElements) };
}
