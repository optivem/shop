---
id: UC-010
title: Manage Profile
status: planned
---

# UC-010 Manage Profile — Acceptance criteria

Narrative: [use-case.md](use-case.md). Business rules: [business-rules.md](business-rules.md).

Tags: `@UC-010.N` for N = 1 to 5 checks business rule BR-N. Tags from `@UC-010.6` on check an extension, which is also tagged `@narrative-<step><letter>`.

```gherkin
Feature: Manage Profile

@UC-010.1
Scenario: Should show the saved profile
  Given the user is logged in as a customer with email "ana.novak@example.com"
  And the customer's profile is:
    | first name | last name | street    | postal code | city   | country |
    | Ana        | Novak     | Main St 1 | 10115       | Berlin | DE      |
  When the customer opens the profile
  Then the profile shows first name "Ana", last name "Novak" and email "ana.novak@example.com"
  And the profile shows shipping address "Main St 1", "10115", "Berlin", DE
  And the email can't be changed

@UC-010.1
Scenario: Should show the changed profile after saving
  Given the user is logged in as a customer
  And the customer's profile is:
    | first name | last name | street    | postal code | city   | country |
    | Ana        | Novak     | Main St 1 | 10115       | Berlin | DE      |
  When the customer changes the profile to:
    | first name | last name | street    | postal code | city   | country |
    | Ana        | Novak     | 1 High St | SW1A 1AA    | London | GB      |
  And the customer saves the profile
  Then the customer is told the profile was saved
  And the saved profile has shipping address "1 High St", "SW1A 1AA", "London", GB

@UC-010.2
Scenario Outline: Should require every name and address field
  Given the user is logged in as a customer
  When the customer saves the profile with an empty <field>
  Then the profile is not saved
  And the customer is told <field> is required

  Examples:
    | field       |
    | first name  |
    | last name   |
    | street      |
    | postal code |
    | city        |
    | country     |

@UC-010.2
Scenario: Should treat a field of only spaces as empty
  Given the user is logged in as a customer
  When the customer saves the profile with city "   "
  Then the profile is not saved
  And the customer is told city is required

@UC-010.3
Scenario: Should offer only the supported shipping countries
  Given the user is logged in as a customer
  When the customer opens the profile
  Then the country choices are US, DE and GB

@UC-010.3
Scenario: Should reject a country that is not supported
  Given the user is logged in as a customer
  When the customer saves the profile with country "FR"
  Then the profile is not saved
  And the customer is told the country must be US, DE or GB

@UC-010.4
Scenario: Should tell the customer that tax follows the shipping country
  Given the user is logged in as a customer
  When the customer opens the profile
  Then the customer is told sales tax is charged at the rate of the shipping address's country

@UC-010.5
Scenario: Should use the saved address as the default at checkout
  Given the user is logged in as a customer
  And the customer has saved the profile:
    | first name | last name | street    | postal code | city   | country |
    | Ana        | Novak     | Main St 1 | 10115       | Berlin | DE      |
  And the customer's cart has:
    | sku   | quantity |
    | HP-15 | 1        |
  When the customer opens the checkout
  Then the shipping address "Ana Novak, Main St 1, 10115 Berlin, DE" is selected

@UC-010.5
Scenario: Should prefill the Quick order country from the profile
  Given the user is logged in as a customer
  And the customer has saved a profile with country GB
  When the customer opens Quick order
  Then the Quick order country is GB

@UC-010.5
Scenario: Should keep each customer's profile private
  Given customer "ana" has saved a profile with street "Main St 1"
  When customer "bob" opens the profile
  Then the profile does not show street "Main St 1"

@UC-010.6 @narrative-1a
Scenario: Should ask an anonymous visitor to sign in and return to the profile
  Given the visitor is not signed in
  When the visitor opens the profile and signs in as a customer
  Then the customer's profile is shown

@UC-010.7 @narrative-2a
Scenario: Should show an empty profile with names from the sign-in account
  Given the user is logged in as a customer whose sign-in account has name "Ana Novak" and email "ana.novak@example.com"
  And the customer has no saved profile
  When the customer opens the profile
  Then the profile shows first name "Ana", last name "Novak" and email "ana.novak@example.com"
  And the street, postal code, city and country are empty

@UC-010.8 @narrative-5a
Scenario: Should keep the saved profile when an invalid change is rejected
  Given the user is logged in as a customer
  And the customer's profile is:
    | first name | last name | street    | postal code | city   | country |
    | Ana        | Novak     | Main St 1 | 10115       | Berlin | DE      |
  When the customer saves the profile with an empty street
  Then the profile is not saved
  And the saved profile has shipping address "Main St 1", "10115", "Berlin", DE
```
