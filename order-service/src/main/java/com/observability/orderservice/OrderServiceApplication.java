package com.observability.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class OrderServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        logger.info("Order Service started successfully!");
    }

    @RestController
    class HealthController {
        @GetMapping("/health")
        public String health() {
            logger.info("Health check requested");
            return "Order Service is healthy!";
        }

        @GetMapping("/hello")
        public String hello() {
            logger.info("Hello endpoint accessed");
            return "Hello from Order Service!";
        }
    }
}
