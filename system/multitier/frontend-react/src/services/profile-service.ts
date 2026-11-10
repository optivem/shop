// Service layer for the signed-in customer's profile (UC-010).

import { fetchJson } from '../common';
import { isProfileResponse } from '../types/api.guards';
import type { ProfileResponse, UpdateProfileRequest } from '../types/api.types';
import type { Result } from '../types/result.types';

export class ProfileGateway {
  private readonly baseUrl: string;

  constructor(baseUrl = '/api/account/profile') {
    this.baseUrl = baseUrl;
  }

  async viewProfile(): Promise<Result<ProfileResponse>> {
    return fetchJson(this.baseUrl, isProfileResponse, { method: 'GET' });
  }

  async updateProfile(request: UpdateProfileRequest): Promise<Result<ProfileResponse>> {
    return fetchJson(this.baseUrl, isProfileResponse, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
  }
}

export const profileService = new ProfileGateway();
