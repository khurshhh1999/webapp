package configs;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Check for query parameters
        if (request.getQueryString() != null) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            response.getWriter().write("Query parameters are not allowed");
            return;
        }

        // Only validate POST and PUT requests for body content
        if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
            String requestBody = request.getReader().lines().collect(Collectors.joining());
            
            // Skip empty bodies
            if (!requestBody.isEmpty()) {
                try {
                    Map<?, ?> bodyMap = objectMapper.readValue(requestBody, Map.class);
                    
                    // Check for restricted fields
                    if (bodyMap.containsKey("dateCreated") || 
                        bodyMap.containsKey("accountCreated") || 
                        bodyMap.containsKey("accountUpdated") || 
                        bodyMap.containsKey("id")) {
                        
                        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
                        response.getWriter().write("Request contains restricted fields");
                        return;
                    }
                    
                    // Create a new request wrapper
                    CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
                    cachedRequest.setCachedBody(requestBody.getBytes());
                    
                    // Continue with the modified request
                    filterChain.doFilter(cachedRequest, response);
                    return;
                    
                } catch (IOException e) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    response.getWriter().write("Invalid request body");
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}