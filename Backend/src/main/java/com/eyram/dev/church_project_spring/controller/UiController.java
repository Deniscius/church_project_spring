package com.eyram.dev.church_project_spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html";
    }

    @GetMapping("/health-ui")
    public String healthPage() {
        return "forward:/health.html";
    }
}
