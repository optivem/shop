package com.mycompany.myshop.backend.api.controller;

import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

final class CurrentUserResolver {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String USERNAME_CLAIM = "preferred_username";
    private static final String EMAIL_CLAIM = "email";
    private static final String GIVEN_NAME_CLAIM = "given_name";
    private static final String FAMILY_NAME_CLAIM = "family_name";

    private CurrentUserResolver() {
    }

    static CurrentUser resolve(Authentication authentication, Jwt jwt) {
        var admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
        var username = jwt.getClaimAsString(USERNAME_CLAIM);
        return new CurrentUser(jwt.getSubject(), username, admin, jwt.getClaimAsString(EMAIL_CLAIM),
                jwt.getClaimAsString(GIVEN_NAME_CLAIM), jwt.getClaimAsString(FAMILY_NAME_CLAIM));
    }
}
