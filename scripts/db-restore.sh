#!/bin/bash

# ========================================
# PostgreSQL Database Restore Script
# ========================================
# This script restores a Mohe database backup
# to the Docker container or local PostgreSQL

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
CONTAINER_NAME="mohe-postgres"
DB_NAME="${POSTGRES_DB:-mohe_db}"
DB_USER="${DB_USERNAME:-mohe_user}"
BACKUP_DIR="./db-backups"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Mohe Database Restore${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Function to show usage
usage() {
    echo "Usage: $0 [backup-file]"
    echo ""
    echo "Options:"
    echo "  backup-file    Path to backup file (optional)"
    echo "                 If not provided, will use latest backup"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Restore latest backup"
    echo "  $0 mohe_backup_20250114_120000.sql  # Restore specific backup"
    echo "  $0 ~/Downloads/mohe_backup.sql       # Restore from custom path"
    exit 1
}

# Parse arguments
if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    usage
fi

# Determine backup file
if [ -z "$1" ]; then
    # Use latest backup
    if [ -L "${BACKUP_DIR}/latest.sql" ]; then
        BACKUP_FILE="${BACKUP_DIR}/latest.sql"
        echo -e "${BLUE}Using latest backup${NC}"
    elif [ -L "${BACKUP_DIR}/latest.sql.gz" ]; then
        BACKUP_FILE="${BACKUP_DIR}/latest.sql.gz"
        echo -e "${BLUE}Using latest compressed backup${NC}"
    else
        echo -e "${RED}Error: No backup file specified and no latest backup found${NC}"
        echo ""
        echo "Available backups in ${BACKUP_DIR}:"
        if [ -d "$BACKUP_DIR" ]; then
            ls -lh "$BACKUP_DIR"/*.sql "$BACKUP_DIR"/*.sql.gz 2>/dev/null || echo "  (none)"
        else
            echo "  (backup directory not found)"
        fi
        echo ""
        usage
    fi
else
    BACKUP_FILE="$1"
    if [ ! -f "$BACKUP_FILE" ]; then
        # Try in backup directory
        if [ -f "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
            BACKUP_FILE="${BACKUP_DIR}/${BACKUP_FILE}"
        else
            echo -e "${RED}Error: Backup file not found: ${BACKUP_FILE}${NC}"
            exit 1
        fi
    fi
fi

echo -e "${YELLOW}Backup file: ${NC}${BACKUP_FILE}"
BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo -e "${YELLOW}Size: ${NC}${BACKUP_SIZE}"
echo ""

# Check if file is compressed
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo -e "${BLUE}Decompressing backup...${NC}"
    TEMP_FILE="${BACKUP_FILE%.gz}"
    gunzip -c "$BACKUP_FILE" > "$TEMP_FILE"
    BACKUP_FILE="$TEMP_FILE"
    COMPRESSED=true
    echo -e "${GREEN}✓ Decompressed${NC}"
    echo ""
fi

# Check if container is running
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    RESTORE_MODE="docker"
    echo -e "${BLUE}Restore mode: Docker container${NC}"
    echo -e "${YELLOW}Container: ${NC}${CONTAINER_NAME}"
elif command -v psql &> /dev/null; then
    RESTORE_MODE="local"
    echo -e "${BLUE}Restore mode: Local PostgreSQL${NC}"
    echo -e "${YELLOW}Note: Container not running, will restore to local PostgreSQL${NC}"
else
    echo -e "${RED}Error: Neither Docker container is running nor local PostgreSQL is available${NC}"
    exit 1
fi

echo -e "${YELLOW}Database: ${NC}${DB_NAME}"
echo -e "${YELLOW}User: ${NC}${DB_USER}"
echo ""

# Warning
echo -e "${RED}⚠️  WARNING: This will drop and recreate the database!${NC}"
echo -e "${RED}   All existing data will be lost.${NC}"
echo ""
read -p "Are you sure you want to continue? (yes/no) " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo -e "${YELLOW}Restore cancelled${NC}"
    [ "$COMPRESSED" = true ] && rm -f "$BACKUP_FILE"
    exit 0
fi

echo ""
echo -e "${GREEN}Starting restore...${NC}"
echo ""

# Restore based on mode
if [ "$RESTORE_MODE" == "docker" ]; then
    # Docker restore - COMPLETE restoration
    echo -e "${BLUE}Stopping application container...${NC}"
    docker compose stop app 2>/dev/null || true
    echo ""

    echo -e "${GREEN}Restoring COMPLETE database...${NC}"
    echo -e "${YELLOW}This will:${NC}"
    echo -e "${YELLOW}  1. Drop existing database${NC}"
    echo -e "${YELLOW}  2. Create new database${NC}"
    echo -e "${YELLOW}  3. Restore all tables, indexes, sequences${NC}"
    echo -e "${YELLOW}  4. Restore all data${NC}"
    echo ""

    # Restore using postgres database (to allow DROP/CREATE of mohe_db)
    docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d postgres < "$BACKUP_FILE"

    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Database restored successfully!${NC}"
        echo ""

        # Verify restoration
        echo -e "${BLUE}Verifying restored database...${NC}"
        TABLE_COUNT=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs)
        PLACES_COUNT=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM places;" 2>/dev/null | xargs || echo "0")
        USERS_COUNT=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")

        echo -e "${GREEN}  ✓ Tables: ${TABLE_COUNT}${NC}"
        echo -e "${GREEN}  ✓ Places: ${PLACES_COUNT}${NC}"
        echo -e "${GREEN}  ✓ Users: ${USERS_COUNT}${NC}"
        echo ""

        echo -e "${BLUE}Restarting application...${NC}"
        docker compose start app
        sleep 3
        echo -e "${GREEN}✓ Application started${NC}"
    else
        echo -e "${RED}✗ Restore failed!${NC}"
        docker compose start app
        [ "$COMPRESSED" = true ] && rm -f "$BACKUP_FILE"
        exit 1
    fi
else
    # Local restore - COMPLETE restoration
    echo -e "${BLUE}Restoring COMPLETE database to local PostgreSQL...${NC}"
    echo ""

    # Load .env for credentials if exists
    if [ -f .env ]; then
        export $(cat .env | grep -v '^#' | xargs)
    fi

    PGPASSWORD="${DB_PASSWORD:-mohe_password}" psql \
        -h localhost \
        -p 5432 \
        -U "$DB_USER" \
        -d postgres \
        < "$BACKUP_FILE"

    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Database restored successfully!${NC}"
        echo ""

        # Verify restoration
        echo -e "${BLUE}Verifying restored database...${NC}"
        TABLE_COUNT=$(PGPASSWORD="${DB_PASSWORD:-mohe_password}" psql -h localhost -p 5432 -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs)
        echo -e "${GREEN}  ✓ Tables: ${TABLE_COUNT}${NC}"
    else
        echo -e "${RED}✗ Restore failed!${NC}"
        [ "$COMPRESSED" = true ] && rm -f "$BACKUP_FILE"
        exit 1
    fi
fi

# Cleanup temporary file
if [ "$COMPRESSED" = true ]; then
    rm -f "$BACKUP_FILE"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Restore completed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Verify the data: docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME"
echo "  2. Check application logs: docker logs -f mohe-spring-app"
echo "  3. Test the API: curl http://localhost:8080/health"
