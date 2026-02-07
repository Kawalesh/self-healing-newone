package com.observability.userservice.service;

import com.observability.userservice.dto.UserDTO;
import com.observability.userservice.model.User;
import com.observability.userservice.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final Counter userCreatedCounter;
	private final Counter userUpdatedCounter;
	private final Counter userDeletedCounter;
	private final Counter userLoginCounter;
	private final Timer userOperationTimer;

	@Autowired
	public UserService(UserRepository userRepository, MeterRegistry meterRegistry) {
		this.userRepository = userRepository;
		this.userCreatedCounter = Counter.builder("user.created").description("Total number of users created")
				.register(meterRegistry);
		this.userUpdatedCounter = Counter.builder("user.updated").description("Total number of users updated")
				.register(meterRegistry);
		this.userDeletedCounter = Counter.builder("user.deleted").description("Total number of users deleted")
				.register(meterRegistry);
		this.userLoginCounter = Counter.builder("user.login").description("Total number of user logins")
				.register(meterRegistry);
		this.userOperationTimer = Timer.builder("user.operation.duration").description("Time taken for user operations")
				.register(meterRegistry);
	}

	@Transactional
	public UserDTO createUser(UserDTO userDTO) throws Exception {
		return userOperationTimer.recordCallable(() -> {
			logger.info("Creating user with username: {}", userDTO.getUsername());

			if (userRepository.existsByUsername(userDTO.getUsername())) {
				throw new IllegalArgumentException("Username already exists: " + userDTO.getUsername());
			}

			if (userRepository.existsByEmail(userDTO.getEmail())) {
				throw new IllegalArgumentException("Email already exists: " + userDTO.getEmail());
			}

			User user = new User();
			user.setUsername(userDTO.getUsername());
			user.setEmail(userDTO.getEmail());
			user.setFullName(userDTO.getFullName());
			user.setActive(true);

			User savedUser = userRepository.save(user);
			userCreatedCounter.increment();

			logger.info("User created successfully with ID: {}", savedUser.getId());
			return convertToDTO(savedUser);
		});
	}

	public List<UserDTO> getAllUsers() throws Exception {
		return userOperationTimer.recordCallable(() -> {
			logger.debug("Fetching all users");
			return userRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
		});
	}

	public Optional<UserDTO> getUserById(Long id) throws Exception {
		return userOperationTimer.recordCallable(() -> {
			logger.debug("Fetching user by ID: {}", id);
			return userRepository.findById(id).map(this::convertToDTO);
		});
	}

	public Optional<UserDTO> getUserByUsername(String username) throws Exception {
		return userOperationTimer.recordCallable(() -> {
			logger.debug("Fetching user by username: {}", username);
			return userRepository.findByUsername(username).map(this::convertToDTO);
		});
	}

	@Transactional
	public UserDTO updateUser(Long id, UserDTO userDTO) throws Exception {
		return userOperationTimer.recordCallable(() -> {
			logger.info("Updating user with ID: {}", id);

			User user = userRepository.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

			// Check if username is being changed and if it's already taken
			if (!user.getUsername().equals(userDTO.getUsername())
					&& userRepository.existsByUsername(userDTO.getUsername())) {
				throw new IllegalArgumentException("Username already exists: " + userDTO.getUsername());
			}

			// Check if email is being changed and if it's already taken
			if (!user.getEmail().equals(userDTO.getEmail()) && userRepository.existsByEmail(userDTO.getEmail())) {
				throw new IllegalArgumentException("Email already exists: " + userDTO.getEmail());
			}

			user.setUsername(userDTO.getUsername());
			user.setEmail(userDTO.getEmail());
			user.setFullName(userDTO.getFullName());
			user.setActive(userDTO.isActive());

			User updatedUser = userRepository.save(user);
			userUpdatedCounter.increment();

			logger.info("User updated successfully with ID: {}", updatedUser.getId());
			return convertToDTO(updatedUser);
		});
	}

	@Transactional
	public void deleteUser(Long id) {
		userOperationTimer.record(() -> {
			logger.info("Deleting user with ID: {}", id);

			if (!userRepository.existsById(id)) {
				throw new IllegalArgumentException("User not found with ID: " + id);
			}

			userRepository.deleteById(id);
			userDeletedCounter.increment();

			logger.info("User deleted successfully with ID: {}", id);
		});
	}

	@Transactional
	public void recordUserLogin(Long userId) {
		userOperationTimer.record(() -> {
			logger.debug("Recording login for user ID: {}", userId);

			User user = userRepository.findById(userId)
					.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

			user.setLastLoginAt(LocalDateTime.now());
			user.setLoginCount(user.getLoginCount() + 1);
			userRepository.save(user);

			userLoginCounter.increment();
			logger.debug("Login recorded for user ID: {}", userId);
		});
	}

	public boolean userExists(Long userId) {
		logger.info("Inside userExists Checking if user exists: {}", userId);
	    if (userId == null) {
	    	logger.info("Checking if user exists: null {}", userId);
	        return false;
	    }

	    try {
	        logger.info("Checking if user exists: {}", userId);
	        return userRepository.existsById(userId);

	    } catch (Exception ex) {   // ✅ catch Exception ONLY
	    	logger.error("check error");
	        logger.error("Failed to check user existence for userId={}", userId, ex);
	        throw ex;              // ✅ DO NOT swallow it
	    }
	}


	public long getActiveUserCount() {
		return userRepository.countActiveUsers();
	}

	public long getInactiveUserCount() {
		return userRepository.countInactiveUsers();
	}

	private UserDTO convertToDTO(User user) {
		return new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getCreatedAt(),
				user.getLastLoginAt(), user.isActive(), user.getLoginCount());
	}
}
