# Crash Detection Processor

A **Spring Boot application with Apache Spark integration** designed for real-time vehicle crash detection in an insurance context. The system processes vehicle telematics data in real-time using a micro-batch approach, consuming from RabbitMQ's `telematics_stream` queue and publishing crash reports to the `crash_reports` queue **only when crashes are detected**.

## 🏗️ Architecture Overview

The system processes vehicle telematics data in real-time using a micro-batch approach:
- **Input**: RabbitMQ `telematics_stream` queue (enhanced telemetry data)
- **Processing**: Apache Spark for stream processing and crash analysis
- **Output**: RabbitMQ `crash_reports` queue (enriched crash reports - **crash events only**)
- **Storage**: Filesystem/HDFS for all telemetry data persistence

## 🔧 Technology Stack

- **Spring Boot 3.5.3** with Java 21
- **Apache Spark 3.5.0** for distributed processing
- **RabbitMQ** for message queuing
- **Maven** for dependency management
- **Spring Cloud Stream** for messaging integration

## 📁 Key Components

### **Main Application**
- `TelematicsApplication.java` - Spring Boot entry point with detailed startup logging

### **Core Services**
- **`CrashDetectionService`** - Core crash detection logic with:
  - Configurable thresholds (G-force, speed, rollover, spinning)
  - Multi-sensor analysis (accelerometer, gyroscope, magnetometer)
  - Environmental factor consideration (weather, altitude)
  - Device health monitoring (battery, signal strength)

- **`SparkCrashProcessor`** - Advanced Spark-based processing:
  - **Queue Logic**: Processes all telematics messages but **only outputs crash reports to the queue**
  - Custom UDFs for crash pattern detection
  - Spark SQL analysis for complex queries
  - Batch processing with windowing
  - Data aggregations and enrichment
  - Risk score calculation with environmental and device health factors

- **`TelemetryDataWriter`** - Data persistence layer (stores ALL telemetry data)
- **`RabbitMQHealthChecker`** - Connection monitoring

### **Data Models**
- `TelematicsMessage` - Enhanced telemetry input format with comprehensive sensor data
- `CrashReport` - Structured crash event output with detailed analysis
- Sensor-specific models: `AccelerometerData`, `GyroscopeData`, `GpsData`, `MagnetometerData`, `DeviceInfo`

## 🚨 Crash Detection Features

### **Detection Types**
- **High G-Force Events** (configurable threshold, default: 4.0g)
- **Rollover Detection** (angular velocity analysis, threshold: 3.0 rad/s)
- **Spinning Detection** (gyroscope Z-axis patterns, threshold: 2.0 rad/s)
- **Side Impact** (lateral G-force analysis, threshold: 3.0g)
- **Sudden Stop** (speed + G-force correlation)
- **Vertical Impact** (altitude/barometric changes, threshold: 2.0g)

### **Multi-Sensor Analysis**
- **GPS**: Location, speed, altitude, accuracy, satellite count, bearing
- **Accelerometer**: 3-axis motion detection (x, y, z)
- **Gyroscope**: Angular velocity and rotation (pitch, roll, yaw)
- **Magnetometer**: Heading and orientation (x, y, z, heading)
- **Barometric**: Pressure and altitude estimation
- **Device Health**: Battery level, signal strength, orientation, charging status

### **Environmental Factors**
- Weather condition detection (pressure < 980 hPa or > 1050 hPa)
- High altitude adjustments (pressure < 900 hPa)
- GPS reliability assessment (accuracy and satellite count)
- Device health impact on thresholds (battery, signal)

## 🔄 Processing Flow

1. **Message Consumption**: RabbitMQ telematics data ingestion from `telematics_stream`
2. **Crash Detection**: Multi-criteria evaluation using `CrashDetectionService.isCrashEvent()`
3. **Conditional Output**: 
   - **Crash Detected**: Generate `CrashReport` with risk scoring and send to `crash_reports` queue
   - **Normal Telemetry**: Log and store data, but **no queue output**
4. **Batch Processing**: Spark micro-batch analysis with custom UDFs
5. **Data Persistence**: All telemetry data stored to filesystem/HDFS with partitioning
6. **Risk Scoring**: Dynamic calculation based on crash type, environment, and device health

## 🚀 Features

- **Apache Spark Integration**: Real-time stream processing with micro-batch capabilities
- **Multi-Sensor Analysis**: Processes accelerometer, gyroscope, magnetometer, barometric, GPS, and device data
- **Advanced Crash Detection**: Detects rollovers, spinning, side impacts, sudden stops, and vertical impacts
- **Smart Queue Logic**: **Only crash events are published to the output queue**
- **Environmental Monitoring**: Weather conditions and altitude considerations
- **Device Health Monitoring**: Battery, signal strength, and device status tracking
- **Data Persistence**: All telemetry data written to filesystem/HDFS using Spark
- **Configurable Storage**: Supports Parquet, JSON, CSV with partitioning and compression
- **Configurable Thresholds**: Adjustable detection thresholds via application properties
- **Comprehensive Alerting**: Detailed crash event logging with multi-sensor analysis
- **Spark SQL Analysis**: Custom UDFs and SQL queries for pattern detection
- **Resilient Processing**: Built-in retry logic and Spark fault tolerance
- **Cloud Ready**: Local development and Cloud Foundry deployment support
- **Spring Cloud Stream**: Compatible with Spring Cloud Data Flow

