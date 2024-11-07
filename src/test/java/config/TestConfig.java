package config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.amazonaws.services.s3.AmazonS3;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

@TestConfiguration
@Profile("test")
public class TestConfig {
    
    @Bean
    @Primary
    public StatsDClient statsDClient() {
        return new NonBlockingStatsDClient("testapp", "localhost", 8125);
    }
    @Bean
    @Primary
    public AmazonS3 amazonS3() {
        return Mockito.mock(AmazonS3.class);
    }
}