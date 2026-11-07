import type { Result } from '../../../../common/result.js';
import type { ErpErrorResponse } from './dtos/errors/ErpErrorResponse.js';
import type { GetProductRequest } from './dtos/GetProductRequest.js';
import type { GetProductResponse } from './dtos/GetProductResponse.js';
import type { ReturnsProductRequest } from './dtos/ReturnsProductRequest.js';
import type { ReturnsPromotionRequest } from './dtos/ReturnsPromotionRequest.js';

export interface ErpDriver {
  goToErp(): Promise<Result<void, ErpErrorResponse>>;
  getProduct(request: GetProductRequest): Promise<Result<GetProductResponse, ErpErrorResponse>>;
  returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>>;
  returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>>;
  /** Makes the ERP unavailable: every ERP API call fails until the driver is closed. */
  returnsUnavailable(): Promise<Result<void, ErpErrorResponse>>;
  close(): Promise<void>;
}
