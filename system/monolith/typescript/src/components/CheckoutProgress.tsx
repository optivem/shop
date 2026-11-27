import { Icon } from "./Icon";

const STEPS = ["Cart", "Checkout", "Confirmation"] as const;

/** Where the visitor is on the way to an order: Cart, then Checkout, then Confirmation (Retail look). */
export function CheckoutProgress({ current }: Readonly<{ current: (typeof STEPS)[number] }>) {
  const currentIndex = STEPS.indexOf(current);
  return (
    <ol className="checkout-progress" aria-label="Checkout progress">
      {STEPS.map((step, index) => {
        let state = "next";
        if (index < currentIndex) state = "done";
        else if (index === currentIndex) state = "current";
        return (
          <li key={step} className={`checkout-progress-step is-${state}`} aria-current={state === "current" ? "step" : undefined}>
            <span className="checkout-progress-marker" aria-hidden="true">
              {state === "done" ? <Icon name="check" size={14} strokeWidth={3} /> : index + 1}
            </span>
            {step}
          </li>
        );
      })}
    </ol>
  );
}
