package services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.timgroup.statsd.StatsDClient;

@Service
public class S3LoggingService {
    private static final Logger logger = LoggerFactory.getLogger(S3LoggingService.class);
    
    @Autowired
    private StatsDClient statsDClient;
    
    public void logS3Operation(String operation, String bucketName, String key, long duration) {
        logger.info("S3 Operation: {} - Bucket: {} - Key: {} - Duration: {}ms", 
            operation, bucketName, key, duration);
        
        statsDClient.incrementCounter("s3.operation." + operation.toLowerCase() + ".count");
        statsDClient.recordExecutionTime("s3.operation." + operation.toLowerCase() + ".time", duration);
    }
    
    public void logS3Error(String operation, String bucketName, String key, String error) {
        logger.error("S3 Error: {} - Bucket: {} - Key: {} - Error: {}", 
            operation, bucketName, key, error);
        statsDClient.incrementCounter("s3.operation." + operation.toLowerCase() + ".error");
    }
}