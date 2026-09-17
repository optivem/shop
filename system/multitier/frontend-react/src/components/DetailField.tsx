import type { ReactNode } from 'react';

export interface DetailFieldProps {
  label: string;
  value: ReactNode;
  ariaLabel?: string;
  valueClassName?: string;
}

/**
 * Detail field component for displaying label-value pairs in a consistent layout
 * Used for displaying structured information like order details
 */
export function DetailField({ label, value, ariaLabel, valueClassName }: Readonly<DetailFieldProps>) {
  return (
    <div className="col-md-6 mb-3">
      <strong>{label}:</strong>
      <p className={valueClassName} aria-label={ariaLabel}>
        {value}
      </p>
    </div>
  );
}
