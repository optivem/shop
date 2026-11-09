import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { ILike, Repository } from 'typeorm';
import { randomUUID } from 'node:crypto';
import Decimal from 'decimal.js';
import { Order } from '../entities/order.entity';
import { OrderStatus } from '../entities/order-status.enum';
import { PlaceOrderRequest } from '../dtos/place-order-request.dto';
import { PlaceOrderResponse } from '../dtos/place-order-response.dto';
import {
  BrowseOrderHistoryResponse,
  BrowseOrderHistoryItemResponse,
} from '../dtos/browse-order-history-response.dto';
import { ViewOrderDetailsResponse } from '../dtos/view-order-details-response.dto';
import { RecallSkuResponse } from '../dtos/recall-sku-response.dto';
import { ValidationException } from '../exceptions/validation.exception';
import { NotExistValidationException } from '../exceptions/not-exist-validation.exception';
import { ConcurrentModificationException } from '../exceptions/concurrent-modification.exception';
import { ErpGateway } from './external/erp.gateway';
import { ClockGateway } from './external/clock.gateway';
import { TaxGateway } from './external/tax.gateway';
import { CouponService } from './coupon.service';
import {
  IdempotencyKeyStore,
  type IdempotencyScope,
} from './idempotency-key.store';
import { PageSpec } from './page-spec';
import type { IdempotencyKey } from '../entities/idempotency-key.entity';
import type { CurrentUser } from '../../auth/current-user';
import { toShippingAddress } from '../dtos/shipping-address.dto';

@Injectable()
export class OrderService {
  private static readonly RESTRICTED_MONTH = 11; // December (0-indexed)
  private static readonly RESTRICTED_DAY = 31;
  private static readonly RESTRICTED_HOUR = 23;
  private static readonly RESTRICTED_MINUTE = 59;

  constructor(
    @InjectRepository(Order)
    private readonly orderRepository: Repository<Order>,
    private readonly erpGateway: ErpGateway,
    private readonly clockGateway: ClockGateway,
    private readonly taxGateway: TaxGateway,
    private readonly couponService: CouponService,
    private readonly idempotencyKeys: IdempotencyKeyStore,
  ) {}

  private static readonly CREATED = 201;

  /**
   * Places an order. With an Idempotency-Key, a repeat of the same request inside 24 hours answers
   * with the stored response instead of placing a second order, and the same key with a different
   * request is rejected. Without one it behaves as before.
   */
  async placeOrder(
    request: PlaceOrderRequest,
    user: CurrentUser,
    rawIdempotencyKey?: string,
  ): Promise<PlaceOrderResponse> {
    const key = IdempotencyKeyStore.normalize(rawIdempotencyKey);
    if (key === null) {
      return this.placeNewOrder(request, user, null);
    }

    const scope: IdempotencyScope = {
      owner: user.subject ?? '',
      key,
      fingerprint: IdempotencyKeyStore.fingerprint(request),
    };
    const stored = await this.idempotencyKeys.findLive(
      scope.owner,
      scope.key,
      new Date(),
    );
    if (stored) {
      return OrderService.answerFromStored(stored, scope.fingerprint);
    }

    try {
      return await this.placeNewOrder(request, user, scope);
    } catch (error) {
      // A concurrent request with the same key committed first; this one rolled back and placed
      // nothing, so it answers from the winner's row.
      if (!IdempotencyKeyStore.isKeyConflict(error)) {
        throw error;
      }
      const winner = await this.idempotencyKeys.find(scope.owner, scope.key);
      if (!winner) {
        throw error;
      }
      return OrderService.answerFromStored(winner, scope.fingerprint);
    }
  }

  private static answerFromStored(
    stored: IdempotencyKey,
    fingerprint: string,
  ): PlaceOrderResponse {
    if (stored.requestHash !== fingerprint) {
      throw new ValidationException(IdempotencyKeyStore.MSG_KEY_REUSED);
    }
    return Object.assign(
      new PlaceOrderResponse(),
      JSON.parse(stored.responseBody) as PlaceOrderResponse,
    );
  }

