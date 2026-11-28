import { useTranslation } from 'react-i18next';
import { Check } from 'lucide-react';

const STEPS = ['Cart', 'Checkout', 'Confirmation'] as const;

const STEP_KEYS = {
  Cart: 'checkoutProgress.cart',
  Checkout: 'checkoutProgress.checkout',
  Confirmation: 'checkoutProgress.confirmation',
} as const;

interface CheckoutProgressProps {
  current: (typeof STEPS)[number];
}

/** Where the visitor is on the way to an order: Cart, then Checkout, then Confirmation (Retail look). */
export function CheckoutProgress({ current }: Readonly<CheckoutProgressProps>) {
  const { t } = useTranslation();
  const currentIndex = STEPS.indexOf(current);
  return (
    <ol className="checkout-progress" aria-label={t('checkoutProgress.label')}>
      {STEPS.map((step, index) => {
        const state = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'next';
        return (
          <li key={step} className={`checkout-progress-step is-${state}`} aria-current={state === 'current' ? 'step' : undefined}>
            <span className="checkout-progress-marker" aria-hidden="true">
              {state === 'done' ? <Check size={14} strokeWidth={3} /> : index + 1}
            </span>
            {t(STEP_KEYS[step])}
          </li>
        );
      })}
    </ol>
  );
}
