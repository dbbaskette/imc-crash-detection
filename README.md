# Crash Detection Processor

A Spring Boot application with Apache Spark integration that consumes vehicle telematics data from RabbitMQ and analyzes it for crash events in real-time using micro-batch processing.

## Features

- **Apache Spark Integration**: Real-time stream processing with micro-batch capabilities
- **Multi-Sensor Analysis**: Processes accelerometer, gyroscope, magnetometer, and barometric data
- **Advanced Crash Detection**: Detects rollovers, spinning, side impacts, and environmental factors
- **RabbitMQ Source**: Consumes enhanced telemetry messages from `telematics_stream` queue
- **Crash Report Publishing**: Publishes enriched crash reports to `crash_reports` queue
- **Environmental Monitoring**: Considers weather conditions and altitude in crash analysis
- **Device Health Monitoring**: Tracks battery, temperature, and signal strength
- **Data Persistence**: Writes all telemetry data to filesystem (local) or HDFS (cloud) using Spark
- **Configurable Storage**: Supports Parquet, JSON, CSV formats with partitioning and compression
- **Configurable Thresholds**: Adjustable thresholds for multiple crash detection criteria
- **Comprehensive Alerting**: Detailed crash event logging with multi-sensor analysis
- **Spark SQL Analysis**: Advanced crash pattern analysis using custom UDFs and SQL queries
- **Resilient Processing**: Built-in retry logic and error handling with Spark fault tolerance
- **Cloud Ready**: Configured for both local development and Cloud Foundry deployment
- **Spring Cloud Stream Compatible**: Can be deployed via Spring Cloud Data Flow
- Built with Spring Boot 3.5.3, Apache Spark 3.5.0, and Java 21

## Message Format

### Enhanced Telemetry Data Format
```json
{
  "policy_id": "IMC-98675",
  "vin": "1HGBH41JXMN109186",
  "timestamp": "2025-07-24T17:19:14.123456Z",
  "speed_mph": 35.5,
  "current_street": "Peachtree St & 10th St",
  "g_force": 1.02,
  "sensors": {
    "gps": {
      "latitude": 33.7749,
      "longitude": -84.3877,
      "altitude": 320.5,
      "speed_ms": 15.87,
      "bearing": 45.2,
      "accuracy": 3.2,
      "satellite_count": 8,
      "gps_fix_time": "2025-07-24T17:19:14.120000Z"
    },
    "accelerometer": {
      "x": 0.1234,
      "y": -0.0567,
      "z": 9.8123
    },
    "gyroscope": {
      "pitch": 0.0123,
      "roll": -0.0045,
      "yaw": 0.0089
    },
    "magnetometer": {
      "x": 23.45,
      "y": -12.67,
      "z": 45.89,
      "heading": 45.2
    },
    "barometric_pressure": 1013.25,
    "device": {
      "battery_level": 0.85,
      "signal_strength": -65,
      "orientation": "portrait",
      "screen_on": true,
      "charging": false
    }
  }
}
```

### Crash Event Data
```json
{
  "policy_id": "IMC-AUTO-98765",
  "timestamp": "2024-07-24T18:31:47.890Z",
  "speed_mph": 35.0,
  "sensors": {
    "gps": {
      "lat": 40.7128,
      "lon": -74.0060
    },
    "accelerometer": {
      "x": 6.5432,
      "y": 5.8901,
      "z": 1.2345
    }
  },
  "g_force": 8.67
}
```

### Generated Crash Report (Published to `crash_reports` queue)
```json
{
  "report_id": "CRASH-IMCAUTO98765-1690217507890",
  "policy_id": "IMC-AUTO-98765",
  "crash_timestamp": "2024-07-24T18:31:47.890Z",
  "crash_type": "HIGH_G_FORCE",
  "severity_level": "CRITICAL",
  "impact_details": {
    "g_force": 8.67,
    "speed_at_impact": 35.0,
    "acceleration_vector": {
      "x": 6.5432,
      "y": 5.8901,
      "z": 1.2345,
      "magnitude": 9.12
    },
    "deceleration_rate": 76.12
  },
  "location": {
    "latitude": 40.7128,
    "longitude": -74.0060
  },
  "vehicle_data": {
    "speed_mph": 35.0,
    "engine_status": "UNKNOWN"
  },
  "emergency_recommended": true,
  "risk_score": 1.0,
  "total_g_force": 9.12,
  "processed_timestamp": "2024-07-24T18:31:48.123Z"
}
```

## Running Locally

### Prerequisites
- Java 21
- Maven 3.6+
- RabbitMQ server running on localhost:5672

### Quick Start with Management Scripts
```bash
# Start the crash detection processor (streams logs to terminal)
./run-local.sh

# Check status (in another terminal)
./status.sh

# Stop the processor (Ctrl+C in running terminal, or)
./run-local.sh --clean
```

### Manual Start
```bash
mvn spring-boot:run
```

**Note**: The application streams logs directly to the terminal, so you'll see real-time crash detection alerts as they happen. Use Ctrl+C to stop the application.

## Running on Cloud Foundry

