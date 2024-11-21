package controllers;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.timgroup.statsd.StatsDClient;

import dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Valid;
import models.User;
import services.UserManagementService;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Autowired
    private UserManagementService userService;

    @Autowired
    private StatsDClient statsDClient;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private static final Set<String> ALLOWED_CREATE_FIELDS = Set.of("email", "password", "firstName", "lastName");
    private static final Set<String> ALLOWED_UPDATE_FIELDS = Set.of("firstName", "lastName", "password");

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO, @RequestHeader Map<String, String> headers) {
        System.out.println("Received DTO: " + userDTO);
        System.out.println("firstName: " + userDTO.getFirstName());
        System.out.println("lastName: " + userDTO.getLastName());
        System.out.println("email: " + userDTO.getEmail());
        try {
            User user = userService.createUser(
                userDTO.getEmail(),
                userDTO.getPassword(),
                userDTO.getFirstName(),
                userDTO.getLastName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(new UserDTO(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/self")
    public ResponseEntity<UserDTO> getUserInfo() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getAuthenticatedUser(email);
        return new ResponseEntity<>(new UserDTO(user), HttpStatus.OK);
    }

    @PutMapping("/self")
    public ResponseEntity<?> updateUser(@Valid @RequestBody Map<String, Object> userMap) {
        if (!userMap.keySet().stream().allMatch(ALLOWED_UPDATE_FIELDS::contains)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid fields in request body");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            UserDTO userDTO = new UserDTO();
            userDTO.setFirstName((String) userMap.get("firstName"));
            userDTO.setLastName((String) userMap.get("lastName"));
            userDTO.setPassword((String) userMap.get("password"));

            userService.updateUser(userDTO, email);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Attempted to update disallowed fields", HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/verify/{token}")
    public ResponseEntity<?> verifyEmail(@PathVariable String token) {
    MDC.put("operation", "verify_email");
    try {
        logger.info("Processing email verification request for token");
        statsDClient.incrementCounter("api.verify.attempt");
        
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret.getBytes())
            .parseClaimsJws(token)
            .getBody();
            
        String email = claims.getSubject();
        MDC.put("user", email);
        logger.info("Token decoded successfully for user: {}", email);
        
        userService.verifyEmail(email);
        statsDClient.incrementCounter("api.verify.success");
        logger.info("Email verification completed successfully for user: {}", email);
        
        return ResponseEntity.ok()
            .body(Map.of("message", "Email verified successfully"));
    } catch (ExpiredJwtException e) {
        logger.warn("Verification token expired: {}", e.getMessage());
        statsDClient.incrementCounter("api.verify.expired");
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of("error", "Verification link has expired"));
    } catch (Exception e) {
        logger.error("Verification failed: {}", e.getMessage(), e);
        statsDClient.incrementCounter("api.verify.error");
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Invalid verification token"));
    } finally {
        MDC.clear();
    }
}

}