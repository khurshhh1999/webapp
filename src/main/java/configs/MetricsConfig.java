package configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

@Configuration
@Profile("!test")
public class MetricsConfig {
    
    @Value("${metrics.statsd.host:localhost}")
    private String statsdHost;
    
    @Value("${metrics.statsd.port:8125}")
    private int statsdPort;
    
    @Value("${metrics.prefix:csye6225}")
    private String prefix;
    
    @Bean
    public StatsDClient statsDClient() {
        return new NonBlockingStatsDClient(
            prefix,
            statsdHost,
            statsdPort
        );
    }
}