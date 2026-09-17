import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TaxDetailsResponse } from '../../dtos/external/tax-details-response.dto';
import { errorMessage, fetchJson, isNotFound } from './fetch-json';

@Injectable()
export class TaxGateway {
  private readonly taxUrl: string;

  constructor(private readonly configService: ConfigService) {
    this.taxUrl = this.configService.get<string>(
      'TAX_API_URL',
      'http://localhost:9001/tax',
    );
  }

  async getTaxDetails(country: string): Promise<TaxDetailsResponse | null> {
    const url = `${this.taxUrl}/api/countries/${encodeURIComponent(country)}`;
    try {
      return await fetchJson(url, TaxDetailsResponse);
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
