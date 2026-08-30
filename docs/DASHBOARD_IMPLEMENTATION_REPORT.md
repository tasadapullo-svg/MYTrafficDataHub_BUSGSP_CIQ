# Dashboard Implementation Report

## Audit outcome

- Java 17 compile target; Spring Boot 4.0.3; Maven executable JAR.
- PostgreSQL 16.4 through Spring Boot datasource/HikariCP (min idle 2, max pool 12).
- Schemas: `core`, `jb`, `kuching`, `kl`, `melaka`.
- Metadata: `core.study_city`, `core.gtfs_feed`.
- Realtime map source: each city schema's `vehicle_latest_state`.
- History/statistics source: each city schema's `vehicle_observation`; `ingest_time` is the collector ingestion instant mapped from `ingest_timestamp_utc`.
- QC source: `vehicle_observation_qc` joined to `vehicle_observation`; Dashboard does not recalculate QC.
- Request metrics source: structured `api_request_log` in every city schema.
- Route data: persisted `route_short_name`, `route_long_name`, and `resolved_route_id`.
- Redis is available but has no established latest-vehicle contract in current code, so Dashboard v1 does not use or modify Redis.
- Existing indexes cover the Dashboard access paths. No DDL or new index was executed.
- No original collection, scheduling, parsing, deduplication, QC, database write, latest-state write, or Redis write code was changed.

## Technical design

- Backend is isolated under `com.mytransitgps.dashboard` and exposes only `/api/dashboard/**` GET endpoints.
- Repository schemas come only from a fixed four-city enum; arbitrary identifiers cannot enter SQL.
- Dashboard queries use a dedicated `JdbcTemplate` with an 8-second timeout and read-only service transactions.
- Map reads only latest-state tables and returns DTOs, never persistence entities.
- Today/yesterday use `Asia/Kuala_Lumpur` boundaries and `ingest_time`.
- Trend is aggregated by PostgreSQL into 5-minute buckets.
- Active vehicles are explicitly defined as eligible latest-state vehicles seen in the last 10 minutes.
- Abnormal vehicles are explicitly defined as distinct vehicles with persisted QC events in the last hour.
- Total records uses PostgreSQL statistics (`n_live_tup`) and is visibly marked as estimated to avoid recurring full scans of future very large tables.
- Frontend is Vue 3, Vite, TypeScript, Axios, Leaflet, ECharts (component imports), Vue I18n, and Vue Router lazy loading.
- One 300-second refresh cycle updates data without recreating the map; API failures are isolated with `Promise.allSettled`.
- Built output is embedded in Spring Boot under `static/dashboard`; no long-running Vite process is needed.

## Verification

- `npm run build`: PASS.
- `mvn test`: PASS, 50 tests and 0 failures/errors.
- `mvn -DskipTests clean package`: PASS.
- Isolated Spring Boot startup on port 18080: PASS.
- `/dashboard`: HTTP 200 and production HTML/assets loaded.
- All Dashboard endpoints: HTTP 200; invalid city: HTTP 400.
- Live vehicle response during smoke test: JB 136, Kuching 47, KL 446, Melaka 43.
- Endpoint latency after connection warm-up: vehicles 14-38 ms; acquisition 12 ms; trend 22 ms; anomalies 20 ms; request log 10 ms; status 8 ms. Initial overview/connection warm-up was about 606 ms.
- 1920x1080 headless-browser visual inspection: PASS.
- Redis TCP connectivity: PASS.
- Existing collector on port 8080 was never stopped. Its health remained UP, raw files advanced through normal cycles, and database latest/request/observation data continued increasing while Dashboard smoke queries ran.

## Production access

The production JAR is `Application/target/Application-0.0.1-SNAPSHOT.jar`. The existing port-8080 process was deliberately not restarted. At an approved maintenance window, the user can launch the `Application` module and access `http://localhost:8080/dashboard`.

## Explicit change declaration

- GTFS-Realtime download business: NO
- data.gov.my API: NO
- collection interval: NO
- Scheduler: NO
- protobuf parsing: NO
- deduplication: NO
- QC rules: NO
- PostgreSQL write business: NO
- vehicle_latest_state write business: NO
- Redis write business: NO
- new database tables: NO
- new indexes: NO
- adverse impact on long-running collection: NO
