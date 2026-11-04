const DECIMAL_PATTERN = /^([+-]?)(\d*)(?:\.(\d*))?$/;

function splitDecimal(value: string): { negative: boolean; intPart: string; fracPart: string } {
  const match = DECIMAL_PATTERN.exec(value.trim());
  if (!match || (match[2] === '' && (match[3] ?? '') === '')) {
    throw new TypeError(`Cannot convert "${value}" to decimal`);
  }
  return { negative: match[1] === '-', intPart: match[2] ?? '', fracPart: match[3] ?? '' };
}

export class Converter {
  /**
   * Money and rates travel as decimal strings ("20.00", "0.2000"). TypeScript has no exact decimal
   * type, so they are compared as normalized strings: leading zeros of the integer part and trailing
   * zeros of the fraction are dropped, so "20.00" and "20" (or a number 20) compare equal by value,
   * without ever going through a binary float.
   */
  static normalizeDecimal(value: string | number): string {
    const text = typeof value === 'number' ? Converter.numberToPlainString(value) : value;
    const { negative, intPart, fracPart } = splitDecimal(text);
    const intDigits = intPart.replace(/^0+/, '') || '0';
    const fracDigits = fracPart.replace(/0+$/, '');
    const magnitude = fracDigits ? `${intDigits}.${fracDigits}` : intDigits;
    return negative && magnitude !== '0' ? `-${magnitude}` : magnitude;
  }

  /** Like {@link normalizeDecimal}, but passes a missing value through, so an assertion reports it. */
  static normalizeDecimalOrNull(value: string | number | null | undefined): string | null {
    return value === null || value === undefined ? null : Converter.normalizeDecimal(value);
  }

  static decimalEquals(actual: string | number, expected: string | number): boolean {
    return Converter.normalizeDecimal(actual) === Converter.normalizeDecimal(expected);
  }

  /** "$1,020.50" -> "1020.50": the displayed amount, unchanged except for currency/grouping symbols. */
  static fromDisplayedAmount(displayed: string): string {
    return displayed.replaceAll(/[$,\s]/g, '');
  }

  /**
   * "20.00%" -> "0.2000": divides by 100 by shifting the decimal point two places left on the digits,
   * not by float division, so no binary rounding enters the value.
   */
  static fromDisplayedPercent(displayed: string): string {
    const { negative, intPart, fracPart } = splitDecimal(displayed.replaceAll(/[%\s]/g, ''));
    const padded = intPart.padStart(3, '0');
    const shiftedInt = padded.slice(0, -2).replace(/^0+/, '') || '0';
    const shiftedFrac = padded.slice(-2) + fracPart;
    return `${negative ? '-' : ''}${shiftedInt}.${shiftedFrac}`;
  }

  private static numberToPlainString(value: number): string {
    if (!Number.isFinite(value)) {
      throw new TypeError(`Cannot convert ${value} to decimal`);
    }
    const text = String(value);
    return /e/i.test(text) ? value.toFixed(20) : text;
  }

  static toNumber(value: string): number {
    const n = Number(value);
    if (Number.isNaN(n)) {
      throw new TypeError(`Cannot convert "${value}" to number`);
    }
    return n;
  }

  static toInteger(value: string): number {
    const n = Number(value);
    if (!Number.isInteger(n)) {
      throw new TypeError(`Cannot convert "${value}" to integer`);
    }
    return n;
  }

  static toDate(value: string): Date {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) {
      throw new TypeError(`Cannot convert "${value}" to Date`);
    }
    return d;
  }

  static fromInteger(value: number): string {
    if (!Number.isInteger(value)) {
      throw new TypeError(`Value ${value} is not an integer`);
    }
    return String(value);
  }

  static fromDate(value: Date): string {
    return value.toISOString();
  }
}
