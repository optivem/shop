import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ErpProductDetailsResponse } from '../../dtos/external/erp-product-details-response.dto';
import { ErpGetPromotionResponse } from '../../dtos/external/erp-get-promotion-response.dto';

@Injectable()
export class ErpGateway {
  private readonly erpUrl: string;

  constructor(private readonly configService: ConfigService) {
    this.erpUrl = this.configService.get<string>(
      'ERP_API_URL',
      'http://localhost:9001/erp',
    );
  }

  async getProductDetails(sku: string): Promise<ErpProductDetailsResponse | null> {
    const url = `${this.erpUrl}/api/products/${sku}`;

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
          `ERP API returned status ${response.status} for SKU: ${sku}. URL: ${url}. Response: ${body}`,
        );
      }

      return (await response.json()) as ErpProductDetailsResponse;
    } catch (e) {
      if (e instanceof Error && e.message.startsWith('ERP API returned')) {
        throw e;
      }
      const err = e as Error;
      throw new Error(
        `Failed to fetch product details for SKU: ${sku} from URL: ${url}. Error: ${err.constructor.name}: ${err.message}`,
      );
    }
  }

  async getPromotionDetails(): Promise<ErpGetPromotionResponse> {
    const url = `${this.erpUrl}/api/promotion`;

    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(10000),
      });

      if (!response.ok) {
        const body = await response.text();
        throw new Error(
          `ERP API returned status ${response.status} for promotion. URL: ${url}. Response: ${body}`,
        );
      }

      return (await response.json()) as ErpGetPromotionResponse;
    } catch (e) {
      if (e instanceof Error && e.message.startsWith('ERP API returned')) {
        throw e;
      }
      const err = e as Error;
      throw new Error(
        `Failed to fetch promotion details from URL: ${url}. Error: ${err.constructor.name}: ${err.message}`,
      );
    }
  }
}
