package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.config.CurrentUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("canPlaceOrder", CurrentUserResolver.canPlaceOrder(authentication));
        return "home";
    }
}
