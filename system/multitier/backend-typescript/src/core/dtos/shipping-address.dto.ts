import { IsDefined, IsString } from 'class-validator';
import { TrimmedText, TYPE_MISMATCH } from './request-parsing';

export const MSG_FIRST_NAME_REQUIRED = 'First name is required';
export const MSG_LAST_NAME_REQUIRED = 'Last name is required';
export const MSG_STREET_REQUIRED = 'Street is required';
export const MSG_POSTAL_CODE_REQUIRED = 'Postal code is required';
export const MSG_CITY_REQUIRED = 'City is required';
export const MSG_COUNTRY_REQUIRED = 'Country is required';

/** The address an order ships to; every field is required and stored trimmed. */
export class ShippingAddressRequest {
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
  country!: string;
}

/** The stored (and returned) shape of a shipping address: orders.shipping_address JSONB. */
export interface ShippingAddress {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}

export function toShippingAddress(
  request: ShippingAddressRequest,
): ShippingAddress {
  return {
    firstName: request.firstName,
    lastName: request.lastName,
    street: request.street,
    postalCode: request.postalCode,
    city: request.city,
    country: request.country,
  };
}
