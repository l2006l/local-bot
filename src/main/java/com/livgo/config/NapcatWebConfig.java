package com.livgo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NapcatWebConfig {

    @Value("${run.napcat-web}")
    private String napcatWeb;

    @GetMapping("/api/napcat")
    public Map<String, String> napcat() {
        return Map.of("url", napcatWeb);
    }

}
