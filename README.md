# Crash Detection Processor

A Spring Boot application that consumes vehicle telematics data from RabbitMQ and analyzes it for crash events in real-time.

## Features

- **Real-time Processing**: Consumes telematics messages from `telematics_stream` queue
- **Crash Detection**: Analyzes G-force and speed data to identify crash events
- **Configurable Thresholds**: Adjustable G-force and speed thresholds for detection
- **Comprehensive Alerting**: Detailed crash event logging with location and sensor data
- **Resilient Processing**: Built-in retry logic and error handling
- **Cloud Ready**: Configured for both local development and Cloud Foundry deployment
- Built with Spring Boot 3.5.3 and Java 21

## Message Format

### Normal Driving Data
```json
{
  "policy_id": "IMC-AUTO-98765",
  "timestamp": "2024-07-24T18:30:45.123Z",
  "speed_mph": 32.5,
  "sensors": {
    "gps": {
      "lat": 40.7130,
      "lon": -74.0058
    },
    "accelerometer": {
      "x": 0.15,
      "y": -0.23,
      "z": 0.98
    }
  },
  "g_force": 1.02
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
- Retry logic: 3 attempts with 1s intervals

### Cloud Environment (application-cloud.yml)
All settings configurable via environment variables:
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- `G_FORCE_THRESHOLD`, `SPEED_THRESHOLD`
- `QUEUE_NAME`, `RETRY_INTERVAL`, `RETRY_MAX_ATTEMPTS`

## Testing

```bash
mvn test
```

## Crash Detection Logic

The application analyzes incoming telematics data and detects crashes based on:

1. **High G-Force Events**: G-force readings ≥ 4.0g (configurable)
2. **Sudden Stops**: Speed ≤ 5.0 mph combined with G-force ≥ 2.0g

When a crash is detected, the application logs:
- 🚨 Crash alert with policy information
- Timestamp and location coordinates  
- Speed at time of impact
- G-force magnitude and accelerometer readings
- 🚑 Emergency notification recommendation