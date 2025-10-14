# Centralized Image Storage

## Overview

The Mohe platform uses a centralized image storage system where all images are stored on a single server (Mac Mini) while multiple worker machines can crawl and upload images remotely.

## Architecture

```
┌─────────────────┐      HTTP API       ┌─────────────────┐
│  MacBook Pro    │ ─────────────────▶  │   Mac Mini      │
│  (Worker)       │  POST /api/images   │  (Image Server) │
│                 │  /upload-from-urls  │                 │
│  - Crawls data  │                     │  - Stores images│
│  - Sends URLs   │                     │  - /images dir  │
└─────────────────┘                     └─────────────────┘

┌─────────────────┐
│  Other Workers  │ ─────────────────▶  Same Mac Mini
│                 │  Remote Upload
└─────────────────┘
```

## Components

### 1. ImageUploadController (Mac Mini)
REST API endpoints for receiving and storing images:
- `POST /api/images/upload-from-urls` - Upload images from URLs
- `POST /api/images/upload` - Direct file upload
- `DELETE /api/images/{placeId}` - Delete images
- `GET /api/images/health` - Health check

### 2. RemoteImageService (All Machines)
Client service that sends image URLs to Mac Mini via HTTP:
- Configured via environment variables
- Includes health checking
- Handles errors gracefully

### 3. DistributedImageService (All Machines)
Unified service that automatically chooses storage method:
- **Local storage**: When `MOHE_IMAGE_USE_REMOTE=false` (Mac Mini)
- **Remote storage**: When `MOHE_IMAGE_USE_REMOTE=true` (Workers)
- Automatic fallback to local if remote fails

## Configuration

### Mac Mini (Image Server)

Edit `.env` file:

```bash
# Image Storage - Mac Mini stores locally
MOHE_IMAGE_STORAGE_PATH=/images
MOHE_IMAGE_USE_REMOTE=false
MOHE_IMAGE_SERVER_URL=

# Port configuration
BACKEND_PORT=8080
```

**Important**:
- Set `MOHE_IMAGE_USE_REMOTE=false` so Mac Mini saves images locally
- Leave `MOHE_IMAGE_SERVER_URL` empty (not needed for server)
- Ensure `/images` directory exists with proper permissions

### MacBook Pro / Other Workers

Edit `.env` file:

```bash
# Image Storage - Workers send to Mac Mini
MOHE_IMAGE_STORAGE_PATH=/images
MOHE_IMAGE_USE_REMOTE=true
MOHE_IMAGE_SERVER_URL=http://192.168.1.100:8080

# Replace with actual Mac Mini IP and port
# Example: http://mac-mini.local:8080
```

**Important**:
- Set `MOHE_IMAGE_USE_REMOTE=true` to enable remote upload
- Set `MOHE_IMAGE_SERVER_URL` to Mac Mini's full URL (IP or hostname + port)
- Ensure network connectivity to Mac Mini
- Test the connection before starting batch jobs

## Setup Instructions

### Step 1: Prepare Mac Mini

1. Create images directory:
```bash
mkdir -p /Users/your-username/images
chmod 755 /Users/your-username/images
```

2. Update `.env` file:
```bash
MOHE_IMAGE_STORAGE_PATH=/Users/your-username/images
MOHE_IMAGE_USE_REMOTE=false
MOHE_IMAGE_SERVER_URL=
```

3. Start the application:
```bash
./gradlew bootRun
```

4. Verify the server is running:
```bash
curl http://localhost:8080/api/images/health
# Expected: {"success":true,"data":"OK",...}
```

### Step 2: Find Mac Mini IP Address

On Mac Mini, run:
```bash
# Method 1: Using ifconfig
ifconfig | grep "inet " | grep -v 127.0.0.1

# Method 2: Using hostname
hostname -I

# Method 3: System Preferences
# System Preferences → Network → Advanced → TCP/IP
```

Example output: `192.168.1.100`

### Step 3: Configure Worker Machines

1. On MacBook Pro / Worker, update `.env`:
```bash
MOHE_IMAGE_USE_REMOTE=true
MOHE_IMAGE_SERVER_URL=http://192.168.1.100:8080
```

