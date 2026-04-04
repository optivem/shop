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
    const country = request.country?.trim() || 'US';
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
    const promotionFactor = promotion.promotionActive ? promotion.discount : 1;
    const basePrice = new Decimal(unitPrice).mul(quantity).mul(promotionFactor).toNumber();

    const discountRate = await this.couponService.getDiscount(couponCode);
    const discountAmount = new Decimal(basePrice).mul(discountRate).toNumber();
    const subtotalPrice = new Decimal(basePrice).sub(discountAmount).toNumber();

    const taxRate = await this.getTaxRate(country);
    const taxAmount = new Decimal(subtotalPrice).mul(taxRate).toNumber();
    const totalPrice = new Decimal(subtotalPrice).add(taxAmount).toNumber();

    const appliedCouponCode = discountRate > 0 ? couponCode ?? null : null;

    const orderNumber = this.generateOrderNumber();

    const order = new Order();
    order.orderNumber = orderNumber;
    order.orderTimestamp = orderTimestamp;
    order.country = country;
    order.sku = sku;
    order.quantity = quantity;
    order.unitPrice = unitPrice;
    order.basePrice = basePrice;
    order.discountRate = discountRate;
    order.discountAmount = discountAmount;
    order.subtotalPrice = subtotalPrice;
    order.taxRate = taxRate;
    order.taxAmount = taxAmount;
    order.totalPrice = totalPrice;
    order.status = OrderStatus.PLACED;
    order.appliedCouponCode = appliedCouponCode ?? null;

    await this.orderRepository.save(order);

    if (appliedCouponCode) {
      await this.couponService.incrementUsageCount(appliedCouponCode);
    }

    const response = new PlaceOrderResponse();
    response.orderNumber = orderNumber;
    return response;
  }

  private async getUnitPrice(sku: string): Promise<number> {
    const productDetails = await this.erpGateway.getProductDetails(sku);
    if (productDetails === null) {
      throw new ValidationException(
        'sku',
        `Product does not exist for SKU: ${sku}`,
      );
    }

    return Number(productDetails.price);
  }

  private async getTaxRate(country: string): Promise<number> {
    const taxDetails = await this.taxGateway.getTaxDetails(country);
    if (taxDetails === null) {
      throw new ValidationException(
        'country',
        `Country does not exist: ${country}`,
      );
    }

    return Number(taxDetails.taxRate);
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
      item.totalPrice = Number(order.totalPrice);
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
    response.unitPrice = Number(order.unitPrice);
    response.basePrice = Number(order.basePrice);
    response.discountRate = Number(order.discountRate);
    response.discountAmount = Number(order.discountAmount);
    response.subtotalPrice = Number(order.subtotalPrice);
    response.taxRate = Number(order.taxRate);
    response.taxAmount = Number(order.taxAmount);
    response.totalPrice = Number(order.totalPrice);
    response.status = order.status;
    response.country = order.country;
    response.appliedCouponCode = order.appliedCouponCode;
    return response;
  }

  private generateOrderNumber(): string {
    return `ORD-${randomUUID().toUpperCase()}`;
  }
}
