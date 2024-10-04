package controllers;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.DemoApplication;

import models.User;
import services.UserManagementService;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserManagementService userManagementService;

    @Test
    public void testCreateUser() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");

        when(userManagementService.createUser(any(), any(), any(), any())).thenReturn(mockUser);

        mockMvc.perform(post("/v1/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\",\"firstName\":\"Test\",\"lastName\":\"User\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    public void contextLoads() {
        // This test will fail if the application context cannot start
        assert(true);
    }
}