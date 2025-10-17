-- Drop Flyway schema history table
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

-- Verify deletion
SELECT tablename FROM pg_tables WHERE tablename LIKE 'flyway%';
