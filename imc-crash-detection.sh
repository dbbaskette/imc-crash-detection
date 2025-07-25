#!/bin/bash

#
# IMC Crash Detection Control Script
# Provides clean start/stop/status functionality for the crash detection processor
#

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="imc-crash-detection"
JAR_NAME="crash-detection-telematics-1.0.0-SNAPSHOT.jar"
PID_FILE="${SCRIPT_DIR}/.${APP_NAME}.pid"
LOG_FILE="${SCRIPT_DIR}/logs/${APP_NAME}.log"
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
MAX_STARTUP_WAIT=60
MAX_SHUTDOWN_WAIT=30

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Create logs directory if it doesn't exist
mkdir -p "${SCRIPT_DIR}/logs"

# Function to read configuration from application.yml
read_config() {
    log_info "Reading configuration from application.yml..."
    
    if [[ -f "src/main/resources/application.yml" ]]; then
        RABBITMQ_HOST=$(awk '/^[[:space:]]*rabbitmq:/{flag=1; next} flag && /^[[:space:]]*host:/{print $2; flag=0}' src/main/resources/application.yml | tr -d '"' || echo "localhost")
        RABBITMQ_PORT=$(awk '/^[[:space:]]*rabbitmq:/{flag=1; next} flag && /^[[:space:]]*port:/{print $2; flag=0}' src/main/resources/application.yml || echo "5672")
        RABBITMQ_USER=$(awk '/^[[:space:]]*rabbitmq:/{flag=1; next} flag && /^[[:space:]]*username:/{print $2; flag=0}' src/main/resources/application.yml | tr -d '"' || echo "guest")
        RABBITMQ_VHOST=$(awk '/^[[:space:]]*rabbitmq:/{flag=1; next} flag && /^[[:space:]]*virtual-host:/{print $2; flag=0}' src/main/resources/application.yml | tr -d '"' || echo "/")
        
        # Read queue names
        INPUT_QUEUE=$(awk '/destination:/{print $2}' src/main/resources/application.yml | head -1 | tr -d '"' || echo "telematics_work_queue")
        OUTPUT_QUEUE=$(awk '/destination:/{print $2}' src/main/resources/application.yml | tail -1 | tr -d '"' || echo "crash_reports")
        
        # Read thresholds
        G_FORCE_THRESHOLD=$(awk '/g-force-threshold:/{print $2}' src/main/resources/application.yml || echo "4.0")
        SPEED_THRESHOLD=$(awk '/speed-threshold:/{print $2}' src/main/resources/application.yml || echo "5.0")
        
        # Read storage path
        STORAGE_PATH=$(awk '/^[[:space:]]*storage:/{flag=1; next} flag && /^[[:space:]]*path:/{print $2; flag=0}' src/main/resources/application.yml | tr -d '"' || echo "file:///tmp/telemetry-data")
        
        # Set defaults for empty values
        [[ -z "$RABBITMQ_HOST" ]] && RABBITMQ_HOST="localhost"
        [[ -z "$RABBITMQ_PORT" ]] && RABBITMQ_PORT="5672"
        [[ -z "$RABBITMQ_USER" ]] && RABBITMQ_USER="guest"
        [[ -z "$RABBITMQ_VHOST" ]] && RABBITMQ_VHOST="/"
        [[ -z "$INPUT_QUEUE" ]] && INPUT_QUEUE="telematics_work_queue"
        [[ -z "$OUTPUT_QUEUE" ]] && OUTPUT_QUEUE="crash_reports"
        [[ -z "$G_FORCE_THRESHOLD" ]] && G_FORCE_THRESHOLD="4.0"
        [[ -z "$SPEED_THRESHOLD" ]] && SPEED_THRESHOLD="5.0"
        [[ -z "$STORAGE_PATH" ]] && STORAGE_PATH="file:///tmp/telemetry-data"
        
        log_success "Configuration loaded successfully"
    else
        # Fallback defaults
        RABBITMQ_HOST="localhost"
        RABBITMQ_PORT="5672"
        RABBITMQ_USER="guest"
        RABBITMQ_VHOST="/"
        INPUT_QUEUE="telematics_work_queue"
        OUTPUT_QUEUE="crash_reports"
        G_FORCE_THRESHOLD="4.0"
        SPEED_THRESHOLD="5.0"
        STORAGE_PATH="file:///tmp/telemetry-data"
        log_warning "Using default configuration"
    fi
}

