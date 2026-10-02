package com.mycompany.myshop.testkit.driver.adapter.shared.client.http;

public enum TestUser {
    CUSTOMER("customer1", "customer1-test-password"),
    ADMIN("admin1", "admin1-test-password");

    private final String username;
    private final String password;

    TestUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
