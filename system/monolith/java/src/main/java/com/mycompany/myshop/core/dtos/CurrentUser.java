package com.mycompany.myshop.core.dtos;

/**
 * The authenticated caller: token subject, display username, whether they hold the ADMIN role, and the
 * sign-in account's email and given/family name (each null when the token does not carry it).
 */
public record CurrentUser(String subject, String username, boolean admin, String email, String givenName, String familyName) {

    public CurrentUser(String subject, String username, boolean admin) {
        this(subject, username, admin, null, null, null);
    }
}