# Function to check if RabbitMQ is running
check_rabbitmq() {
    log_info "Checking RabbitMQ connection..."
    log_info "   Host: ${RABBITMQ_HOST}:${RABBITMQ_PORT}"
    log_info "   User: ${RABBITMQ_USER}"
    log_info "   VHost: ${RABBITMQ_VHOST}"
    
    if ! command -v nc >/dev/null 2>&1; then
        log_warning "netcat (nc) not available, skipping RabbitMQ connectivity check"
        return 0
    fi
    
    if ! nc -z "$RABBITMQ_HOST" "$RABBITMQ_PORT" 2>/dev/null; then
        log_warning "RabbitMQ is not running on ${RABBITMQ_HOST}:${RABBITMQ_PORT}"
        log_info "💡 Start RabbitMQ with: brew services start rabbitmq"
        if [[ "$RABBITMQ_HOST" == "localhost" && "$RABBITMQ_PORT" == "5672" ]]; then
            log_info "💡 Or with Docker: docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management"
        else
            log_info "💡 Or with Docker: docker run -d --name rabbitmq -p ${RABBITMQ_PORT}:5672 -p 15672:15672 rabbitmq:3-management"
        fi
        log_info "🚀 Starting app anyway - it will wait for RabbitMQ to come up (up to 5 minutes)"
        return 0
    fi
    
    log_success "RabbitMQ is running on ${RABBITMQ_HOST}:${RABBITMQ_PORT}"
    return 0
}

# Function to build the application
build_application() {
    log_info "Building application..."
    
    if ! mvn clean package -DskipTests -q; then
        log_error "Build failed"
        return 1
    fi
    
    log_success "Build successful"
    return 0
}

# Function to get process status
get_status() {
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "running"
            return 0
        else
            # PID file exists but process is dead
            rm -f "$PID_FILE"
            echo "stopped"
            return 1
        fi
    else
        echo "stopped"
        return 1
    fi
}

# Function to wait for application startup
wait_for_startup() {
    log_info "Waiting for application to start processing messages..."
    local count=0
    while [[ $count -lt $MAX_STARTUP_WAIT ]]; do
        # Check if the process is still running and if there are recent log entries indicating it's processing
        local pid=$(cat "$PID_FILE" 2>/dev/null)
        if [[ -n "$pid" ]] && ps -p "$pid" > /dev/null 2>&1; then
            # Check if we see recent processing logs (Spring Boot startup or message processing)
            if [[ -f "$LOG_FILE" ]] && tail -20 "$LOG_FILE" | grep -E "(Started TelematicsApplication|Processing telemetrics message|Spark|Spring Boot)" >/dev/null 2>&1; then
                log_success "Application is running and processing messages"
                return 0
            fi
        else
            log_error "Application process died during startup"
            return 1
        fi
        sleep 2
        ((count++))
    done
    log_error "Application failed to show processing activity within ${MAX_STARTUP_WAIT} seconds"
    return 1
}

