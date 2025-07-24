#!/bin/bash

# Telematics Simulator Runner Script
# Usage: ./run-telematics.sh [--clean]

set -e

APP_NAME="crash-detection-telematics"
JAR_FILE="target/${APP_NAME}-1.0.0-SNAPSHOT.jar"
PID_FILE=".telematics.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}🚨 Crash Detection Processor${NC}"
    echo -e "${BLUE}=============================${NC}"
}

stop_existing_processes() {
    echo -e "${YELLOW}🔍 Looking for existing telematics processes...${NC}"
    
    # Find processes by jar name
    EXISTING_PIDS=$(ps aux | grep "${APP_NAME}" | grep -v grep | awk '{print $2}' || true)
    
    if [ -n "$EXISTING_PIDS" ]; then
        echo -e "${YELLOW}📋 Found existing processes: $EXISTING_PIDS${NC}"
        for pid in $EXISTING_PIDS; do
            echo -e "${RED}🛑 Stopping process $pid...${NC}"
            kill -TERM $pid 2>/dev/null || true
            sleep 2
            # Force kill if still running
            if kill -0 $pid 2>/dev/null; then
                echo -e "${RED}💀 Force killing process $pid...${NC}"
                kill -KILL $pid 2>/dev/null || true
            fi
        done
        echo -e "${GREEN}✅ All existing processes stopped${NC}"
    else
        echo -e "${GREEN}✅ No existing processes found${NC}"
    fi
    
    # Clean up PID file
    if [ -f "$PID_FILE" ]; then
        rm -f "$PID_FILE"
    fi
}

check_rabbitmq() {
    echo -e "${YELLOW}🐰 Checking RabbitMQ connection...${NC}"
    
    # Try to connect to RabbitMQ
    if ! nc -z localhost 5672 2>/dev/null; then
        echo -e "${RED}❌ RabbitMQ is not running on localhost:5672${NC}"
        echo -e "${YELLOW}💡 Start RabbitMQ with: brew services start rabbitmq${NC}"
        echo -e "${YELLOW}💡 Or with Docker: docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ RabbitMQ is running${NC}"
}

build_application() {
    echo -e "${YELLOW}🔨 Building application...${NC}"
    
    if ! mvn clean package -DskipTests -q; then
        echo -e "${RED}❌ Build failed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Build successful${NC}"
}

start_application() {
    echo -e "${YELLOW}🚀 Starting crash detection processor...${NC}"
    
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}❌ JAR file not found: $JAR_FILE${NC}"
        echo -e "${YELLOW}💡 Run without --clean to build the application${NC}"
        exit 1
    fi
    
    # Start the application in background
    nohup java -jar "$JAR_FILE" > telematics.log 2>&1 &
    APP_PID=$!
    
    # Save PID
    echo $APP_PID > "$PID_FILE"
    
    echo -e "${GREEN}✅ Application started with PID: $APP_PID${NC}"
    echo -e "${BLUE}👂 Listening to queue: telematics_stream${NC}"
    echo -e "${BLUE}🔍 G-force threshold: 4.0g${NC}"
    echo -e "${BLUE}⚡ Speed threshold: 5.0 mph${NC}"
    echo -e "${BLUE}🚨 Crash detection: ACTIVE${NC}"
    
    # Wait a moment and check if it's still running
    sleep 3
    if ! kill -0 $APP_PID 2>/dev/null; then
        echo -e "${RED}❌ Application failed to start. Check telematics.log for details.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}🎉 Crash detection processor is running!${NC}"
    echo -e "${YELLOW}📝 View logs: tail -f telematics.log${NC}"
    echo -e "${YELLOW}🛑 Stop with: kill $APP_PID or ./run-telematics.sh --clean${NC}"
}

show_usage() {
    echo "Usage: $0 [--clean]"
    echo ""
    echo "Options:"
    echo "  --clean    Only stop existing processes, don't start new instance"
    echo "  --help     Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0              # Stop old processes, build, and start new instance"
    echo "  $0 --clean      # Only stop existing processes"
}

# Main script logic
main() {
    print_header
    
    case "${1:-}" in
        --clean)
            stop_existing_processes
            echo -e "${GREEN}🧹 Cleanup complete${NC}"
            ;;
        --help)
            show_usage
            ;;
        "")
            stop_existing_processes
            check_rabbitmq
            build_application
            start_application
            ;;
        *)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            show_usage
            exit 1
            ;;
    esac
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi