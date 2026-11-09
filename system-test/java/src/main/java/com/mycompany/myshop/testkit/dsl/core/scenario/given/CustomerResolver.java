package com.mycompany.myshop.testkit.dsl.core.scenario.given;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;

/** Resolves the scenario's customer aliases to test customers, for steps that name a customer after setup. */
public interface CustomerResolver {
    /** The default customer (customer1). */
    UserIdentity defaultCustomer();

    /** The customer behind the alias; an alias not seen before is declared now. */
    UserIdentity customer(String alias);

    /** A resolver for a scenario without a given stage: aliases are assigned from customer1 onwards. */
    static CustomerResolver standalone() {
        var aliases = new CustomerAliases();
        return new CustomerResolver() {
            @Override
            public UserIdentity defaultCustomer() {
                return aliases.defaultCustomer();
            }

            @Override
            public UserIdentity customer(String alias) {
                aliases.register(alias);
                return aliases.resolve(alias);
            }
        };
    }
}
