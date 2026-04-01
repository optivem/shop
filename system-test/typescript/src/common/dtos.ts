export interface PlaceOrderRequest {
  sku: string;
  quantity: string | null;
}

export interface PlaceOrderResponse {
  orderNumber: string;
}

export interface ViewOrderResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
}

export enum OrderStatus {
  PLACED = 'PLACED',
}

export interface ErrorResponse {
  message: string;
  fieldErrors: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ProblemDetailResponse {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  fieldErrors?: { field: string; message: string; code?: string; rejectedValue?: string }[];
}

export interface GetProductResponse {
  sku: string;
  price: number;
}

export interface ReturnsProductRequest {
  sku: string;
  price: string;
}

export interface GetTimeResponse {
  time: string;
}

export interface ReturnsTimeRequest {
  time: string;
}