## 📋 Message Formats

### Input: Telematics Message Format (`telematics_stream` queue)

The system consumes comprehensive telematics messages with the following structure:

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
      "x": 0.0123,
      "y": -0.0045,
      "z": 0.0089
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

### Output: Crash Report Format (`crash_reports` queue)

**Important**: Only crash-detected events are published to this queue. Normal telemetry data is stored but not published.

```json
{
  "report_id": "CRASH-IMC98675-1721834354123",
  "policy_id": "IMC-98675",
  "vin": "1HGBH41JXMN109186",
  "crash_timestamp": "2025-07-24T17:19:14.123456Z",
  "crash_type": "HIGH_G_FORCE",
  "severity_level": "CRITICAL",
  "impact_details": {
    "total_g_force": 8.67,
    "lateral_g_force": 6.23,
    "vertical_g_force": 2.45,
    "speed_at_impact": 35.5,
    "acceleration_vector": {
      "x": 6.5432,
      "y": 5.8901,
      "z": 1.2345,
      "magnitude": 8.67
    },
    "deceleration_rate": 76.12
  },
  "location": {
    "latitude": 33.7749,
    "longitude": -84.3877,
    "current_street": "Peachtree St & 10th St",
    "heading": 45.2,
    "accuracy_meters": 3.2
  },
  "vehicle_data": {
    "vin": "1HGBH41JXMN109186",
    "speed_mph": 35.5,
    "heading": 45.2
  },
  "sensor_analysis": {
    "accelerometer": {
      "raw_values": {
        "x": 6.5432,
        "y": 5.8901,
        "z": 1.2345,
        "magnitude": 8.67
      },
      "total_magnitude": 8.67,
      "lateral_magnitude": 6.23,
      "vertical_magnitude": 2.45
    },
    "gyroscope": {
      "x": 0.0123,
      "y": -0.0045,
      "z": 0.0089,
      "magnitude": 0.015,
      "indicates_rollover": false,
      "indicates_spinning": false
    },
    "magnetometer": {
      "x": 23.45,
      "y": -12.67,
      "z": 45.89,
      "heading_degrees": 45.2
    },
    "rollover_detected": false,
    "spinning_detected": false
  },
  "emergency_recommended": true,
  "risk_score": 85.4,
  "total_g_force": 8.67,
  "processed_timestamp": "2025-07-24T17:19:14.456789Z"
}
```

## Running Locally

### Prerequisites
- **Java 21** (current) or **Java 17** (recommended for production)
- Maven 3.6+
- RabbitMQ server running on localhost:5672

#### Java Version Compatibility

**Current Setup (Java 21):**
- Uses minimal JVM arguments to resolve Spark 3.5.0 + Java 21 module system conflicts
- Required JVM args: `--add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED`
- Spark UI disabled to avoid servlet dependency issues

**Production Recommendation (Java 17):**
- **More Stable**: Spark 3.5.0 has mature Java 17 support
- **No JVM Args Needed**: Clean startup without module system workarounds
- **Enterprise Ready**: Most enterprises standardize on Java 17 LTS
- To switch: Install Java 17 and update `java.version` in `pom.xml` to `17`

**Future Option (Java 21):**
- **Apache Spark 4.0**: Will have native Java 21 support (when released)
- **Alternative Frameworks**: Apache Flink, Kafka Streams have better Java 21 compatibility

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
**ALL incoming telemetry data** is written to persistent storage using Spark:

**Complete Data Fields Written:**
- **Basic**: `policy_id`, `vin`, `timestamp`, `speed_mph`, `current_street`, `g_force`
- **GPS**: `latitude`, `longitude`, `altitude`, `speed_ms`, `bearing`, `accuracy`, `satellite_count`, `gps_fix_time`
- **Accelerometer**: `x`, `y`, `z` (raw acceleration vectors)
- **Gyroscope**: `pitch`, `roll`, `yaw` (angular velocity)
- **Magnetometer**: `x`, `y`, `z`, `heading` (magnetic field and compass)
- **Environmental**: `barometric_pressure`
- **Device**: `battery_level`, `signal_strength`, `orientation`, `screen_on`, `charging`
- **Processing**: `processed_timestamp`, partitioning columns (`year`, `month`, `date`, `hour`)

**Local Mode**: 
- Path: `/tmp/telemetry-data`
- Format: Parquet with Snappy compression
- Structure: `policy_id=XXX/year=YYYY/month=MM/date=YYYY-MM-DD/part-*.parquet`

**Cloud Mode (HDFS)**:
- Path: `hdfs://namenode:9000/telemetry-data` (configurable)
- Format: Parquet (configurable to JSON, CSV, Delta)
- Partitioning: Organized by policy ID and date for optimal query performance
- Compression: Snappy (configurable to Gzip, LZ4)

**Note**: Partitioning columns (`policy_id`, `year`, `month`, `date`) are used for directory organization to enable efficient querying, but **ALL original telemetry data is preserved** in the files.

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