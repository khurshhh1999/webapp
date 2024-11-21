package services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import controllers.UserController;

@Service
public class SNSService {
    private final AmazonSNS snsClient;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    @Value("${aws.sns.topic.arn}")
    private String snsTopicArn;

    public SNSService() {
        this.snsClient = AmazonSNSClientBuilder.standard()
                .withRegion("us-east-1")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void publishUserRegistration(Long userId, String email) {
        MDC.put("operation", "publish_verification");
        MDC.put("user", email);
        
        try {
            logger.info("Publishing verification request for user ID: {}", userId);
            
            Map<String, String> message = new HashMap<>();
            message.put("userId", userId.toString());
            message.put("email", email);
    
            String messageJson = objectMapper.writeValueAsString(message);
            
            PublishRequest request = new PublishRequest()
                .withTopicArn(snsTopicArn)
                .withMessage(messageJson);
    
            snsClient.publish(request);
            logger.info("Successfully published verification request for user: {}", email);
        } catch (Exception e) {
            logger.error("Failed to publish SNS message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to trigger email verification", e);
        } finally {
            MDC.clear();
        }
    }
}