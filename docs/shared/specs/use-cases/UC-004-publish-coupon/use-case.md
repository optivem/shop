---
id: UC-004
title: Publish Coupon
---

# UC-004 Publish Coupon

**Primary actor:** Admin
**Secondary actors:** none (the coupon's validity period is checked against the Clock later, when a customer uses the coupon in UC-001 Place Order)

## Main Success Scenario
1. The admin opens Coupon Management and enters a coupon code, a discount rate and, optionally, a valid-from time, a valid-to time and a usage limit.
2. The system checks that the user is an admin.
3. The system validates the code, the discount rate and the usage limit ([BR-1 to BR-3](business-rules.md)).
4. The system checks that no coupon has this code yet.
5. The system stores the coupon with a used count of 0 ([BR-4](business-rules.md)).
6. The system tells the admin the coupon was created, and the coupon appears in the coupon list (UC-005 Browse Coupons).

## Extensions
- 2a. The user is a customer:
    - The system tells the user they don't have permission. The use case ends.
- 3a. The coupon code is missing or blank:
    - The system tells the admin the coupon code must not be blank. Resume at step 1.
- 3b. The discount rate is 0 or less, or more than 1:
    - The system tells the admin the discount rate must be greater than 0.00 and at most 1.00. Resume at step 1.
- 3c. The usage limit is 0 or less:
    - The system tells the admin the usage limit must be positive. Resume at step 1.
- 4a. A coupon with this code already exists:
    - The system tells the admin the coupon code already exists. Resume at step 1.

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
