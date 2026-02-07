package com.observability.orderservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.reactor.retry.RetryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "userService";
    private static final String RETRY_NAME = "userService";

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public UserServiceClient(
            @Value("${user.service.url}") String userServiceUrl,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        logger.info("🔧 UserServiceClient initialized with Base URL: {}", userServiceUrl);
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.retry = retryRegistry.retry(RETRY_NAME);
        
        // Register event listeners for monitoring
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    logger.warn("🔄 Circuit Breaker state changed: {} -> {}", 
                            event.getStateTransition().getFromState(), 
                            event.getStateTransition().getToState());
                });
    }


    @SuppressWarnings("unchecked")
    public boolean userExists(Long userId) {
        logger.info("Checking user existence for userId={} (GET /api/users/{}/exists)", userId, userId);
        try {
            Supplier<Boolean> supplier = () ->
                    webClient.get()
                            .uri("/api/users/{id}/exists", userId)
                            .accept(MediaType.APPLICATION_JSON)
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(Map.class)
                                            .map(body -> {
                                                Object existsVal = body != null ? body.get("exists") : null;
                                                boolean exists = Boolean.TRUE.equals(existsVal)
                                                        || "true".equalsIgnoreCase(String.valueOf(existsVal));
                                                logger.info("User {} exists: {} (from user-service)", userId, exists);
                                                return exists;
                                            });
                                }
                                logger.warn("User-service returned non-2xx for /api/users/{}/exists: {}", userId, response.statusCode());
                                return Mono.just(false);
                            })
                            .timeout(Duration.ofSeconds(5))
                            .block();

            Supplier<Boolean> retrySupplier = Retry.decorateSupplier(retry, supplier);
            Supplier<Boolean> decoratedSupplier =
                    CircuitBreaker.decorateSupplier(circuitBreaker, retrySupplier);

            return Boolean.TRUE.equals(decoratedSupplier.get());

        } catch (Throwable e) {
            logger.error("❌ Error checking user existence [{}]: {}", userId, e.getMessage(), e);
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
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .map(response -> Boolean.TRUE.equals(response.get("exists")))
                .doOnError(err -> logger.error("Async error checking user {}: {}", userId, err.getMessage()))
                .onErrorReturn(false);
    }
}
