package services;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.timgroup.statsd.StatsDClient;

import data.UserRepo;
import dto.UserDTO;
import models.User;

@Service
public class UserManagementService {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final UserRepo userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SNSService snsService;
    private final StatsDClient statsDClient;

    @Autowired
    public UserManagementService(
            UserRepo userRepository, 
            BCryptPasswordEncoder passwordEncoder,
            SNSService snsService,
            StatsDClient statsDClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.snsService = snsService;
        this.statsDClient = statsDClient;
        logger.info("UserManagementService initialized");
    }

    private boolean isValidEmail(String email) {
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    @Transactional
    public User createUser(String email, String password, String firstName, String lastName) {
        logger.info("Attempting to create user with email: {}", email);
        statsDClient.incrementCounter("api.user.create.attempt");

        if (!isValidEmail(email)) {
            logger.warn("Invalid email format: {}", email);
            statsDClient.incrementCounter("api.user.create.error.invalid_email");
            throw new IllegalArgumentException("Invalid email format");
        }

        if (userRepository.existsByEmail(email)) {
            logger.warn("User already exists with email: {}", email);
            statsDClient.incrementCounter("api.user.create.error.existing_email");
            throw new RuntimeException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAccountCreated(LocalDateTime.now());
        user.setAccountUpdated(LocalDateTime.now());
        user.setEmailVerified(false);

        user = userRepository.save(user);
        logger.info("User created successfully with ID: {}", user.getId());

        try {
            snsService.publishUserRegistration(user.getId(), user.getEmail());
            statsDClient.incrementCounter("api.user.create.sns.success");
        } catch (Exception e) {
            logger.error("Failed to publish SNS message for user verification: {}", e.getMessage(), e);
            statsDClient.incrementCounter("api.user.create.sns.error");
            // Continue with user creation even if SNS fails
        }

        statsDClient.incrementCounter("api.user.create.success");
        return user;
    }

    public User getAuthenticatedUser(String email) {
        logger.debug("Fetching authenticated user: {}", email);
        statsDClient.incrementCounter("api.user.get.attempt");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", email);
                    statsDClient.incrementCounter("api.user.get.error.not_found");
                    return new RuntimeException("User not found");
                });

        if (!user.isEmailVerified()) {
            logger.warn("Unverified user attempting access: {}", email);
            statsDClient.incrementCounter("api.user.get.error.not_verified");
            throw new RuntimeException("Email not verified. Please check your email for verification link.");
        }

        statsDClient.incrementCounter("api.user.get.success");
        return user;
    }

    @Transactional
    public void updateUser(UserDTO updatedUserDTO, String email) {
        logger.debug("Attempting to update user: {}", email);
        statsDClient.incrementCounter("api.user.update.attempt");

        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found for update: {}", email);
                    statsDClient.incrementCounter("api.user.update.error.not_found");
                    return new RuntimeException("User not found");
                });

        if (!existingUser.isEmailVerified()) {
            logger.warn("Unverified user attempting update: {}", email);
            statsDClient.incrementCounter("api.user.update.error.not_verified");
            throw new RuntimeException("Email not verified. Please verify your email before updating profile.");
        }

        if (updatedUserDTO.getEmail() != null ||
            updatedUserDTO.getAccountCreated() != null ||
            updatedUserDTO.getAccountUpdated() != null) {
            logger.warn("Attempt to update disallowed fields for user: {}", email);
            statsDClient.incrementCounter("api.user.update.error.disallowed_fields");
            throw new IllegalArgumentException("Attempted to update disallowed fields");
        }

        updateUserFields(existingUser, updatedUserDTO);
        existingUser.setAccountUpdated(LocalDateTime.now());
        userRepository.save(existingUser);
        
        statsDClient.incrementCounter("api.user.update.success");
        logger.info("User updated successfully: {}", email);
    }

    private void updateUserFields(User existingUser, UserDTO updatedUserDTO) {
        if (updatedUserDTO.getFirstName() != null) {
            existingUser.setFirstName(updatedUserDTO.getFirstName());
        }
        if (updatedUserDTO.getLastName() != null) {
            existingUser.setLastName(updatedUserDTO.getLastName());
        }
        if (updatedUserDTO.getPassword() != null && !updatedUserDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDTO.getPassword()));
        }
    }

    @Transactional
    public void verifyEmail(String email) {
        logger.debug("Attempting to verify email for user: {}", email);
        statsDClient.incrementCounter("api.user.verify.attempt");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found for verification: {}", email);
                    statsDClient.incrementCounter("api.user.verify.error.not_found");
                    return new RuntimeException("User not found");
                });

        user.setEmailVerified(true);
        user.setAccountUpdated(LocalDateTime.now());
        userRepository.save(user);

        statsDClient.incrementCounter("api.user.verify.success");
        logger.info("Email verified successfully for user: {}", email);
    }
}