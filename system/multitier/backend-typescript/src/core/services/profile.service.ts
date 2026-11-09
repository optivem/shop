import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CustomerProfile } from '../entities/customer-profile.entity';
import { UpdateProfileRequest } from '../dtos/update-profile-request.dto';
import { ViewProfileResponse } from '../dtos/view-profile-response.dto';
import type { CurrentUser } from '../../auth/current-user';

/** UC-010 Manage Profile: one profile per customer, keyed by the token subject. */
@Injectable()
export class ProfileService {
  constructor(
    @InjectRepository(CustomerProfile)
    private readonly profileRepository: Repository<CustomerProfile>,
  ) {}

  /** The saved profile; without one, the name comes from the token and the address is empty. */
  async viewProfile(user: CurrentUser): Promise<ViewProfileResponse> {
    const profile = await this.profileRepository.findOne({
      where: { owner: user.subject },
    });
    if (profile) {
      return ProfileService.toResponse(profile, user);
    }
    const response = new ViewProfileResponse();
    response.firstName = user.firstName;
    response.lastName = user.lastName;
    response.email = user.email;
    response.street = null;
    response.postalCode = null;
    response.city = null;
    response.country = null;
    return response;
  }

  /** Inserts or replaces the caller's profile (the request is already validated and trimmed). */
  async updateProfile(
    request: UpdateProfileRequest,
    user: CurrentUser,
  ): Promise<ViewProfileResponse> {
    const profile = new CustomerProfile();
    profile.owner = user.subject;
    profile.firstName = request.firstName;
    profile.lastName = request.lastName;
    profile.street = request.street;
    profile.postalCode = request.postalCode;
    profile.city = request.city;
    profile.country = request.country;
    profile.updatedAt = new Date();
    await this.profileRepository.upsert(profile, {
      conflictPaths: ['owner'],
      skipUpdateIfNoValuesChanged: false,
    });
    return ProfileService.toResponse(profile, user);
  }

  private static toResponse(
    profile: CustomerProfile,
    user: CurrentUser,
  ): ViewProfileResponse {
    const response = new ViewProfileResponse();
    response.firstName = profile.firstName;
    response.lastName = profile.lastName;
    response.email = user.email;
    response.street = profile.street;
    response.postalCode = profile.postalCode;
    response.city = profile.city;
    response.country = profile.country;
    return response;
  }
}
