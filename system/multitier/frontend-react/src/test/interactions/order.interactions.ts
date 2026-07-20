// Pact interaction builders — one home per request/response shape and its matcher policy.
//
// Only the latest/ suite imports these. The legacy/ suite hand-writes the same interactions
// inline, on purpose: it is the "before" state the maintainable-contract-tests article refactors
// away from, so extracting it here would erase what that article is teaching.
//
// Both suites write into the SAME pact file (same consumer + provider), and Pact merges by
// interaction description — a merge is only idempotent when the two sides emit byte-identical
// interactions. So any edit here must be mirrored in the legacy/ copies, or the pact file ends up
// with two conflicting versions of one interaction. That coupling is precisely the cost these
// builders exist to remove, and the legacy suite is the one still paying it.
import { MatchersV3 } from '@pact-foundation/pact';
import type { V3Interaction } from '@pact-foundation/pact';
import { OrderStatus } from '../../types/api.types';

const { like, eachLike, integer, decimal } = MatchersV3;

interface PlaceOrderParams {
  sku: string;
  quantity: number;
  country: string;
  couponCode?: string;
  orderNumber?: string;
}

export function placeOrderInteraction({ sku, quantity, country, couponCode, orderNumber = 'ORD-1' }: PlaceOrderParams): V3Interaction {
  const body: Record<string, unknown> = { sku, quantity, country };
  if (couponCode) body.couponCode = couponCode;

  const label = couponCode
    ? `a place-order request for ${sku} qty ${quantity} from ${country} with coupon ${couponCode}`
    : `a place-order request for ${sku} qty ${quantity} from ${country}`;

  const states: { description: string }[] = [{ description: `product ${sku} exists and ${country} is taxable` }];
  if (couponCode) states.push({ description: `coupon ${couponCode} exists` });

  return {
    states,
    uponReceiving: label,
    withRequest: {
      method: 'POST',
      path: '/api/orders',
      headers: { 'Content-Type': 'application/json' },
      body,
    },
    willRespondWith: {
      status: 201,
      headers: { 'Content-Type': 'application/json' },
      body: { orderNumber: like(orderNumber) },
    },
  };
}

export function browseOrderHistoryInteraction(): V3Interaction {
  return {
    states: [{ description: 'at least one order exists' }],
    uponReceiving: 'a browse-order-history request',
    withRequest: {
      method: 'GET',
      path: '/api/orders',
    },
    willRespondWith: {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
      body: {
        orders: eachLike({
          orderNumber: like('ORD-1'),
          orderTimestamp: like('2026-03-10T12:00:00Z'),
          country: like('US'),
          sku: like('BOOK-123'),
          quantity: integer(2),
          totalPrice: decimal(22),
          appliedCouponCode: null,
          status: like('PLACED'),
        }),
      },
    },
  };
}

export function viewOrderDetailsInteraction(
  orderNumber: string,
  status: OrderStatus = OrderStatus.PLACED,
): V3Interaction {
  // Each status has its own provider state so verification seeds a real order in that state.
  return {
    states: [{ description: `order ${orderNumber} is ${status.toLowerCase()}` }],
    uponReceiving: `a view-order-details request for ${orderNumber} (${status.toLowerCase()})`,
    withRequest: { method: 'GET', path: `/api/orders/${orderNumber}` },
    willRespondWith: {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
      body: {
        orderNumber,
        orderTimestamp: like('2026-03-10T12:00:00Z'),
        country: like('US'),
        sku: like('BOOK-123'),
        quantity: integer(2),
        unitPrice: decimal(10),
        basePrice: decimal(20),
        discountRate: decimal(0),
        discountAmount: decimal(0),
        subtotalPrice: decimal(20),
        taxRate: decimal(0.1),
        taxAmount: decimal(2),
        totalPrice: decimal(22),
        appliedCouponCode: null,
        // Exact (not like): the frontend BRANCHES on this value, so the contract must fail if the
        // backend ever stops returning this exact status — that's the drift the branch depends on.
        status,
      },
    },
  };
}

// Cancelling answers with 204 and no body, so what the frontend depends on is the STATUS CODE and
// nothing else: it turns a 2xx into "Order has been cancelled successfully" and re-reads the order.
export function cancelOrderInteraction(orderNumber: string): V3Interaction {
  return {
    states: [{ description: `order ${orderNumber} is placed` }],
    uponReceiving: `a cancel-order request for ${orderNumber}`,
    withRequest: { method: 'POST', path: `/api/orders/${orderNumber}/cancel` },
    willRespondWith: { status: 204 },
  };
}

