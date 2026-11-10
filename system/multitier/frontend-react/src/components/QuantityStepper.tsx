import { useState, type KeyboardEvent } from 'react';
import { clampQuantity, parseQuantity } from '../utils/quantity';

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
export function QuantityStepper({
  quantity,
  onChange,
  onInvalid,
  ariaLabel,
  decreaseLabel,
  increaseLabel,
}: Readonly<QuantityStepperProps>) {
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
    if (event.key === 'Enter') {
      event.preventDefault();
      commit();
    }
  };

  return (
    <div className="input-group input-group-sm quantity-stepper">
      <button
        type="button"
        className="btn btn-outline-secondary"
        aria-label={decreaseLabel}
        onClick={() => step(-1)}
      >
        −
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
      <button
        type="button"
        className="btn btn-outline-secondary"
        aria-label={increaseLabel}
        onClick={() => step(1)}
      >
        +
      </button>
    </div>
  );
}