```bash
# Build the application
mvn clean package

# Deploy with cloud profile
cf push crash-detection-processor -p target/crash-detection-telematics-1.0.0-SNAPSHOT.jar --env SPRING_PROFILES_ACTIVE=cloud
```

## Configuration

### Local Environment (application.yml)
- RabbitMQ: localhost:5672 with guest/guest credentials
- G-force threshold: 4.0g (configurable)
- Speed threshold: 5.0 mph for sudden stops
- Queue: telematics_stream
- Spark: local[*] mode with 2-second micro-batches
- Storage: `/tmp/telemetry-data` (filesystem) in Parquet format
- Retry logic: 3 attempts with 1s intervals

### Cloud Environment (application-cloud.yml)
All settings configurable via environment variables:
- **RabbitMQ**: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- **Detection**: `G_FORCE_THRESHOLD`, `SPEED_THRESHOLD`
- **Queue**: `QUEUE_NAME`, `RETRY_INTERVAL`, `RETRY_MAX_ATTEMPTS`
- **Spark**: `SPARK_APP_NAME`, `SPARK_MASTER`, `SPARK_TRIGGER_INTERVAL`
- **Storage**: `SPARK_STORAGE_PATH` (default: `hdfs://namenode:9000/telemetry-data`)
- **Format**: `SPARK_STORAGE_FORMAT` (parquet, json, csv), `SPARK_COMPRESSION` (snappy, gzip)

## Testing

```bash
mvn test
```

## Crash Detection Logic

The application uses Apache Spark to analyze incoming telematics data in micro-batches and detects crashes based on:

1. **High G-Force Events**: G-force readings ≥ 4.0g (configurable)
2. **Sudden Stops**: Speed ≤ 5.0 mph combined with G-force ≥ 2.0g

### Spark Processing Architecture
- **RabbitMQ Integration**: Custom bridge consumes messages from RabbitMQ queue
- **Micro-batch Processing**: Messages are processed in 2-second intervals using Spark
- **Dataset Operations**: Telematics data is processed as Spark Datasets for optimal performance
- **Fault Tolerance**: Spark's built-in resilience ensures reliable crash detection

When a crash is detected, the application:
- 🚨 Logs comprehensive crash alert with multi-sensor analysis
- 📤 Publishes detailed crash report to `crash_reports` queue  
- 💾 Writes crash report to persistent storage (filesystem/HDFS)
- 📍 Includes precise GPS location (accuracy, altitude, satellites)
- 🧭 Records vehicle heading and motion type (rolling, spinning, pitching)
- 📱 Captures device status (battery, signal, orientation)
- 🌦️ Considers environmental factors (weather, altitude)
- 🚑 Provides emergency notification recommendation with severity analysis

## Enhanced Sensor Capabilities

### GPS Intelligence
- **High-Precision Location**: Sub-5m accuracy with satellite count validation
- **Altitude Awareness**: Elevation data for mountain/highway crash analysis  
- **Speed Validation**: GPS speed vs vehicle speed cross-validation
- **Quality Assessment**: Automatic GPS reliability scoring

### Motion Analysis  
- **Gyroscope (Pitch/Roll/Yaw)**: Detects rollovers, spinning, and vehicle instability
- **Accelerometer**: Multi-axis G-force analysis (total, lateral, vertical)
- **Magnetometer**: Compass heading and directional impact analysis

### Environmental Monitoring
- **Barometric Pressure**: Weather event and altitude detection
- **Device Health**: Battery, signal strength, and orientation monitoring
- **Data Quality**: Adaptive thresholds based on sensor reliability

### Crash Type Classification
- **ROLLOVER**: Roll angular velocity > 3.0 rad/s
- **SPINNING**: Yaw rotation > 2.0 rad/s  
- **SIDE_IMPACT**: Lateral G-force > 3.0g
- **VERTICAL_IMPACT**: Vertical G-force > 2.0g (jumps, drops)
- **PITCHING**: Forward/backward motion > 2.0 rad/s
- **HIGH_G_FORCE**: Total acceleration > configurable threshold

## Data Storage Architecture

### Telemetry Data Storage
All incoming telemetry data is written to persistent storage using Spark:

**Local Mode**: 
- Path: `/tmp/telemetry-data`
- Format: Parquet with Snappy compression
- Partitioning: `policy_id/year/month/date`

**Cloud Mode (HDFS)**:
- Path: `hdfs://namenode:9000/telemetry-data` (configurable)
- Format: Parquet (configurable to JSON, CSV, Delta)
- Partitioning: Automatic by policy ID and date for optimal query performance
- Compression: Snappy (configurable to Gzip, LZ4)

### Crash Reports Storage
Crash reports are stored separately:
- Path: `{storage_path}/crash-reports`
- Partitioning: `severity_level/report_date`
- Includes enriched data: risk scores, severity analysis, total G-force calculations

### Storage Features
- **Automatic Partitioning**: Efficient querying by policy, date, and severity
- **Compression**: Reduced storage footprint with configurable codecs
- **Format Flexibility**: Supports multiple formats based on downstream requirements
- **Retry Logic**: Fallback mechanisms for storage failures
- **Statistics Logging**: Automatic generation of storage and processing statistics