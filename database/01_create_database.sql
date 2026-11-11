-- Smart Healthcare Management System - database bootstrap (MySQL 8+)
-- Run as a MySQL admin user (e.g. root):   mysql -u root -p < database/01_create_database.sql
-- Change the password below BEFORE running it. Do not commit real passwords to GitHub.

CREATE DATABASE IF NOT EXISTS smart_healthcare
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Dedicated application user (recommended instead of using root).
CREATE USER IF NOT EXISTS 'hms_user'@'localhost' IDENTIFIED BY 'ChangeThisPassword#1';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON smart_healthcare.* TO 'hms_user'@'localhost';
FLUSH PRIVILEGES;
