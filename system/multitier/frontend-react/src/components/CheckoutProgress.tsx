import { Check } from 'lucide-react';

const STEPS = ['Cart', 'Checkout', 'Confirmation'] as const;

interface CheckoutProgressProps {
  current: (typeof STEPS)[number];
}

/** Where the visitor is on the way to an order: Cart, then Checkout, then Confirmation (Retail look). */
export function CheckoutProgress({ current }: Readonly<CheckoutProgressProps>) {
  const currentIndex = STEPS.indexOf(current);
  return (
    <ol className="checkout-progress" aria-label="Checkout progress">
      {STEPS.map((step, index) => {
        const state = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'next';
        return (
          <li key={step} className={`checkout-progress-step is-${state}`} aria-current={state === 'current' ? 'step' : undefined}>
            <span className="checkout-progress-marker" aria-hidden="true">
              {state === 'done' ? <Check size={14} strokeWidth={3} /> : index + 1}
            </span>
            {step}
          </li>
        );
      })}
    </ol>
  );
}
