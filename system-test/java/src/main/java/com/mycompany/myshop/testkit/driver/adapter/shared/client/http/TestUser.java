package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

public enum TestUser {
    CUSTOMER("customer1", "customer1-test-password"),
    CUSTOMER2("customer2", "customer2-test-password"),
    ADMIN("admin1", "admin1-test-password");

    private final String username;
    private final String password;

    TestUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** The test customer at the given zero-based position (0 = customer1). */
    public static TestUser customer(int index) {
        return switch (index) {
            case 0 -> CUSTOMER;
            case 1 -> CUSTOMER2;
            default -> throw new IllegalArgumentException("No test customer at index " + index);
        };
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
