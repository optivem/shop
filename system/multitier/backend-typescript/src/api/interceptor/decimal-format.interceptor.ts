import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import Decimal from 'decimal.js';
import type { Response } from 'express';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

const DECIMAL_FIELDS: Record<string, number> = {
  unitPrice: 2,
  basePrice: 2,
  discountRate: 4,
  discountAmount: 2,
  subtotalPrice: 2,
  taxRate: 4,
  taxAmount: 2,
  totalPrice: 2,
};

const SENTINEL = '__DECIMAL_';

function toDecimal(value: unknown): Decimal | null {
  if (Decimal.isDecimal(value)) {
    return value.isFinite() ? value : null;
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return new Decimal(value);
  }
  return null;
}

function markDecimals(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj;
  }

  if (Decimal.isDecimal(obj)) {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(markDecimals);
  }

  if (typeof obj === 'object') {
    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      const scale = DECIMAL_FIELDS[key];
      const decimal = scale === undefined ? null : toDecimal(value);
      if (scale !== undefined && decimal !== null) {
        result[key] =
          `${SENTINEL}${decimal.toFixed(scale, Decimal.ROUND_HALF_UP)}${SENTINEL}`;
      } else {
        result[key] = markDecimals(value);
      }
    }
    return result;
  }

  return obj;
}

const SENTINEL_REGEX = new RegExp(
  String.raw`"${SENTINEL}(-?[\d.]+)${SENTINEL}"`,
  'g',
);

@Injectable()
export class DecimalFormatInterceptor implements NestInterceptor {
  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    return next.handle().pipe(
      map((data: unknown) => {
        if (data === undefined || data === null) {
          return data;
        }

        const marked = markDecimals(data);
        const json = JSON.stringify(marked);
        const formatted = json.replaceAll(SENTINEL_REGEX, '$1');

        // Hand the pre-serialised JSON back to Nest, which sends it: a string body goes out via
        // res.send() verbatim, keeping the Content-Type set here instead of re-serialising it.
        const response = context.switchToHttp().getResponse<Response>();
        response.setHeader('Content-Type', 'application/json; charset=utf-8');
        return formatted;
      }),
    );
  }
}
