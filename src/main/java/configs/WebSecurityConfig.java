package configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final UserDetailsService userDetailsService;

    public WebSecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("Configuring security chain");
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.GET, "/healthz").permitAll()
            .requestMatchers(HttpMethod.GET, "/cicd").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/user").permitAll()
            .requestMatchers(HttpMethod.GET, "/v1/user/self").authenticated()
            .requestMatchers(HttpMethod.PUT, "/v1/user/self").authenticated()
            .requestMatchers(HttpMethod.POST, "/v1/user/self/pic").authenticated()
            .requestMatchers(HttpMethod.GET, "/v1/user/self/pic").authenticated()
            .requestMatchers(HttpMethod.DELETE, "/v1/user/self/pic").authenticated()
            .requestMatchers(HttpMethod.GET, "/v1/user/verify/**").permitAll()
            .anyRequest().denyAll()
            )
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .httpBasic(httpBasic -> {})
            .userDetailsService(userDetailsService);

        //http.addFilterBefore(new RequestValidationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new ApiMethodNotAllowedFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }
}