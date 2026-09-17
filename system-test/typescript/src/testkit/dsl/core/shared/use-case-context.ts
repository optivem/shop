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

  // Overloads: a null/undefined/blank alias is returned unchanged, so the result is only
  // nullable when the alias is.
  getParamValue(alias: string): string;
  getParamValue(alias: string | null): string | null;
  getParamValue(alias: string | null | undefined): string | null | undefined;
  getParamValue(alias: string | null | undefined): string | null | undefined {
    if (alias === undefined || alias === null || this.isBlank(alias)) {
      return alias;
    }

    const existing = this.paramMap.get(alias);
    if (existing !== undefined) {
      return existing;
    }

    const value = this.generateParamValue(alias);
    this.paramMap.set(alias, value);

    return value;
  }

  getParamValueOrLiteral(alias: string): string;
  getParamValueOrLiteral(alias: string | null): string | null;
  getParamValueOrLiteral(alias: string | null | undefined): string | null | undefined;
  getParamValueOrLiteral(alias: string | null | undefined): string | null | undefined {
    if (alias === undefined || alias === null || this.isBlank(alias)) {
      return alias;
    }
    switch (this.externalSystemMode) {
      case 'stub':
        return this.getParamValue(alias);
      case 'real':
        return alias;
      default:
        throw new Error(`Unsupported external system mode: ${String(this.externalSystemMode)}`);
    }
  }

  setResultEntry(alias: string, value: string): void {
    this.resultMap.set(alias, value);
  }

  getResultValue(alias: string): string;
  getResultValue(alias: string | null): string | null;
  getResultValue(alias: string | null | undefined): string | null | undefined;
  getResultValue(alias: string | null | undefined): string | null | undefined {
    if (alias === undefined || alias === null || this.isBlank(alias)) {
      return alias;
    }
    const value = this.resultMap.get(alias);
    if (value === undefined) {
      return alias;
    }
    if (value.includes('FAILED')) {
      throw new Error(`Cannot get result value for alias '${alias}' because the operation failed: ${value}`);
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

  private isBlank(alias: string): boolean {
    return alias.trim().length === 0;
  }
}
