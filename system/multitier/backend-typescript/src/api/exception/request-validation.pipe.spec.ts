import { type ArgumentMetadata, BadRequestException } from '@nestjs/common';
import Decimal from 'decimal.js';
import { PlaceOrderRequest } from '../../core/dtos/place-order-request.dto';
import { PublishCouponRequest } from '../../core/dtos/publish-coupon-request.dto';
import { UpdateProfileRequest } from '../../core/dtos/update-profile-request.dto';
import { RequestValidationPipe } from './request-validation.pipe';
import { RequestValidationException } from './request-validation.exception';

const pipe = new RequestValidationPipe();

function parse(metatype: ArgumentMetadata['metatype'], body: unknown) {
  return pipe.transform(body, { type: 'body', metatype });
}

async function fieldErrors(
  metatype: ArgumentMetadata['metatype'],
  body: unknown,
) {
  const error = await parse(metatype, body).catch((e: unknown) => e);
  if (!(error instanceof RequestValidationException)) {
    throw new Error('Expected a RequestValidationException');
  }
  return error.fieldErrors.map(({ field, message, code }) => ({
    field,
    message,
    code,
  }));
}

const validOrder = { sku: 'BOOK-123', quantity: 2, country: 'US' };
const validCoupon = { code: 'SAVE10', discountRate: '0.10' };
const validAddress = {
  firstName: 'Ana',
  lastName: 'Novak',
  street: 'Main St 1',
  postalCode: '10115',
  city: 'Berlin',
  country: 'DE',
};
const requiredMessages: [string, string][] = [
  ['firstName', 'First name is required'],
  ['lastName', 'Last name is required'],
  ['street', 'Street is required'],
  ['postalCode', 'Postal code is required'],
  ['city', 'City is required'],
  ['country', 'Country is required'],
];

