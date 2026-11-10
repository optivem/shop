import type { ShippingAddress } from '../../types/api.types';
import type { AddressLabels } from './address-labels';
import { SHIPPING_COUNTRIES, type AddressErrors } from './shipping';

const VISIBLE_LABELS: AddressLabels = {
  firstName: 'First name',
  lastName: 'Last name',
  street: 'Street',
  postalCode: 'Postal code',
  city: 'City',
  country: 'Country',
};

interface AddressFieldsProps {
  idPrefix: string;
  address: ShippingAddress;
  errors: AddressErrors;
  labels: AddressLabels;
  onChange: (address: ShippingAddress) => void;
  /** Leave out the name fields (the profile page shows them in its own section). */
  withoutNames?: boolean;
}

/** Name and address inputs plus the country select (US, DE, GB), with each field's error under it. */
export function AddressFields({ idPrefix, address, errors, labels, onChange, withoutNames = false }: Readonly<AddressFieldsProps>) {
  const textField = (field: Exclude<keyof ShippingAddress, 'country'>, colClass: string) => {
    const id = `${idPrefix}-${field}`;
    const error = errors[field];
    return (
      <div className={colClass} key={field}>
        <label htmlFor={id} className="form-label">
          {VISIBLE_LABELS[field]}
        </label>
        <input
          id={id}
          type="text"
          className={`form-control${error ? ' is-invalid' : ''}`}
          aria-label={labels[field]}
          aria-invalid={error ? true : undefined}
          value={address[field]}
          onChange={(e) => onChange({ ...address, [field]: e.target.value })}
        />
        {error && <div className="invalid-feedback">{error}</div>}
      </div>
    );
  };

  const countryId = `${idPrefix}-country`;
  return (
    <div className="row g-3">
      {!withoutNames && textField('firstName', 'col-md-6')}
      {!withoutNames && textField('lastName', 'col-md-6')}
      {textField('street', 'col-12')}
      {textField('postalCode', 'col-md-4')}
      {textField('city', 'col-md-4')}
      <div className="col-md-4">
        <label htmlFor={countryId} className="form-label">
          {VISIBLE_LABELS.country}
        </label>
        <select
          id={countryId}
          className={`form-select${errors.country ? ' is-invalid' : ''}`}
          aria-label={labels.country}
          aria-invalid={errors.country ? true : undefined}
          value={address.country}
          onChange={(e) => onChange({ ...address, country: e.target.value })}
        >
          <option value="">Select a country</option>
          {SHIPPING_COUNTRIES.map((country) => (
            <option key={country} value={country}>
              {country}
            </option>
          ))}
        </select>
        {errors.country && <div className="invalid-feedback">{errors.country}</div>}
      </div>
    </div>
  );
}
