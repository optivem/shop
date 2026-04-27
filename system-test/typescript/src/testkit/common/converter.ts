export class Converter {
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
