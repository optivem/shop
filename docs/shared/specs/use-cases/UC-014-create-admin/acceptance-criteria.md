---
id: UC-014
title: Create Admin
status: planned
---

# UC-014 Create Admin — Acceptance criteria

Narrative: [use-case-narrative.md](use-case-narrative.md).

```gherkin
Feature: Create Admin

@UC-014.1
Scenario: Should let a new admin use the admin pages but not place orders
  Given an admin has created an admin account
  When the new admin signs in
  Then the new admin can browse the coupons
  And the new admin can't place an order
```
