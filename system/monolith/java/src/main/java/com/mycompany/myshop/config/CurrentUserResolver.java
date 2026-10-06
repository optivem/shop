package com.mycompany.myshop.config;

import com.mycompany.myshop.core.dtos.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves the caller from either a Bearer JWT (API clients) or an OIDC browser session (UI). */
public final class CurrentUserResolver {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    private static final String USERNAME_CLAIM = "preferred_username";

    private CurrentUserResolver() {
    }

    public static CurrentUser resolve(Authentication authentication) {
        var admin = isAdmin(authentication);
        var principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return new CurrentUser(jwt.getSubject(), jwt.getClaimAsString(USERNAME_CLAIM), admin);
        }
        if (principal instanceof OidcUser oidcUser) {
            return new CurrentUser(oidcUser.getSubject(), oidcUser.getClaimAsString(USERNAME_CLAIM), admin);
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
    }

    public static boolean canPlaceOrder(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_CUSTOMER.equals(authority.getAuthority()));
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }
}
