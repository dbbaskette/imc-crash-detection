package com.insurancemegacorp.crashdetection.config;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    @Value("${spark.app.name:CrashDetectionProcessor}")
    private String appName;

    @Value("${spark.master:local[*]}")
    private String masterUri;

    @Value("${spark.sql.adaptive.enabled:true}")
    private boolean adaptiveQueryEnabled;

    @Value("${spark.sql.adaptive.coalescePartitions.enabled:true}")
    private boolean coalescePartitionsEnabled;

    @Bean
    public SparkConf sparkConf() {
        return new SparkConf()
                .setAppName(appName)
                .setMaster(masterUri)
                .set("spark.sql.adaptive.enabled", String.valueOf(adaptiveQueryEnabled))
                .set("spark.sql.adaptive.coalescePartitions.enabled", String.valueOf(coalescePartitionsEnabled))
                .set("spark.sql.streaming.checkpointLocation", "/tmp/spark-checkpoint")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.sql.streaming.forceDeleteTempCheckpointLocation", "true")
                // Disable Spark UI and metrics to avoid servlet dependencies and Java 21 issues
                .set("spark.ui.enabled", "false")
                .set("spark.metrics.enabled", "false")
                .set("spark.eventLog.enabled", "false")
                // Disable other unnecessary components for headless operation  
                .set("spark.sql.ui.retainedExecutions", "1")
                .set("spark.sql.execution.arrow.pyspark.enabled", "false");
    }

    @Bean
    public SparkSession sparkSession(SparkConf sparkConf) {
        return SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();
    }
}