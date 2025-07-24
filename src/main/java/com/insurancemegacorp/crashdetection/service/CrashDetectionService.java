package com.insurancemegacorp.crashdetection.service;

import com.insurancemegacorp.crashdetection.model.TelematicsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CrashDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrashDetectionService.class);
    
    @Value("${crash-detection.g-force-threshold:4.0}")
    private double gForceThreshold;
    
    @Value("${crash-detection.speed-threshold:5.0}")
    private double speedThreshold;
    
    public boolean isCrashEvent(TelematicsMessage message) {
        double gForce = message.gForce();
        double speed = message.speedMph();
        
        // Crash criteria: High G-force OR sudden deceleration
        boolean highGForce = gForce >= gForceThreshold;
        boolean suddenStop = speed <= speedThreshold && gForce >= 2.0;
        
        return highGForce || suddenStop;
    }
    
    public void processCrashEvent(TelematicsMessage message) {
        logger.warn("🚨💥 CRASH DETECTED! 💥🚨");
        logger.warn("Policy ID: {}", message.policyId());
        logger.warn("Timestamp: {}", message.timestamp());
        logger.warn("Speed: {} mph", message.speedMph());
        logger.warn("G-Force: {}g", String.format("%.2f", message.gForce()));
        logger.warn("Location: {}, {}", 
            message.sensors().gps().latitude(), 
            message.sensors().gps().longitude());
        logger.warn("Accelerometer: X={}, Y={}, Z={}", 
            message.sensors().accelerometer().x(),
            message.sensors().accelerometer().y(),
            message.sensors().accelerometer().z());
        logger.warn("🚑 Emergency services should be notified immediately!");
    }
    
    public void processNormalTelemetry(TelematicsMessage message) {
        logger.info("Normal telemetry - Policy: {}, Speed: {} mph, G-Force: {}g", 
            message.policyId(), 
            message.speedMph(), 
            String.format("%.2f", message.gForce()));
    }
}