describe('request validation pipe', () => {
  describe('PlaceOrderRequest', () => {
    it('accepts numeric strings and drops unknown fields', async () => {
      await expect(
        parse(PlaceOrderRequest, { ...validOrder, quantity: '5', extra: 1 }),
      ).resolves.toEqual(
        Object.assign(new PlaceOrderRequest(), { ...validOrder, quantity: 5 }),
      );
    });

    it.each([undefined, null, '', '   '])(
      'reports an empty quantity (%p)',
      async (quantity) => {
        expect(
          await fieldErrors(PlaceOrderRequest, { ...validOrder, quantity }),
        ).toEqual([
          {
            field: 'quantity',
            message: 'Quantity must not be empty',
            code: null,
          },
        ]);
      },
    );

    it.each(['3.5', 'lala', -3.5, true, {}])(
      'reports a non-integer quantity as a type mismatch (%p)',
      async (quantity) => {
        expect(
          await fieldErrors(PlaceOrderRequest, { ...validOrder, quantity }),
        ).toEqual([
          {
            field: 'quantity',
            message: 'Quantity must be an integer',
            code: 'TYPE_MISMATCH',
          },
        ]);
      },
    );

    it.each([0, -10, '-1'])(
      'reports a non-positive quantity (%p)',
      async (quantity) => {
        expect(
          await fieldErrors(PlaceOrderRequest, { ...validOrder, quantity }),
        ).toEqual([
          {
            field: 'quantity',
            message: 'Quantity must be positive',
            code: null,
          },
        ]);
      },
    );

    it('reports every invalid field, one error each', async () => {
      expect(
        await fieldErrors(PlaceOrderRequest, {
          sku: '',
          quantity: null,
          country: 5,
          couponCode: 7,
        }),
      ).toEqual([
        { field: 'sku', message: 'SKU must not be empty', code: null },
        {
          field: 'quantity',
          message: 'Quantity must not be empty',
          code: null,
        },
        {
          field: 'country',
          message: 'Country must not be empty',
          code: 'TYPE_MISMATCH',
        },
        {
          field: 'couponCode',
          message: 'Coupon code must be a string',
          code: 'TYPE_MISMATCH',
        },
      ]);
    });

    it('rejects a body that is not an object as a bad request', async () => {
      await expect(parse(PlaceOrderRequest, [])).rejects.toBeInstanceOf(
        BadRequestException,
      );
    });
  });

  describe('PublishCouponRequest', () => {
    it('parses the discount rate exactly and optional fields to typed values', async () => {
      const request = await parse(PublishCouponRequest, {
        code: 'SAVE10',
        discountRate: '0.15',
        validFrom: '2024-06-01T00:00:00Z',
        validTo: '',
        usageLimit: '3',
      });

      expect(request).toEqual(
        Object.assign(new PublishCouponRequest(), {
          code: 'SAVE10',
          discountRate: new Decimal('0.15'),
          validFrom: new Date('2024-06-01T00:00:00Z'),
          validTo: undefined,
          usageLimit: 3,
        }),
      );
    });

    it('accepts any discount rate above zero, however small', async () => {
      await expect(
        parse(PublishCouponRequest, { ...validCoupon, discountRate: 0.00001 }),
      ).resolves.toBeInstanceOf(PublishCouponRequest);
    });

    it.each([undefined, null, ''])(
      'reports a missing discount rate (%p)',
      async (discountRate) => {
        expect(
          await fieldErrors(PublishCouponRequest, {
            ...validCoupon,
            discountRate,
          }),
        ).toEqual([
          {
            field: 'discountRate',
            message: 'Discount rate must not be null',
            code: null,
          },
        ]);
      },
    );

    it.each(['abc', true, 'NaN', 'Infinity'])(
      'reports a non-numeric discount rate as a type mismatch (%p)',
      async (discountRate) => {
        expect(
          await fieldErrors(PublishCouponRequest, {
            ...validCoupon,
            discountRate,
          }),
        ).toEqual([
          {
            field: 'discountRate',
            message: 'Discount rate must be a number',
            code: 'TYPE_MISMATCH',
          },
        ]);
      },
    );

    it.each(['0.0', '-0.01', 0])(
      'reports a zero or negative discount rate (%p)',
      async (discountRate) => {
        expect(
          await fieldErrors(PublishCouponRequest, {
            ...validCoupon,
            discountRate,
          }),
        ).toEqual([
          {
            field: 'discountRate',
            message: 'Discount rate must be greater than 0.00',
            code: null,
          },
        ]);
      },
    );

    it.each(['1.01', 2])(
      'reports a discount rate above 1 (%p)',
      async (discountRate) => {
        expect(
          await fieldErrors(PublishCouponRequest, {
            ...validCoupon,
            discountRate,
          }),
        ).toEqual([
          {
            field: 'discountRate',
            message: 'Discount rate must be at most 1.00',
            code: null,
          },
        ]);
      },
    );

    it.each(['  ', 42])(
      'reports a blank or non-string code (%p)',
      async (code) => {
        const errors = await fieldErrors(PublishCouponRequest, {
          ...validCoupon,
          code,
        });
        expect(errors.map((e) => e.message)).toEqual([
          'Coupon code must not be blank',
        ]);
      },
    );

    it('reports an invalid date as a type mismatch', async () => {
      expect(
        await fieldErrors(PublishCouponRequest, {
          ...validCoupon,
          validFrom: 'not-a-date',
        }),
      ).toEqual([
        {
          field: 'validFrom',
          message: 'Valid from must be a valid date-time',
          code: 'TYPE_MISMATCH',
        },
      ]);
    });

    it.each(['0', '-1', 2.5, 'x'])(
      'reports an invalid usage limit (%p)',
      async (usageLimit) => {
        const errors = await fieldErrors(PublishCouponRequest, {
          ...validCoupon,
          usageLimit,
        });
        expect(errors.map((e) => e.field)).toEqual(['usageLimit']);
      },
    );
  });

  describe('PlaceOrderRequest with a shipping address', () => {
    const addressOrder = { sku: 'BOOK-123', quantity: 1 };

    it('does not require the top-level country and trims the address', async () => {
      const parsed = (await parse(PlaceOrderRequest, {
        ...addressOrder,
        shippingAddress: { ...validAddress, city: '  Berlin  ' },
      })) as PlaceOrderRequest;
      expect(parsed.country).toBeUndefined();
      expect(parsed.shippingAddress).toEqual(
        expect.objectContaining({ ...validAddress, city: 'Berlin' }),
      );
    });

    it.each(requiredMessages)(
      'reports an empty shippingAddress.%s',
      async (field, message) => {
        for (const value of [undefined, null, '', '   ']) {
          expect(
            await fieldErrors(PlaceOrderRequest, {
              ...addressOrder,
              shippingAddress: { ...validAddress, [field]: value },
            }),
          ).toEqual([
            { field: `shippingAddress.${field}`, message, code: null },
          ]);
        }
      },
    );

    it('reports address errors together with the other field errors', async () => {
      const errors = await fieldErrors(PlaceOrderRequest, {
        sku: '',
        quantity: 1,
        shippingAddress: { ...validAddress, street: '', city: '' },
      });
      expect(errors.map((e) => e.field).sort()).toEqual([
        'shippingAddress.city',
        'shippingAddress.street',
        'sku',
      ]);
    });

    it('still requires the country without an address', async () => {
      expect(
        await fieldErrors(PlaceOrderRequest, {
          ...addressOrder,
          shippingAddress: null,
        }),
      ).toEqual([
        { field: 'country', message: 'Country must not be empty', code: null },
      ]);
    });
  });

  describe('UpdateProfileRequest', () => {
    it('trims every field', async () => {
      await expect(
        parse(UpdateProfileRequest, { ...validAddress, firstName: ' Ana ' }),
      ).resolves.toEqual(
        Object.assign(new UpdateProfileRequest(), validAddress),
      );
    });

    it('reports every missing field, treating only spaces as empty', async () => {
      const errors = await fieldErrors(UpdateProfileRequest, {
        firstName: '   ',
        lastName: null,
        street: '',
      });
      expect(errors).toEqual(
        expect.arrayContaining(
          requiredMessages.map(([field, message]) => ({
            field,
            message,
            code: null,
          })),
        ),
      );
      expect(errors).toHaveLength(6);
    });

    it.each(['FR', 'de', 'XX'])(
      'rejects the unsupported country %p',
      async (country) => {
        expect(
          await fieldErrors(UpdateProfileRequest, { ...validAddress, country }),
        ).toEqual([
          {
            field: 'country',
            message: 'Country must be US, DE or GB',
            code: null,
          },
        ]);
      },
    );
  });
});
