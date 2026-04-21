import { randomUUID } from 'node:crypto';

export type ExternalSystemMode = 'real' | 'stub';

export class UseCaseContext {
  private readonly externalSystemMode: ExternalSystemMode;
  private readonly paramMap: Map<string, string>;
  private readonly resultMap: Map<string, string>;

  constructor(externalSystemMode: ExternalSystemMode) {
    this.externalSystemMode = externalSystemMode;
    this.paramMap = new Map<string, string>();
    this.resultMap = new Map<string, string>();
  }

  getExternalSystemMode(): ExternalSystemMode {
    return this.externalSystemMode;
  }

  getParamValue(alias: string | null | undefined): string | null | undefined {
    if (this.isNullOrBlank(alias)) {
      return alias;
    }

    const key = alias as string;
    if (this.paramMap.has(key)) {
      return this.paramMap.get(key)!;
    }

    const value = this.generateParamValue(key);
    this.paramMap.set(key, value);

    return value;
  }

  getParamValueOrLiteral(alias: string | null | undefined): string | null | undefined {
    if (this.isNullOrBlank(alias)) {
      return alias;
    }
    switch (this.externalSystemMode) {
      case 'stub':
        return this.getParamValue(alias);
      case 'real':
        return alias;
      default:
        throw new Error(`Unsupported external system mode: ${this.externalSystemMode}`);
    }
  }

  setResultEntry(alias: string, value: string): void {
    this.resultMap.set(alias, value);
  }

  getResultValue(alias: string | null | undefined): string | null | undefined {
    if (this.isNullOrBlank(alias)) {
      return alias;
    }
    const key = alias as string;
    const value = this.resultMap.get(key);
    if (value === undefined) {
      return alias;
    }
    if (value.includes('FAILED')) {
      throw new Error(`Cannot get result value for alias '${key}' because the operation failed: ${value}`);
    }
    return value;
  }

  expandAliases(message: string): string {
    let expanded = message;
    for (const [alias, actual] of this.paramMap.entries()) {
      expanded = expanded.split(alias).join(actual);
    }
    for (const [alias, actual] of this.resultMap.entries()) {
      expanded = expanded.split(alias).join(actual);
    }
    return expanded;
  }

  private generateParamValue(alias: string): string {
    const suffix = randomUUID().substring(0, 8);
    return `${alias}-${suffix}`;
  }

  private isNullOrBlank(alias: string | null | undefined): boolean {
    return alias === undefined || alias === null || alias.trim().length === 0;
  }
}
