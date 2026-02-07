package com.observability.userservice.config;

import com.observability.userservice.model.User;
import com.observability.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultUser() {
        try {
            if (userRepository.count() == 0) {
                User user1 = new User();
                user1.setUsername("default");
                user1.setEmail("default@example.com");
                user1.setFullName("Default User");
                user1.setActive(true);
                userRepository.save(user1);
                User user2 = new User();
                user2.setUsername("user2");
                user2.setEmail("user2@example.com");
                user2.setFullName("User Two");
                user2.setActive(true);
                userRepository.save(user2);
                logger.info("Seeded users: id=1 (default), id=2 (user2)");
                return;
            }
            if (!userRepository.existsById(1L)) {
                ensureUserWithIdExists(1, "user1", "user1@example.com", "User One");
            }
            if (!userRepository.existsById(2L)) {
                ensureUserWithIdExists(2, "user2", "user2@example.com", "User Two");
            }
        } catch (Exception e) {
            logger.warn("Could not seed default users: {}", e.getMessage());
        }
    }

    private void ensureUserWithIdExists(long id, String username, String email, String fullName) {
        try {
            int updated = jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, created_at, last_login_at, active, login_count) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true, 0)",
                id, username, email, fullName);
            if (updated > 0) {
                jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 1) FROM users))");
                logger.info("Seeded user with id={}: username={}, email={}", id, username, email);
            }
        } catch (Exception e) {
            logger.warn("Could not ensure user id={} exists: {}", id, e.getMessage());
        }
    }
}
