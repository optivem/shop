import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TaxDetailsResponse } from '../../dtos/external/tax-details-response.dto';

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
    const url = `${this.taxUrl}/api/countries/${country}`;

    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(10000),
      });

      if (response.status === 404) {
        return null;
      }

      if (!response.ok) {
        const body = await response.text();
        throw new Error(
          `Tax API returned status ${response.status} for country: ${country}. URL: ${url}. Response: ${body}`,
        );
      }

      return (await response.json()) as TaxDetailsResponse;
    } catch (e) {
      if (e instanceof Error && e.message.startsWith('Tax API returned')) {
        throw e;
      }
      const err = e as Error;
      throw new Error(
        `Failed to fetch tax details for country: ${country} from URL: ${url}. Error: ${err.constructor.name}: ${err.message}`,
      );
    }
  }
}
