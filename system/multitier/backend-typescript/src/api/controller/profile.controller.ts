import { Body, Controller, Get, Put } from '@nestjs/common';
import { ProfileService } from '../../core/services/profile.service';
import { UpdateProfileRequest } from '../../core/dtos/update-profile-request.dto';
import { ViewProfileResponse } from '../../core/dtos/view-profile-response.dto';
import { Caller, type CurrentUser } from '../../auth/current-user';

@Controller('api/account/profile')
export class ProfileController {
  constructor(private readonly profileService: ProfileService) {}

  @Get()
  async viewProfile(@Caller() user: CurrentUser): Promise<ViewProfileResponse> {
    return this.profileService.viewProfile(user);
  }

  @Put()
  async updateProfile(
    @Body() request: UpdateProfileRequest,
    @Caller() user: CurrentUser,
  ): Promise<ViewProfileResponse> {
    return this.profileService.updateProfile(request, user);
  }
}
