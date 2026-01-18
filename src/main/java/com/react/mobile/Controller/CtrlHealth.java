package com.react.mobile.Controller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CtrlHealth {
    public String health() {
        return "Okay";
    }
    @GetMapping("/health")
    public String checkHealth() {
        return health();
    }
}