import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ErpProductDetailsResponse } from '../../dtos/external/erp-product-details-response.dto';
import { ErpGetPromotionResponse } from '../../dtos/external/erp-get-promotion-response.dto';
import {
  errorMessage,
  fetchJson,
  fetchJsonArray,
  isNotFound,
} from './fetch-json';
import { ErpUnavailableException } from '../../exceptions/erp-unavailable.exception';
import {
  type IdempotentCallPolicy,
  idempotentCallPolicy,
  resilienceOptionsFrom,
} from './resilience';

@Injectable()
export class ErpGateway {
  private readonly erpUrl: string;
  // One breaker for the whole ERP: every ERP call feeds and honours it.
  private readonly policy: IdempotentCallPolicy;

  constructor(private readonly configService: ConfigService) {
    this.erpUrl = this.configService.get<string>(
      'ERP_API_URL',
      'http://localhost:9001/erp',
    );
    this.policy = idempotentCallPolicy(
      resilienceOptionsFrom(this.configService),
    );
  }

  async getProductDetails(
    sku: string,
  ): Promise<ErpProductDetailsResponse | null> {
    const url = `${this.erpUrl}/api/products/${encodeURIComponent(sku)}`;
    try {
      return await this.policy((signal) =>
        fetchJson(url, ErpProductDetailsResponse, signal),
      );
    } catch (e) {
      if (isNotFound(e)) {
        return null;
      }
      throw new ErpUnavailableException(
        `Failed to fetch product details for SKU: ${sku}. ${errorMessage(e)}`,
        { cause: e },
      );
    }
  }

  async getProducts(): Promise<ErpProductDetailsResponse[]> {
    const url = `${this.erpUrl}/api/products`;
    try {
      return await this.policy((signal) =>
        fetchJsonArray(url, ErpProductDetailsResponse, signal),
      );
    } catch (e) {
      throw new ErpUnavailableException(
        `Failed to fetch products. ${errorMessage(e)}`,
        { cause: e },
      );
    }
  }

  async getPromotionDetails(): Promise<ErpGetPromotionResponse> {
    const url = `${this.erpUrl}/api/promotion`;
    try {
      return await this.policy((signal) =>
        fetchJson(url, ErpGetPromotionResponse, signal),
      );
    } catch (e) {
      throw new ErpUnavailableException(
        `Failed to fetch promotion details. ${errorMessage(e)}`,
        {
          cause: e,
        },
      );
    }
  }
}
