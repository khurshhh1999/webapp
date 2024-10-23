package controllers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import data.UserTestRepository;
import models.User;

@SpringBootTest(classes = com.example.DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebAppIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserTestRepository userTestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private HttpHeaders headers;

    @BeforeEach
    public void setUp() {
        userTestRepository.deleteAll();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/healthz", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testCreateUser() throws Exception {
        Map<String, String> userMap = new HashMap<>();
        userMap.put("email", "test@example.com");
        userMap.put("password", "password123");
        userMap.put("firstName", "John");
        userMap.put("lastName", "Doe");

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/user", 
            new HttpEntity<>(userMap, headers), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    public void testGetUser() throws Exception {
        createTestUser("get@example.com", "password123");

        headers.setBasicAuth("get@example.com", "password123");
        ResponseEntity<String> response = restTemplate.exchange("/v1/user/self", 
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testUpdateUser() throws Exception {
        createTestUser("update@example.com", "password123");

        Map<String, String> updateFields = new HashMap<>();
        updateFields.put("firstName", "UpdatedFirst");
        updateFields.put("lastName", "UpdatedLast");

        headers.setBasicAuth("update@example.com", "password123");
        ResponseEntity<String> response = restTemplate.exchange("/v1/user/self",
            HttpMethod.PUT, new HttpEntity<>(updateFields, headers), String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private void createTestUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAccountCreated(LocalDateTime.now());
        user.setAccountUpdated(LocalDateTime.now());
        userTestRepository.save(user);
    }
}