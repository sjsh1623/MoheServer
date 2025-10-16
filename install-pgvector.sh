#!/bin/bash

# Execute SQL on remote PostgreSQL database
PGPASSWORD=mohe_password psql -h 100.99.236.50 -p 16239 -U mohe_user -d mohe_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
PGPASSWORD=mohe_password psql -h 100.99.236.50 -p 16239 -U mohe_user -d mohe_db -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
