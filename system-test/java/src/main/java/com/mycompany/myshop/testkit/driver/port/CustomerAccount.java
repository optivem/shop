package com.mycompany.myshop.testkit.driver.port;

/** A customer sign-in account created for one scenario: what the drivers sign in with. */
public record CustomerAccount(String username, String password) {
    @Override
    public String toString() {
        return "CustomerAccount[username=" + username + "]";
    }
}