// The rejection a user CAN provoke from the screen. Cancelling an already-cancelled order can't be
// reached this way — the button is hidden for CANCELLED orders, which is what the status-gating
// spec asserts — so the blackout is the one cancel rejection the UI has to render.
export function cancelOrderBlackoutInteraction(orderNumber: string): V3Interaction {
  return {
    states: [{ description: 'order cancellation is blocked by the New Year blackout' }],
    uponReceiving: `a cancel-order request for ${orderNumber} during the blackout`,
    withRequest: { method: 'POST', path: `/api/orders/${orderNumber}/cancel` },
    willRespondWith: {
      status: 422,
      headers: { 'Content-Type': 'application/problem+json' },
      body: {
        status: 422,
        detail: 'Order cancellation is not allowed on December 31st between 22:00 and 23:00',
      },
    },
  };
}

export function viewMissingOrderInteraction(orderNumber: string): V3Interaction {
  return {
    states: [{ description: `no order ${orderNumber} exists` }],
    uponReceiving: `a view-order-details request for a missing order`,
    withRequest: { method: 'GET', path: `/api/orders/${orderNumber}` },
    willRespondWith: {
      status: 404,
      headers: { 'Content-Type': 'application/problem+json' },
      body: { status: 404, detail: like('Order not found') },
    },
  };
}

// A rejection that carries errors[] — the shape the frontend turns into the per-field
// messages under the notification banner. detail and errors[] are matched EXACTLY, not
// like(): the frontend renders both verbatim ("couponCode: Coupon code X does not exist"),
// so if the backend reworded either, the contract must break rather than shrug.
export function placeOrderUnknownCouponInteraction(couponCode: string): V3Interaction {
  return {
    states: [{ description: `coupon ${couponCode} does not exist` }],
    uponReceiving: `a place-order request with the unknown coupon ${couponCode}`,
    withRequest: {
      method: 'POST',
      path: '/api/orders',
      headers: { 'Content-Type': 'application/json' },
      body: { sku: 'BOOK-123', quantity: 2, country: 'US', couponCode },
    },
    willRespondWith: {
      status: 422,
      headers: { 'Content-Type': 'application/problem+json' },
      body: {
        status: 422,
        detail: 'The request contains one or more validation errors',
        errors: [{ field: 'couponCode', message: `Coupon code ${couponCode} does not exist` }],
      },
    },
  };
}

// Two more rejections the frontend cannot reach on its own: whether the SKU is a real product
// and whether the country is taxable are the backend's to know (ERP and Tax answer them). Each
// carries its own errors[] field name, and the frontend renders "<field>: <message>" against
// that name — so a backend that renamed the field would silently mis-render, and each field the
// UI renders is pinned by its own interaction.
export function placeOrderUnknownSkuInteraction(sku: string): V3Interaction {
  return {
    states: [{ description: `product ${sku} does not exist` }],
    uponReceiving: `a place-order request for the unknown SKU ${sku}`,
    withRequest: {
      method: 'POST',
      path: '/api/orders',
      headers: { 'Content-Type': 'application/json' },
      body: { sku, quantity: 2, country: 'US' },
    },
    willRespondWith: {
      status: 422,
      headers: { 'Content-Type': 'application/problem+json' },
      body: {
        status: 422,
        detail: 'The request contains one or more validation errors',
        errors: [{ field: 'sku', message: `Product does not exist for SKU: ${sku}` }],
      },
    },
  };
}

export function placeOrderUntaxedCountryInteraction(country: string): V3Interaction {
  return {
    states: [{ description: `product BOOK-123 exists and ${country} is not taxable` }],
    uponReceiving: `a place-order request from the untaxed country ${country}`,
    withRequest: {
      method: 'POST',
      path: '/api/orders',
      headers: { 'Content-Type': 'application/json' },
      body: { sku: 'BOOK-123', quantity: 2, country },
    },
    willRespondWith: {
      status: 422,
      headers: { 'Content-Type': 'application/problem+json' },
      body: {
        status: 422,
        detail: 'The request contains one or more validation errors',
        errors: [{ field: 'country', message: `Country does not exist: ${country}` }],
      },
    },
  };
}

export function placeOrderBlackoutInteraction(): V3Interaction {
  return {
    states: [{ description: 'order placement is blocked by the New Year blackout' }],
    uponReceiving: 'a place-order request during the blackout',
    withRequest: {
      method: 'POST',
      path: '/api/orders',
      headers: { 'Content-Type': 'application/json' },
      body: { sku: 'BOOK-123', quantity: 2, country: 'US' },
    },
    willRespondWith: {
      status: 422,
      headers: { 'Content-Type': 'application/problem+json' },
      body: { status: 422, detail: like('Orders cannot be placed on December 31') },
    },
  };
}
