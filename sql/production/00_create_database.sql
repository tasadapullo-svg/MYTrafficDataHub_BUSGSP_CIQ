-- MYTrafficDataHub production database creation
-- Run once while connected to the PostgreSQL maintenance database (normally "postgres").
-- DBeaver: enable Auto-commit and execute CREATE DATABASE as a standalone statement.
-- If the database already exists, do not execute this file again.

CREATE DATABASE mytrafficdatahub
    WITH
    TEMPLATE = template0
    ENCODING = 'UTF8';

