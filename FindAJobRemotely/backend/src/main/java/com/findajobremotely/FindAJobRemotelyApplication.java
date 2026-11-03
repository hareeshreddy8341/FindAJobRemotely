package com.findajobremotely;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FindAJobRemotelyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindAJobRemotelyApplication.class, args);
    }
}
