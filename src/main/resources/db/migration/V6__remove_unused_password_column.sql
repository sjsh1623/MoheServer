-- Remove unused 'password' column from users table
-- The application uses 'password_hash' instead

ALTER TABLE users 
    ALTER COLUMN password DROP NOT NULL;

-- Optional: Remove the column entirely
-- ALTER TABLE users DROP COLUMN password;
