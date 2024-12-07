package configs;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiMethodNotAllowedFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ((path.equals("/healthz") && !method.equals("GET")) ||
            (path.equals("/cicd") && !method.equals("GET")) ||
            (path.equals("/v1/user") && !method.equals("POST")) ||
            (path.equals("/v1/user/self/pic") && !(method.equals("POST") || method.equals("GET") || method.equals("DELETE")))) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}