#!/bin/bash

# ========================================
# PostgreSQL Database Backup Script
# ========================================
# This script creates a backup of the Mohe database
# from the Docker container

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
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="mohe_backup_${TIMESTAMP}.sql"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Mohe Database Backup${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Create backup directory if it doesn't exist
if [ ! -d "$BACKUP_DIR" ]; then
    echo -e "${YELLOW}Creating backup directory: ${BACKUP_DIR}${NC}"
    mkdir -p "$BACKUP_DIR"
fi

# Check if container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo -e "${RED}Error: Container '${CONTAINER_NAME}' is not running${NC}"
    echo "Please start the database with: docker compose up postgres"
    exit 1
fi

echo -e "${YELLOW}Container: ${NC}${CONTAINER_NAME}"
echo -e "${YELLOW}Database: ${NC}${DB_NAME}"
echo -e "${YELLOW}User: ${NC}${DB_USER}"
echo -e "${YELLOW}Backup file: ${NC}${BACKUP_PATH}"
echo ""

# Create backup - FULL database dump with all structures
echo -e "${GREEN}Creating COMPLETE database backup...${NC}"
echo -e "${YELLOW}Including: Database, Schemas, Tables, Indexes, Sequences, Constraints, Data${NC}"
echo ""

docker exec "$CONTAINER_NAME" pg_dump \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    --clean \
    --if-exists \
    --create \
    --encoding=UTF8 \
    --no-owner \
    --no-privileges \
    > "$BACKUP_PATH" 2>&1

# Filter out verbose messages that are not SQL
grep -v "^pg_dump:" "$BACKUP_PATH" > "${BACKUP_PATH}.tmp" && mv "${BACKUP_PATH}.tmp" "$BACKUP_PATH"

if [ $? -eq 0 ]; then
    BACKUP_SIZE=$(du -h "$BACKUP_PATH" | cut -f1)
    echo -e "${GREEN}✓ Backup completed successfully!${NC}"
    echo -e "${GREEN}  File: ${BACKUP_PATH}${NC}"
    echo -e "${GREEN}  Size: ${BACKUP_SIZE}${NC}"
    echo ""

    # Verify backup contents
    echo -e "${BLUE}Verifying backup contents...${NC}"
    TABLE_COUNT=$(grep -c "CREATE TABLE" "$BACKUP_PATH" || echo "0")
    INDEX_COUNT=$(grep -c "CREATE INDEX" "$BACKUP_PATH" || echo "0")
    SEQUENCE_COUNT=$(grep -c "CREATE SEQUENCE" "$BACKUP_PATH" || echo "0")

    echo -e "${GREEN}  ✓ Tables: ${TABLE_COUNT}${NC}"
    echo -e "${GREEN}  ✓ Indexes: ${INDEX_COUNT}${NC}"
    echo -e "${GREEN}  ✓ Sequences: ${SEQUENCE_COUNT}${NC}"
    echo ""

    # Optional: Compress the backup
    read -p "Do you want to compress the backup? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${GREEN}Compressing backup...${NC}"
        gzip "$BACKUP_PATH"
        COMPRESSED_SIZE=$(du -h "${BACKUP_PATH}.gz" | cut -f1)
        echo -e "${GREEN}✓ Compressed: ${BACKUP_PATH}.gz${NC}"
        echo -e "${GREEN}  Size: ${COMPRESSED_SIZE} (original: ${BACKUP_SIZE})${NC}"

        # Create symbolic link to compressed file
        ln -sf "${BACKUP_FILE}.gz" "${BACKUP_DIR}/latest.sql.gz"
        echo -e "${GREEN}✓ Created symlink: ${BACKUP_DIR}/latest.sql.gz${NC}"
    else
        # Create symbolic link to uncompressed file
        ln -sf "$BACKUP_FILE" "${BACKUP_DIR}/latest.sql"
        echo -e "${GREEN}✓ Created symlink: ${BACKUP_DIR}/latest.sql${NC}"
    fi

    echo ""

    # Show summary
    echo -e "${YELLOW}To transfer this backup to Mac Mini:${NC}"
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "  1. Copy the compressed file to Mac Mini:"
        echo "     scp ${BACKUP_PATH}.gz user@mac-mini-ip:~/mohe-backup.sql.gz"
    else
        echo "  1. Copy the file to Mac Mini:"
        echo "     scp $BACKUP_PATH user@mac-mini-ip:~/mohe-backup.sql"
    fi
    echo ""
    echo "  2. Or use the restore script on Mac Mini:"
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "     ./scripts/db-restore.sh ${BACKUP_FILE}.gz"
    else
        echo "     ./scripts/db-restore.sh $BACKUP_FILE"
    fi
    echo ""
else
    echo -e "${RED}✗ Backup failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Backup completed!${NC}"
echo -e "${GREEN}========================================${NC}"
