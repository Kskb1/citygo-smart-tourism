package com.citygo.tourism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CityGoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityGoApplication.class, args);
    }
}
