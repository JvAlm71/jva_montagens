package com.java10x.jvaMontagens.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequestMapping("/users")
public class UserController {
    
    @ GetMapping("/info")
    public String getUserInfo() {
        return "User information endpoint.";
}
}