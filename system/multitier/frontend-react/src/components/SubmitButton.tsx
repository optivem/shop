import type { ButtonHTMLAttributes } from 'react';

export interface SubmitButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type' | 'disabled' | 'aria-label'> {
  /**
   * Whether the button is in a submitting/loading state
   */
  isSubmitting: boolean;

  /**
   * Text to display when not submitting
   */
  text: string;

  /**
   * Text to display when submitting (e.g., "Creating...", "Placing Order...")
   */
  loadingText: string;

  /**
   * Accessible label for the button action
   */
  ariaLabel: string;

  /**
   * Additional CSS classes to apply to the button
   * @default "btn btn-primary"
   */
  className?: string;
}

/**
 * Reusable submit button with loading state
 * Automatically handles disabled state and text switching during submission
 */
export function SubmitButton({
  isSubmitting,
  text,
  loadingText,
  ariaLabel,
  className = 'btn btn-primary',
  ...rest
}: Readonly<SubmitButtonProps>) {
  return (
    <button
      type="submit"
      className={className}
      aria-label={ariaLabel}
      disabled={isSubmitting}
      {...rest}
    >
      {isSubmitting && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
      {isSubmitting ? loadingText : text}
    </button>
  );
}
