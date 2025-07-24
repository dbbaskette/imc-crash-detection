package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record CrashReport(
    @JsonProperty("report_id") String reportId,
    @JsonProperty("policy_id") String policyId,
    @JsonProperty("vin") String vin,
    @JsonProperty("crash_timestamp") String crashTimestamp,
    @JsonProperty("crash_type") String crashType,
    @JsonProperty("severity_level") String severityLevel,
    @JsonProperty("impact_details") ImpactDetails impactDetails,
    @JsonProperty("location") LocationData locationData,
    @JsonProperty("vehicle_data") VehicleData vehicleData,
    @JsonProperty("sensor_analysis") SensorAnalysis sensorAnalysis,
    @JsonProperty("emergency_recommended") boolean emergencyRecommended,
    @JsonProperty("risk_score") double riskScore,
    @JsonProperty("total_g_force") double totalGForce,
    @JsonProperty("processed_timestamp") Instant processedTimestamp
) {
    
    public record ImpactDetails(
        @JsonProperty("total_g_force") double totalGForce,
        @JsonProperty("lateral_g_force") double lateralGForce,
        @JsonProperty("vertical_g_force") double verticalGForce,
        @JsonProperty("speed_at_impact") double speedAtImpact,
        @JsonProperty("acceleration_vector") AccelerationVector accelerationVector,
        @JsonProperty("deceleration_rate") double decelerationRate
    ) {}
    
    public record AccelerationVector(
        @JsonProperty("x") double x,
        @JsonProperty("y") double y,
        @JsonProperty("z") double z,
        @JsonProperty("magnitude") double magnitude
    ) {}
    
    public record LocationData(
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("current_street") String currentStreet,
        @JsonProperty("heading") double heading,
        @JsonProperty("accuracy_meters") Double accuracyMeters
    ) {}
    
    public record VehicleData(
        @JsonProperty("vin") String vin,
        @JsonProperty("speed_mph") double speedMph,
        @JsonProperty("heading") double heading
    ) {}
    
    public record SensorAnalysis(
        @JsonProperty("accelerometer") AccelerometerAnalysis accelerometer,
        @JsonProperty("gyroscope") GyroscopeAnalysis gyroscope,
        @JsonProperty("magnetometer") MagnetometerAnalysis magnetometer,
        @JsonProperty("rollover_detected") boolean rolloverDetected,
        @JsonProperty("spinning_detected") boolean spinningDetected
    ) {}
    
    public record AccelerometerAnalysis(
        @JsonProperty("raw_values") AccelerationVector rawValues,
        @JsonProperty("total_magnitude") double totalMagnitude,
        @JsonProperty("lateral_magnitude") double lateralMagnitude,
        @JsonProperty("vertical_magnitude") double verticalMagnitude
    ) {}
    
    public record GyroscopeAnalysis(
        @JsonProperty("x") double x,
        @JsonProperty("y") double y,
        @JsonProperty("z") double z,
        @JsonProperty("magnitude") double magnitude,
        @JsonProperty("indicates_rollover") boolean indicatesRollover,
        @JsonProperty("indicates_spinning") boolean indicatesSpinning
    ) {}
    
    public record MagnetometerAnalysis(
        @JsonProperty("x") double x,
        @JsonProperty("y") double y,
        @JsonProperty("z") double z,
        @JsonProperty("heading_degrees") double headingDegrees
    ) {}
    
    public static CrashReport fromTelematicsMessage(TelematicsMessage message, 
                                                   String crashType, 
                                                   double riskScore, 
                                                   double totalGForce) {
        
        String reportId = generateReportId(message.policyId(), Instant.parse(message.timestamp()));
        String severityLevel = determineSeverityLevel(message.gForce(), totalGForce, riskScore);
        boolean emergencyRecommended = riskScore >= 0.8 || message.gForce() >= 6.0;
        
        ImpactDetails impactDetails = new ImpactDetails(
            totalGForce,
            message.lateralGForce(),
            message.verticalGForce(),
            message.speedMph(),
            new AccelerationVector(
                message.sensors().accelerometer().x(),
                message.sensors().accelerometer().y(),
                message.sensors().accelerometer().z(),
                totalGForce
            ),
            calculateDecelerationRate(message.speedMph(), message.gForce())
        );
        
        LocationData locationData = new LocationData(
            message.sensors().gps().latitude(),
            message.sensors().gps().longitude(),
            message.currentStreet(),
            message.getHeading(),
            null // Could be enhanced with GPS accuracy
        );
        
        VehicleData vehicleData = new VehicleData(
            message.vin(),
            message.speedMph(),
            message.getHeading()
        );
        
        SensorAnalysis sensorAnalysis = new SensorAnalysis(
            new AccelerometerAnalysis(
                new AccelerationVector(
                    message.sensors().accelerometer().x(),
                    message.sensors().accelerometer().y(),
                    message.sensors().accelerometer().z(),
                    totalGForce
                ),
                totalGForce,
                message.lateralGForce(),
                message.verticalGForce()
            ),
            new GyroscopeAnalysis(
                message.sensors().gyroscope().x(),
                message.sensors().gyroscope().y(),
                message.sensors().gyroscope().z(),
                message.sensors().gyroscope().getMagnitude(),
                message.sensors().gyroscope().indicatesRollover(3.0),
                message.sensors().gyroscope().indicatesSpinning(2.0)
            ),
            new MagnetometerAnalysis(
                message.sensors().magnetometer().x(),
                message.sensors().magnetometer().y(),
                message.sensors().magnetometer().z(),
                message.getHeading()
            ),
            message.indicatesRollover(),
            message.indicatesSpinning()
        );
        
        return new CrashReport(
            reportId,
            message.policyId(),
            message.vin(),
            message.timestamp(),
            crashType,
            severityLevel,
            impactDetails,
            locationData,
            vehicleData,
            sensorAnalysis,
            emergencyRecommended,
            riskScore,
            totalGForce,
            Instant.now()
        );
    }
    
    private static String generateReportId(String policyId, Instant timestamp) {
        return String.format("CRASH-%s-%d", 
            policyId.replaceAll("[^A-Za-z0-9]", ""), 
            timestamp.toEpochMilli());
    }
    
    private static String determineSeverityLevel(double gForce, double totalGForce, double riskScore) {
        if (gForce >= 8.0 || totalGForce >= 10.0 || riskScore >= 0.9) {
            return "CRITICAL";
        } else if (gForce >= 6.0 || totalGForce >= 7.0 || riskScore >= 0.7) {
            return "HIGH";
        } else if (gForce >= 4.0 || totalGForce >= 5.0 || riskScore >= 0.5) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    private static double calculateDecelerationRate(double speed, double gForce) {
        // Simplified deceleration calculation based on G-force
        // In reality, this would need more sophisticated physics calculations
        return gForce > 2.0 ? speed * (gForce / 4.0) : 0.0;
    }
}