SELECT current_database();
SELECT version();
SELECT current_user;
SELECT current_schema();

SELECT schema_name
FROM information_schema.schemata
ORDER BY schema_name;

SELECT extname, extversion
FROM pg_extension
ORDER BY extname;

SELECT name, default_version, installed_version
FROM pg_available_extensions
WHERE name IN ('postgis', 'pgcrypto')
ORDER BY name;
