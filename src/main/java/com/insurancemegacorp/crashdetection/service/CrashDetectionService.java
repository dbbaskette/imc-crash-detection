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
    
    @Value("${crash-detection.rollover-threshold:3.0}")
    private double rolloverThreshold;
    
    @Value("${crash-detection.spinning-threshold:2.0}")
    private double spinningThreshold;
    
    public boolean isCrashEvent(TelematicsMessage message) {
        double gForce = message.gForce(); // Use pre-calculated G-force
        double lateralGForce = message.lateralGForce();
        double verticalGForce = message.verticalGForce();
        double speed = message.speedMph();
        
        // Enhanced crash criteria with multi-sensor analysis
        boolean highGForce = gForce >= gForceThreshold;
        boolean suddenStop = speed <= speedThreshold && gForce >= 2.0;
        boolean rolloverDetected = message.indicatesRollover();
        boolean spinningDetected = message.indicatesSpinning();
        boolean highLateralForce = lateralGForce >= 3.0; // Side impact
        boolean highVerticalForce = verticalGForce >= 2.0; // Jump/drop impact
        
        // Environmental factors that might affect crash severity
        boolean environmentalRisk = false;
        if (message.sensors().barometricPressure() != null) {
            boolean weatherEvent = message.sensors().indicatesWeatherEvent();
            boolean highAltitude = message.sensors().isAtHighAltitude();
            environmentalRisk = weatherEvent || highAltitude;
        }
        
        // Device health factors
        boolean deviceIssues = false;
        if (message.sensors().device() != null) {
            deviceIssues = message.sensors().device().isLowBattery() || 
                          message.sensors().device().isWeakSignal();
        }
        
        // GPS accuracy factors
        boolean gpsReliable = true;
        if (message.sensors().gps() != null) {
            gpsReliable = message.sensors().gps().isHighAccuracy() && 
                         message.sensors().gps().hasGoodSatelliteReception();
        }
        
        // Adjust thresholds based on environmental and data quality factors
        double adjustedGForceThreshold = gForceThreshold;
        if (environmentalRisk) adjustedGForceThreshold *= 0.9; // Lower threshold in bad weather
        if (deviceIssues || !gpsReliable) adjustedGForceThreshold *= 1.1; // Higher threshold for unreliable data
        
        boolean adjustedHighGForce = gForce >= adjustedGForceThreshold;
        
        return adjustedHighGForce || suddenStop || rolloverDetected || spinningDetected || 
               highLateralForce || highVerticalForce;
    }
    
    public String determineCrashType(TelematicsMessage message) {
        if (message.indicatesRollover()) {
            return "ROLLOVER";
        } else if (message.indicatesSpinning()) {
            return "SPINNING";
        } else if (message.lateralGForce() >= 3.0) {
            return "SIDE_IMPACT";
        } else if (message.verticalGForce() >= 2.0) {
            return "VERTICAL_IMPACT";
        } else if (message.speedMph() <= speedThreshold && message.gForce() >= 2.0) {
            return "SUDDEN_STOP";
        } else if (message.gForce() >= gForceThreshold) {
            return "HIGH_G_FORCE";
        } else {
            return "UNKNOWN";
        }
    }
    
    public void processCrashEvent(TelematicsMessage message) {
        String crashType = determineCrashType(message);
        
        logger.warn("🚨💥 {} CRASH DETECTED! 💥🚨", crashType);
        logger.warn("Policy ID: {}", message.policyId());
        logger.warn("VIN: {}", message.vin());
        logger.warn("Timestamp: {}", message.timestamp());
        logger.warn("Location: {} at {}, {} ({}m accuracy, {} satellites)", 
            message.currentStreet(),
            message.sensors().gps().latitude(), 
            message.sensors().gps().longitude(),
            message.sensors().gps().accuracy() != null ? String.format("%.1f", message.sensors().gps().accuracy()) : "N/A",
            message.sensors().gps().satelliteCount() != null ? message.sensors().gps().satelliteCount() : "N/A");
        logger.warn("GPS Quality: {}, Altitude: {}m, Bearing: {}° ({})", 
            message.sensors().gps().getGpsQuality(),
            message.sensors().gps().altitude() != null ? String.format("%.1f", message.sensors().gps().altitude()) : "N/A",
            message.sensors().gps().bearing() != null ? String.format("%.1f", message.sensors().gps().bearing()) : "N/A",
            message.sensors().magnetometer().getCardinalDirection());
        logger.warn("Speed: {} mph", message.speedMph());
        logger.warn("Total G-Force: {}g", String.format("%.2f", message.gForce()));
        logger.warn("Lateral G-Force: {}g", String.format("%.2f", message.lateralGForce()));
        logger.warn("Vertical G-Force: {}g", String.format("%.2f", message.verticalGForce()));
        
        logger.warn("Sensor Data:");
        logger.warn("  Accelerometer: X={}, Y={}, Z={}", 
            message.sensors().accelerometer().x(),
            message.sensors().accelerometer().y(),
            message.sensors().accelerometer().z());
        logger.warn("  Gyroscope: Pitch={}, Roll={}, Yaw={} ({}, {} rad/s magnitude)", 
            message.sensors().gyroscope().pitch(),
            message.sensors().gyroscope().roll(),
            message.sensors().gyroscope().yaw(),
            message.sensors().gyroscope().getMotionType(),
            String.format("%.2f", message.sensors().gyroscope().getMagnitude()));
        logger.warn("  Magnetometer: Heading={}°", String.format("%.1f", message.getHeading()));
        
        // Environmental data
        if (message.sensors().barometricPressure() != null) {
            logger.warn("Environmental Data:");
            logger.warn("  Barometric Pressure: {} hPa", message.sensors().barometricPressure());
            logger.warn("  Estimated Altitude: {} m", String.format("%.1f", message.sensors().getEstimatedAltitudeMeters()));
            if (message.sensors().indicatesWeatherEvent()) {
                logger.warn("  ⛈️  SEVERE WEATHER CONDITIONS DETECTED!");
            }
            if (message.sensors().isAtHighAltitude()) {
                logger.warn("  🏔️  HIGH ALTITUDE CONDITIONS!");
            }
        }
        
        // Device health data
        if (message.sensors().device() != null) {
            logger.warn("Device Status: {} (Battery: {}%, Signal: {})", 
                message.sensors().device().getDeviceHealthStatus(),
                message.sensors().device().getBatteryPercentage(),
                message.sensors().device().getSignalQuality());
            logger.warn("  Orientation: {}, Screen: {}, Charging: {}", 
                message.sensors().device().orientation(),
                message.sensors().device().isScreenActive() ? "ON" : "OFF",
                message.sensors().device().isCharging() ? "YES" : "NO");
            
            if (message.sensors().device().isCriticalBattery()) {
                logger.warn("  🔋 CRITICAL BATTERY WARNING - Device may shut down!");
            } else if (message.sensors().device().isLowBattery()) {
                logger.warn("  🔋 LOW BATTERY WARNING - Device may lose power!");
            }
            if (message.sensors().device().isWeakSignal()) {
                logger.warn("  📶 WEAK SIGNAL - Communication may be compromised!");
            }
        }
        
        if (message.indicatesRollover()) {
            logger.warn("⚠️  ROLLOVER DETECTED - High angular velocity!");
        }
        if (message.indicatesSpinning()) {
            logger.warn("🌀 VEHICLE SPINNING DETECTED!");
        }
        
        logger.warn("🚑 Emergency services should be notified immediately!");
    }
    
    public void processNormalTelemetry(TelematicsMessage message) {
        logger.info("Normal telemetry - Policy: {}, VIN: {}, Speed: {} mph, G-Force: {}g, Location: {}", 
            message.policyId(),
            message.vin(),
            message.speedMph(), 
            String.format("%.2f", message.gForce()),
            message.currentStreet());
    }
}