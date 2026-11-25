package com.mycompany.myshop.core.dtos;

/**
 * The authenticated caller: token subject, display username, whether they hold the ADMIN role, and the
 * sign-in account's email, given/family name and the shipping country chosen at sign-up (each null when the
 * token does not carry it).
 */
public record CurrentUser(String subject, String username, boolean admin, String email, String givenName, String familyName,
        String country) {

    public CurrentUser(String subject, String username, boolean admin) {
        this(subject, username, admin, null, null, null, null);
    }

    public CurrentUser(String subject, String username, boolean admin, String email, String givenName, String familyName) {
        this(subject, username, admin, email, givenName, familyName, null);
    }
}
