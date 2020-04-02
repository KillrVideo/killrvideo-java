package com.killrvideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main
 */
@ComponentScan(basePackages="com.killrvideo")
@EnableAutoConfiguration
public class KillrVideoServices {

    /**
     * As SpringBoot application, this is the "main" class
     */
    public static void main(String[] args) {
        new SpringApplication(KillrVideoServices.class).run(args);
    }
    
}