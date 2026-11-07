---
id: UC-005
title: Browse Coupons
---

# UC-005 Browse Coupons

**Primary actor:** Admin
**Secondary actors:** none

## Main Success Scenario
1. The admin opens Coupon Management.
2. The system checks that the user is an admin.
3. The system shows the coupons, newest published first, one page at a time ([BR-1](business-rules.md), [BR-3](business-rules.md)).
4. For each coupon the system shows its code, discount rate, validity period, usage limit and how many times it has been used ([BR-2](business-rules.md), [BR-4](business-rules.md)).
5. The admin can publish another coupon (UC-004 Publish Coupon) or refresh the list.

## Extensions
- 2a. The user is a customer:
    - The system tells the user they don't have permission. The use case ends.
- 3a. The requested page is below 1, or the page size is not between 1 and 100:
    - The system rejects the request and says which value is wrong. Resume at step 1.
- 3b. No coupon has been published yet:
    - The system tells the admin no coupons were found. The admin can publish one (UC-004 Publish Coupon).

## Acceptance criteria

See [acceptance-criteria.md](acceptance-criteria.md).
