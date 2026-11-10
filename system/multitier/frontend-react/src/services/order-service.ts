// Service layer for Order API operations

import { fetchJson, fetchNoContent } from '../common';
import { isBrowseOrderHistoryResponse, isPlaceOrderResponse, isViewOrderDetailsResponse } from '../types/api.guards';
import type {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderDetailsResponse,
  BrowseOrderHistoryResponse,
  BrowseOrderHistoryQuery,
  ShippingAddress,
} from '../types/api.types';
import type { Result } from '../types/result.types';

export class OrderGateway {
  private readonly baseUrl: string;

  constructor(baseUrl = '/api/orders') {
    this.baseUrl = baseUrl;
  }

  async placeOrder(sku: string, quantity: number, country: string, couponCode?: string): Promise<Result<PlaceOrderResponse>> {
    return this.submitOrder({ sku, quantity, country, ...(couponCode ? { couponCode } : {}) });
  }

  /** Checkout: the tax country is the shipping address's country. */
  async placeOrderTo(
    sku: string,
    quantity: number,
    shippingAddress: ShippingAddress,
    couponCode?: string,
  ): Promise<Result<PlaceOrderResponse>> {
    return this.submitOrder({ sku, quantity, shippingAddress, ...(couponCode ? { couponCode } : {}) });
  }

  private async submitOrder(requestBody: PlaceOrderRequest): Promise<Result<PlaceOrderResponse>> {
    return fetchJson(this.baseUrl, isPlaceOrderResponse, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });
  }

  async getOrder(orderNumber: string): Promise<Result<ViewOrderDetailsResponse>> {
    return fetchJson(`${this.baseUrl}/${orderNumber}`, isViewOrderDetailsResponse, {
      method: 'GET'
    });
  }

  async cancelOrder(orderNumber: string): Promise<Result<void>> {
    return fetchNoContent(`${this.baseUrl}/${orderNumber}/cancel`, {
      method: 'POST'
    });
  }

  async deliverOrder(orderNumber: string): Promise<Result<void>> {
    return fetchNoContent(`${this.baseUrl}/${orderNumber}/deliver`, {
      method: 'POST'
    });
  }

  async browseOrderHistory(query: string | BrowseOrderHistoryQuery = {}): Promise<Result<BrowseOrderHistoryResponse>> {
    const { orderNumber, customer, page } = typeof query === 'string' ? { orderNumber: query } : query;
    const params = new URLSearchParams();
    if (orderNumber?.trim()) params.set('orderNumber', orderNumber.trim());
    if (customer) params.set('customer', customer);
    if (page !== undefined) params.set('page', String(page));
    const search = params.toString();
    return fetchJson(search ? `${this.baseUrl}?${search}` : this.baseUrl, isBrowseOrderHistoryResponse, {
      method: 'GET'
    });
  }

}

export const orderService = new OrderGateway();
