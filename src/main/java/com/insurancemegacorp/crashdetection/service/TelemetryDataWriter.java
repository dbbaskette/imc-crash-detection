package com.insurancemegacorp.crashdetection.service;

import com.insurancemegacorp.crashdetection.model.TelematicsMessage;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.apache.spark.sql.functions.*;

@Service
public class TelemetryDataWriter {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryDataWriter.class);

    @Autowired
    private SparkSession sparkSession;

    @Value("${spark.storage.enabled:true}")
    private boolean storageEnabled;

    @Value("${spark.storage.path:file:///tmp/telemetry-data}")
    private String storagePath;

    @Value("${spark.storage.format:parquet}")
    private String storageFormat;

    @Value("${spark.storage.partitioning.enabled:true}")
    private boolean partitioningEnabled;

    @Value("${spark.storage.compression:snappy}")
    private String compressionCodec;

    @Value("${spark.storage.write-mode:append}")
    private String writeMode;

    public void writeTelemetryData(Dataset<TelematicsMessage> telematicsDataset) {
        if (!storageEnabled) {
            logger.debug("📁 Telemetry data storage is disabled, skipping write operation");
            return;
        }

        try {
            logger.info("💾 Writing telemetry data to {} in {} format", storagePath, storageFormat);

            // Convert to Dataset<Row> and add partitioning columns
            Dataset<Row> enrichedData = telematicsDataset.toDF()
                    .withColumn("date", 
                        date_format(
                            to_timestamp(col("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), 
                            "yyyy-MM-dd"))
                    .withColumn("hour", 
                        date_format(
                            to_timestamp(col("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), 
                            "HH"))
                    .withColumn("year", 
                        date_format(
                            to_timestamp(col("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), 
                            "yyyy"))
                    .withColumn("month", 
                        date_format(
                            to_timestamp(col("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), 
                            "MM"))
                    .withColumn("processed_timestamp", current_timestamp());

            // Configure the writer
            var writer = enrichedData.write()
                    .mode(SaveMode.valueOf(writeMode.toUpperCase()))
                    .option("compression", compressionCodec);

            // Add partitioning if enabled
            if (partitioningEnabled) {
                writer = writer.partitionBy("policy_id", "year", "month", "date");
                logger.info("📂 Partitioning enabled by: policy_id, year, month, date");
            }

            // Write based on format
            switch (storageFormat.toLowerCase()) {
                case "parquet":
                    writer.parquet(storagePath);
                    break;
                case "json":
                    writer.json(storagePath);
                    break;
                case "csv":
                    writer.option("header", "true").csv(storagePath);
                    break;
                case "delta":
                    writer.format("delta").save(storagePath);
                    break;
                default:
                    logger.warn("⚠️ Unsupported format: {}, defaulting to parquet", storageFormat);
                    writer.parquet(storagePath);
            }

            long recordCount = enrichedData.count();
            logger.info("✅ Successfully wrote {} telemetry records to {}", recordCount, storagePath);

            // Log storage statistics
            logStorageStatistics(enrichedData);

        } catch (Exception e) {
            logger.error("❌ Failed to write telemetry data to storage: {}", e.getMessage(), e);
            
            // Attempt retry with simplified configuration
            retryWriteWithFallback(telematicsDataset);
        }
    }

    private void retryWriteWithFallback(Dataset<TelematicsMessage> telematicsDataset) {
        try {
            logger.info("🔄 Attempting fallback write without partitioning");
            
            Dataset<Row> simpleData = telematicsDataset.toDF()
                    .withColumn("write_timestamp", current_timestamp());

            simpleData.write()
                    .mode(SaveMode.Append)
                    .json(storagePath + "/fallback/" + System.currentTimeMillis());

            logger.info("✅ Fallback write completed successfully");

        } catch (Exception fallbackError) {
            logger.error("❌ Fallback write also failed: {}", fallbackError.getMessage(), fallbackError);
        }
    }

    private void logStorageStatistics(Dataset<Row> data) {
        try {
            // Log basic statistics
            Dataset<Row> stats = data
                    .groupBy("policy_id", "date")
                    .agg(
                        count("*").alias("record_count"),
                        avg("speed_mph").alias("avg_speed"),
                        max("g_force").alias("max_g_force"),
                        min("g_force").alias("min_g_force")
                    )
                    .orderBy(col("date").desc(), col("record_count").desc());

            logger.info("📊 Storage Statistics Summary:");
            stats.show(10, false);

            // Log total counts by date
            Dataset<Row> dailyCounts = data
                    .groupBy("date")
                    .agg(count("*").alias("total_records"))
                    .orderBy(col("date").desc());

            logger.info("📅 Daily Record Counts:");
            dailyCounts.show(7, false);

        } catch (Exception e) {
            logger.warn("⚠️ Could not generate storage statistics: {}", e.getMessage());
        }
    }

    public void writeCrashReportsToStorage(List<com.insurancemegacorp.crashdetection.model.CrashReport> crashReports) {
        if (!storageEnabled || crashReports.isEmpty()) {
            return;
        }

        try {
            String crashReportPath = storagePath + "/crash-reports";
            logger.info("🚨 Writing {} crash reports to {}", crashReports.size(), crashReportPath);

            // Convert crash reports to Spark Dataset
            Dataset<com.insurancemegacorp.crashdetection.model.CrashReport> crashDataset = sparkSession
                    .createDataset(crashReports, org.apache.spark.sql.Encoders.bean(com.insurancemegacorp.crashdetection.model.CrashReport.class));

            Dataset<Row> enrichedCrashData = crashDataset.toDF()
                    .withColumn("report_date", 
                        date_format(col("crash_timestamp"), "yyyy-MM-dd"))
                    .withColumn("report_hour", 
                        date_format(col("crash_timestamp"), "HH"));

            var crashWriter = enrichedCrashData.write()
                    .mode(SaveMode.Append)
                    .option("compression", compressionCodec);

            if (partitioningEnabled) {
                crashWriter = crashWriter.partitionBy("severity_level", "report_date");
            }

            switch (storageFormat.toLowerCase()) {
                case "parquet":
                    crashWriter.parquet(crashReportPath);
                    break;
                case "json":
                    crashWriter.json(crashReportPath);
                    break;
                default:
                    crashWriter.parquet(crashReportPath);
            }

            logger.info("✅ Successfully wrote crash reports to storage");

        } catch (Exception e) {
            logger.error("❌ Failed to write crash reports to storage: {}", e.getMessage(), e);
        }
    }

    public boolean isStorageEnabled() {
        return storageEnabled;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getStorageFormat() {
        return storageFormat;
    }
}