package com.acme.insurance.telematics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelematicsApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(TelematicsApplication.class);
    
    public static void main(String[] args) {
        logger.info("🚨 Starting Crash Detection Processor");
        logger.info("👂 Listening for telematics data from queue 'telematics_stream'");
        logger.info("🔍 Analyzing incoming messages for crash events...");
        logger.info("Press Ctrl+C to stop.");
        
        SpringApplication.run(TelematicsApplication.class, args);
    }
}