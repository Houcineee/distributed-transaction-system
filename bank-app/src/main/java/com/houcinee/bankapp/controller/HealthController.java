package com.houcinee.bankapp.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@Slf4j
@RestController
public class HealthController {
    @GetMapping("/api/health")
    Map<String, String> getHealth(){
        log.info("health endpoint was called !");
        return Map.of("status" , "ok");
    }
}
