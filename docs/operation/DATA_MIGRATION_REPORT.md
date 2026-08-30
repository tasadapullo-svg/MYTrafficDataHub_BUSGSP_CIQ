# Runtime Data Migration Report

Generated on 2026-08-30.

## Moved Out Of Source Root

The following runtime or historical research data directories were moved out of the official source tree:

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

Destination:

`../MYTrafficDataHub_DATA_BACKUP_20260830`

## Reason

Runtime data must not be mixed with Maven source modules. Long-running production data should use `TRAFFIC_WORKSPACE_ROOT`, recommended as:

`C:\Users\DELL\Desktop\MYTrafficDataHub_20260829\data_download`

Recommended structure:

```text
C:\Users\DELL\Desktop\MYTrafficDataHub_20260829\data_download
|-- BusGPS/
|   |-- json/
|   |-- archive/
|   `-- temp/
|-- CIQ/
|   |-- YYYYMMDD/
|   |-- archive/
|   `-- temp/
`-- logs/
    |-- info/
    |-- warn/
    `-- error/
```

## Safety

No historical data was deleted. Existing tests can still locate the backup through reactor-root-relative resolution while a smaller curated `test-data/busgps` fixture set is prepared later.
