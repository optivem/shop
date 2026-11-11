"use client";

import { useState, type KeyboardEvent } from "react";
import type { AddressLabels } from "@/lib/storefront/address-labels";
import { brandColour } from "@/lib/storefront/brand-colour";
import { clampQuantity, parseQuantity } from "@/lib/storefront/quantity";
import { SHIPPING_COUNTRIES, type AddressErrors } from "@/lib/storefront/shipping";
import type { ShippingAddress } from "@/lib/storefront/types";
import { Icon } from "./Icon";

// Product image placeholder: the ERP has no image field, so each product gets a tile with its brand name,
// coloured by a deterministic hash of the brand (storefront-ux §5). No external images.
export function ProductImage({ brand, shape = "card" }: Readonly<{ brand: string; shape?: "card" | "square" }>) {
  return (
    <div
      className={`product-image product-image-${shape}`}
      style={{ backgroundColor: brandColour(brand) }}
      role="img"
      aria-label={`${brand} product image`}
    >
      <span>{brand.toUpperCase()}</span>
    </div>
  );
}

interface QuantityStepperProps {
  quantity: number;
  /** Called with a valid quantity from the stepper, or once a typed value is committed (blur or Enter). */
  onChange: (quantity: number) => void;
  /** Called when a typed value is not a whole number from 1 to 99; the previous quantity is kept. */
  onInvalid: () => void;
  ariaLabel: string;
  decreaseLabel: string;
  increaseLabel: string;
}

/** [-][ n ][+]: the buttons move by 1 within 1..99; typing is checked when committed. */
export function QuantityStepper({ quantity, onChange, onInvalid, ariaLabel, decreaseLabel, increaseLabel }: Readonly<QuantityStepperProps>) {
  // A draft only exists while the visitor is typing; otherwise the input shows the quantity itself.
  const [draft, setDraft] = useState<string | null>(null);

  const commit = () => {
    if (draft === null) return;
    const parsed = parseQuantity(draft);
    setDraft(null);
    if (parsed === undefined) {
      onInvalid();
    } else if (parsed !== quantity) {
      onChange(parsed);
    }
  };

  // The buttons stay enabled at the limits and simply don't go past them.
  const step = (delta: number) => {
    const next = clampQuantity(quantity + delta);
    if (next !== quantity) onChange(next);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter") {
      event.preventDefault();
      commit();
    }
  };

  return (
    <div className="input-group quantity-stepper">
      <button type="button" className="btn btn-outline-secondary" aria-label={decreaseLabel} onClick={() => step(-1)}>
        <Icon name="minus" />
      </button>
      <input
        type="text"
        inputMode="numeric"
        className="form-control text-center"
        aria-label={ariaLabel}
        value={draft ?? String(quantity)}
        onChange={(e) => setDraft(e.target.value)}
        onBlur={commit}
        onKeyDown={onKeyDown}
      />
      <button type="button" className="btn btn-outline-secondary" aria-label={increaseLabel} onClick={() => step(1)}>
        <Icon name="plus" />
      </button>
    </div>
  );
}

const VISIBLE_LABELS: AddressLabels = {
  firstName: "First name",
  lastName: "Last name",
  street: "Street",
  postalCode: "Postal code",
  city: "City",
  country: "Country",
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
  const textField = (field: Exclude<keyof ShippingAddress, "country">, colClass: string) => {
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
          className={`form-control${error ? " is-invalid" : ""}`}
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
      {!withoutNames && textField("firstName", "col-md-6")}
      {!withoutNames && textField("lastName", "col-md-6")}
      {textField("street", "col-12")}
      {textField("postalCode", "col-md-4")}
      {textField("city", "col-md-4")}
      <div className="col-md-4">
        <label htmlFor={countryId} className="form-label">
          {VISIBLE_LABELS.country}
        </label>
        <select
          id={countryId}
          className={`form-select${errors.country ? " is-invalid" : ""}`}
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
