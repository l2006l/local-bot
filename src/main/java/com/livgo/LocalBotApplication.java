package com.livgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.erupt.core.annotation.EruptScan;

@SpringBootApplication
@EnableScheduling
@EntityScan({"xyz.erupt", "com.livgo"})
@EruptScan({"xyz.erupt", "com.livgo"})
@ComponentScan(basePackages = {"com.livgo", "xyz.erupt"})
public class LocalBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalBotApplication.class, args);
    }

}
