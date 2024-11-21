package configs;
import java.util.Date;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AccessLogValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessLogValve extends AccessLogValve {
    private static final Logger log = LoggerFactory.getLogger("http.access");
    private static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

    @Override
    public void log(Request request, Response response, long time) {
        StringBuilder sb = new StringBuilder();
        
        // Standard access log
        sb.append(new Date()).append(" ");
        sb.append("[").append(request.getMethod()).append("] ");
        sb.append(request.getRequestURI()).append(" ");
        sb.append("Status: ").append(response.getStatus()).append(" ");
        sb.append("Time: ").append(time).append("ms ");

        // Add error information if present
        Throwable throwable = (Throwable) request.getAttribute(ERROR_EXCEPTION);
        if (throwable != null) {
            sb.append("Error: ").append(throwable.getMessage()).append(" ");
            if (throwable.getCause() != null) {
                sb.append("Cause: ").append(throwable.getCause().getMessage());
            }
        }

        // Add request attributes from controller
        String username = (String) request.getAttribute("username");
        if (username != null) {
            sb.append("User: ").append(username).append(" ");
        }

        String operation = (String) request.getAttribute("operation");
        if (operation != null) {
            sb.append("Operation: ").append(operation).append(" ");
        }

        // Write to access.log
        try {
            log.info(sb.toString());
        } catch (Exception e) {
            log.error("Failed to write to access log", e);
        }
    }
}
