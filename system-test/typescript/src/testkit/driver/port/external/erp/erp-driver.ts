import { Result } from '../../../../common/result.js';
import { ErpErrorResponse } from './dtos/errors/ErpErrorResponse.js';
import { GetProductRequest } from './dtos/GetProductRequest.js';
import { GetProductResponse } from './dtos/GetProductResponse.js';
import { ReturnsProductRequest } from './dtos/ReturnsProductRequest.js';
import { ReturnsPromotionRequest } from './dtos/ReturnsPromotionRequest.js';

export interface ErpDriver {
  goToErp(): Promise<Result<void, ErpErrorResponse>>;
  getProduct(request: GetProductRequest): Promise<Result<GetProductResponse, ErpErrorResponse>>;
  returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErpErrorResponse>>;
  returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErpErrorResponse>>;
  close(): Promise<void>;
}
