package com.mycompany.myshop.controllers.web.storefront;

import com.mycompany.myshop.config.CurrentUserResolver;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/** Who the header shows as signed in, and which links they get (storefront-ux §3). */
public record StoreUser(String username, boolean admin, boolean canPlaceOrder) {

    /** The signed-in user, or null for an anonymous visitor. */
    public static StoreUser of(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        var user = CurrentUserResolver.resolve(authentication);
        var username = user.username() != null ? user.username() : authentication.getName();
        return new StoreUser(username, user.admin(), CurrentUserResolver.canPlaceOrder(authentication));
    }
}