2. Test connectivity:
```bash
curl http://192.168.1.100:8080/api/images/health
```

3. Start crawling job:
```bash
./gradlew bootRun --args='--spring.batch.job.names=distributedCrawlingJob'
```

## Testing

### Test 1: Health Check

From any machine:
```bash
curl http://mac-mini-ip:8080/api/images/health
```

Expected response:
```json
{
  "success": true,
  "data": "OK",
  "message": "Image server is running",
  "timestamp": "2025-01-14T10:30:00"
}
```

### Test 2: Upload Images from URLs

```bash
curl -X POST http://mac-mini-ip:8080/api/images/upload-from-urls \
  -H "Content-Type: application/json" \
  -d '{
    "placeId": 999,
    "placeName": "Test Place",
    "imageUrls": [
      "https://example.com/image1.jpg",
      "https://example.com/image2.jpg"
    ]
  }'
```

Expected response:
```json
{
  "success": true,
  "data": [
    "/images/999_test-place_1_abc123.jpg",
    "/images/999_test-place_2_def456.jpg"
  ],
  "message": "Successfully uploaded 2 images"
}
```

### Test 3: Verify Files on Mac Mini

```bash
ls -lh /Users/your-username/images/
# Should show downloaded image files
```

### Test 4: Run Distributed Crawling

1. Start job on Mac Mini:
```bash
./gradlew bootRun --args='--spring.batch.job.names=distributedCrawlingJob'
```

2. Start job on MacBook Pro:
```bash
./gradlew bootRun --args='--spring.batch.job.names=distributedCrawlingJob'
```

3. Monitor logs:
```bash
# Mac Mini logs should show:
💾 Using local image storage for: Place Name (123)

# Worker logs should show:
📡 Using remote image storage for: Place Name (456)
📤 Uploading 3 images to remote server for: Place Name (id=456)
✅ Successfully uploaded 3/3 images to remote server
```

## Monitoring

### Check Image Count

On Mac Mini:
```bash
# Count total images
ls -1 /Users/your-username/images/ | wc -l

# Count images by place ID
ls -1 /Users/your-username/images/ | cut -d'_' -f1 | sort | uniq -c
```

### Database Query

Check which places have images:
```sql
SELECT
    p.id,
    p.name,
    COUNT(pi.id) as image_count
FROM place p
LEFT JOIN place_image pi ON p.id = pi.place_id
GROUP BY p.id, p.name
HAVING COUNT(pi.id) > 0
ORDER BY image_count DESC;
```

### Application Logs

Monitor image upload activity:
```bash
# Mac Mini
tail -f logs/spring.log | grep "Image"

# Worker
tail -f logs/spring.log | grep "📡\|📤"
```

## Troubleshooting

### Issue: Worker cannot connect to Mac Mini

**Symptoms**:
```
❌ Error uploading images to remote server: Connection refused
⚠️ Remote upload failed, falling back to local storage
```

**Solutions**:
1. Check Mac Mini is running:
   ```bash
   curl http://mac-mini-ip:8080/health
   ```

2. Check firewall settings on Mac Mini:
   ```bash
   # macOS
   # System Preferences → Security & Privacy → Firewall
   # Allow incoming connections for Java
   ```

3. Verify IP address:
   ```bash
   ping mac-mini-ip
   ```

4. Check port is correct (default: 8080)

### Issue: Images saved locally instead of remotely

**Symptoms**:
```
💾 Using local image storage for: Place Name (123)
```

**Solutions**:
1. Check `.env` configuration:
   ```bash
   grep MOHE_IMAGE .env
   ```
   Should show:
   ```
   MOHE_IMAGE_USE_REMOTE=true
   MOHE_IMAGE_SERVER_URL=http://...
   ```

2. Restart the application after changing `.env`

### Issue: Permission denied when saving images

**Symptoms**:
```
❌ Failed to save image: Permission denied
```

**Solutions**:
1. Check directory permissions:
   ```bash
   ls -ld /Users/your-username/images
   ```

