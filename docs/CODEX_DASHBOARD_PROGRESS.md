# Malaysia Bus GPS Dashboard - Codex Development Progress

## 1. Project Goal

Add a high-performance, read-only monitoring dashboard to the existing GTFS-Realtime collector. The dashboard must remain isolated from and must never impair the collector.

## 2. Current Overall Status

STATUS: COMPLETED

Current Phase: STEP 11 - Final Verification and Handoff

Last Completed Task: Fixed acquisition-trend time axis clipping with contained ECharts labels and a true remaining-height grid layout.

Next Task: Restart the IDEA Spring Boot run once so port 8080 loads the updated route API and frontend bundle, then refresh the browser.

Last Updated: 2026-08-29 11:00:00 +08:00

## 3. Original Constraints

- Do not modify GTFS-Realtime download logic
- Do not modify polling interval
- Do not modify Scheduler
- Do not modify deduplication
- Do not modify QC
- Do not modify PostgreSQL write logic
- Do not modify Redis write logic
- Dashboard is READ ONLY
- Dashboard vehicle refresh = 5 minutes
- Dashboard failure must never affect Collector
- Any required business-code modification requires user approval

## 4. Architecture Decisions

- Frontend: Vue 3 + Vite + TypeScript + Axios + Leaflet + ECharts + Vue I18n.
- Backend: new `com.mytransitgps.dashboard` package only; REST namespace `/api/dashboard/**`.
- Database source: PostgreSQL city schemas `jb`, `kuching`, `kl`, `melaka`; core metadata in `core`.
- Map source: `*.vehicle_latest_state`; no history scan.
- Redis usage: none for Dashboard v1; current code exposes Redis clients/startup ping but no reliable latest-state read contract.
- Bus route: `route_short_name`, falling back to `resolved_route_id`; never inferred from trip/vehicle IDs.
- Statistics time: `vehicle_observation.ingest_time`, using `Asia/Kuala_Lumpur` day boundaries.
- Refresh interval: one frontend 300-second refresh manager; Header clock updates locally each second.
- API prefix: `/api/dashboard`.
- Production URL: `/dashboard` served by Spring Boot static resources.
- Route geometry: one focused active route at a time; road-aligned line comes from the most-used recent GTFS static shape for each direction, while bounded historical GPS observations remain separate point samples.

## 5. Database Findings

- PostgreSQL: 16.4.
- Schemas: `core`, `jb`, `kuching`, `kl`, `melaka` all exist.
- Latest table: `vehicle_latest_state` in each city schema.
- History table: `vehicle_observation` in each city schema.
- City table: `core.study_city`; all four requested cities are enabled.
- Feed table: `core.gtfs_feed`; five feeds (two KL feeds), all enabled, collector interval 120 seconds.
- Vehicle event time: `vehicle_time`.
- API/collector receive and ingestion time: snapshot `ingest_timestamp_utc` is mapped to `vehicle_observation.ingest_time`; latest state `last_seen_time` is the same ingestion instant.
- Database row creation time: `create_time`.
- Route fields: `route_short_name`, `route_long_name`, `resolved_route_id` are persisted.
- QC fields: `vehicle_latest_state.qc_flags`, `vehicle_observation.qc_flags`, and structured `vehicle_observation_qc` rows.
- API request logs: structured `api_request_log` exists in every city schema with request time, status, latency, result, and errors.
- Redis structure: no current vehicle-state write/read contract was found; Dashboard will not modify it.
- Live audit at 2026-08-28 23:19 +08: JB 136, Kuching 47, KL 448, Melaka 43 latest vehicles; 9,792 historical observations total.
- Existing indexes cover latest state time/vehicle/route, observation ingest time and QC, and request time/status. No DDL is proposed.

## 6. Completed Tasks

- [x] Project audit
- [x] Database inspection
- [x] Dashboard API design
- [x] Dashboard package
- [x] Vehicles API
- [x] Overview API
- [x] Trend API
- [x] Frontend map
- [x] Fullscreen
- [x] i18n
- [x] Spring Boot integration
- [x] Regression tests

## 7. Files Created

- `docs/CODEX_DASHBOARD_PROGRESS.md` - durable continuation checkpoint.
- `src/main/java/com/mytransitgps/dashboard/**` - isolated read-only backend.
- `frontend/**` - Vue dashboard source and build configuration.
- `src/main/resources/static/dashboard/**` - production frontend assets served by Spring Boot.

## 8. Files Modified

- None outside the new Dashboard files. Existing collector/business files and `pom.xml` were not modified.

Collector impact: NONE

## 9. APIs Completed

- `GET /api/dashboard/cities` - DONE
- `GET /api/dashboard/vehicles?city=...` - DONE; live counts validated for all four cities
- `GET /api/dashboard/route-traces?city=...&route=...&all=...` - DONE; focused-route or all-active-routes mode, per-direction GTFS shapes, and bounded focused-route GPS history
- `GET /api/dashboard/acquisition` - DONE
- `GET /api/dashboard/acquisition/trend` - DONE
- `GET /api/dashboard/overview` - DONE
- `GET /api/dashboard/anomalies/recent` - DONE
- `GET /api/dashboard/api-requests/recent` - DONE
- `GET /api/dashboard/system-status` - DONE

## 10. SQL Completed

