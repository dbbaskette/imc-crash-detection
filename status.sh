#!/bin/bash

# Telematics Simulator Status Script

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

APP_NAME="crash-detection-telematics"

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
    
    # Show process details
    for pid in $RUNNING_PIDS; do
        echo -e "${BLUE}📊 Process details for PID $pid:${NC}"
        ps -o pid,ppid,user,state,start,time,command -p $pid 2>/dev/null || echo "  No details available"
    done
    
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

# Show connection status
echo -e "\n${YELLOW}📡 Connection Status:${NC}"
if [ -n "$RUNNING_PIDS" ]; then
    echo -e "${GREEN}✅ Application is actively listening for telematics messages${NC}"
    echo -e "${BLUE}📋 To see real-time crash detection, watch the running application terminal${NC}"
else
    echo -e "${YELLOW}ℹ️  No active crash detection processor found${NC}"
fi

echo -e "\n${YELLOW}💡 Commands:${NC}"
echo -e "  Start:  ./run-local.sh    (logs stream to terminal)"
echo -e "  Stop:   ./run-local.sh --clean (or Ctrl+C in running terminal)"
echo -e "  Status: ./status.sh"