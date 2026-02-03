package dev.java10x.jva_Montagens;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/financial")
public class Financial {

    @GetMapping("/status")
    public String getStatus() {
        return "Financial service is running.";
        }
}