- Audited real city/feed metadata, city schemas, tables, columns, and indexes using read-only SELECT statements.
- Validated current latest vehicle, history, and API-request counts against the live database.
- Dashboard query plan will use schema names selected only from a fixed enum/whitelist.
- No DDL executed and no new index required.

## 11. Frontend Components

- DashboardView: DONE
- DashboardHeader and language switch: DONE
- CityMapPanel / VehicleMap / fullscreen / popup: DONE
- Single focused route selector and auto-fit: DONE
- Road-aligned GTFS shape line with vehicle-matching stable color: DONE
- Latest vehicle for each available direction rendered as a bus icon: DONE
- Historical GPS timestamps retained as independent points (no cross-vehicle spider lines): DONE
- All-active-routes toggle below the route selector: DONE; all mode prioritizes colored route lines and compact vehicle points, single mode retains direction bus icons
- Point usability: DONE; historical dots radius 4.5 with 11px hover target, current non-latest vehicles radius 5.5/7 with 16px popup hit target
- All-route clarity: DONE; 5.5px colored lines with 11px dark casing plus compact latest-direction bus icons for every route
- All-route completeness: DONE; frontend compares active vehicle route keys with returned shapes, fetches every missing route individually, normalizes legacy shape responses without route keys, and draws a historical-GPS fallback line when no static shape exists
- Enlarged transparent vehicle hit targets, hover highlight, tooltip, and click snapping: DONE
- Acquisition cards / ECharts trend: DONE
- Operations KPI / anomaly / API request tables: DONE

## 12. Current Problems

- ISSUE-001: Project has no Git repository, so branch/diff-based recovery is unavailable. Decision: preserve all existing files and track Dashboard files explicitly here.
- ISSUE-002: README database section is stale and contradicts the current PostgreSQL/MyBatis implementation. Decision: do not rewrite unrelated README content; document Dashboard accurately.
- ISSUE-003: RESOLVED. Live validation returned road shapes for all four cities: JB 2/1314 points, Kuching 2/95, KL 2/1371, Melaka 1/312; endpoint latency 39-648 ms on first isolated-app calls.

## 13. Business Code Modification Requests

NONE

## 14. Tests Already Passed

- [x] Live PostgreSQL connection
- [x] Four city schemas and expected tables found
- [x] Real latest/history/request data available
- [x] Existing index coverage reviewed
- [x] Single-route API verified against the live PostgreSQL database for all four cities
- [x] Browser visual QA: one route only, two direction bus icons where both directions exist, route selector works, no console warnings/errors
- [x] All-routes live QA (JB): 18 routes, 35 directional shapes, 12,350 GTFS points, first request 781 ms; toggle back restores one route and two direction bus icons
- [x] Revised all-routes visual QA: 18 lines, 91 live vehicles, 35 compact direction bus icons, no browser warnings/errors
- [x] Legacy live-8080 compatibility QA: old API initially returned only 1 route; reconciliation completed 18 routes, 103 vehicles, and 34 direction icons with no browser warnings/errors
- [x] Trend-axis QA at 1280x720: chart bottom moved from 23px outside the panel to 22px inside it; time labels remain fully visible with no browser warnings/errors
- [x] Frontend TypeScript/Vite production build
- [x] Maven tests: 50 passed, 0 failures/errors/skips
- [x] Production JAR packaged at 2026-08-29 10:31 +08:00

## 15. Tests Still Required

- Operational restart of the already-running port-8080 collector process is intentionally left to an approved maintenance window; the process was not interrupted by Dashboard development.

## 16. Current Git State

- Branch: N/A (no `.git` repository found in workspace or Spring Boot module)
- Modified/untracked: cannot be reported by Git; Dashboard-created files are tracked in sections 7 and 8.
- No push/reset/clean actions performed.

## 17. Build Status

- Backend compile: PASS
- Frontend type-check/build: PASS
- Maven tests: PASS (50 tests, 0 failures/errors)
- Spring Boot isolated startup: PASS on port 18080 with collector disabled for smoke testing
- Production package: PASS (`Application/target/Application-0.0.1-SNAPSHOT.jar`, Dashboard assets embedded)
- Live APIs and `/dashboard`: PASS
- 1920x1080 browser visual QA: PASS
- Existing collector regression: PASS; port-8080 health stayed UP and raw/database data continued across more than 3 normal 120-second cycles while Dashboard queries ran
- Viewport-fill responsive layout: PASS at 1829x921; bottom unused area removed and both tables expand with the operations panel
- Live port-8080 asset sync: PASS; `/dashboard` returns the new CSS bundle
- Active route trace live-data test: PASS; JB 12 routes/19 segments/632 points, KL 11 routes/11 segments/331 points, Melaka 1 route/1 segment/50 points at verification time
- Route query warm latency: PASS (approximately 21-29 ms; first connection/query warm-up approximately 584 ms)
- Vehicle point snap target: PASS; 5px visible marker retains a 14px-radius interactive hit area

## 18. EXACT NEXT ACTION

NEXT ACTION: Restart the IDEA Spring Boot run once, then refresh `http://localhost:8080/dashboard#/`.

Required files: no code changes required.

Definition of done: active route traces render and nearby pointer clicks open the vehicle popup through the enlarged snap target.

After completion: retain this file as the implementation and recovery record.
