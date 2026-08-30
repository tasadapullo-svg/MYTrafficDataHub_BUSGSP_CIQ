# Multi-Module Migration Plan

Generated on 2026-08-30 before physical migration.

## Current Audit

Current root contains one active Spring Boot project plus one nested legacy project:

- root `pom.xml`
- root `src`
- root `frontend`
- root `docs`
- nested legacy `pom.xml`
- nested legacy `src`
- nested legacy `frontend`
- `.idea` referencing nested project modules
- runtime data directories mixed into source root

Runtime/data directories detected:

- `json_data`
- `raw_data`
- `daily_archive`
- `weekly_archive`
- `logs`
- `database_collection`
- `data`
- `runtime`
- `run_reports`
- `outputs`

## Target Structure

The source root will be converted to a Maven reactor:

- `Common`: platform abstractions, HTTP, collection, monitoring, common JSON/hash/workspace support.
- `BusGPS`: GTFS, BUS GPS QC, BUS GPS persistence, BUS GPS dashboard business, BUS GPS archive.
- `CIQ`: CIQ/LTA API01 code only.
- `Application`: Spring Boot application entrypoint, global configuration, dashboard controllers/static resources.

## Package Policy

To reduce regression risk, Java package names remain mostly unchanged in this round. The goal is physical Maven module separation first, not global package renaming.

## Dependency Direction

- `Common` depends on no business module.
- `BusGPS` depends on `Common`.
- `CIQ` depends on `Common`.
- `Application` depends on `Common`, `BusGPS`, and `CIQ`.

No BusGPS-CIQ dependency is allowed.

## Safety Actions

- Move nested legacy project to `../MYTrafficDataHub_LEGACY_BACKUP_20260830`.
- Move `.idea` to `../MYTrafficDataHub_IDEA_BACKUP_20260830`.
- Move runtime data directories to `../MYTrafficDataHub_DATA_BACKUP_20260830`.
- Delete generated `target` directories only after verifying paths.
- Keep `mvnw`, `.mvn`, `frontend`, `sql`, `docs`, `scripts`, `README.md`, `.gitignore` at the root.

## P0/P1 Code Fixes Included

- Application-level ObjectMapper independent of DB/CIQ/Redis flags.
- CIQ archive job requires both `traffic.ciq.enabled=true` and `traffic.ciq.archive.enabled=true`.
- Exact six-combination study_area readiness validation.
- API01 batch spatial query and batch persistence shape.
- collection_run finalization guard.
- raw artifact registration before parser.
- reusable JDK HttpClient.
- project-relative BUS GPS test fixture resolution.
- longrun profile and IDEA SOP.
