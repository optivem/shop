import Decimal from 'decimal.js';
import type { ValueTransformer } from 'typeorm';

/**
 * Maps a Postgres `numeric(p, scale)` column to a `Decimal`.
 *
 * node-postgres returns `numeric` as a string; it is read with `new Decimal(str)` so the value stays
 * exact (never `parseFloat`). On write the Decimal is rounded HALF_UP to the column's scale and sent
 * as a fixed-point string, so what is persisted is exactly what the entity holds.
 */
export function numericTransformer(scale: number): ValueTransformer {
  return {
    to: (value: Decimal | null | undefined): string | null | undefined =>
      value == null ? value : value.toFixed(scale, Decimal.ROUND_HALF_UP),
    from: (value: string | null): Decimal | null =>
      value === null ? null : new Decimal(value),
  };
}
