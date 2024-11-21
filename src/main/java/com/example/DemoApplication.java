package com.example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.amazonaws.services.s3.AmazonS3;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@ComponentScan(basePackages = { "controllers","services","data","models","configs","dto","com.example","aspects"})
@EnableJpaRepositories(basePackages = "data") 
@EntityScan(basePackages = "models") 
public class DemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);
    
    @Value("${aws.s3.bucket}")
    private String bucketName;  
    
    @Autowired
    private AmazonS3 amazonS3Client;
    
    public static void main(String[] args) {
        System.out.println("DEBUG: Starting DemoApplication");
        SpringApplication.run(DemoApplication.class, args);
    }
    @PostConstruct
    public void testS3Connectivity() {
        try {
            System.out.println("DEBUG: Testing S3 connectivity for bucket: " + bucketName);
            logger.info("Testing S3 connectivity for bucket: {}", bucketName);
            
            if (amazonS3Client.doesBucketExistV2(bucketName)) {
                System.out.println("DEBUG: Successfully connected to S3 bucket: " + bucketName);
                logger.info("Successfully connected to S3 bucket: {}", bucketName);
            } else {
                System.out.println("ERROR: S3 bucket does not exist: " + bucketName);
                logger.error("S3 bucket does not exist: {}", bucketName);
            }
        } catch (Exception e) {
            System.out.println("ERROR: S3 connectivity test failed: " + e.getMessage());
            logger.error("Error during S3 connectivity test: {}", e.getMessage(), e);
        }
    }
}
