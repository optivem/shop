---
id: UC-005
title: Browse Coupons
status: planned
---

# UC-005 Browse Coupons — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-005.N` for N = 1 to 4 checks business rule BR-N. Tags from `@UC-005.5` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Browse Coupons

Background:
  Given the user is logged in as an admin

@UC-005.1
Scenario: Should list every coupon, newest first
  Given these coupons were published, in this order:
    | code        | discount rate | valid from           | valid to             | usage limit |
    | SUMMER2025  | 0.15          | 2025-06-01T00:00:00Z | 2025-08-31T23:59:59Z | 100         |
    | SAVE10      | 0.10          |                      |                      |             |
    | LIMITED2026 | 0.20          | 2026-01-01T00:00:00Z |                      | 2           |
  When the admin browses the coupons
  Then the coupon list shows coupons LIMITED2026, SAVE10, SUMMER2025

@UC-005.2
Scenario: Should show the details of each coupon
  Given a coupon "SUMMER2025" with discount rate 0.15, valid from 2025-06-01T00:00:00Z, valid to 2025-08-31T23:59:59Z and usage limit 100
  When the admin browses the coupons
  Then coupon SUMMER2025 shows discount rate "15.00%", valid from 2025-06-01T00:00:00Z, valid to 2025-08-31T23:59:59Z, usage limit 100 and used count 0

@UC-005.2
Scenario: Should show a coupon without validity period or usage limit as open-ended
  Given a coupon "SAVE10" with discount rate 0.10 and no valid-from time, valid-to time or usage limit
  When the admin browses the coupons
  Then coupon SAVE10 shows valid from "Immediate", valid to "Never" and usage limit "Unlimited"

@UC-005.3
Scenario Outline: Should read the coupons one page at a time
  Given these coupons were published, in this order:
    | code        | discount rate | valid from           | valid to             | usage limit |
    | SUMMER2025  | 0.15          | 2025-06-01T00:00:00Z | 2025-08-31T23:59:59Z | 100         |
    | SAVE10      | 0.10          |                      |                      |             |
    | LIMITED2026 | 0.20          | 2026-01-01T00:00:00Z |                      | 2           |
  When the admin opens page <page> of the coupons with page size 2
  Then the coupon list shows coupons <coupons>
  And the total number of coupons is 3

  Examples:
    | page | coupons             |
    | 1    | LIMITED2026, SAVE10 |
    | 2    | SUMMER2025          |
    | 3    | (none)              |

@UC-005.4
Scenario: Should show how many times a coupon has been used
  Given a coupon "LIMITED2026" with usage limit 2
  And 2 orders have used coupon "LIMITED2026"
  When the admin browses the coupons
  Then coupon LIMITED2026 shows used count 2

@UC-005.5 @narrative-2a
Scenario: Customer should not be able to browse coupons
  Given the user is logged in as a customer
  When the user browses the coupons
  Then the request is rejected
  And the user is told "You do not have permission to perform this action"

@UC-005.6 @narrative-3a
Scenario Outline: Should reject a page or page size out of range
  When the admin opens page <page> of the coupons with page size <size>
  Then the request is rejected
  And the admin is told "<message>" for field <field>

  Examples:
    | page | size | field | message                             |
    | 0    | 50   | page  | Page must be 1 or greater           |
    | 1    | 101  | size  | Page size must be between 1 and 100 |

@UC-005.7 @narrative-3b
Scenario: Should tell the admin when there are no coupons
  Given no coupon has been published
  When the admin browses the coupons
  Then the coupon list shows no coupons
  And the admin is told "No coupons found"
```