  private async placeNewOrder(
    request: PlaceOrderRequest,
    user: CurrentUser,
    idempotency: IdempotencyScope | null,
  ): Promise<PlaceOrderResponse> {
    const sku = request.sku;
    const quantity = request.quantity;
    // With a shipping address the tax country is the address's; the top-level country is ignored.
    const shippingAddress = request.shippingAddress
      ? toShippingAddress(request.shippingAddress)
      : null;
    const country = shippingAddress ? shippingAddress.country : request.country;
    const countryField = shippingAddress
      ? 'shippingAddress.country'
      : 'country';
    const couponCode = request.couponCode;

    const orderTimestamp = await this.clockGateway.getCurrentTime();

    const utcMonth = orderTimestamp.getUTCMonth();
    const utcDay = orderTimestamp.getUTCDate();

    if (
      utcMonth === OrderService.RESTRICTED_MONTH &&
      utcDay === OrderService.RESTRICTED_DAY
    ) {
      const utcHour = orderTimestamp.getUTCHours();
      const utcMinute = orderTimestamp.getUTCMinutes();

      if (
        utcHour > OrderService.RESTRICTED_HOUR ||
        (utcHour === OrderService.RESTRICTED_HOUR &&
          utcMinute >= OrderService.RESTRICTED_MINUTE)
      ) {
        throw new ValidationException(
          'Orders cannot be placed between 23:59 and 00:00 on December 31st',
        );
      }
    }

    const unitPrice = await this.getUnitPrice(sku);
    const promotion = await this.erpGateway.getPromotionDetails();
    const promotionFactor = promotion.promotionActive
      ? new Decimal(promotion.discount)
      : new Decimal(1);
    const basePrice = unitPrice.mul(quantity);
    const promotedPrice = basePrice.mul(promotionFactor);

    const discountRate = await this.couponService.getDiscount(couponCode);
    const discountAmount = promotedPrice.mul(discountRate);
    const subtotalPrice = promotedPrice.sub(discountAmount);

    const taxRate = await this.getTaxRate(country, countryField);
    const taxAmount = subtotalPrice.mul(taxRate);
    const totalPrice = subtotalPrice.add(taxAmount);

    const appliedCouponCode = discountRate.gt(0) ? (couponCode ?? null) : null;

    const orderNumber = this.generateOrderNumber();

    const order = new Order();
    order.orderNumber = orderNumber;
    order.orderTimestamp = orderTimestamp;
    order.country = country;
    order.sku = sku;
    order.quantity = quantity;
    order.unitPrice = OrderService.roundMoney(unitPrice);
    order.basePrice = OrderService.roundMoney(basePrice);
    order.discountRate = OrderService.roundRate(discountRate);
    order.discountAmount = OrderService.roundMoney(discountAmount);
    order.subtotalPrice = OrderService.roundMoney(subtotalPrice);
    order.taxRate = OrderService.roundRate(taxRate);
    order.taxAmount = OrderService.roundMoney(taxAmount);
    order.totalPrice = OrderService.roundMoney(totalPrice);
    order.status = OrderStatus.PLACED;
    order.appliedCouponCode = appliedCouponCode ?? null;
    order.owner = user.subject;
    order.ownerName = user.username;
    order.shippingAddress = shippingAddress;

    const response = new PlaceOrderResponse();
    response.orderNumber = orderNumber;

    // The idempotency key, the coupon use and the order commit together, so a failure leaves none
    // of them behind. The key goes first: a concurrent request with the same key fails on its
    // unique index before it can touch the coupon.
    await this.orderRepository.manager.transaction(async (manager) => {
      if (idempotency) {
        await this.idempotencyKeys.claim(
          idempotency,
          OrderService.CREATED,
          JSON.stringify(response),
          new Date(),
          manager,
        );
      }
      if (appliedCouponCode) {
        await this.couponService.claimUsage(appliedCouponCode, manager);
      }
      await manager.withRepository(this.orderRepository).save(order);
    });

    return response;
  }

  // Intermediate amounts stay exact; each value is rounded exactly once, here, to the scale of the
  // column it is persisted in (numeric(10,2) for money, numeric(5,4) for rates).
  private static roundMoney(value: Decimal): Decimal {
    return value.toDecimalPlaces(2, Decimal.ROUND_HALF_UP);
  }

  private static roundRate(value: Decimal): Decimal {
    return value.toDecimalPlaces(4, Decimal.ROUND_HALF_UP);
  }

  private async getUnitPrice(sku: string): Promise<Decimal> {
    const productDetails = await this.erpGateway.getProductDetails(sku);
    if (productDetails === null) {
      throw new ValidationException(
        'sku',
        `Product does not exist for SKU: ${sku}`,
      );
    }

    return new Decimal(productDetails.price);
  }

  private async getTaxRate(country: string, field: string): Promise<Decimal> {
    const taxDetails = await this.taxGateway.getTaxDetails(country);
    if (taxDetails === null) {
      throw new ValidationException(
        field,
        `Country does not exist: ${country}`,
      );
    }

    return new Decimal(taxDetails.taxRate);
  }

  async deliverOrder(orderNumber: string): Promise<void> {
    const order = await this.orderRepository.findOne({
      where: { orderNumber },
    });

    if (!order) {
      throw new NotExistValidationException(
        `Order ${orderNumber} does not exist.`,
      );
    }

    if (order.status !== OrderStatus.PLACED) {
      throw new ValidationException(
        'Order cannot be delivered in its current status',
      );
    }

    await this.updateStatus(order, OrderStatus.DELIVERED);
  }

  async recallSku(sku: string): Promise<RecallSkuResponse> {
    if (!sku || sku.trim() === '') {
      throw new ValidationException('sku', 'SKU must not be empty');
    }

    const orders = await this.orderRepository.find({ where: { sku } });

    let cancelledCount = 0;
    for (const order of orders) {
      if (order.status === OrderStatus.PLACED) {
        await this.updateStatus(order, OrderStatus.CANCELLED);
        cancelledCount++;
      }
    }

    const response = new RecallSkuResponse();
    response.sku = sku;
    response.cancelledCount = cancelledCount;
    return response;
  }

