#!/bin/bash

# Telematics Simulator Status Script

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

APP_NAME="crash-detection-telematics"
PID_FILE=".telematics.pid"

echo -e "${BLUE}🚨 Crash Detection Processor Status${NC}"
echo -e "${BLUE}====================================${NC}"

# Check for running processes
RUNNING_PIDS=$(ps aux | grep "${APP_NAME}" | grep -v grep | awk '{print $2}' || true)

if [ -n "$RUNNING_PIDS" ]; then
    echo -e "${GREEN}✅ Crash detection processor is running${NC}"
    echo -e "${BLUE}📋 Process IDs: $RUNNING_PIDS${NC}"
    
    # Show memory usage
    for pid in $RUNNING_PIDS; do
        MEM_USAGE=$(ps -o pid,pmem,rss,comm -p $pid | tail -n 1)
        echo -e "${BLUE}💾 Memory usage: $MEM_USAGE${NC}"
    done
    
    # Show recent log entries if log file exists
    if [ -f "telematics.log" ]; then
        echo -e "\n${YELLOW}📝 Recent log entries (last 5 lines):${NC}"
        tail -5 telematics.log
    fi
    
else
    echo -e "${RED}❌ Crash detection processor is not running${NC}"
fi

# Check RabbitMQ status
echo -e "\n${YELLOW}🐰 RabbitMQ Status:${NC}"
if nc -z localhost 5672 2>/dev/null; then
    echo -e "${GREEN}✅ RabbitMQ is accessible on localhost:5672${NC}"
else
    echo -e "${RED}❌ RabbitMQ is not accessible on localhost:5672${NC}"
fi

# Check for PID file
if [ -f "$PID_FILE" ]; then
    STORED_PID=$(cat "$PID_FILE")
    if kill -0 "$STORED_PID" 2>/dev/null; then
        echo -e "${BLUE}📄 PID file matches running process: $STORED_PID${NC}"
    else
        echo -e "${YELLOW}⚠️  Stale PID file found (process $STORED_PID not running)${NC}"
    fi
fi

echo -e "\n${YELLOW}💡 Commands:${NC}"
echo -e "  Start:  ./run-local.sh"
echo -e "  Stop:   ./run-local.sh --clean"
echo -e "  Logs:   tail -f telematics.log"