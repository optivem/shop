// Service layer for Order API operations

import { fetchJson, fetchNoContent } from '../common';
import { isBrowseOrderHistoryResponse, isPlaceOrderResponse, isViewOrderDetailsResponse } from '../types/api.guards';
import type { PlaceOrderRequest, PlaceOrderResponse, ViewOrderDetailsResponse, BrowseOrderHistoryResponse } from '../types/api.types';
import type { Result } from '../types/result.types';

export class OrderGateway {
  private readonly baseUrl: string;

  constructor(baseUrl = '/api/orders') {
    this.baseUrl = baseUrl;
  }

  async placeOrder(sku: string, quantity: number, country: string, couponCode?: string): Promise<Result<PlaceOrderResponse>> {
    const requestBody: PlaceOrderRequest = { sku, quantity, country, ...(couponCode ? { couponCode } : {}) };

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

  async browseOrderHistory(orderNumberFilter?: string): Promise<Result<BrowseOrderHistoryResponse>> {
    const url = orderNumberFilter?.trim()
      ? `${this.baseUrl}?orderNumber=${encodeURIComponent(orderNumberFilter.trim())}`
      : this.baseUrl;
    return fetchJson(url, isBrowseOrderHistoryResponse, {
      method: 'GET'
    });
  }

}

export const orderService = new OrderGateway();
