import Decimal from 'decimal.js';
import type { EntityManager, Repository } from 'typeorm';
import { Order } from '../entities/order.entity';
import { OrderStatus } from '../entities/order-status.enum';
import { PlaceOrderRequest } from '../dtos/place-order-request.dto';
import { ValidationException } from '../exceptions/validation.exception';
import { ConcurrentModificationException } from '../exceptions/concurrent-modification.exception';
import { NotExistValidationException } from '../exceptions/not-exist-validation.exception';
import { OrderService } from './order.service';
import type { ErpGateway } from './external/erp.gateway';
import type { TaxGateway } from './external/tax.gateway';
import type { ClockGateway } from './external/clock.gateway';
import type { CouponService } from './coupon.service';
import { IdempotencyKeyStore } from './idempotency-key.store';
import { IdempotencyKey } from '../entities/idempotency-key.entity';
import { QueryFailedError } from 'typeorm';
import type { CurrentUser } from '../../auth/current-user';

const CUSTOMER: CurrentUser = {
  subject: 'sub-customer',
  username: 'customer1',
  admin: false,
};
const OTHER_CUSTOMER: CurrentUser = {
  subject: 'sub-other',
  username: 'customer2',
  admin: false,
};
const ADMIN: CurrentUser = {
  subject: 'sub-admin',
  username: 'admin1',
  admin: true,
};

const NORMAL_TIME = new Date('2025-06-15T10:00:00Z');
const DEC_31_YEAR_END_BLACKOUT = new Date('2025-12-31T23:59:00Z');
const DEC_31_CANCEL_BLACKOUT = new Date('2025-12-31T22:15:00Z');