  async cancelOrder(orderNumber: string, user: CurrentUser): Promise<void> {
    const now = await this.clockGateway.getCurrentTime();

    const utcMonth = now.getUTCMonth();
    const utcDay = now.getUTCDate();

    if (utcMonth === 11 && utcDay === 31) {
      const blackoutStart = new Date(
        Date.UTC(now.getUTCFullYear(), 11, 31, 22, 0, 0),
      );
      const blackoutEnd = new Date(
        Date.UTC(now.getUTCFullYear(), 11, 31, 22, 30, 0),
      );

      if (now >= blackoutStart && now <= blackoutEnd) {
        throw new ValidationException(
          'Order cancellation is not allowed on December 31st between 22:00 and 22:30',
        );
      }
    }

    const order = await this.findAccessibleOrder(orderNumber, user);

    if (order.status === OrderStatus.CANCELLED) {
      throw new ValidationException('Order has already been cancelled');
    }

    await this.updateStatus(order, OrderStatus.CANCELLED);
  }

  /**
   * A customer always sees only their own orders; an admin sees every order, or with `customer` only
   * the orders placed under that username (exact match). The customer filter is ignored for customers.
   */
  async browseOrderHistory(
    orderNumberFilter: string | undefined,
    user: CurrentUser,
    page?: string,
    size?: string,
    customerFilter?: string,
  ): Promise<BrowseOrderHistoryResponse> {
    const pageSpec = PageSpec.of(page, size);
    const filter = orderNumberFilter?.trim();
    const customer = user.admin ? customerFilter?.trim() : undefined;
    const where = {
      ...(filter ? { orderNumber: ILike(`%${filter}%`) } : {}),
      ...(user.admin ? {} : { owner: user.subject }),
      ...(customer ? { ownerName: customer } : {}),
    };
    // Paged in the database: a COUNT over the same predicate, then one page of rows.
    const totalElements = await this.orderRepository.count({ where });
    const orders = pageSpec.isPastEnd(totalElements)
      ? []
      : await this.orderRepository.find({
          where,
          order: { orderTimestamp: 'DESC', id: 'DESC' },
          skip: pageSpec.offset(),
          take: pageSpec.size,
        });

    const items = orders.map((order) => {
      const item = new BrowseOrderHistoryItemResponse();
      item.orderNumber = order.orderNumber;
      item.orderTimestamp = new Date(order.orderTimestamp).toISOString();
      item.sku = order.sku;
      item.country = order.country;
      item.quantity = order.quantity;
      item.totalPrice = order.totalPrice;
      item.status = order.status;
      item.appliedCouponCode = order.appliedCouponCode;
      item.customer = order.ownerName;
      return item;
    });

    const result = new BrowseOrderHistoryResponse();
    result.orders = items;
    result.page = pageSpec.page;
    result.size = pageSpec.size;
    result.totalElements = totalElements;
    result.totalPages = pageSpec.totalPages(totalElements);
    return result;
  }

  async getOrder(
    orderNumber: string,
    user: CurrentUser,
  ): Promise<ViewOrderDetailsResponse> {
    const order = await this.findAccessibleOrder(orderNumber, user);

    const response = new ViewOrderDetailsResponse();
    response.orderNumber = order.orderNumber;
    response.orderTimestamp = new Date(order.orderTimestamp).toISOString();
    response.sku = order.sku;
    response.quantity = order.quantity;
    response.unitPrice = order.unitPrice;
    response.basePrice = order.basePrice;
    response.discountRate = order.discountRate;
    response.discountAmount = order.discountAmount;
    response.subtotalPrice = order.subtotalPrice;
    response.taxRate = order.taxRate;
    response.taxAmount = order.taxAmount;
    response.totalPrice = order.totalPrice;
    response.status = order.status;
    response.country = order.country;
    response.appliedCouponCode = order.appliedCouponCode;
    response.shippingAddress = order.shippingAddress ?? null;
    return response;
  }

  // Optimistic locking: applies only if the row still has the version that was read.
  private async updateStatus(order: Order, status: OrderStatus): Promise<void> {
    const result = await this.orderRepository.update(
      { id: order.id, version: order.version },
      { status, version: () => 'version + 1' },
    );
    if (result.affected !== 1) {
      throw new ConcurrentModificationException(
        `Order ${order.orderNumber} was modified concurrently`,
      );
    }
    order.status = status;
  }

  /** Another customer's order is reported as non-existent, so its existence is not revealed. */
  private async findAccessibleOrder(
    orderNumber: string,
    user: CurrentUser,
  ): Promise<Order> {
    const order = await this.orderRepository.findOne({
      where: { orderNumber },
    });
    if (!order || !(user.admin || order.owner === user.subject)) {
      throw new NotExistValidationException(
        `Order ${orderNumber} does not exist.`,
      );
    }
    return order;
  }

  private generateOrderNumber(): string {
    return `ORD-${randomUUID().toUpperCase()}`;
  }
}
