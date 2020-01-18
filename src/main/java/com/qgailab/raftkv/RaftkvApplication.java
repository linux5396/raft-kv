package com.qgailab.raftkv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackageClasses = RaftNodeBootStrap.class)
public class RaftkvApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaftkvApplication.class, args);
    }

}
