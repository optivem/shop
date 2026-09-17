import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ErpProductDetailsResponse } from '../../dtos/external/erp-product-details-response.dto';
import { ErpGetPromotionResponse } from '../../dtos/external/erp-get-promotion-response.dto';
import { errorMessage, fetchJson, isNotFound } from './fetch-json';

@Injectable()
export class ErpGateway {
  private readonly erpUrl: string;

  constructor(private readonly configService: ConfigService) {
    this.erpUrl = this.configService.get<string>(
      'ERP_API_URL',
      'http://localhost:9001/erp',
    );
  }

  async getProductDetails(
    sku: string,
  ): Promise<ErpProductDetailsResponse | null> {
    const url = `${this.erpUrl}/api/products/${encodeURIComponent(sku)}`;
    try {
      return await fetchJson(url, ErpProductDetailsResponse);
    } catch (e) {
      if (isNotFound(e)) {
        return null;
      }
      throw new Error(
        `Failed to fetch product details for SKU: ${sku}. ${errorMessage(e)}`,
        { cause: e },
      );
    }
  }

  async getPromotionDetails(): Promise<ErpGetPromotionResponse> {
    const url = `${this.erpUrl}/api/promotion`;
    try {
      return await fetchJson(url, ErpGetPromotionResponse);
    } catch (e) {
      throw new Error(`Failed to fetch promotion details. ${errorMessage(e)}`, {
        cause: e,
      });
    }
  }
}
