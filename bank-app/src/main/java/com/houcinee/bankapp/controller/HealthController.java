package com.houcinee.bankapp.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
public class HealthController {
    @GetMapping("/api/health")
    Map<String, String> getHealth(){
        return Map.of("status" , "ok");
    }
}
