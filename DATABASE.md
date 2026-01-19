# Database Schema (PostgreSQL - javadb)

This file documents the exact database creation and table schema for the **Personal Expense Tracker** project.

## 1. Database Creation

```sql
DROP DATABASE IF EXISTS javadb;

CREATE DATABASE javadb
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'English_United States.1252'
    LC_CTYPE = 'English_United States.1252'
    LOCALE_PROVIDER = 'libc'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;
-- Users table (for login)
CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,          
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transactions table (income & expenses)
CREATE TABLE IF NOT EXISTS transactions (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER REFERENCES users(id) ON DELETE CASCADE,
    date        DATE NOT NULL,
    description VARCHAR(255),
    category    VARCHAR(100),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('Income', 'Expense')),
    amount      DECIMAL(15,2) NOT NULL,           -- positive = income, negative = expense
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);