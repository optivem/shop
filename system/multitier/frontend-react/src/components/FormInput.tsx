import { ChangeEvent, InputHTMLAttributes } from 'react';

export interface FormInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  label: string;
  id?: string;
  value: string;
  onChange: (e: ChangeEvent<HTMLInputElement>) => void;
  ariaLabel: string;
}

/**
 * Form input component for consistent form field rendering
 * Provides label, input, and proper accessibility attributes
 * Auto-generates ID from label if not provided
 */
export function FormInput({
  label,
  id,
  value,
  onChange,
  ariaLabel,
  ...inputProps
}: Readonly<FormInputProps>) {
  // Auto-generate ID from label if not provided
  const inputId = id ?? label.toLowerCase().replace(/[^a-z0-9]+/g, '-');

  return (
    <div className="mb-3">
      <label htmlFor={inputId} className="form-label">{label}:</label>
      <input
        type="text"
        className="form-control"
        id={inputId}
        aria-label={ariaLabel}
        value={value}
        onChange={onChange}
        {...inputProps}
      />
    </div>
  );
}