describe('OrderService', () => {
  let service: OrderService;
  let orderRepository: jest.Mocked<
    Pick<Repository<Order>, 'save' | 'update' | 'findOne' | 'find' | 'count'>
  > & {
    manager: Pick<EntityManager, 'transaction' | 'withRepository'>;
  };
  let erpGateway: jest.Mocked<
    Pick<ErpGateway, 'getProductDetails' | 'getPromotionDetails'>
  >;
  let clockGateway: jest.Mocked<Pick<ClockGateway, 'getCurrentTime'>>;
  let taxGateway: jest.Mocked<Pick<TaxGateway, 'getTaxDetails'>>;
  let couponService: jest.Mocked<Pick<CouponService, 'getDiscount'>>;
  let idempotencyKeys: jest.Mocked<
    Pick<IdempotencyKeyStore, 'find' | 'findLive' | 'claim'>
  >;

  beforeEach(() => {
    orderRepository = {
      save: jest.fn(),
      update: jest.fn().mockResolvedValue({ affected: 1 }),
      findOne: jest.fn(),
      find: jest.fn(),
      count: jest.fn().mockResolvedValue(1),
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
    idempotencyKeys = {
      find: jest.fn().mockResolvedValue(null),
      findLive: jest.fn().mockResolvedValue(null),
      claim: jest.fn().mockResolvedValue(undefined),
    };

    service = new OrderService(
      orderRepository as unknown as Repository<Order>,
      erpGateway as unknown as ErpGateway,
      clockGateway as unknown as ClockGateway,
      taxGateway as unknown as TaxGateway,
      couponService as unknown as CouponService,
      idempotencyKeys as unknown as IdempotencyKeyStore,
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
        CUSTOMER,
      );

      expect(response.orderNumber).toMatch(/^ORD-/);
      assertSavedOrder(response.orderNumber);
    });

    it('throws when ordered on year-end blackout', async () => {
      clockGateway.getCurrentTime.mockResolvedValue(DEC_31_YEAR_END_BLACKOUT);

      await expect(
        service.placeOrder(buildRequest('BOOK-123', 1, 'US'), CUSTOMER),
      ).rejects.toThrow('December 31');
    });

    it('throws when SKU is unknown', async () => {
      givenNormalTime();
      erpGateway.getProductDetails.mockResolvedValue(null);

      const error = await service
        .placeOrder(buildRequest('UNKNOWN', 1, 'US'), CUSTOMER)
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
        .placeOrder(buildRequest('BOOK-123', 1, 'XX'), CUSTOMER)
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
      expect(orderRepository.update).toHaveBeenCalled();
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

      await service.cancelOrder('ORD-001', CUSTOMER);

      expect(order.status).toBe(OrderStatus.CANCELLED);
      expect(orderRepository.update).toHaveBeenCalled();
    });

    it('reports a concurrent modification when the version no longer matches', async () => {
      givenNormalTime();
      orderRepository.findOne.mockResolvedValue(placedOrder('ORD-001'));
      orderRepository.update.mockResolvedValue({ affected: 0 } as never);

      await expect(
        service.cancelOrder('ORD-001', CUSTOMER),
      ).rejects.toBeInstanceOf(ConcurrentModificationException);
    });

    it('throws during December 31 cancellation blackout', async () => {
      clockGateway.getCurrentTime.mockResolvedValue(DEC_31_CANCEL_BLACKOUT);

      await expect(service.cancelOrder('ORD-001', CUSTOMER)).rejects.toThrow(
        'December 31',
      );
    });

    it('throws when order already cancelled', async () => {
      givenNormalTime();
      const order = placedOrder('ORD-001');
      order.status = OrderStatus.CANCELLED;
      orderRepository.findOne.mockResolvedValue(order);

      await expect(service.cancelOrder('ORD-001', CUSTOMER)).rejects.toThrow(
        'already been cancelled',
      );
    });
  });

  describe('ownership', () => {
    it("reports another customer's order as non-existent on view and cancel", async () => {
      givenNormalTime();
      orderRepository.findOne.mockResolvedValue(placedOrder('ORD-001'));

      await expect(service.getOrder('ORD-001', OTHER_CUSTOMER)).rejects.toThrow(
        'Order ORD-001 does not exist.',
      );
      await expect(
        service.cancelOrder('ORD-001', OTHER_CUSTOMER),
      ).rejects.toBeInstanceOf(NotExistValidationException);
    });

    it('lets an admin view and cancel any order, including one without an owner', async () => {
      givenNormalTime();
      const order = placedOrder('ORD-001');
      order.owner = null;
      orderRepository.findOne.mockResolvedValue(order);
      orderRepository.save.mockResolvedValue({} as Order);

      await expect(service.getOrder('ORD-001', ADMIN)).resolves.toBeDefined();
      await service.cancelOrder('ORD-001', ADMIN);
      expect(order.status).toBe(OrderStatus.CANCELLED);
    });

    it('hides an order without an owner from customers', async () => {
      const order = placedOrder('ORD-001');
      order.owner = null;
      orderRepository.findOne.mockResolvedValue(order);

      await expect(
        service.getOrder('ORD-001', CUSTOMER),
      ).rejects.toBeInstanceOf(NotExistValidationException);
    });

    it("filters a customer's history by owner and shows the customer to admins", async () => {
      orderRepository.find.mockResolvedValue([placedOrder('ORD-001')]);

      await service.browseOrderHistory(undefined, CUSTOMER);
      expect(orderRepository.find).toHaveBeenLastCalledWith(
        expect.objectContaining({ where: { owner: CUSTOMER.subject } }),
      );

      const result = await service.browseOrderHistory(undefined, ADMIN);
      expect(orderRepository.find).toHaveBeenLastCalledWith(
        expect.objectContaining({ where: {} }),
      );
      expect(result.orders[0]?.customer).toBe('customer1');
    });
  });

  describe('placeOrder with an Idempotency-Key', () => {
    beforeEach(() => {
      givenNormalTime();
      givenProductExists('BOOK-123', 10);
      givenNoPromotion();
      givenNoDiscount();
      givenTaxRate('US', 0.1);
      orderRepository.save.mockResolvedValue({} as Order);
    });

    it('places the order and claims the key in the same transaction', async () => {
      const response = await service.placeOrder(
        buildRequest('BOOK-123', 2, 'US'),
        CUSTOMER,
        'key-1',
      );

      expect(idempotencyKeys.claim).toHaveBeenCalledWith(
        {
          owner: CUSTOMER.subject,
          key: 'key-1',
          fingerprint: IdempotencyKeyStore.fingerprint(
            buildRequest('BOOK-123', 2, 'US'),
          ),
        },
        201,
        JSON.stringify({ orderNumber: response.orderNumber }),
        expect.any(Date),
        expect.anything(),
      );
      expect(orderRepository.save).toHaveBeenCalledTimes(1);
    });

    it.each([undefined, '', '   '])(
      'treats a missing or blank key (%p) as no key',
      async (key) => {
        await service.placeOrder(
          buildRequest('BOOK-123', 2, 'US'),
          CUSTOMER,
          key,
        );

        expect(idempotencyKeys.findLive).not.toHaveBeenCalled();
        expect(idempotencyKeys.claim).not.toHaveBeenCalled();
        expect(orderRepository.save).toHaveBeenCalledTimes(1);
      },
    );

    it('replays the stored response for the same key and request', async () => {
      idempotencyKeys.findLive.mockResolvedValue(
        storedKey(buildRequest('BOOK-123', 2, 'US'), 'ORD-ORIGINAL'),
      );

      const response = await service.placeOrder(
        buildRequest('BOOK-123', 2, 'US'),
        CUSTOMER,
        'key-1',
      );

      expect(response.orderNumber).toBe('ORD-ORIGINAL');
      expect(orderRepository.save).not.toHaveBeenCalled();
      expect(erpGateway.getProductDetails).not.toHaveBeenCalled();
    });

    it('rejects the same key with a different request', async () => {
      idempotencyKeys.findLive.mockResolvedValue(
        storedKey(buildRequest('BOOK-123', 1, 'US'), 'ORD-ORIGINAL'),
      );

      const error = await service
        .placeOrder(buildRequest('BOOK-123', 2, 'US'), CUSTOMER, 'key-1')
        .catch((e: unknown) => e);

      expect(error).toBeInstanceOf(ValidationException);
      expect((error as ValidationException).fieldName).toBeNull();
      expect((error as ValidationException).message).toBe(
        'Idempotency-Key has already been used with a different request',
      );
      expect(orderRepository.save).not.toHaveBeenCalled();
    });

    it('rejects a key longer than 255 characters on the header field', async () => {
      const error = await service
        .placeOrder(
          buildRequest('BOOK-123', 2, 'US'),
          CUSTOMER,
          'k'.repeat(256),
        )
        .catch((e: unknown) => e);

      expect(error).toBeInstanceOf(ValidationException);
      expect((error as ValidationException).fieldName).toBe('Idempotency-Key');
      expect((error as ValidationException).message).toBe(
        'Idempotency-Key must be at most 255 characters',
      );
    });

    it('answers from the winning row when a concurrent request claimed the key first', async () => {
      idempotencyKeys.claim.mockRejectedValue(uniqueViolation());
      idempotencyKeys.find.mockResolvedValue(
        storedKey(buildRequest('BOOK-123', 2, 'US'), 'ORD-WINNER'),
      );

      const response = await service.placeOrder(
        buildRequest('BOOK-123', 2, 'US'),
        CUSTOMER,
        'key-1',
      );

      expect(response.orderNumber).toBe('ORD-WINNER');
      expect(orderRepository.save).not.toHaveBeenCalled();
    });

    it('rethrows a failure that is not a key conflict, so the key stays free', async () => {
      orderRepository.save.mockRejectedValue(new Error('insert failed'));

      await expect(
        service.placeOrder(
          buildRequest('BOOK-123', 2, 'US'),
          CUSTOMER,
          'key-1',
        ),
      ).rejects.toThrow('insert failed');
      expect(idempotencyKeys.find).not.toHaveBeenCalled();
    });
  });

  describe('browseOrderHistory paging', () => {
    it('defaults to the first page of 50, newest first, and reports the totals', async () => {
      orderRepository.count.mockResolvedValue(120);
      orderRepository.find.mockResolvedValue([placedOrder('ORD-001')]);

      const result = await service.browseOrderHistory(undefined, ADMIN);

      expect(orderRepository.find).toHaveBeenLastCalledWith(
        expect.objectContaining({
          order: { orderTimestamp: 'DESC', id: 'DESC' },
          skip: 0,
          take: 50,
        }),
      );
      expect(result).toMatchObject({
        page: 1,
        size: 50,
        totalElements: 120,
        totalPages: 3,
      });
    });

    it('skips the row query for a page past the end', async () => {
      orderRepository.count.mockResolvedValue(3);

      const result = await service.browseOrderHistory(
        undefined,
        ADMIN,
        '2',
        '10',
      );

      expect(orderRepository.find).not.toHaveBeenCalled();
      expect(result.orders).toEqual([]);
      expect(result).toMatchObject({ page: 2, size: 10, totalPages: 1 });
    });

    it.each([
      ['0', undefined, 'page', 'Page must be 1 or greater'],
      ['abc', undefined, 'page', 'Page must be 1 or greater'],
      [undefined, '0', 'size', 'Page size must be between 1 and 100'],
      [undefined, '101', 'size', 'Page size must be between 1 and 100'],
    ])(
      'rejects page %p / size %p on field %s',
      async (page, size, field, message) => {
        const error = await service
          .browseOrderHistory(undefined, ADMIN, page, size)
          .catch((e: unknown) => e);

        expect(error).toBeInstanceOf(ValidationException);
        expect((error as ValidationException).fieldName).toBe(field);
        expect((error as ValidationException).message).toBe(message);
      },
    );
  });

  function storedKey(
    request: PlaceOrderRequest,
    orderNumber: string,
  ): IdempotencyKey {
    const row = new IdempotencyKey();
    row.owner = CUSTOMER.subject;
    row.key = 'key-1';
    row.requestHash = IdempotencyKeyStore.fingerprint(request);
    row.responseStatus = 201;
    row.responseBody = JSON.stringify({ orderNumber });
    row.createdAt = NORMAL_TIME;
    return row;
  }

  function uniqueViolation(): QueryFailedError {
    return new QueryFailedError('INSERT', [], {
      code: '23505',
      constraint: 'uq_idempotency_keys_owner_key',
    } as unknown as Error);
  }

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
    order.owner = CUSTOMER.subject;
    order.ownerName = CUSTOMER.username;
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
        owner: CUSTOMER.subject,
        ownerName: CUSTOMER.username,
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
