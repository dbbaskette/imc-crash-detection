package com.insurancemegacorp.crashdetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelematicsApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(TelematicsApplication.class);
    
    public static void main(String[] args) {
        logger.info("🚨 Starting Crash Detection Processor with Apache Spark Integration");
        logger.info("📋 System Requirements:");
        logger.info("   ☕ Java: {}", System.getProperty("java.version"));
        logger.info("   🐰 RabbitMQ: Will attempt connection and wait if not available");
        logger.info("   ⚡ Spark: Running in local[*] mode (embedded)");
        logger.info("   💾 Storage: Writing telemetry data to filesystem/HDFS");
        logger.info("");
        logger.info("🔧 Configuration:");
        logger.info("   📥 Input Queue: telematics_stream");
        logger.info("   📤 Output Queue: crash_reports");
        logger.info("   🎯 Processing: Real-time micro-batch analysis");
        logger.info("");
        logger.info("🚀 Initializing application components...");
        logger.info("💡 Tip: If RabbitMQ is not running, the app will wait up to 5 minutes for it to start");
        logger.info("⚠️  Press Ctrl+C to stop gracefully");
        logger.info("");
        
        SpringApplication.run(TelematicsApplication.class, args);
    }
}