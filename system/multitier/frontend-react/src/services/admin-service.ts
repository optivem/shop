// Service layer for the admin-only API operations under /api/admin

import { fetchJson } from '../common';
import { isBrowseCustomersResponse } from '../types/api.guards';
import type { BrowseCustomersResponse } from '../types/api.types';
import type { Result } from '../types/result.types';

export class AdminGateway {
  private readonly baseUrl: string;

  constructor(baseUrl = '/api/admin') {
    this.baseUrl = baseUrl;
  }

  /** The usernames that have placed an order, distinct, A to Z: the choices of the order history's customer filter. */
  async browseCustomers(): Promise<Result<BrowseCustomersResponse>> {
    return fetchJson(`${this.baseUrl}/customers`, isBrowseCustomersResponse, {
      method: 'GET'
    });
  }
}

export const adminService = new AdminGateway();
