import Decimal from 'decimal.js';
import { EntityManager, Repository } from 'typeorm';
import { Order } from '../entities/order.entity';
import { OrderStatus } from '../entities/order-status.enum';
import { PlaceOrderRequest } from '../dtos/place-order-request.dto';
import { ValidationException } from '../exceptions/validation.exception';
import { NotExistValidationException } from '../exceptions/not-exist-validation.exception';
import { OrderService } from './order.service';
import { ErpGateway } from './external/erp.gateway';
import { TaxGateway } from './external/tax.gateway';
import { ClockGateway } from './external/clock.gateway';
import { CouponService } from './coupon.service';

const NORMAL_TIME = new Date('2025-06-15T10:00:00Z');
const DEC_31_YEAR_END_BLACKOUT = new Date('2025-12-31T23:59:00Z');
const DEC_31_CANCEL_BLACKOUT = new Date('2025-12-31T22:15:00Z');

describe('OrderService', () => {
  let service: OrderService;
  let orderRepository: jest.Mocked<
    Pick<Repository<Order>, 'save' | 'findOne'>
  > & {
    manager: Pick<EntityManager, 'transaction' | 'withRepository'>;
  };
  let erpGateway: jest.Mocked<
    Pick<ErpGateway, 'getProductDetails' | 'getPromotionDetails'>
  >;
  let clockGateway: jest.Mocked<Pick<ClockGateway, 'getCurrentTime'>>;
  let taxGateway: jest.Mocked<Pick<TaxGateway, 'getTaxDetails'>>;
  let couponService: jest.Mocked<Pick<CouponService, 'getDiscount'>>;

  beforeEach(() => {
    orderRepository = {
      save: jest.fn(),
      findOne: jest.fn(),
      // Runs the transaction inline against the same mocked repository.
      manager: {
        transaction: jest.fn(
          (work: (manager: EntityManager) => Promise<unknown>) =>
            work(orderRepository.manager as EntityManager),
        ),
        withRepository: jest.fn(() => orderRepository),
      } as unknown as Pick<EntityManager, 'transaction' | 'withRepository'>,
    };
    erpGateway = {
      getProductDetails: jest.fn(),
      getPromotionDetails: jest.fn(),
    };
    clockGateway = { getCurrentTime: jest.fn() };
    taxGateway = { getTaxDetails: jest.fn() };
    couponService = { getDiscount: jest.fn() };

    service = new OrderService(
      orderRepository as unknown as Repository<Order>,
      erpGateway as unknown as ErpGateway,
      clockGateway as unknown as ClockGateway,
      taxGateway as unknown as TaxGateway,
      couponService as unknown as CouponService,
    );
  });

  describe('placeOrder', () => {
    it('returns order number starting with ORD- and saves correct pricing', async () => {
      givenNormalTime();
      givenProductExists('BOOK-123', 10);
      givenNoPromotion();
      givenNoDiscount();
      givenTaxRate('US', 0.1);
      orderRepository.save.mockResolvedValue({} as Order);

      const response = await service.placeOrder(
        buildRequest('BOOK-123', 2, 'US'),
      );

      expect(response.orderNumber).toMatch(/^ORD-/);
      assertSavedOrder(response.orderNumber);
    });

    it('throws when ordered on year-end blackout', async () => {
      clockGateway.getCurrentTime.mockResolvedValue(DEC_31_YEAR_END_BLACKOUT);

      await expect(
        service.placeOrder(buildRequest('BOOK-123', 1, 'US')),
      ).rejects.toThrow('December 31');
    });

    it('throws when SKU is unknown', async () => {
      givenNormalTime();
      erpGateway.getProductDetails.mockResolvedValue(null);

      const error = await service
        .placeOrder(buildRequest('UNKNOWN', 1, 'US'))
        .catch((e: unknown) => e);

      expect(error).toBeInstanceOf(ValidationException);
      expect((error as ValidationException).fieldName).toBe('sku');
    });

    it('throws when country is unknown', async () => {
      givenNormalTime();
      givenProductExists('BOOK-123', 10);
      givenNoPromotion();
      givenNoDiscount();
      taxGateway.getTaxDetails.mockResolvedValue(null);

      const error = await service
        .placeOrder(buildRequest('BOOK-123', 1, 'XX'))
        .catch((e: unknown) => e);

      expect(error).toBeInstanceOf(ValidationException);
      expect((error as ValidationException).fieldName).toBe('country');
    });
  });

  describe('deliverOrder', () => {
    it('transitions status to DELIVERED', async () => {
      const order = placedOrder('ORD-001');
      orderRepository.findOne.mockResolvedValue(order);
      orderRepository.save.mockResolvedValue({} as Order);

      await service.deliverOrder('ORD-001');

      expect(order.status).toBe(OrderStatus.DELIVERED);
      expect(orderRepository.save).toHaveBeenCalledWith(order);
    });

    it('throws when order not found', async () => {
      orderRepository.findOne.mockResolvedValue(null);

      await expect(service.deliverOrder('ORD-999')).rejects.toBeInstanceOf(
        NotExistValidationException,
      );
    });

    it('throws when order already delivered', async () => {
      const order = placedOrder('ORD-001');
      order.status = OrderStatus.DELIVERED;
      orderRepository.findOne.mockResolvedValue(order);

      await expect(service.deliverOrder('ORD-001')).rejects.toThrow(
        'cannot be delivered',
      );
    });
  });

  describe('cancelOrder', () => {
    it('transitions status to CANCELLED', async () => {
      givenNormalTime();
      const order = placedOrder('ORD-001');
      orderRepository.findOne.mockResolvedValue(order);
      orderRepository.save.mockResolvedValue({} as Order);

      await service.cancelOrder('ORD-001');

      expect(order.status).toBe(OrderStatus.CANCELLED);
      expect(orderRepository.save).toHaveBeenCalledWith(order);
    });

    it('throws during December 31 cancellation blackout', async () => {
      clockGateway.getCurrentTime.mockResolvedValue(DEC_31_CANCEL_BLACKOUT);

      await expect(service.cancelOrder('ORD-001')).rejects.toThrow(
        'December 31',
      );
    });

    it('throws when order already cancelled', async () => {
      givenNormalTime();
      const order = placedOrder('ORD-001');
      order.status = OrderStatus.CANCELLED;
      orderRepository.findOne.mockResolvedValue(order);

      await expect(service.cancelOrder('ORD-001')).rejects.toThrow(
        'already been cancelled',
      );
    });
  });

  function givenNormalTime() {
    clockGateway.getCurrentTime.mockResolvedValue(NORMAL_TIME);
  }

  function givenProductExists(sku: string, price: number) {
    erpGateway.getProductDetails.mockResolvedValue({ price });
  }

  function givenNoPromotion() {
    erpGateway.getPromotionDetails.mockResolvedValue({
      promotionActive: false,
      discount: 1,
    });
  }

  function givenNoDiscount() {
    couponService.getDiscount.mockResolvedValue(new Decimal(0));
  }

  function givenTaxRate(_country: string, rate: number) {
    taxGateway.getTaxDetails.mockResolvedValue({ taxRate: rate });
  }

  function buildRequest(
    sku: string,
    quantity: number,
    country: string,
  ): PlaceOrderRequest {
    const request = new PlaceOrderRequest();
    request.sku = sku;
    request.quantity = quantity;
    request.country = country;
    return request;
  }

  function placedOrder(orderNumber: string): Order {
    const order = new Order();
    order.orderNumber = orderNumber;
    order.orderTimestamp = NORMAL_TIME;
    order.country = 'US';
    order.sku = 'BOOK-123';
    order.quantity = 1;
    order.unitPrice = new Decimal('10.00');
    order.basePrice = new Decimal('10.00');
    order.discountRate = new Decimal('0.0000');
    order.discountAmount = new Decimal('0.00');
    order.subtotalPrice = new Decimal('10.00');
    order.taxRate = new Decimal('0.1000');
    order.taxAmount = new Decimal('1.00');
    order.totalPrice = new Decimal('11.00');
    order.status = OrderStatus.PLACED;
    order.appliedCouponCode = null;
    return order;
  }

  function assertSavedOrder(expectedOrderNumber: string) {
    expect(orderRepository.save).toHaveBeenCalledWith(
      expect.objectContaining({
        orderNumber: expectedOrderNumber,
        orderTimestamp: NORMAL_TIME,
        sku: 'BOOK-123',
        quantity: 2,
        country: 'US',
        status: OrderStatus.PLACED,
        appliedCouponCode: null,
      }),
    );

    // Amounts are Decimals: compare their fixed-point values, not the objects' internals.
    const [saved] = orderRepository.save.mock.calls.at(-1) ?? [];
    expect(saved).toBeDefined();
    const amounts = saved as Order;
    expect({
      unitPrice: amounts.unitPrice.toFixed(2),
      basePrice: amounts.basePrice.toFixed(2),
      discountRate: amounts.discountRate.toFixed(4),
      discountAmount: amounts.discountAmount.toFixed(2),
      subtotalPrice: amounts.subtotalPrice.toFixed(2),
      taxRate: amounts.taxRate.toFixed(4),
      taxAmount: amounts.taxAmount.toFixed(2),
      totalPrice: amounts.totalPrice.toFixed(2),
    }).toEqual({
      unitPrice: '10.00',
      basePrice: '20.00',
      discountRate: '0.0000',
      discountAmount: '0.00',
      subtotalPrice: '20.00',
      taxRate: '0.1000',
      taxAmount: '2.00',
      totalPrice: '22.00',
    });
  }
});
