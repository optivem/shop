package com.optivem.starter.monolith.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GreetingWebController {

    @GetMapping("/greeting")
    public String greeting() {
        return "greeting.html";
    }
}
