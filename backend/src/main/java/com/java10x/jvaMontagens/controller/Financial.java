package com.java10x.jvaMontagens.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RestController
@RequestMapping("/financial")
public class Financial {

    @GetMapping("/status")
    public String getStatus() {
        return "Financial service is running.";
        }
}
