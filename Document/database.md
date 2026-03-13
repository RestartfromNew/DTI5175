# Database Design Document
Project: Multi-AI intergration system

Version: 1.0

Author: Beining Yang

Update Date: 2026-02

## 1.Overview
### 1.1 Purpose 
This document describes the database schema design for the Database System module.
It defines tables, relationships, constraints, and security rules.

### 1.2 Scope
This version includes:
+ User authentication
+ Log in authorization and token

## 2.Entity Relationship Overview
+ User
+ Role 
+ User_Role

## 3.Table definition
### 3.1 user
| Field Name | Type | Constraint | Description|
|:----:|:----:|:----:|:----:|
|id|UUID|primary key;Not null|
|email|VARCHAR(255)|unique;Not null|
|username|VARCHAR(50)|Not null|
|password_hash| TEXT|Not null|
|is_active |BOOLEAN |Default True|
|is_verified|BOOLEAN |Default False|
|created_at|TIMESTAMPTZ| Default NOW()|
| updated_at| TIMESTAMPTZ|  Default NOW()|

```SQL
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 3.2 refresh_tokens Table
| Field Name   |     Type    |                      Constraint                      | Description                                                      |
|:----:|:----:|:----:|:----:|
|     id     |     UUID    |   Primary Key; Not Null; Default gen_random_uuid()   | Unique identifier for each refresh token                         |
|   user_id  |     UUID    | Foreign Key → users(id); On Delete Cascade; Not Null | References the user who owns this token                          |
| token_hash |     TEXT    |                       Not Null                       | Hashed version of the refresh token (never store raw token)      |
| expires_at | TIMESTAMPTZ |                       Not Null                       | Expiration timestamp of the refresh token                        |
|   revoked  |   BOOLEAN   |                     Default FALSE                    | Indicates whether the token has been revoked (logout / rotation) |
| created_at | TIMESTAMPTZ |                     Default NOW()                    | Timestamp when the token was created                             |

```SQL
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```
 