import { ValidationException } from '../exceptions/validation.exception';

/**
 * Which page of a list to read. Pages are numbered from one; a missing page is the first and a
 * missing size is DEFAULT_SIZE. The cap exists because how many rows one request may cost is not the
 * caller's decision.
 */
export class PageSpec {
  static readonly FIRST_PAGE = 1;
  static readonly DEFAULT_SIZE = 50;
  static readonly MAX_SIZE = 100;

  private constructor(
    readonly page: number,
    readonly size: number,
  ) {}

  /** Validates the raw query values; out-of-range or non-integer values are a field validation error. */
  static of(page?: string, size?: string): PageSpec {
    const requestedPage = PageSpec.parse(page);
    if (
      requestedPage !== null &&
      (Number.isNaN(requestedPage) || requestedPage < PageSpec.FIRST_PAGE)
    ) {
      throw new ValidationException(
        'page',
        `Page must be ${PageSpec.FIRST_PAGE} or greater`,
      );
    }
    const requestedSize = PageSpec.parse(size);
    if (
      requestedSize !== null &&
      (Number.isNaN(requestedSize) ||
        requestedSize < 1 ||
        requestedSize > PageSpec.MAX_SIZE)
    ) {
      throw new ValidationException(
        'size',
        `Page size must be between 1 and ${PageSpec.MAX_SIZE}`,
      );
    }
    return new PageSpec(
      requestedPage ?? PageSpec.FIRST_PAGE,
      requestedSize ?? PageSpec.DEFAULT_SIZE,
    );
  }

  /** How many rows to skip. A page number is a stranger's number, so it can be huge: compare before using it. */
  offset(): number {
    return (this.page - PageSpec.FIRST_PAGE) * this.size;
  }

  /** True when the page starts at or past the last row, so the row query can be skipped. */
  isPastEnd(totalElements: number): boolean {
    return this.offset() >= totalElements;
  }

  totalPages(totalElements: number): number {
    return Math.ceil(totalElements / this.size);
  }

  private static parse(value?: string): number | null {
    if (value === undefined || value === null || value.trim() === '') {
      return null;
    }
    return /^-?\d+$/.test(value.trim()) ? Number(value.trim()) : Number.NaN;
  }
}
