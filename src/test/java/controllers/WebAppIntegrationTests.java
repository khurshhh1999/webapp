package controllers;

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
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import data.UserTestRepository;
import dto.UserDTO;

@SpringBootTest(classes = com.example.DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebAppIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserTestRepository userTestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        userTestRepository.deleteAll();
    }

    @Test
    public void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/healthz", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testCreateUser() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("test@example.com");
        userDTO.setPassword("password123");
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(userDTO), headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/user", request, String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        UserDTO createdUser = objectMapper.readValue(response.getBody(), UserDTO.class);
        assertEquals(userDTO.getEmail(), createdUser.getEmail());
        assertEquals(userDTO.getFirstName(), createdUser.getFirstName());
        assertEquals(userDTO.getLastName(), createdUser.getLastName());
    }

    @Test
    public void testGetUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("get@example.com");
        userDTO.setPassword("password123");
        userDTO.setFirstName("Jane");
        userDTO.setLastName("Doe");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> createRequest = new HttpEntity<>(objectMapper.writeValueAsString(userDTO), headers);
        restTemplate.postForEntity("/v1/user", createRequest, String.class);

        headers.setBasicAuth(userDTO.getEmail(), userDTO.getPassword());
        HttpEntity<String> getRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("/v1/user/self", HttpMethod.GET, getRequest, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserDTO retrievedUser = objectMapper.readValue(response.getBody(), UserDTO.class);
        assertEquals(userDTO.getEmail(), retrievedUser.getEmail());
        assertEquals(userDTO.getFirstName(), retrievedUser.getFirstName());
        assertEquals(userDTO.getLastName(), retrievedUser.getLastName());
    }
//update to add;


    
}