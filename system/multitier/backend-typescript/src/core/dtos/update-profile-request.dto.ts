import { IsDefined, IsIn, IsString } from 'class-validator';
import { TrimmedText, TYPE_MISMATCH } from './request-parsing';
import {
  MSG_CITY_REQUIRED,
  MSG_COUNTRY_REQUIRED,
  MSG_FIRST_NAME_REQUIRED,
  MSG_LAST_NAME_REQUIRED,
  MSG_POSTAL_CODE_REQUIRED,
  MSG_STREET_REQUIRED,
} from './shipping-address.dto';

export const PROFILE_COUNTRIES = ['US', 'DE', 'GB'] as const;
export const MSG_PROFILE_COUNTRY_UNSUPPORTED = 'Country must be US, DE or GB';

/** PUT /api/account/profile: the name and default shipping address, stored trimmed. */
export class UpdateProfileRequest {
  @TrimmedText()
  @IsDefined({ message: MSG_FIRST_NAME_REQUIRED })
  @IsString({ message: MSG_FIRST_NAME_REQUIRED, context: TYPE_MISMATCH })
  firstName!: string;

  @TrimmedText()
  @IsDefined({ message: MSG_LAST_NAME_REQUIRED })
  @IsString({ message: MSG_LAST_NAME_REQUIRED, context: TYPE_MISMATCH })
  lastName!: string;

  @TrimmedText()
  @IsDefined({ message: MSG_STREET_REQUIRED })
  @IsString({ message: MSG_STREET_REQUIRED, context: TYPE_MISMATCH })
  street!: string;

  @TrimmedText()
  @IsDefined({ message: MSG_POSTAL_CODE_REQUIRED })
  @IsString({ message: MSG_POSTAL_CODE_REQUIRED, context: TYPE_MISMATCH })
  postalCode!: string;

  @TrimmedText()
  @IsDefined({ message: MSG_CITY_REQUIRED })
  @IsString({ message: MSG_CITY_REQUIRED, context: TYPE_MISMATCH })
  city!: string;

  @TrimmedText()
  @IsDefined({ message: MSG_COUNTRY_REQUIRED })
  @IsString({ message: MSG_COUNTRY_REQUIRED, context: TYPE_MISMATCH })
  @IsIn(PROFILE_COUNTRIES, { message: MSG_PROFILE_COUNTRY_UNSUPPORTED })
  country!: string;
}