# Function to start the application
start_app() {
    local current_status=$(get_status)
    
    if [[ "$current_status" == "running" ]]; then
        log_warning "Application is already running (PID: $(cat "$PID_FILE"))"
        return 0
    fi
    
    log_info "Starting $APP_NAME..."
    
    # Read configuration
    read_config
    
    # Check RabbitMQ
    check_rabbitmq
    
    # Build application
    if ! build_application; then
        log_error "Failed to build application"
        return 1
    fi
    
    # Check if JAR exists
    local jar_file="target/${JAR_NAME}"
    if [[ ! -f "$jar_file" ]]; then
        log_error "JAR file not found: $jar_file"
        return 1
    fi
    
    # Start the Spring Boot application in background with logging
    cd "$SCRIPT_DIR"
    log_info "Application output will be logged to: $LOG_FILE"
    
    {
        echo "=== IMC Crash Detection Processor Started at $(date) ==="
        echo "🚨 Starting Crash Detection Processor with Apache Spark Integration"
        echo "🐰 RabbitMQ: ${RABBITMQ_HOST}:${RABBITMQ_PORT} (user: ${RABBITMQ_USER}, vhost: ${RABBITMQ_VHOST})"
        echo "⚡ Spark: Embedded local[*] mode for real-time processing"
        echo "👂 Input queue: ${INPUT_QUEUE}"
        echo "📤 Output queue: ${OUTPUT_QUEUE}"
        echo "🔍 G-force threshold: ${G_FORCE_THRESHOLD}g"
        echo "⚡ Speed threshold: ${SPEED_THRESHOLD} mph"
        echo "💾 Storage: ${STORAGE_PATH}"
        echo "🚨 Crash detection: ACTIVE with multi-sensor analysis"
        echo "========================================================"
        
        # Use Java with necessary JVM args for Spark 3.5.0 + Java 21 compatibility
        java --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
             --add-opens=java.base/java.lang=ALL-UNNAMED \
             --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
             -jar "$jar_file" 2>&1
    } >> "$LOG_FILE" &
    local app_pid=$!
    
    # Save PID to file
    echo "$app_pid" > "$PID_FILE"
    log_info "Application started with PID: $app_pid"
    
    # Wait for application to start processing
    if wait_for_startup; then
        log_success "$APP_NAME started successfully"
        echo ""
        log_info "📋 Monitoring:"
        log_info "   Logs: $0 --logs"
        log_info "   Process: ps -p $app_pid"
        log_info "   Status: $0 --status"
        echo ""
        log_info "🔧 Configuration:"
        log_info "   G-force threshold: ${G_FORCE_THRESHOLD}g"
        log_info "   Speed threshold: ${SPEED_THRESHOLD} mph"
        log_info "   Input queue: ${INPUT_QUEUE}"
        log_info "   Output queue: ${OUTPUT_QUEUE}"
        echo ""
        log_info "📱 Use '$0 --stop' to shutdown cleanly"
        return 0
    else
        log_error "Application failed to start properly"
        stop_app
        return 1
    fi
}

# Function to stop the application
stop_app() {
    local current_status=$(get_status)
    
    if [[ "$current_status" == "stopped" ]]; then
        log_warning "Application is not running"
        return 0
    fi
    
    local pid=$(cat "$PID_FILE")
    log_info "Stopping $APP_NAME (PID: $pid)..."
    
    # Send SIGTERM for graceful shutdown
    log_info "Sending SIGTERM for graceful shutdown"
    kill -TERM "$pid" 2>/dev/null || true
    
    # Wait for graceful shutdown
    local count=0
    while [[ $count -lt $MAX_SHUTDOWN_WAIT ]]; do
        if ! ps -p "$pid" > /dev/null 2>&1; then
            log_success "Application stopped gracefully"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
        ((count++))
    done
    
    # Force kill if still running
    log_warning "Graceful shutdown timed out, forcing termination"
    kill -KILL "$pid" 2>/dev/null || true
    rm -f "$PID_FILE"
    log_success "Application stopped (forced)"
}

# Function to tail logs
tail_logs() {
    if [[ ! -f "$LOG_FILE" ]]; then
        log_error "Log file does not exist: $LOG_FILE"
        log_info "Start the application first with: $0 --start"
        return 1
    fi
    
    log_info "Tailing logs from: $LOG_FILE"
    log_info "Press Ctrl+C to stop tailing"
    echo ""
    tail -f "$LOG_FILE"
}

