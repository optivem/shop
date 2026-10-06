package com.mycompany.myshop.core.dtos;

/** The authenticated caller: token subject, display username, and whether they hold the ADMIN role. */
public record CurrentUser(String subject, String username, boolean admin) {
}
