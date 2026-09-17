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
import { ErpGateway } from './external/erp.gateway';
import { ClockGateway } from './external/clock.gateway';
import { TaxGateway } from './external/tax.gateway';
import { CouponService } from './coupon.service';

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
  ) {}

  async placeOrder(request: PlaceOrderRequest): Promise<PlaceOrderResponse> {
    const sku = request.sku;
    const quantity = request.quantity;
    const country = request.country;
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

    const taxRate = await this.getTaxRate(country);
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

    // The coupon use is claimed in the same transaction as the insert, so a failed insert does not
    // use up the coupon.
    await this.orderRepository.manager.transaction(async (manager) => {
      if (appliedCouponCode) {
        await this.couponService.claimUsage(appliedCouponCode, manager);
      }
      await manager.withRepository(this.orderRepository).save(order);
    });

    const response = new PlaceOrderResponse();
    response.orderNumber = orderNumber;
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

  private async getTaxRate(country: string): Promise<Decimal> {
    const taxDetails = await this.taxGateway.getTaxDetails(country);
    if (taxDetails === null) {
      throw new ValidationException(
        'country',
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

    order.status = OrderStatus.DELIVERED;
    await this.orderRepository.save(order);
  }

  async recallSku(sku: string): Promise<RecallSkuResponse> {
    if (!sku || sku.trim() === '') {
      throw new ValidationException('sku', 'SKU must not be empty');
    }

    const orders = await this.orderRepository.find({ where: { sku } });

    let cancelledCount = 0;
    for (const order of orders) {
      if (order.status === OrderStatus.PLACED) {
        order.status = OrderStatus.CANCELLED;
        await this.orderRepository.save(order);
        cancelledCount++;
      }
    }

    const response = new RecallSkuResponse();
    response.sku = sku;
    response.cancelledCount = cancelledCount;
    return response;
  }

  async cancelOrder(orderNumber: string): Promise<void> {
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
          'Order cancellation is not allowed on December 31st between 22:00 and 23:00',
        );
      }
    }

    const order = await this.orderRepository.findOne({
      where: { orderNumber },
    });

    if (!order) {
      throw new NotExistValidationException(
        `Order ${orderNumber} does not exist.`,
      );
    }

    if (order.status === OrderStatus.CANCELLED) {
      throw new ValidationException('Order has already been cancelled');
    }

    order.status = OrderStatus.CANCELLED;
    await this.orderRepository.save(order);
  }

  async browseOrderHistory(
    orderNumberFilter?: string,
  ): Promise<BrowseOrderHistoryResponse> {
    let orders: Order[];

    if (!orderNumberFilter || orderNumberFilter.trim() === '') {
      orders = await this.orderRepository.find({
        order: { orderTimestamp: 'DESC' },
      });
    } else {
      orders = await this.orderRepository.find({
        where: {
          orderNumber: ILike(`%${orderNumberFilter.trim()}%`),
        },
        order: { orderTimestamp: 'DESC' },
      });
    }

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
      return item;
    });

    const result = new BrowseOrderHistoryResponse();
    result.orders = items;
    return result;
  }

  async getOrder(orderNumber: string): Promise<ViewOrderDetailsResponse> {
    const order = await this.orderRepository.findOne({
      where: { orderNumber },
    });

    if (!order) {
      throw new NotExistValidationException(
        `Order ${orderNumber} does not exist.`,
      );
    }

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
    return response;
  }

  private generateOrderNumber(): string {
    return `ORD-${randomUUID().toUpperCase()}`;
  }
}
