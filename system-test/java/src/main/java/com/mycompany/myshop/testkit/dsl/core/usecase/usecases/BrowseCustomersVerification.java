package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BrowseCustomersVerification extends ResponseVerification<BrowseCustomersResponse> {
    public BrowseCustomersVerification(BrowseCustomersResponse response, UseCaseContext context) {
        super(response, context);
    }

    /**
     * Each of these customers (usernames) is listed exactly once, in this order. The list is shared, so other
     * customers may come before, between or after them.
     */
    public BrowseCustomersVerification listsCustomersInOrder(List<String> expectedCustomers) {
        assertThat(getResponse()).as("Response should not be null").isNotNull();
        var listed = getResponse().getCustomers();
        assertThat(listed).as("Customers listed").isNotNull();
        for (var customer : expectedCustomers) {
            assertThat(listed.stream().filter(customer::equals).count())
                    .as("Times customer '%s' is listed, in %s", customer, listed)
                    .isEqualTo(1);
        }
        assertThat(listed).as("Customers listed, in order").containsSubsequence(expectedCustomers);
        return this;
    }
}
