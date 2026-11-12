package com.mycompany.myshop.testkit.dsl.core.scenario.given;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario-local mapping from customer aliases to test customers.
 * <p>
 * The default customer (the no-argument forms) is always customer1. When a scenario uses the default customer,
 * customer1 is reserved for it and aliases are assigned from the remaining customers; otherwise aliases are
 * assigned from customer1 onwards. A scenario with a new customer has that one as its default customer, so its aliases
 * are assigned from customer1 onwards too. Within the pool, aliases are assigned in order of first use, and the same alias
 * always resolves to the same customer.
 */
class CustomerAliases {
    private final List<String> aliases = new ArrayList<>();
    private boolean defaultCustomerUsed;
    private boolean newDefaultCustomer;

    void register(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Customer alias must not be blank");
        }
        if (!aliases.contains(alias)) {
            aliases.add(alias);
        }
    }

    void reserveDefaultCustomer() {
        defaultCustomerUsed = true;
    }

    /** The default customer is a new one, not customer1, so customer1 is free for aliases. */
    void useNewDefaultCustomer() {
        newDefaultCustomer = true;
    }

    UserIdentity defaultCustomer() {
        return UserIdentity.customer(0);
    }

    UserIdentity resolve(String alias) {
        var index = aliases.indexOf(alias);
        if (index < 0) {
            throw new IllegalStateException("Customer alias '" + alias + "' was never declared in this scenario");
        }
        var offset = defaultCustomerUsed && !newDefaultCustomer ? 1 : 0;
        var customerIndex = index + offset;
        if (customerIndex >= UserIdentity.CUSTOMER_COUNT) {
            throw new IllegalStateException("Cannot resolve customer alias '" + alias + "': the scenario needs "
                    + (aliases.size() + offset) + " distinct shared customers but only "
                    + UserIdentity.CUSTOMER_COUNT + " test customers exist");
        }
        return UserIdentity.customer(customerIndex);
    }
}
