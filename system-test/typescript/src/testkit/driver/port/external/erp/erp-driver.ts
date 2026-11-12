import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from './dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from './dtos/GetProductRequest.js';
import type { GetProductResponse } from './dtos/GetProductResponse.js';
import type { GetProductsResponse } from './dtos/GetProductsResponse.js';
import type { ReturnsProductRequest } from './dtos/ReturnsProductRequest.js';
import type { ReturnsProductsRequest } from './dtos/ReturnsProductsRequest.js';
import type { ReturnsPromotionRequest } from './dtos/ReturnsPromotionRequest.js';

export interface ErpDriver {
  goToErp(): Promise<Result<void, ErpErrorResponse>>;
  getProduct(request: GetProductRequest): Promise<Result<GetProductResponse, ErpErrorResponse>>;
  getProducts(): Promise<Result<GetProductsResponse, ErpErrorResponse>>;
  returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>>;
  /** Stages the ERP's enumerable product list (GET /api/products). */
  returnsProducts(request: ReturnsProductsRequest): Promise<Result<void, ErpErrorResponse>>;
  returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>>;
  /** Makes the ERP unavailable: every ERP API call fails until the driver is closed. */
  returnsUnavailable(): Promise<Result<void, ErpErrorResponse>>;
  /** Makes the ERP slow: every ERP API call answers only after a delay well past the system's outbound timeout, until the driver is closed. */
  returnsSlow(): Promise<Result<void, ErpErrorResponse>>;
  /** Ends an outage started by returnsUnavailable(): the ERP answers with what is staged again. */
  returnsAvailable(): Promise<Result<void, ErpErrorResponse>>;
  /** The ERP no longer offers this product: asking for it answers "not found", whatever was staged before. */
  returnsProductNotFound(request: GetProductRequest): Promise<Result<void, ErpErrorResponse>>;
  close(): Promise<void>;
}
