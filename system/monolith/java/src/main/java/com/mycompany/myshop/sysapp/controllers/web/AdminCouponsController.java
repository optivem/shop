package com.mycompany.myshop.sysapp.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminCouponsController {

    @GetMapping("/admin-coupons")
    public String adminCoupons() {
        return "admin-coupons";
    }
}
