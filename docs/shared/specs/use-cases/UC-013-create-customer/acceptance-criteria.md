---
id: UC-013
title: Create Customer
status: implemented
---

# UC-013 Create Customer — Acceptance criteria

Narrative: [use-case-narrative.md](use-case-narrative.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-013.N` checks business rule BR-N. The sign-up form's own field checks (BR-1, extension 3a) are the identity provider's, so they have no scenario here.

```gherkin
Feature: Create Customer

@UC-013.1
Scenario: Should take the name and shipping country from the sign-up
  Given a visitor has signed up as "Ana Novak" with shipping country GB
  When the customer opens the profile
  Then the profile shows first name "Ana", last name "Novak" and country GB
  And the street, postal code and city are empty

@UC-013.2
Scenario: Should let the new customer place an order straight away
  Given a visitor has signed up with shipping country GB
  When the customer places a Quick order with the prefilled country
  Then the order is placed
  And the order's country is GB
```
