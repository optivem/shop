interface LoadingSpinnerProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
}

/**
 * Loading spinner component for displaying loading states
 * @param message - Optional loading message to display
 * @param size - Size variant (sm, md, lg)
 */
export function LoadingSpinner({ message = 'Loading...', size = 'md' }: LoadingSpinnerProps) {
  const sizeClass = size === 'sm' ? 'spinner-border-sm' : '';

  return (
    <div className="text-center py-5">
      <output>
        <div className={`spinner-border text-primary ${sizeClass}`}>
          <span className="visually-hidden">Loading...</span>
        </div>
      </output>
      {message && <p className="mt-3">{message}</p>}
    </div>
  );
}
