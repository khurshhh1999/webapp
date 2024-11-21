package config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.amazonaws.services.s3.AmazonS3;
import com.timgroup.statsd.StatsDClient;

import services.UserManagementService;

@TestConfiguration
@Profile("test")
public class TestConfig {
    
    @Bean
    @Primary
    public StatsDClient statsDClient() {
        return Mockito.mock(StatsDClient.class);
    }

    @Bean
    @Primary
    public AmazonS3 amazonS3() {
        return Mockito.mock(AmazonS3.class);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserManagementService userManagementService;

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }

    @Bean
    @Primary
    public UserManagementService userManagementService() {
        return Mockito.mock(UserManagementService.class);
    }
}
