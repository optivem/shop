---
id: UC-004
title: Publish Coupon
status: planned
---

# UC-004 Publish Coupon — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-004.N` for N = 1 to 4 checks business rule BR-N. Tags from `@UC-004.5` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Publish Coupon

Background:
  Given the user is logged in as an admin

@UC-004.1
Scenario Outline: Should accept a discount rate above 0 and up to 1
  When the admin publishes coupon "SAVE<code suffix>" with discount rate <discount rate>
  Then the coupon is published successfully
  And coupon "SAVE<code suffix>" has discount rate <discount rate>

  Examples:
    | code suffix | discount rate |
    | 1           | 0.01          |
    | 15          | 0.15          |
    | 100         | 1.00          |

@UC-004.2
Scenario: Should publish a coupon with a validity period
  When the admin publishes coupon "SUMMER2025" with discount rate 0.15, valid from 2024-06-01T00:00:00Z and valid to 2024-08-31T23:59:59Z
  Then the coupon is published successfully
  And coupon "SUMMER2025" is valid from 2024-06-01T00:00:00Z to 2024-08-31T23:59:59Z

@UC-004.2
Scenario: Should publish a coupon that is valid immediately and never expires
  When the admin publishes coupon "SUMMER2025" with discount rate 0.15 and no valid-from or valid-to time
  Then the coupon is published successfully
  And coupon "SUMMER2025" shows valid from "Immediate" and valid to "Never"

@UC-004.3
Scenario: Should publish a coupon with a usage limit
  When the admin publishes coupon "LIMITED2024" with discount rate 0.10 and usage limit 100
  Then the coupon is published successfully
  And coupon "LIMITED2024" has usage limit 100

@UC-004.3
Scenario: Should publish a coupon without a usage limit as unlimited
  When the admin publishes coupon "SAVE10" with discount rate 0.10 and no usage limit
  Then the coupon is published successfully
  And coupon "SAVE10" shows usage limit "Unlimited"

@UC-004.4
Scenario: Should save the coupon as entered, unused
  When the admin publishes coupon "SUMMER2025" with discount rate 0.15, valid from 2024-06-01T00:00:00Z, valid to 2024-08-31T23:59:00Z and usage limit 100
  Then the coupon is published successfully
  And the admin is told "Coupon 'SUMMER2025' created successfully!"
  And coupon "SUMMER2025" has discount rate 0.15
  And coupon "SUMMER2025" is valid from 2024-06-01T00:00:00Z to 2024-08-31T23:59:00Z
  And coupon "SUMMER2025" has usage limit 100
  And coupon "SUMMER2025" has been used 0 times

@UC-004.5 @narrative-2a
Scenario: Customer should not be able to publish a coupon
  Given the user is logged in as a customer
  When the user publishes coupon "SAVE10" with discount rate 0.10
  Then the coupon is rejected
  And the user is told "You do not have permission to perform this action"

@UC-004.6 @narrative-3a
Scenario Outline: Should reject a coupon with a blank code
  When the admin publishes coupon "<code>" with discount rate 0.10
  Then the coupon is rejected
  And the admin is told "Coupon code must not be blank"

  Examples:
    | code |
    |      |
    | "  " |

@UC-004.7 @narrative-3b
Scenario Outline: Should reject a discount rate that is not above 0 and up to 1
  When the admin publishes coupon "INVALID-COUPON" with discount rate <discount rate>
  Then the coupon is rejected
  And the admin is told "<message>"

  Examples:
    | discount rate | message                                 |
    | 0.0           | Discount rate must be greater than 0.00 |
    | -0.01         | Discount rate must be greater than 0.00 |
    | -0.15         | Discount rate must be greater than 0.00 |
    | 1.01          | Discount rate must be at most 1.00      |
    | 2.00          | Discount rate must be at most 1.00      |

@UC-004.8 @narrative-3c
Scenario Outline: Should reject a usage limit that is not positive
  When the admin publishes coupon "INVALID-LIMIT" with discount rate 0.15 and usage limit <usage limit>
  Then the coupon is rejected
  And the admin is told "Usage limit must be positive"

  Examples:
    | usage limit |
    | 0           |
    | -1          |
    | -100        |

@UC-004.9 @narrative-4a
Scenario: Should reject a coupon whose code already exists
  Given a coupon "EXISTING-COUPON" with discount rate 0.10
  When the admin publishes coupon "EXISTING-COUPON" with discount rate 0.20
  Then the coupon is rejected
  And the admin is told "Coupon code EXISTING-COUPON already exists"
  And coupon "EXISTING-COUPON" has discount rate 0.10
```
