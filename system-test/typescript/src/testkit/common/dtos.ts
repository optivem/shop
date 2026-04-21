// Barrel re-export — DTOs are now split by domain under driver/port/
// This file preserves backward compatibility for existing imports.

// Shop
export type { PlaceOrderRequest } from '../driver/port/shop/dtos/PlaceOrderRequest.js';
export type { PlaceOrderResponse } from '../driver/port/shop/dtos/PlaceOrderResponse.js';
export type { ViewOrderResponse } from '../driver/port/shop/dtos/ViewOrderResponse.js';
export { OrderStatus } from '../driver/port/shop/dtos/OrderStatus.js';
export type { PublishCouponRequest } from '../driver/port/shop/dtos/PublishCouponRequest.js';
export type { BrowseCouponItem, BrowseCouponsResponse } from '../driver/port/shop/dtos/BrowseCouponsResponse.js';
export type { SystemError, FieldError } from '../driver/port/shop/dtos/errors/SystemError.js';
export type { ProblemDetailResponse } from '../driver/adapter/shop/api/client/dtos/errors/ProblemDetailResponse.js';

// Clock
export type { GetTimeResponse } from '../driver/port/external/clock/dtos/GetTimeResponse.js';
export type { ReturnsTimeRequest } from '../driver/port/external/clock/dtos/ReturnsTimeRequest.js';
export type { ClockErrorResponse } from '../driver/port/external/clock/dtos/errors/ClockErrorResponse.js';

// ERP
export type { GetProductResponse } from '../driver/port/external/erp/dtos/GetProductResponse.js';
export type { ReturnsProductRequest } from '../driver/port/external/erp/dtos/ReturnsProductRequest.js';
export type { ReturnsPromotionRequest } from '../driver/port/external/erp/dtos/ReturnsPromotionRequest.js';
export type { ErpErrorResponse } from '../driver/port/external/erp/dtos/errors/ErpErrorResponse.js';

// Tax
export type { GetTaxResponse } from '../driver/port/external/tax/dtos/GetTaxResponse.js';
export type { ReturnsTaxRateRequest } from '../driver/port/external/tax/dtos/ReturnsTaxRateRequest.js';
export type { TaxErrorResponse } from '../driver/port/external/tax/dtos/errors/TaxErrorResponse.js';