2. Fix permissions:
   ```bash
   chmod 755 /Users/your-username/images
   chown your-username:staff /Users/your-username/images
   ```

### Issue: Images not appearing in database

**Symptoms**:
- Files saved on disk
- `place_image` table empty

**Solutions**:
1. Check batch job logs for errors
2. Verify transaction is committing:
   ```bash
   grep "Saved.*places" logs/spring.log
   ```

3. Check database connection:
   ```sql
   SELECT COUNT(*) FROM place_image;
   ```

## Performance Optimization

### Network Bandwidth

For large-scale crawling with many images:

1. **Increase timeout** if images are large:
   ```yaml
   # application.yml
   spring:
     mvc:
       async:
         request-timeout: 300000  # 5 minutes
   ```

2. **Use compression** for image transfer (future enhancement)

3. **Monitor network usage**:
   ```bash
   # macOS
   nettop -m tcp
   ```

### Storage Space

Monitor disk usage on Mac Mini:
```bash
# Check available space
df -h /Users/your-username/images

# Check total image size
du -sh /Users/your-username/images

# Check largest images
du -ah /Users/your-username/images | sort -rh | head -20
```

### Concurrent Uploads

The system handles concurrent uploads automatically:
- RestTemplate uses connection pooling
- Mac Mini's ImageUploadController is thread-safe
- File writes use unique filenames (includes UUID)

## Disaster Recovery

### Backup Images

Regular backup of images directory:
```bash
# Create backup
tar -czf images-backup-$(date +%Y%m%d).tar.gz /Users/your-username/images

# Restore backup
tar -xzf images-backup-20250114.tar.gz -C /
```

### Sync to Cloud

Optional: Sync to cloud storage (AWS S3, Google Cloud Storage):
```bash
# AWS S3 example
aws s3 sync /Users/your-username/images s3://mohe-images/

# Google Cloud Storage example
gsutil -m rsync -r /Users/your-username/images gs://mohe-images/
```

## Advanced Configuration

### Custom Storage Path per Worker

If different workers need different paths:

```bash
# Worker 1
MOHE_IMAGE_STORAGE_PATH=/mnt/storage1/images

# Worker 2
MOHE_IMAGE_STORAGE_PATH=/mnt/storage2/images
```

### Load Balancing (Future)

For high-volume scenarios, use multiple image servers:
- NGINX load balancer
- Multiple Mac Mini servers
- Round-robin or least-connections algorithm

### CDN Integration (Future)

Serve images via CDN for better performance:
- Upload to S3/GCS after saving locally
- Configure CloudFront/Cloud CDN
- Update `place_image.url` to CDN URLs

## API Reference

### POST /api/images/upload-from-urls

Upload images from URLs to the image server.

**Request Body**:
```json
{
  "placeId": 123,
  "placeName": "My Place",
  "imageUrls": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.jpg"
  ]
}
```

**Response**:
```json
{
  "success": true,
  "data": [
    "/images/123_my-place_1_abc123.jpg",
    "/images/123_my-place_2_def456.jpg"
  ],
  "message": "Successfully uploaded 2 images",
  "timestamp": "2025-01-14T10:30:00"
}
```

### GET /api/images/health

Check image server health.

**Response**:
```json
{
  "success": true,
  "data": "OK",
  "message": "Image server is running",
  "timestamp": "2025-01-14T10:30:00"
}
```

### DELETE /api/images/{placeId}

Delete all images for a specific place.

**Response**:
```json
{
  "success": true,
  "data": null,
  "message": "Deleted images for place: 123",
  "timestamp": "2025-01-14T10:30:00"
}
```

## Summary

The centralized image storage system provides:

✅ **Single source of truth**: All images on Mac Mini
✅ **Distributed crawling**: Multiple workers can upload
✅ **Automatic routing**: Local vs remote based on config
✅ **Fault tolerance**: Fallback to local if remote fails
✅ **Easy monitoring**: Health checks and logs
✅ **No code changes**: Just environment variables

For questions or issues, check the troubleshooting section or review the logs.
