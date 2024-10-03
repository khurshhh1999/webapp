package controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dto.UserDTO;
import jakarta.validation.Valid;
import models.User;
import services.UserManagementService;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Autowired
    private UserManagementService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            User user = userService.createUser(userDTO.getEmail(), userDTO.getPassword(), 
                                               userDTO.getFirstName(), userDTO.getLastName());
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
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.updateUser(userDTO, email);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);  // Email format error
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);  // Other errors (e.g., user not found)
        }
    }
}

