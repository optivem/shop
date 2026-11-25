package com.mycompany.myshop.testkit.driver.port;

/** A sign-in account (a customer's or an admin's) created for one scenario: what the drivers sign in with. */
public record UserAccount(String username, String password) {
    @Override
    public String toString() {
        return "UserAccount[username=" + username + "]";
    }
}
