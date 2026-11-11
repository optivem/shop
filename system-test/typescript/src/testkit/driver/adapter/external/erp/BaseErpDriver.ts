import { type Result, success, failure } from '../../../../common/result.js';
import { nonEmptyOr } from '../../../../common/fallback.js';
import type { ErpErrorResponse } from '../../../port/external/erp/dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from '../../../port/external/erp/dtos/GetProductRequest.js';
import type { GetProductResponse } from '../../../port/external/erp/dtos/GetProductResponse.js';
import type { GetProductsResponse } from '../../../port/external/erp/dtos/GetProductsResponse.js';
import type { ReturnsProductRequest } from '../../../port/external/erp/dtos/ReturnsProductRequest.js';
import type { ReturnsProductsRequest } from '../../../port/external/erp/dtos/ReturnsProductsRequest.js';
import type { ReturnsPromotionRequest } from '../../../port/external/erp/dtos/ReturnsPromotionRequest.js';
import type { ErpDriver } from '../../../port/external/erp/erp-driver.js';
import { ErpProductDefaults } from './erp-product-defaults.js';
import type { BaseErpClient } from './client/BaseErpClient.js';
import type { ExtProductDetailsResponse } from './client/dtos/ExtProductDetailsResponse.js';

export abstract class BaseErpDriver<TClient extends BaseErpClient> implements ErpDriver {
  /** Product details used when a scenario does not set them; shared so the stub and real ERP stay in parity. */
  protected static readonly DEFAULT_TITLE = ErpProductDefaults.TITLE;
  protected static readonly DEFAULT_DESCRIPTION = ErpProductDefaults.DESCRIPTION;
  protected static readonly DEFAULT_CATEGORY = ErpProductDefaults.CATEGORY;
  protected static readonly DEFAULT_BRAND = ErpProductDefaults.BRAND;

  protected constructor(protected readonly client: TClient) {}

  async goToErp(): Promise<Result<void, ErpErrorResponse>> {
    return this.client.checkHealth();
  }

  async getProduct(request: GetProductRequest): Promise<Result<GetProductResponse, ErpErrorResponse>> {
    const result = await this.client.getProduct(request.sku);
    if (!result.success) return failure(result.error);
    return success(BaseErpDriver.toGetProductResponse(result.value, request.sku));
  }

  async getProducts(): Promise<Result<GetProductsResponse, ErpErrorResponse>> {
    const result = await this.client.getProducts();
    if (!result.success) return failure(result.error);
    return success({ products: result.value.map((product) => BaseErpDriver.toGetProductResponse(product, '')) });
  }

  protected static toExtProductDetails(request: ReturnsProductRequest): ExtProductDetailsResponse {
    return {
      id: request.sku,
      title: request.title ?? BaseErpDriver.DEFAULT_TITLE,
      description: request.description ?? BaseErpDriver.DEFAULT_DESCRIPTION,
      price: Number.parseFloat(request.price),
      category: request.category ?? BaseErpDriver.DEFAULT_CATEGORY,
      brand: request.brand ?? BaseErpDriver.DEFAULT_BRAND,
    };
  }

  private static toGetProductResponse(product: ExtProductDetailsResponse, requestedSku: string): GetProductResponse {
    return {
      sku: nonEmptyOr(product.id, nonEmptyOr(product.sku, requestedSku)),
      title: product.title ?? '',
      description: product.description ?? '',
      category: product.category ?? '',
      brand: product.brand ?? '',
      price: Number.parseFloat(String(product.price)),
    };
  }

  abstract returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>>;
  abstract returnsProducts(request: ReturnsProductsRequest): Promise<Result<void, ErpErrorResponse>>;
  abstract returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>>;
  abstract returnsUnavailable(): Promise<Result<void, ErpErrorResponse>>;
  abstract returnsAvailable(): Promise<Result<void, ErpErrorResponse>>;
  abstract returnsProductNotFound(request: GetProductRequest): Promise<Result<void, ErpErrorResponse>>;
  abstract close(): Promise<void>;
}
