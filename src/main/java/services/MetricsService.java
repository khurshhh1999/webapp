package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.timgroup.statsd.StatsDClient;

@Service
public class MetricsService {
    @Autowired
    private StatsDClient statsDClient;

    // API metrics
    public void trackApiCall(String endpoint) {
        statsDClient.incrementCounter("api." + endpoint + ".count");
    }

    public void trackApiTiming(String endpoint, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        statsDClient.recordExecutionTime("api." + endpoint + ".time", duration);
    }

    public void trackApiError(String endpoint) {
        statsDClient.incrementCounter("api." + endpoint + ".error");
    }

    // Database metrics
    public void trackDatabaseOperation(String operation, long duration) {
        statsDClient.incrementCounter("database." + operation + ".count");
        statsDClient.recordExecutionTime("database." + operation + ".time", duration);
    }

    // S3 metrics
    public void trackS3Operation(String operation, long duration) {
        statsDClient.incrementCounter("s3." + operation + ".count");
        statsDClient.recordExecutionTime("s3." + operation + ".time", duration);
    }

    public void trackS3Error(String operation) {
        statsDClient.incrementCounter("s3." + operation + ".error");
    }
}