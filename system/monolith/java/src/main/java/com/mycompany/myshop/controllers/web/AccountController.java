package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.controllers.web.storefront.ShippingAddresses;
import com.mycompany.myshop.core.dtos.ViewProfileResponse;
import com.mycompany.myshop.core.services.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * `/account` (My account › Profile &amp; address, UC-010): name and default shipping address. The page saves
 * through {@code PUT /api/account/profile}, which validates and reports each field to correct.
 */
@Controller
public class AccountController {

    private final ProfileService profileService;

    public AccountController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        var profile = profileService.getProfile(CurrentUserResolver.resolve(authentication));
        model.addAttribute("profile", profile == null ? new ViewProfileResponse() : profile);
        model.addAttribute("countries", ShippingAddresses.COUNTRIES);
        return "account";
    }
}
