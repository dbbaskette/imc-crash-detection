package com.insurancemegacorp.crashdetection.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RabbitMQHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQHealthChecker.class);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Value("${rabbitmq.startup.max-wait-time:300}")
    private int maxWaitTimeSeconds;

    @Value("${rabbitmq.startup.check-interval:5}")
    private int checkIntervalSeconds;

    @EventListener(ApplicationReadyEvent.class)
    public void waitForRabbitMQ() {
        logger.info("🐰 Checking RabbitMQ connection availability...");
        
        Instant startTime = Instant.now();
        Instant maxWaitTime = startTime.plus(Duration.ofSeconds(maxWaitTimeSeconds));
        
        int attempt = 1;
        boolean connected = false;

        while (!connected && Instant.now().isBefore(maxWaitTime)) {
            try {
                logger.info("🔍 RabbitMQ connection attempt {} ({}s elapsed)", 
                    attempt, Duration.between(startTime, Instant.now()).getSeconds());
                
                // Test the connection
                var connection = connectionFactory.createConnection();
                if (connection != null && connection.isOpen()) {
                    connection.close();
                    connected = true;
                    logger.info("✅ RabbitMQ connection established successfully after {} attempts ({}s)", 
                        attempt, Duration.between(startTime, Instant.now()).getSeconds());
                }
                
            } catch (Exception e) {
                logger.warn("⚠️  RabbitMQ connection attempt {} failed: {} (retrying in {}s)", 
                    attempt, e.getMessage(), checkIntervalSeconds);
                
                try {
                    Thread.sleep(checkIntervalSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("❌ RabbitMQ connection check interrupted");
                    return;
                }
            }
            attempt++;
        }

        if (!connected) {
            long totalWaitTime = Duration.between(startTime, Instant.now()).getSeconds();
            logger.error("❌ Failed to connect to RabbitMQ after {}s and {} attempts", 
                totalWaitTime, attempt - 1);
            logger.error("🚨 Application will continue but message processing may fail until RabbitMQ is available");
        } else {
            logger.info("🚀 Application ready - RabbitMQ connection verified");
            logger.info("📥 Listening for telemetry messages on queue: telematics_stream");
            logger.info("📤 Crash reports will be published to queue: crash_reports");
        }
    }

    public boolean isRabbitMQAvailable() {
        try {
            var connection = connectionFactory.createConnection();
            if (connection != null && connection.isOpen()) {
                connection.close();
                return true;
            }
        } catch (Exception e) {
            logger.debug("RabbitMQ availability check failed: {}", e.getMessage());
        }
        return false;
    }
}