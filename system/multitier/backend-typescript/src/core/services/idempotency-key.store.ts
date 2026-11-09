import { createHash } from 'node:crypto';
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import {
  EntityManager,
  LessThanOrEqual,
  QueryFailedError,
  Repository,
} from 'typeorm';
import { IdempotencyKey } from '../entities/idempotency-key.entity';
import { ValidationException } from '../exceptions/validation.exception';
import type { PlaceOrderRequest } from '../dtos/place-order-request.dto';

/** Who used a key, the key itself, and the fingerprint of the request it was used for. */
export interface IdempotencyScope {
  owner: string;
  key: string;
  fingerprint: string;
}

/**
 * The idempotency_keys table: keys are scoped per caller and live 24 hours. A row is written in the
 * same transaction as the order it answers for, so a failed request leaves the key free.
 */
@Injectable()
export class IdempotencyKeyStore {
  static readonly HEADER = 'Idempotency-Key';
  static readonly MAX_KEY_LENGTH = 255;
  static readonly TTL_MS = 24 * 60 * 60 * 1000;
  static readonly UNIQUE_CONSTRAINT = 'uq_idempotency_keys_owner_key';
  static readonly MSG_KEY_TOO_LONG = `Idempotency-Key must be at most ${IdempotencyKeyStore.MAX_KEY_LENGTH} characters`;
  static readonly MSG_KEY_REUSED =
    'Idempotency-Key has already been used with a different request';

  constructor(
    @InjectRepository(IdempotencyKey)
    private readonly repository: Repository<IdempotencyKey>,
  ) {}

  /** A blank header is the same as no header; an over-long one is a field validation error. */
  static normalize(rawKey?: string): string | null {
    if (rawKey === undefined || rawKey === null || rawKey.trim() === '') {
      return null;
    }
    if (rawKey.length > IdempotencyKeyStore.MAX_KEY_LENGTH) {
      throw new ValidationException(
        IdempotencyKeyStore.HEADER,
        IdempotencyKeyStore.MSG_KEY_TOO_LONG,
      );
    }
    return rawKey;
  }

  /**
   * SHA-256 (hex) over sku, quantity, country and coupon code as bound, then the shipping address
   * fields when there is one, null as empty, joined by \n.
   */
  static fingerprint(request: PlaceOrderRequest): string {
    const address = request.shippingAddress;
    const parts = [
      request.sku,
      request.quantity,
      request.country,
      request.couponCode,
      ...(address
        ? [
            address.firstName,
            address.lastName,
            address.street,
            address.postalCode,
            address.city,
            address.country,
          ]
        : []),
    ].map((value) =>
      value === undefined || value === null ? '' : String(value),
    );
    return createHash('sha256').update(parts.join('\n')).digest('hex');
  }

  /** Unique-index violation on (owner, idempotency_key): a concurrent request with the same key won. */
  static isKeyConflict(error: unknown): boolean {
    if (!(error instanceof QueryFailedError)) {
      return false;
    }
    const driverError = error.driverError as {
      code?: string;
      constraint?: string;
    };
    return (
      driverError?.code === '23505' &&
      driverError?.constraint === IdempotencyKeyStore.UNIQUE_CONSTRAINT
    );
  }

  static isExpired(row: IdempotencyKey, now: Date): boolean {
    return (
      new Date(row.createdAt).getTime() <=
      now.getTime() - IdempotencyKeyStore.TTL_MS
    );
  }

  find(owner: string, key: string): Promise<IdempotencyKey | null> {
    return this.repository.findOne({ where: { owner, key } });
  }

  /** The stored answer for this key, or null when there is none or it has expired. */
  async findLive(
    owner: string,
    key: string,
    now: Date,
  ): Promise<IdempotencyKey | null> {
    const row = await this.find(owner, key);
    return row && !IdempotencyKeyStore.isExpired(row, now) ? row : null;
  }

  /**
   * Claims the key inside the caller's transaction: an expired row is replaced, a live one makes the
   * insert fail on the unique index (the caller then answers from the winner's row).
   */
  async claim(
    scope: IdempotencyScope,
    responseStatus: number,
    responseBody: string,
    now: Date,
    manager: EntityManager,
  ): Promise<void> {
    const repository = manager.getRepository(IdempotencyKey);
    await repository.delete({
      owner: scope.owner,
      key: scope.key,
      createdAt: LessThanOrEqual(
        new Date(now.getTime() - IdempotencyKeyStore.TTL_MS),
      ),
    });
    await repository.insert({
      owner: scope.owner,
      key: scope.key,
      requestHash: scope.fingerprint,
      responseStatus,
      responseBody,
      createdAt: now,
    });
  }
}
