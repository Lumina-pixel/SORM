package com.sorm.sorm_backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello SORM";
    }

    @GetMapping("/api/ping")
    public PingResponse ping() {
        return new PingResponse("ok", "SORM backend is running");
    }
}