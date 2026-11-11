import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TaxDetailsResponse } from '../../dtos/external/tax-details-response.dto';
import { errorMessage, fetchJson, isNotFound } from './fetch-json';
import {
  type IdempotentCallPolicy,
  idempotentCallPolicy,
  resilienceOptionsFrom,
} from './resilience';

@Injectable()
export class TaxGateway {
  private readonly taxUrl: string;
  private readonly policy: IdempotentCallPolicy;

  constructor(private readonly configService: ConfigService) {
    this.taxUrl = this.configService.get<string>(
      'TAX_API_URL',
      'http://localhost:9001/tax',
    );
    this.policy = idempotentCallPolicy(
      resilienceOptionsFrom(this.configService),
    );
  }

  async getTaxDetails(country: string): Promise<TaxDetailsResponse | null> {
    const url = `${this.taxUrl}/api/countries/${encodeURIComponent(country)}`;
    try {
      return await this.policy((signal) =>
        fetchJson(url, TaxDetailsResponse, signal),
      );
    } catch (e) {
      if (isNotFound(e)) {
        return null;
      }
      throw new Error(
        `Failed to fetch tax details for country: ${country}. ${errorMessage(e)}`,
        { cause: e },
      );
    }
  }
}
