package com.observability.orderservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient webClient;

    public UserServiceClient(@Value("${user.service.url}") String userServiceUrl) {
        logger.info("🔧 UserServiceClient initialized with Base URL: {}", userServiceUrl);
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }


    public boolean userExists(Long userId) {
        try {
            logger.debug("Checking if user exists: {}", userId);

            Map<String, Object> response = webClient.get()
                    .uri("/api/users/{id}/exists", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(1))
                                    .filter(ex -> ex instanceof TimeoutException ||
                                                  ex instanceof WebClientRequestException)
                    )
                    .block();

            boolean exists = response != null &&
                    Boolean.TRUE.equals(response.get("exists"));

            logger.debug("User {} exists: {}", userId, exists);
            return exists;

        } catch (Exception e) {
            logger.error("❌ Error checking user existence [{}]: {} - {}",
                    userId, e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public Mono<Boolean> userExistsAsync(Long userId) {
        logger.debug("Checking if user exists (async): {}", userId);

        return webClient.get()
                .uri("/api/users/{id}/exists", userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(ex -> ex instanceof TimeoutException)
                )
                .map(response -> Boolean.TRUE.equals(response.get("exists")))
                .doOnError(err -> logger.error("Async error checking user {}: {}", userId, err.getMessage()))
                .onErrorReturn(false);
    }
}
