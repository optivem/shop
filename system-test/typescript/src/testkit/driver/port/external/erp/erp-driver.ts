import { Result } from '../../../../common/result.js';
import {
  ErrorResponse,
  GetProductResponse,
  ReturnsProductRequest,
  ReturnsPromotionRequest,
} from '../../../../common/dtos.js';

export interface ErpDriver {
  goToErp(): Promise<Result<void, ErrorResponse>>;
  getProduct(sku: string): Promise<Result<GetProductResponse, ErrorResponse>>;
  returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErrorResponse>>;
  returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}
