# Business Rules: Publish Coupon

Use case: [UC-004 Publish Coupon](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. The discount rate is a fraction of the price: greater than 0 and at most 1 (0.15 means 15% off, 1.00 means 100% off).
- BR-2. Valid from and valid to are optional. With no valid-from time the coupon is valid immediately; with no valid-to time it never expires. Validity is not checked when the coupon is published: it is checked against the Clock each time an order uses the coupon (UC-001 Place Order).
- BR-3. The usage limit is optional and, when given, is a positive whole number. With no usage limit the coupon can be used any number of times.
- BR-4. A published coupon is stored with exactly the code, discount rate, valid-from time, valid-to time and usage limit the admin entered, and a used count of 0. Coupon codes are unique.
