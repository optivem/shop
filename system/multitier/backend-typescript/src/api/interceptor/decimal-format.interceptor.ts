import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Response } from 'express';
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

function markDecimals(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(markDecimals);
  }

  if (typeof obj === 'object') {
    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
      const scale = DECIMAL_FIELDS[key];
      if (scale !== undefined && typeof value === 'number') {
        result[key] = `${SENTINEL}${value.toFixed(scale)}${SENTINEL}`;
      } else {
        result[key] = markDecimals(value);
      }
    }
    return result;
  }

  return obj;
}

const SENTINEL_REGEX = new RegExp(
  String.raw`"${SENTINEL}([\d.]+)${SENTINEL}"`,
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

        const response = context.switchToHttp().getResponse<Response>();
        if (!response.headersSent) {
          response.setHeader('Content-Type', 'application/json; charset=utf-8');
          response.send(formatted);
        }

        return undefined;
      }),
    );
  }
}
