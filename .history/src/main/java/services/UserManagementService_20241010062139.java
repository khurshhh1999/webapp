package services;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import data.UserRepo;
import dto.UserDTO;
import models.User;

@Service
public class UserManagementService {

    private final UserRepo userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @Autowired
    public UserManagementService(UserRepo userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean isValidEmail(String email) {
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    public User createUser(String email, String password, String firstName, String lastName) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAccountCreated(LocalDateTime.now());
        user.setAccountUpdated(LocalDateTime.now());

        return userRepository.save(user);
    }

    public User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void updateUser(UserDTO updatedUserDTO, String email) {
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updatedUserDTO.getEmail() != null || 
            updatedUserDTO.getAccountCreated() != null || 
            updatedUserDTO.getAccountUpdated() != null) {
            throw new IllegalArgumentException("Attempted to update disallowed fields");
        }

        if (updatedUserDTO.getFirstName() != null) {
            existingUser.setFirstName(updatedUserDTO.getFirstName());
        }
        if (updatedUserDTO.getLastName() != null) {
            existingUser.setLastName(updatedUserDTO.getLastName());
        }
        if (updatedUserDTO.getPassword() != null && !updatedUserDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDTO.getPassword()));
        }

        existingUser.setAccountUpdated(LocalDateTime.now());
        userRepository.save(existingUser);
    }
}