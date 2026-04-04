// Service layer for Order API operations

import { fetchJson } from '../common';
import type { PlaceOrderRequest, PlaceOrderResponse, ViewOrderDetailsResponse, BrowseOrderHistoryResponse } from '../types/api.types';
import type { Result } from '../types/result.types';

class OrderService {
  private readonly baseUrl: string;

  constructor(baseUrl: string = '/api/orders') {
    this.baseUrl = baseUrl;
  }

  async placeOrder(sku: string, quantity: number, country: string, couponCode?: string): Promise<Result<PlaceOrderResponse>> {
    const requestBody: PlaceOrderRequest = { sku, quantity, ...(country.trim() ? { country } : {}), ...(couponCode ? { couponCode } : {}) };

    return fetchJson<PlaceOrderResponse>(this.baseUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });
  }

  async getOrder(orderNumber: string): Promise<Result<ViewOrderDetailsResponse>> {
    return fetchJson<ViewOrderDetailsResponse>(`${this.baseUrl}/${orderNumber}`, {
      method: 'GET'
    });
  }

  async browseOrderHistory(orderNumberFilter?: string): Promise<Result<BrowseOrderHistoryResponse>> {
    const url = orderNumberFilter?.trim()
      ? `${this.baseUrl}?orderNumber=${encodeURIComponent(orderNumberFilter.trim())}`
      : this.baseUrl;
    return fetchJson<BrowseOrderHistoryResponse>(url, {
      method: 'GET'
    });
  }

}

export const orderService = new OrderService();
