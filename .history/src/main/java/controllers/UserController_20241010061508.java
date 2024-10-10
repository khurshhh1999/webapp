package controllers;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dto.UserDTO;
import models.User;
import services.UserManagementService;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Autowired
    private UserManagementService userService;

    private static final Set<String> ALLOWED_CREATE_FIELDS = Set.of("email", "password", "firstName", "lastName");
    private static final Set<String> ALLOWED_UPDATE_FIELDS = Set.of("firstName", "lastName", "password");

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userMap, 
                                        @RequestParam(required = false) Map<String, String> queryParams) {
        // Check for query parameters
        if (!queryParams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // Check for disallowed fields
        if (!userMap.keySet().stream().allMatch(ALLOWED_CREATE_FIELDS::contains)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid fields in request body");
        }

        try {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail((String) userMap.get("email"));
            userDTO.setPassword((String) userMap.get("password"));
            userDTO.setFirstName((String) userMap.get("firstName"));
            userDTO.setLastName((String) userMap.get("lastName"));

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
    public ResponseEntity<?> updateUser(@RequestBody Map<String, Object> userMap, 
                                        @RequestParam(required = false) Map<String, String> queryParams) {
        // Check for query parameters
        if (!queryParams.isEmpty()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // Check for disallowed fields
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
            return new ResponseEntity<>("Invalid input", HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}