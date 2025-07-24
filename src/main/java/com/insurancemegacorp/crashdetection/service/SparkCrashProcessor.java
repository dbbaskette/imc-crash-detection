package com.insurancemegacorp.crashdetection.service;

import com.insurancemegacorp.crashdetection.model.CrashReport;
import com.insurancemegacorp.crashdetection.model.TelematicsMessage;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.apache.spark.sql.functions.*;

@Service
public class SparkCrashProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SparkCrashProcessor.class);

    @Autowired
    private SparkSession sparkSession;

    @Autowired
    private CrashDetectionService crashDetectionService;

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private TelemetryDataWriter telemetryDataWriter;

    @Value("${spark.processing.window.duration:30 seconds}")
    private String windowDuration;

    @Value("${crash-detection.g-force-threshold:4.0}")
    private double gForceThreshold;

    @Value("${crash-detection.speed-threshold:5.0}")
    private double speedThreshold;

    private final ConcurrentLinkedQueue<TelematicsMessage> messageBuffer = new ConcurrentLinkedQueue<>();
    private volatile boolean isProcessing = false;

    @PostConstruct
    public void initializeSparkTempView() {
        // Register enhanced UDFs for multi-sensor crash detection
        sparkSession.udf().register("calculateTotalGForce", 
            (Double x, Double y, Double z) -> Math.sqrt(x*x + y*y + z*z) / 9.81, DataTypes.DoubleType);
            
        sparkSession.udf().register("calculateLateralGForce", 
            (Double x, Double y) -> Math.sqrt(x*x + y*y) / 9.81, DataTypes.DoubleType);
            
        sparkSession.udf().register("calculateVerticalGForce", 
            (Double z) -> Math.abs((z - 9.81) / 9.81), DataTypes.DoubleType);
            
        sparkSession.udf().register("calculateGyroscopeMagnitude", 
            (Double gx, Double gy, Double gz) -> Math.sqrt(gx*gx + gy*gy + gz*gz), DataTypes.DoubleType);
            
        sparkSession.udf().register("calculateHeading", 
            (Double mx, Double my) -> {
                double heading = Math.toDegrees(Math.atan2(my, mx));
                return heading < 0 ? heading + 360 : heading;
            }, DataTypes.DoubleType);
        
        sparkSession.udf().register("detectAdvancedCrashPattern", 
            (Double gForce, Double lateralGForce, Double verticalGForce, Double gyroMagnitude, Double speed) -> {
                boolean highGForce = gForce >= gForceThreshold;
                boolean suddenStop = speed <= speedThreshold && gForce >= 2.0;
                boolean rollover = gyroMagnitude >= 3.0;
                boolean sideImpact = lateralGForce >= 3.0;
                boolean verticalImpact = verticalGForce >= 2.0;
                
                return highGForce || suddenStop || rollover || sideImpact || verticalImpact;
            }, DataTypes.BooleanType);
            
        sparkSession.udf().register("determineCrashType", 
            (Double gForce, Double lateralGForce, Double verticalGForce, Double gyroMagnitude, Double gyroZ, Double speed) -> {
                if (gyroMagnitude >= 3.0) return "ROLLOVER";
                else if (Math.abs(gyroZ) >= 2.0) return "SPINNING";
                else if (lateralGForce >= 3.0) return "SIDE_IMPACT";
                else if (verticalGForce >= 2.0) return "VERTICAL_IMPACT";
                else if (speed <= speedThreshold && gForce >= 2.0) return "SUDDEN_STOP";
                else if (gForce >= gForceThreshold) return "HIGH_G_FORCE";
                else return "UNKNOWN";
            }, DataTypes.StringType);
            
        logger.info("🔧 Enhanced Spark UDFs registered for multi-sensor crash detection");
    }

    @Bean
    public Function<TelematicsMessage, TelematicsMessage> crashDetectionProcessor() {
        return telematicsMessage -> {
            // Add message to buffer for batch processing
            messageBuffer.offer(telematicsMessage);
            
            // Trigger batch processing when buffer reaches threshold or time interval
            if (messageBuffer.size() >= 10 || !isProcessing) {
                processBatchWithSpark();
            }
            
            return telematicsMessage;
        };
    }

    private synchronized void processBatchWithSpark() {
        if (isProcessing || messageBuffer.isEmpty()) {
            return;
        }
        
        isProcessing = true;
        
        try {
            // Drain messages from buffer
            List<TelematicsMessage> batch = new ArrayList<>();
            TelematicsMessage message;
            while ((message = messageBuffer.poll()) != null && batch.size() < 100) {
                batch.add(message);
            }
            
            if (batch.isEmpty()) {
                return;
            }
            
            logger.info("🚀 Processing Spark batch with {} messages", batch.size());
            
            // Create Spark Dataset from batch
            Dataset<TelematicsMessage> telematicsDF = sparkSession
                    .createDataset(batch, Encoders.bean(TelematicsMessage.class))
                    .cache(); // Cache for multiple operations
            
            // Write all telemetry data to storage (filesystem/HDFS)
            writeTelemetryDataToStorage(telematicsDF);
            
            // Register as temporary view for SQL operations
            telematicsDF.createOrReplaceTempView("telematics_stream");
            
            // Advanced Spark SQL analysis
            performSparkSQLAnalysis();
            
            // Enhanced Dataset transformations for multi-sensor crash detection
            Dataset<Row> enrichedData = telematicsDF
                    .withColumn("total_g_force", 
                        callUDF("calculateTotalGForce", 
                            col("sensors.accelerometer.x"),
                            col("sensors.accelerometer.y"), 
                            col("sensors.accelerometer.z")))
                    .withColumn("lateral_g_force", 
                        callUDF("calculateLateralGForce", 
                            col("sensors.accelerometer.x"),
                            col("sensors.accelerometer.y")))
                    .withColumn("vertical_g_force", 
                        callUDF("calculateVerticalGForce", col("sensors.accelerometer.z")))
                    .withColumn("gyro_magnitude", 
                        callUDF("calculateGyroscopeMagnitude", 
                            col("sensors.gyroscope.x"),
                            col("sensors.gyroscope.y"),
                            col("sensors.gyroscope.z")))
                    .withColumn("heading", 
                        callUDF("calculateHeading", 
                            col("sensors.magnetometer.x"),
                            col("sensors.magnetometer.y")))
                    .withColumn("is_crash", 
                        callUDF("detectAdvancedCrashPattern", 
                            col("total_g_force"), 
                            col("lateral_g_force"), 
                            col("vertical_g_force"), 
                            col("gyro_magnitude"), 
                            col("speedMph")))
                    .withColumn("crash_type", 
                        callUDF("determineCrashType", 
                            col("total_g_force"), 
                            col("lateral_g_force"), 
                            col("vertical_g_force"), 
                            col("gyro_magnitude"), 
                            col("sensors.gyroscope.z"), 
                            col("speedMph")))
                    .withColumn("risk_score", 
                        when(col("crash_type").equalTo("ROLLOVER"), lit(1.0))
                        .when(col("crash_type").equalTo("SPINNING"), lit(0.95))
                        .when(col("crash_type").equalTo("SIDE_IMPACT"), lit(0.9))
                        .when(col("crash_type").equalTo("VERTICAL_IMPACT"), lit(0.85))
                        .when(col("crash_type").equalTo("HIGH_G_FORCE"), lit(0.8))
                        .when(col("crash_type").equalTo("SUDDEN_STOP"), lit(0.7))
                        .otherwise(lit(0.1)));
            
            // Process crash events with Spark transformations
            Dataset<Row> crashEvents = enrichedData.filter(col("is_crash").equalTo(true));
            
            long crashCount = crashEvents.count();
            logger.info("🚨 Found {} crash events in batch", crashCount);
            
            if (crashCount > 0) {
                // Use Spark to create enriched crash reports
                List<CrashReport> crashReports = generateCrashReportsWithSpark(crashEvents, batch);
                
                // Publish crash reports to queue
                publishCrashReports(crashReports);
                
                // Also write crash reports to storage
                telemetryDataWriter.writeCrashReportsToStorage(crashReports);
                
                // Also process through existing service for logging
                crashEvents.foreach(row -> {
                    TelematicsMessage crashMessage = findOriginalMessage(batch, row.getAs("policyId"));
                    if (crashMessage != null) {
                        crashDetectionService.processCrashEvent(crashMessage);
                    }
                });
            }
            
            // Process normal telemetry with aggregations
            processNormalTelemetryWithAggregations(enrichedData);
            
            // Cleanup
            telematicsDF.unpersist();
            
            logger.info("✅ Completed Spark batch processing");
            
        } catch (Exception e) {
            logger.error("❌ Error in Spark batch processing: {}", e.getMessage(), e);
        } finally {
            isProcessing = false;
        }
    }
    
    private void performSparkSQLAnalysis() {
        try {
            // Enhanced crash pattern analysis with multi-sensor SQL
            String sqlQuery = String.format("""
                SELECT 
                    policy_id,
                    vin,
                    current_street,
                    speed_mph,
                    sensors.gps.latitude,
                    sensors.gps.longitude,
                    calculateTotalGForce(sensors.accelerometer.x, sensors.accelerometer.y, sensors.accelerometer.z) as total_g_force,
                    calculateLateralGForce(sensors.accelerometer.x, sensors.accelerometer.y) as lateral_g_force,
                    calculateVerticalGForce(sensors.accelerometer.z) as vertical_g_force,
                    calculateGyroscopeMagnitude(sensors.gyroscope.x, sensors.gyroscope.y, sensors.gyroscope.z) as gyro_magnitude,
                    calculateHeading(sensors.magnetometer.x, sensors.magnetometer.y) as heading,
                    determineCrashType(
                        calculateTotalGForce(sensors.accelerometer.x, sensors.accelerometer.y, sensors.accelerometer.z),
                        calculateLateralGForce(sensors.accelerometer.x, sensors.accelerometer.y),
                        calculateVerticalGForce(sensors.accelerometer.z),
                        calculateGyroscopeMagnitude(sensors.gyroscope.x, sensors.gyroscope.y, sensors.gyroscope.z),
                        sensors.gyroscope.z,
                        speed_mph
                    ) as crash_type,
                    CASE 
                        WHEN calculateGyroscopeMagnitude(sensors.gyroscope.x, sensors.gyroscope.y, sensors.gyroscope.z) >= 3.0 THEN 'ROLLOVER_RISK'
                        WHEN abs(sensors.gyroscope.z) >= 2.0 THEN 'SPINNING_RISK'
                        WHEN calculateLateralGForce(sensors.accelerometer.x, sensors.accelerometer.y) >= 3.0 THEN 'SIDE_IMPACT_RISK'
                        ELSE 'NORMAL_RISK'
                    END as risk_category
                FROM telematics_stream
                WHERE detectAdvancedCrashPattern(
                    calculateTotalGForce(sensors.accelerometer.x, sensors.accelerometer.y, sensors.accelerometer.z),
                    calculateLateralGForce(sensors.accelerometer.x, sensors.accelerometer.y),
                    calculateVerticalGForce(sensors.accelerometer.z),
                    calculateGyroscopeMagnitude(sensors.gyroscope.x, sensors.gyroscope.y, sensors.gyroscope.z),
                    speed_mph
                ) = true
                ORDER BY gyro_magnitude DESC, total_g_force DESC
                """);
                
            Dataset<Row> crashPatterns = sparkSession.sql(sqlQuery);
            
            logger.info("📊 Spark SQL analysis found {} potential incidents", crashPatterns.count());
            
            // Show top risk events
            if (crashPatterns.count() > 0) {
                logger.info("🔍 Top risk events identified by Spark SQL:");
                crashPatterns.show(5, false);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error in Spark SQL analysis: {}", e.getMessage(), e);
        }
    }
    
    private void processNormalTelemetryWithAggregations(Dataset<Row> enrichedData) {
        try {
            // Aggregate normal driving patterns
            Dataset<Row> normalTelemetry = enrichedData.filter(col("is_crash").equalTo(false));
            
            // Calculate driving statistics
            Dataset<Row> drivingStats = normalTelemetry
                    .groupBy("policyId")
                    .agg(
                        avg("speedMph").alias("avg_speed"),
                        max("gForce").alias("max_g_force"),
                        min("gForce").alias("min_g_force"),
                        avg("risk_score").alias("avg_risk_score"),
                        count("*").alias("message_count")
                    );
            
            logger.info("📈 Processed driving statistics for {} policies", drivingStats.count());
            
            // Log summary stats
            drivingStats.foreach(row -> {
                String policyId = row.getAs("policyId");
                Double avgSpeed = row.getAs("avg_speed");
                Double maxGForce = row.getAs("max_g_force");
                Long messageCount = row.getAs("message_count");
                
                logger.info("📊 Policy {}: Avg Speed={} mph, Max G-Force={}, Messages={}", 
                    policyId, String.format("%.1f", avgSpeed), String.format("%.2f", maxGForce), messageCount);
            });
            
        } catch (Exception e) {
            logger.error("❌ Error processing normal telemetry aggregations: {}", e.getMessage(), e);
        }
    }
    
    private void writeTelemetryDataToStorage(Dataset<TelematicsMessage> telematicsDataset) {
        try {
            logger.info("💾 Writing telemetry batch to storage using Spark");
            telemetryDataWriter.writeTelemetryData(telematicsDataset);
            
        } catch (Exception e) {
            logger.error("❌ Error writing telemetry data to storage: {}", e.getMessage(), e);
        }
    }
    
    private List<CrashReport> generateCrashReportsWithSpark(Dataset<Row> crashEvents, List<TelematicsMessage> batch) {
        try {
            logger.info("🔧 Generating crash reports using Spark transformations");
            
            // Enhanced crash analysis with Spark
            Dataset<Row> enrichedCrashData = crashEvents
                    .withColumn("crash_type", 
                        when(col("gForce").gt(8.0), lit("SEVERE_IMPACT"))
                        .when(col("gForce").gt(gForceThreshold), lit("HIGH_G_FORCE"))
                        .when(col("speedMph").lt(speedThreshold).and(col("gForce").gt(2.0)), lit("SUDDEN_STOP"))
                        .otherwise(lit("UNKNOWN")))
                    .withColumn("severity_priority", 
                        when(col("gForce").gt(8.0), lit(1))
                        .when(col("gForce").gt(6.0), lit(2))
                        .when(col("gForce").gt(4.0), lit(3))
                        .otherwise(lit(4)))
                    .orderBy(col("severity_priority"), col("gForce").desc());
            
            logger.info("📊 Spark enriched {} crash events with severity analysis", enrichedCrashData.count());
            
            // Convert Spark Dataset to CrashReport objects
            List<CrashReport> crashReports = new ArrayList<>();
            
            enrichedCrashData.collectAsList().forEach(row -> {
                try {
                    String policyId = row.getAs("policyId");
                    TelematicsMessage originalMessage = findOriginalMessage(batch, policyId);
                    
                    if (originalMessage != null) {
                        String crashType = row.getAs("crash_type");
                        Double riskScore = row.getAs("risk_score");
                        Double totalGForce = row.getAs("total_g_force");
                        
                        CrashReport crashReport = CrashReport.fromTelematicsMessage(
                            originalMessage, 
                            crashType, 
                            riskScore != null ? riskScore : 0.0, 
                            totalGForce != null ? totalGForce : 0.0
                        );
                        
                        crashReports.add(crashReport);
                        
                        logger.info("🚨 Generated crash report: {} for policy: {} with severity: {}", 
                            crashReport.reportId(), policyId, crashReport.severityLevel());
                    }
                } catch (Exception e) {
                    logger.error("❌ Error generating crash report for row: {}", e.getMessage(), e);
                }
            });
            
            return crashReports;
            
        } catch (Exception e) {
            logger.error("❌ Error in Spark crash report generation: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    private void publishCrashReports(List<CrashReport> crashReports) {
        try {
            logger.info("📤 Publishing {} crash reports to crash_reports queue", crashReports.size());
            
            for (CrashReport crashReport : crashReports) {
                boolean sent = streamBridge.send("crashDetectionProcessor-out-0", crashReport);
                
                if (sent) {
                    logger.info("✅ Published crash report: {} to queue", crashReport.reportId());
                } else {
                    logger.error("❌ Failed to publish crash report: {} to queue", crashReport.reportId());
                }
            }
            
            logger.info("📊 Crash report publishing summary: {}/{} reports sent successfully", 
                crashReports.size(), crashReports.size());
                
        } catch (Exception e) {
            logger.error("❌ Error publishing crash reports: {}", e.getMessage(), e);
        }
    }
    
    private TelematicsMessage findOriginalMessage(List<TelematicsMessage> batch, String policyId) {
        return batch.stream()
                .filter(msg -> msg.policyId().equals(policyId))
                .findFirst()
                .orElse(null);
    }
}