# Function to show status
show_status() {
    local status=$(get_status)
    
    echo "=== IMC Crash Detection Processor Status ==="
    echo "Application: $status"
    
    if [[ "$status" == "running" ]]; then
        local pid=$(cat "$PID_FILE")
        echo "PID: $pid"
        echo "Uptime: $(ps -o etime= -p "$pid" 2>/dev/null || echo "unknown")"
        
        # Check if application is processing messages
        if [[ -f "$LOG_FILE" ]] && tail -10 "$LOG_FILE" | grep -E "(Processing telemetrics message|RAW MESSAGE DEBUG)" >/dev/null 2>&1; then
            echo "Status: Processing messages"
        else
            echo "Status: Idle or starting up"
        fi
        
        # Check RabbitMQ connectivity
        if [[ -n "${RABBITMQ_HOST:-}" && -n "${RABBITMQ_PORT:-}" ]] && command -v nc >/dev/null 2>&1; then
            if nc -z "$RABBITMQ_HOST" "$RABBITMQ_PORT" 2>/dev/null; then
                echo "RabbitMQ: connected (${RABBITMQ_HOST}:${RABBITMQ_PORT})"
            else
                echo "RabbitMQ: disconnected (${RABBITMQ_HOST}:${RABBITMQ_PORT})"
            fi
        else
            echo "RabbitMQ: unknown"
        fi
        
        echo "Log file: $LOG_FILE"
        echo "Message processing: $(tail -5 "$LOG_FILE" | grep -c "Processing telemetrics message" || echo "0") recent messages"
    fi
    
    echo "============================================="
}

# Function to show usage
show_usage() {
    echo "Usage: $0 {--start|--stop|--restart|--status|--logs|--clean}"
    echo ""
    echo "Commands:"
    echo "  --start    Start the crash detection processor"
    echo "  --stop     Stop the crash detection processor"
    echo "  --restart  Restart the crash detection processor"
    echo "  --status   Show current status"
    echo "  --logs     Tail the application logs (Ctrl+C to stop)"
    echo "  --clean    Stop application and clean up old processes"
    echo ""
    echo "Examples:"
    echo "  $0 --start     # Start the application"
    echo "  $0 --stop      # Stop the application"
    echo "  $0 --status    # Check if running"
    echo "  $0 --logs      # Follow the logs in real-time"
    echo "  $0 --clean     # Clean stop and remove old processes"
}

# Function to clean up old processes
clean_old_processes() {
    log_info "Looking for existing crash detection processes..."
    
    # Find processes by jar name and also look for Spring Boot apps with our app name
    local existing_pids=$(ps aux | grep -E "(${JAR_NAME}|crash-detection-telematics)" | grep -v grep | awk '{print $2}' || true)
    
    # Also check for any java process with our specific JVM args
    local jvm_pids=$(ps aux | grep java | grep -E "(add-opens.*java.base|crashdetection)" | grep -v grep | awk '{print $2}' || true)
    
    # Combine both searches
    local all_pids="$existing_pids $jvm_pids"
    all_pids=$(echo "$all_pids" | tr ' ' '\n' | sort -u | tr '\n' ' ')
    
    if [[ -n "$all_pids" && "$all_pids" != " " ]]; then
        log_warning "Found existing processes: $all_pids"
        for pid in $all_pids; do
            if [[ -n "$pid" && "$pid" != "" ]]; then
                log_info "Stopping process $pid..."
                kill -TERM "$pid" 2>/dev/null || true
                sleep 2
                # Force kill if still running
                if kill -0 "$pid" 2>/dev/null; then
                    log_warning "Force killing process $pid..."
                    kill -KILL "$pid" 2>/dev/null || true
                fi
            fi
        done
        log_success "All existing processes stopped"
    else
        log_success "No existing processes found"
    fi
    
    # Clean up PID file
    if [[ -f "$PID_FILE" ]]; then
        rm -f "$PID_FILE"
        log_info "Cleaned up PID file"
    fi
}

# Main execution
case "${1:-}" in
    --start)
        start_app
        ;;
    --stop)
        stop_app
        ;;
    --restart)
        stop_app
        sleep 2
        start_app
        ;;
    --status)
        show_status
        ;;
    --logs)
        tail_logs
        ;;
    --clean)
        clean_old_processes
        log_success "Cleanup complete"
        ;;
    --help|-h|help)
        show_usage
        ;;
    *)
        log_error "Invalid or missing command"
        echo ""
        show_usage
        exit 1
        ;;
esac