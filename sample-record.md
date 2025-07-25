# Sample Telemetry Message Format

This document contains the actual JSON message structure received from the RabbitMQ `telematics_work_queue` queue.

## Message Structure

The telemetry messages are JSON objects with the following structure:

```json
{
  "policyId": "POL-2024-001234",
  "vin": "1HGBH41JXMN109186",
  "timestamp": "2024-07-24T15:30:45.123Z",
  "speedMph": 65.5,
  "currentStreet": "Main Street",
  "gForce": 1.2,
  "sensors": {
    "accelerometer": {
      "x": 0.15,
      "y": -0.23,
      "z": 9.81
    },
    "gyroscope": {
      "x": 0.02,
      "y": -0.01,
      "z": 0.03
    },
    "magnetometer": {
      "x": 25.4,
      "y": -12.8,
      "z": 45.2
    },
    "barometric": {
      "pressure": 1013.25,
      "altitude": 150.0
    },
    "gps": {
      "latitude": 37.7749,
      "longitude": -122.4194,
      "accuracy": 5.0,
      "speed": 29.3
    },
    "deviceHealth": {
      "batteryLevel": 85,
      "signalStrength": -65,
      "temperature": 23.5
    }
  }
}
```

## Field Descriptions

### Root Level Fields
- `policyId`: Insurance policy identifier (String, may be null in test data)
- `vin`: Vehicle Identification Number (String)
- `timestamp`: ISO 8601 timestamp string
- `speedMph`: Vehicle speed in miles per hour (double)
- `currentStreet`: Current street name (String)
- `gForce`: Pre-calculated G-force value (double)

### Sensor Data (`sensors` object)
- `accelerometer`: 3-axis acceleration data (x, y, z in m/s²)
- `gyroscope`: 3-axis angular velocity data (x, y, z in rad/s)
- `magnetometer`: 3-axis magnetic field data (x, y, z in µT)
- `barometric`: Atmospheric pressure and calculated altitude
- `gps`: Location and GPS-derived speed data
- `deviceHealth`: Device status information

## Notes

1. **Field Naming**: All fields use camelCase naming convention
2. **Null Values**: Some fields like `policyId` may be null in test/sample data
3. **Data Types**: Numeric values are transmitted as JSON numbers (double precision)
4. **Timestamp Format**: ISO 8601 string format, not parsed to LocalDateTime
5. **Consumer Group**: Messages are consumed from `telematics_work_queue.crash-detection-group`

## Usage in Code

The `TelematicsMessage` record in the application matches this JSON structure:

```java
public record TelematicsMessage(
    @JsonProperty("policyId") String policyId,
    @JsonProperty("vin") String vin,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("speedMph") double speedMph,
    @JsonProperty("currentStreet") String currentStreet,
    @JsonProperty("gForce") double gForce,
    @JsonProperty("sensors") Sensors sensors
) { ... }
```

This documentation serves as the authoritative reference for the message format to avoid repeated clarification during development.
