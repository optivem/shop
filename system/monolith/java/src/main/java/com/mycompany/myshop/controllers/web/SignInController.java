package com.mycompany.myshop.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * `/sign-in?returnUrl=…` (storefront-ux §6.4): the header's "Sign in" link. The route requires a login, so an
 * anonymous visitor is sent to Keycloak and, once signed in, comes back here and on to {@code returnUrl}
 * (default `/`); a visitor who is already signed in goes straight there.
 */
@Controller
public class SignInController {

    @GetMapping("/sign-in")
    public String signIn(@RequestParam(required = false) String returnUrl) {
        return "redirect:" + safeReturnUrl(returnUrl);
    }

    /** Only same-site relative paths are accepted as destinations (no open redirect). */
    static String safeReturnUrl(String value) {
        var sameSite = value != null && value.startsWith("/") && !value.startsWith("//") && !value.startsWith("/\\");
        return sameSite ? value : "/";
    }
}
