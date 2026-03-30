package com.optivem.starter.monolith.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TodoWebController {

    @GetMapping("/todos")
    public String todos() {
        return "todos.html";
    }
}