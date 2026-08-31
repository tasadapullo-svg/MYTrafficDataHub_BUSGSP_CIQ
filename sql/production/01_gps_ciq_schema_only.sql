-- MYTrafficDataHub production schema-only DDL
-- Source: PostgreSQL catalog; schemas: core, jb, kuching, kl, melaka, lta
-- Contains no INSERT, COPY, or existing table rows.
-- Run in the target database after 00_create_database.sql.

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET TIME ZONE 'UTC';
SET lock_timeout = '10s';
SET statement_timeout = '0';

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SCHEMA IF NOT EXISTS "core";
CREATE SCHEMA IF NOT EXISTS "jb";
CREATE SCHEMA IF NOT EXISTS "kuching";
CREATE SCHEMA IF NOT EXISTS "kl";
CREATE SCHEMA IF NOT EXISTS "melaka";
CREATE SCHEMA IF NOT EXISTS "lta";



CREATE TABLE "core"."collection_run" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "run_code" character varying(120) NOT NULL,
    "run_type" character varying(50) NOT NULL,
    "run_status" character varying(30) NOT NULL,
    "timezone_name" character varying(64) DEFAULT 'Asia/Kuala_Lumpur'::character varying NOT NULL,
    "planned_start_time" timestamp with time zone,
    "actual_start_time" timestamp with time zone,
    "actual_end_time" timestamp with time zone,
    "planned_cycle_count" integer,
    "actual_cycle_count" integer,
    "feed_count" integer,
    "planned_request_count" integer,
    "actual_request_count" integer,
    "successful_request_count" integer,
    "failed_request_count" integer,
    "http_429_count" integer,
    "http_5xx_count" integer,
    "timeout_count" integer,
    "scheduler_drift_p50_ms" double precision,
    "scheduler_drift_p95_ms" double precision,
    "scheduler_drift_max_ms" double precision,
    "remarks" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "core"."daily_archive" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "archive_date" date NOT NULL,
    "archive_status" character varying(30) NOT NULL,
    "timezone_name" character varying(64) DEFAULT 'Asia/Kuala_Lumpur'::character varying NOT NULL,
    "archive_file_name" character varying(255),
    "archive_path" text,
    "archive_size_bytes" bigint,
    "archive_sha256" character varying(64),
    "manifest_path" text,
    "checksum_path" text,
    "source_file_count" integer,
    "archive_entry_count" integer,
    "realtime_request_count" integer,
    "static_request_count" integer,
    "successful_request_count" integer,
    "failed_request_count" integer,
    "parsed_json_count" integer,
    "enriched_json_count" integer,
    "raw_object_count" integer,
    "static_object_count" integer,
    "run_report_file_count" integer,
    "archive_created_at" timestamp with time zone,
    "verified_at" timestamp with time zone,
    "warning_message" text,
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "core"."gtfs_feed" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "city_uid" uuid NOT NULL,
    "feed_id" character varying(100) NOT NULL,
    "feed_name" character varying(150) NOT NULL,
    "operator_name" character varying(150),
    "service_type" character varying(50) NOT NULL,
    "realtime_url" text NOT NULL,
    "static_url" text NOT NULL,
    "realtime_feed_type" character varying(50) DEFAULT 'vehicle_position'::character varying NOT NULL,
    "source_platform" character varying(100) DEFAULT 'data.gov.my'::character varying NOT NULL,
    "file_prefix" character varying(100) NOT NULL,
    "poll_interval_seconds" integer DEFAULT 120 NOT NULL,
    "stagger_offset_seconds" integer,
    "timezone_name" character varying(64) DEFAULT 'Asia/Kuala_Lumpur'::character varying NOT NULL,
    "enabled" boolean DEFAULT true NOT NULL,
    "description" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "core"."study_city" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "city_code" character varying(50) NOT NULL,
    "city_name" character varying(100) NOT NULL,
    "schema_name" character varying(63) NOT NULL,
    "country_code" character varying(2) DEFAULT 'MY'::character varying NOT NULL,
    "timezone_name" character varying(64) DEFAULT 'Asia/Kuala_Lumpur'::character varying NOT NULL,
    "enabled" boolean DEFAULT true NOT NULL,
    "description" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."api_request_log" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "duplicate_of_request_uid" uuid,
    "request_type" character varying(30) NOT NULL,
    "request_sequence" integer,
    "cycle_number" integer,
    "requested_url" text NOT NULL,
    "final_url" text,
    "scheduled_at" timestamp with time zone,
    "request_started_at" timestamp with time zone,
    "response_received_at" timestamp with time zone,
    "latency_ms" bigint,
    "scheduler_drift_ms" bigint,
    "http_status" integer,
    "content_type" character varying(255),
    "response_bytes" bigint,
    "response_sha256" character varying(64),
    "redirect_count" integer,
    "parse_status" character varying(30),
    "entity_count" integer,
    "vehicle_count" integer,
    "duplicate_snapshot" boolean,
    "raw_object_path" text,
    "parsed_json_path" text,
    "enriched_json_path" text,
    "result" character varying(30) NOT NULL,
    "error_class" character varying(255),
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."realtime_snapshot" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "enrichment_static_version_uid" uuid,
    "batch_id" character varying(120),
    "gtfs_realtime_version" character varying(20),
    "incrementality" character varying(50),
    "feed_timestamp_raw" bigint,
    "feed_time" timestamp with time zone,
    "entity_count" integer,
    "vehicle_count" integer,
    "unique_route_count" integer,
    "route_direct_match_count" integer,
    "route_trip_fallback_match_count" integer,
    "route_resolved_count" integer,
    "route_unresolved_count" integer,
    "trip_match_count" integer,
    "trip_unmatched_count" integer,
    "direction_match_count" integer,
    "direction_mismatch_count" integer,
    "direction_not_comparable_count" integer,
    "response_sha256" character varying(64),
    "duplicate_snapshot" boolean DEFAULT false NOT NULL,
    "referenced_snapshot_uid" uuid,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_route" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_id" character varying(255) NOT NULL,
    "agency_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_desc" text,
    "route_type" integer,
    "route_url" text,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_sort_order" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "network_id" character varying(255),
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_shape" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_id" character varying(255) NOT NULL,
    "is_referenced" boolean DEFAULT false NOT NULL,
    "trip_count_using_shape" integer,
    "point_count" integer,
    "geom" geometry(LineString,4326),
    "shape_length_m" double precision,
    "shape_length_km" double precision,
    "analysis_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "qc_summary" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_shape_point" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_uid" uuid,
    "shape_id" character varying(255) NOT NULL,
    "shape_pt_lat" double precision,
    "shape_pt_lon" double precision,
    "geom" geometry(Point,4326),
    "shape_pt_sequence" integer NOT NULL,
    "shape_dist_traveled" double precision,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_stop" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_code" character varying(255),
    "stop_name" text,
    "tts_stop_name" text,
    "stop_desc" text,
    "stop_lat" double precision,
    "stop_lon" double precision,
    "geom" geometry(Point,4326),
    "zone_id" character varying(255),
    "stop_url" text,
    "location_type" integer,
    "parent_station" character varying(255),
    "stop_timezone" character varying(100),
    "wheelchair_boarding" integer,
    "level_id" character varying(255),
    "platform_code" character varying(255),
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_stop_time" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "trip_uid" uuid,
    "stop_uid" uuid,
    "trip_id" character varying(255) NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_sequence" integer NOT NULL,
    "arrival_time_raw" character varying(20),
    "arrival_seconds" integer,
    "departure_time_raw" character varying(20),
    "departure_seconds" integer,
    "stop_headsign" text,
    "pickup_type" integer,
    "drop_off_type" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "shape_dist_traveled" double precision,
    "timepoint" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_trip" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_uid" uuid,
    "shape_uid" uuid,
    "route_id" character varying(255),
    "service_id" character varying(255),
    "trip_id" character varying(255) NOT NULL,
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "direction_id" smallint,
    "block_id" character varying(255),
    "shape_id" character varying(255),
    "wheelchair_accessible" integer,
    "bikes_allowed" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."static_version" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "source_request_uid" uuid,
    "version_code" character varying(120),
    "static_sha256" character varying(64) NOT NULL,
    "static_object_path" text NOT NULL,
    "zip_size_bytes" bigint,
    "downloaded_at" timestamp with time zone,
    "effective_from" timestamp with time zone,
    "effective_to" timestamp with time zone,
    "is_current" boolean DEFAULT false NOT NULL,
    "routes_file_present" boolean,
    "trips_file_present" boolean,
    "stops_file_present" boolean,
    "stop_times_file_present" boolean,
    "shapes_file_present" boolean,
    "calendar_file_present" boolean,
    "calendar_dates_file_present" boolean,
    "frequencies_file_present" boolean,
    "routes_count" integer,
    "trips_count" integer,
    "stops_count" integer,
    "stop_times_count" integer,
    "shapes_count" integer,
    "shape_points_count" integer,
    "notes" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."vehicle_latest_state" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "observation_uid" uuid NOT NULL,
    "vehicle_id" character varying(255) NOT NULL,
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "trip_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "static_direction_id" smallint,
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "source_speed_raw" double precision,
    "derived_speed_kmh" double precision,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "last_seen_time" timestamp with time zone NOT NULL,
    "freshness_seconds" bigint,
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."vehicle_observation" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "snapshot_uid" uuid NOT NULL,
    "static_version_uid" uuid,
    "static_route_uid" uuid,
    "static_trip_uid" uuid,
    "static_shape_uid" uuid,
    "static_stop_uid" uuid,
    "entity_sequence" integer NOT NULL,
    "entity_id" character varying(255),
    "vehicle_id" character varying(255),
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "wheelchair_accessible" character varying(50),
    "trip_id" character varying(255),
    "trip_start_time_raw" character varying(20),
    "trip_start_seconds" integer,
    "trip_start_date_raw" character varying(20),
    "trip_start_date" date,
    "realtime_schedule_relationship" character varying(50),
    "realtime_route_id" character varying(255),
    "static_route_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_type" integer,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_resolution_method" character varying(50),
    "realtime_route_direct_matched" boolean,
    "route_resolved" boolean,
    "static_service_id" character varying(255),
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "shape_id" character varying(255),
    "trip_static_matched" boolean,
    "realtime_direction_id" smallint,
    "static_direction_id" smallint,
    "direction_comparison" character varying(30),
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "bearing" double precision,
    "odometer_m" double precision,
    "source_speed_raw" double precision,
    "source_speed_present" boolean DEFAULT false NOT NULL,
    "source_speed_unit_declared" character varying(50),
    "source_speed_unit_interpretation" character varying(100),
    "current_stop_sequence" integer,
    "stop_id" character varying(255),
    "current_status" character varying(50),
    "congestion_level" character varying(50),
    "occupancy_status" character varying(50),
    "occupancy_percentage" integer,
    "multi_carriage_details" jsonb,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "previous_vehicle_time" timestamp with time zone,
    "ingest_time" timestamp with time zone,
    "freshness_seconds" bigint,
    "vehicle_gap_seconds" bigint,
    "distance_from_previous_m" double precision,
    "derived_speed_kmh" double precision,
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "feed_bounds_valid" boolean,
    "position_qc_status" character varying(50),
    "gps_jump_status" character varying(50),
    "jump_calculation_skipped_reason" character varying(100),
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "duplicate_observation" boolean DEFAULT false NOT NULL,
    "observation_key" character varying(64),
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "realtime_entity" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "jb"."vehicle_observation_qc" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "observation_uid" uuid NOT NULL,
    "qc_code" character varying(100) NOT NULL,
    "qc_category" character varying(50),
    "severity" character varying(30),
    "qc_value_numeric" double precision,
    "qc_value_text" text,
    "details" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."api_request_log" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "duplicate_of_request_uid" uuid,
    "request_type" character varying(30) NOT NULL,
    "request_sequence" integer,
    "cycle_number" integer,
    "requested_url" text NOT NULL,
    "final_url" text,
    "scheduled_at" timestamp with time zone,
    "request_started_at" timestamp with time zone,
    "response_received_at" timestamp with time zone,
    "latency_ms" bigint,
    "scheduler_drift_ms" bigint,
    "http_status" integer,
    "content_type" character varying(255),
    "response_bytes" bigint,
    "response_sha256" character varying(64),
    "redirect_count" integer,
    "parse_status" character varying(30),
    "entity_count" integer,
    "vehicle_count" integer,
    "duplicate_snapshot" boolean,
    "raw_object_path" text,
    "parsed_json_path" text,
    "enriched_json_path" text,
    "result" character varying(30) NOT NULL,
    "error_class" character varying(255),
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."realtime_snapshot" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "enrichment_static_version_uid" uuid,
    "batch_id" character varying(120),
    "gtfs_realtime_version" character varying(20),
    "incrementality" character varying(50),
    "feed_timestamp_raw" bigint,
    "feed_time" timestamp with time zone,
    "entity_count" integer,
    "vehicle_count" integer,
    "unique_route_count" integer,
    "route_direct_match_count" integer,
    "route_trip_fallback_match_count" integer,
    "route_resolved_count" integer,
    "route_unresolved_count" integer,
    "trip_match_count" integer,
    "trip_unmatched_count" integer,
    "direction_match_count" integer,
    "direction_mismatch_count" integer,
    "direction_not_comparable_count" integer,
    "response_sha256" character varying(64),
    "duplicate_snapshot" boolean DEFAULT false NOT NULL,
    "referenced_snapshot_uid" uuid,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_route" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_id" character varying(255) NOT NULL,
    "agency_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_desc" text,
    "route_type" integer,
    "route_url" text,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_sort_order" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "network_id" character varying(255),
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_shape" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_id" character varying(255) NOT NULL,
    "is_referenced" boolean DEFAULT false NOT NULL,
    "trip_count_using_shape" integer,
    "point_count" integer,
    "geom" geometry(LineString,4326),
    "shape_length_m" double precision,
    "shape_length_km" double precision,
    "analysis_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "qc_summary" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_shape_point" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_uid" uuid,
    "shape_id" character varying(255) NOT NULL,
    "shape_pt_lat" double precision,
    "shape_pt_lon" double precision,
    "geom" geometry(Point,4326),
    "shape_pt_sequence" integer NOT NULL,
    "shape_dist_traveled" double precision,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_stop" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_code" character varying(255),
    "stop_name" text,
    "tts_stop_name" text,
    "stop_desc" text,
    "stop_lat" double precision,
    "stop_lon" double precision,
    "geom" geometry(Point,4326),
    "zone_id" character varying(255),
    "stop_url" text,
    "location_type" integer,
    "parent_station" character varying(255),
    "stop_timezone" character varying(100),
    "wheelchair_boarding" integer,
    "level_id" character varying(255),
    "platform_code" character varying(255),
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_stop_time" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "trip_uid" uuid,
    "stop_uid" uuid,
    "trip_id" character varying(255) NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_sequence" integer NOT NULL,
    "arrival_time_raw" character varying(20),
    "arrival_seconds" integer,
    "departure_time_raw" character varying(20),
    "departure_seconds" integer,
    "stop_headsign" text,
    "pickup_type" integer,
    "drop_off_type" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "shape_dist_traveled" double precision,
    "timepoint" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_trip" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_uid" uuid,
    "shape_uid" uuid,
    "route_id" character varying(255),
    "service_id" character varying(255),
    "trip_id" character varying(255) NOT NULL,
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "direction_id" smallint,
    "block_id" character varying(255),
    "shape_id" character varying(255),
    "wheelchair_accessible" integer,
    "bikes_allowed" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."static_version" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "source_request_uid" uuid,
    "version_code" character varying(120),
    "static_sha256" character varying(64) NOT NULL,
    "static_object_path" text NOT NULL,
    "zip_size_bytes" bigint,
    "downloaded_at" timestamp with time zone,
    "effective_from" timestamp with time zone,
    "effective_to" timestamp with time zone,
    "is_current" boolean DEFAULT false NOT NULL,
    "routes_file_present" boolean,
    "trips_file_present" boolean,
    "stops_file_present" boolean,
    "stop_times_file_present" boolean,
    "shapes_file_present" boolean,
    "calendar_file_present" boolean,
    "calendar_dates_file_present" boolean,
    "frequencies_file_present" boolean,
    "routes_count" integer,
    "trips_count" integer,
    "stops_count" integer,
    "stop_times_count" integer,
    "shapes_count" integer,
    "shape_points_count" integer,
    "notes" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."vehicle_latest_state" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "observation_uid" uuid NOT NULL,
    "vehicle_id" character varying(255) NOT NULL,
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "trip_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "static_direction_id" smallint,
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "source_speed_raw" double precision,
    "derived_speed_kmh" double precision,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "last_seen_time" timestamp with time zone NOT NULL,
    "freshness_seconds" bigint,
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."vehicle_observation" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "snapshot_uid" uuid NOT NULL,
    "static_version_uid" uuid,
    "static_route_uid" uuid,
    "static_trip_uid" uuid,
    "static_shape_uid" uuid,
    "static_stop_uid" uuid,
    "entity_sequence" integer NOT NULL,
    "entity_id" character varying(255),
    "vehicle_id" character varying(255),
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "wheelchair_accessible" character varying(50),
    "trip_id" character varying(255),
    "trip_start_time_raw" character varying(20),
    "trip_start_seconds" integer,
    "trip_start_date_raw" character varying(20),
    "trip_start_date" date,
    "realtime_schedule_relationship" character varying(50),
    "realtime_route_id" character varying(255),
    "static_route_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_type" integer,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_resolution_method" character varying(50),
    "realtime_route_direct_matched" boolean,
    "route_resolved" boolean,
    "static_service_id" character varying(255),
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "shape_id" character varying(255),
    "trip_static_matched" boolean,
    "realtime_direction_id" smallint,
    "static_direction_id" smallint,
    "direction_comparison" character varying(30),
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "bearing" double precision,
    "odometer_m" double precision,
    "source_speed_raw" double precision,
    "source_speed_present" boolean DEFAULT false NOT NULL,
    "source_speed_unit_declared" character varying(50),
    "source_speed_unit_interpretation" character varying(100),
    "current_stop_sequence" integer,
    "stop_id" character varying(255),
    "current_status" character varying(50),
    "congestion_level" character varying(50),
    "occupancy_status" character varying(50),
    "occupancy_percentage" integer,
    "multi_carriage_details" jsonb,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "previous_vehicle_time" timestamp with time zone,
    "ingest_time" timestamp with time zone,
    "freshness_seconds" bigint,
    "vehicle_gap_seconds" bigint,
    "distance_from_previous_m" double precision,
    "derived_speed_kmh" double precision,
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "feed_bounds_valid" boolean,
    "position_qc_status" character varying(50),
    "gps_jump_status" character varying(50),
    "jump_calculation_skipped_reason" character varying(100),
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "duplicate_observation" boolean DEFAULT false NOT NULL,
    "observation_key" character varying(64),
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "realtime_entity" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kuching"."vehicle_observation_qc" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "observation_uid" uuid NOT NULL,
    "qc_code" character varying(100) NOT NULL,
    "qc_category" character varying(50),
    "severity" character varying(30),
    "qc_value_numeric" double precision,
    "qc_value_text" text,
    "details" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."api_request_log" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "duplicate_of_request_uid" uuid,
    "request_type" character varying(30) NOT NULL,
    "request_sequence" integer,
    "cycle_number" integer,
    "requested_url" text NOT NULL,
    "final_url" text,
    "scheduled_at" timestamp with time zone,
    "request_started_at" timestamp with time zone,
    "response_received_at" timestamp with time zone,
    "latency_ms" bigint,
    "scheduler_drift_ms" bigint,
    "http_status" integer,
    "content_type" character varying(255),
    "response_bytes" bigint,
    "response_sha256" character varying(64),
    "redirect_count" integer,
    "parse_status" character varying(30),
    "entity_count" integer,
    "vehicle_count" integer,
    "duplicate_snapshot" boolean,
    "raw_object_path" text,
    "parsed_json_path" text,
    "enriched_json_path" text,
    "result" character varying(30) NOT NULL,
    "error_class" character varying(255),
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."realtime_snapshot" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "enrichment_static_version_uid" uuid,
    "batch_id" character varying(120),
    "gtfs_realtime_version" character varying(20),
    "incrementality" character varying(50),
    "feed_timestamp_raw" bigint,
    "feed_time" timestamp with time zone,
    "entity_count" integer,
    "vehicle_count" integer,
    "unique_route_count" integer,
    "route_direct_match_count" integer,
    "route_trip_fallback_match_count" integer,
    "route_resolved_count" integer,
    "route_unresolved_count" integer,
    "trip_match_count" integer,
    "trip_unmatched_count" integer,
    "direction_match_count" integer,
    "direction_mismatch_count" integer,
    "direction_not_comparable_count" integer,
    "response_sha256" character varying(64),
    "duplicate_snapshot" boolean DEFAULT false NOT NULL,
    "referenced_snapshot_uid" uuid,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_route" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_id" character varying(255) NOT NULL,
    "agency_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_desc" text,
    "route_type" integer,
    "route_url" text,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_sort_order" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "network_id" character varying(255),
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_shape" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_id" character varying(255) NOT NULL,
    "is_referenced" boolean DEFAULT false NOT NULL,
    "trip_count_using_shape" integer,
    "point_count" integer,
    "geom" geometry(LineString,4326),
    "shape_length_m" double precision,
    "shape_length_km" double precision,
    "analysis_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "qc_summary" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_shape_point" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_uid" uuid,
    "shape_id" character varying(255) NOT NULL,
    "shape_pt_lat" double precision,
    "shape_pt_lon" double precision,
    "geom" geometry(Point,4326),
    "shape_pt_sequence" integer NOT NULL,
    "shape_dist_traveled" double precision,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_stop" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_code" character varying(255),
    "stop_name" text,
    "tts_stop_name" text,
    "stop_desc" text,
    "stop_lat" double precision,
    "stop_lon" double precision,
    "geom" geometry(Point,4326),
    "zone_id" character varying(255),
    "stop_url" text,
    "location_type" integer,
    "parent_station" character varying(255),
    "stop_timezone" character varying(100),
    "wheelchair_boarding" integer,
    "level_id" character varying(255),
    "platform_code" character varying(255),
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_stop_time" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "trip_uid" uuid,
    "stop_uid" uuid,
    "trip_id" character varying(255) NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_sequence" integer NOT NULL,
    "arrival_time_raw" character varying(20),
    "arrival_seconds" integer,
    "departure_time_raw" character varying(20),
    "departure_seconds" integer,
    "stop_headsign" text,
    "pickup_type" integer,
    "drop_off_type" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "shape_dist_traveled" double precision,
    "timepoint" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_trip" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_uid" uuid,
    "shape_uid" uuid,
    "route_id" character varying(255),
    "service_id" character varying(255),
    "trip_id" character varying(255) NOT NULL,
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "direction_id" smallint,
    "block_id" character varying(255),
    "shape_id" character varying(255),
    "wheelchair_accessible" integer,
    "bikes_allowed" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."static_version" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "source_request_uid" uuid,
    "version_code" character varying(120),
    "static_sha256" character varying(64) NOT NULL,
    "static_object_path" text NOT NULL,
    "zip_size_bytes" bigint,
    "downloaded_at" timestamp with time zone,
    "effective_from" timestamp with time zone,
    "effective_to" timestamp with time zone,
    "is_current" boolean DEFAULT false NOT NULL,
    "routes_file_present" boolean,
    "trips_file_present" boolean,
    "stops_file_present" boolean,
    "stop_times_file_present" boolean,
    "shapes_file_present" boolean,
    "calendar_file_present" boolean,
    "calendar_dates_file_present" boolean,
    "frequencies_file_present" boolean,
    "routes_count" integer,
    "trips_count" integer,
    "stops_count" integer,
    "stop_times_count" integer,
    "shapes_count" integer,
    "shape_points_count" integer,
    "notes" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."vehicle_latest_state" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "observation_uid" uuid NOT NULL,
    "vehicle_id" character varying(255) NOT NULL,
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "trip_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "static_direction_id" smallint,
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "source_speed_raw" double precision,
    "derived_speed_kmh" double precision,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "last_seen_time" timestamp with time zone NOT NULL,
    "freshness_seconds" bigint,
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."vehicle_observation" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "snapshot_uid" uuid NOT NULL,
    "static_version_uid" uuid,
    "static_route_uid" uuid,
    "static_trip_uid" uuid,
    "static_shape_uid" uuid,
    "static_stop_uid" uuid,
    "entity_sequence" integer NOT NULL,
    "entity_id" character varying(255),
    "vehicle_id" character varying(255),
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "wheelchair_accessible" character varying(50),
    "trip_id" character varying(255),
    "trip_start_time_raw" character varying(20),
    "trip_start_seconds" integer,
    "trip_start_date_raw" character varying(20),
    "trip_start_date" date,
    "realtime_schedule_relationship" character varying(50),
    "realtime_route_id" character varying(255),
    "static_route_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_type" integer,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_resolution_method" character varying(50),
    "realtime_route_direct_matched" boolean,
    "route_resolved" boolean,
    "static_service_id" character varying(255),
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "shape_id" character varying(255),
    "trip_static_matched" boolean,
    "realtime_direction_id" smallint,
    "static_direction_id" smallint,
    "direction_comparison" character varying(30),
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "bearing" double precision,
    "odometer_m" double precision,
    "source_speed_raw" double precision,
    "source_speed_present" boolean DEFAULT false NOT NULL,
    "source_speed_unit_declared" character varying(50),
    "source_speed_unit_interpretation" character varying(100),
    "current_stop_sequence" integer,
    "stop_id" character varying(255),
    "current_status" character varying(50),
    "congestion_level" character varying(50),
    "occupancy_status" character varying(50),
    "occupancy_percentage" integer,
    "multi_carriage_details" jsonb,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "previous_vehicle_time" timestamp with time zone,
    "ingest_time" timestamp with time zone,
    "freshness_seconds" bigint,
    "vehicle_gap_seconds" bigint,
    "distance_from_previous_m" double precision,
    "derived_speed_kmh" double precision,
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "feed_bounds_valid" boolean,
    "position_qc_status" character varying(50),
    "gps_jump_status" character varying(50),
    "jump_calculation_skipped_reason" character varying(100),
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "duplicate_observation" boolean DEFAULT false NOT NULL,
    "observation_key" character varying(64),
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "realtime_entity" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "kl"."vehicle_observation_qc" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "observation_uid" uuid NOT NULL,
    "qc_code" character varying(100) NOT NULL,
    "qc_category" character varying(50),
    "severity" character varying(30),
    "qc_value_numeric" double precision,
    "qc_value_text" text,
    "details" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."api_request_log" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "duplicate_of_request_uid" uuid,
    "request_type" character varying(30) NOT NULL,
    "request_sequence" integer,
    "cycle_number" integer,
    "requested_url" text NOT NULL,
    "final_url" text,
    "scheduled_at" timestamp with time zone,
    "request_started_at" timestamp with time zone,
    "response_received_at" timestamp with time zone,
    "latency_ms" bigint,
    "scheduler_drift_ms" bigint,
    "http_status" integer,
    "content_type" character varying(255),
    "response_bytes" bigint,
    "response_sha256" character varying(64),
    "redirect_count" integer,
    "parse_status" character varying(30),
    "entity_count" integer,
    "vehicle_count" integer,
    "duplicate_snapshot" boolean,
    "raw_object_path" text,
    "parsed_json_path" text,
    "enriched_json_path" text,
    "result" character varying(30) NOT NULL,
    "error_class" character varying(255),
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."realtime_snapshot" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "enrichment_static_version_uid" uuid,
    "batch_id" character varying(120),
    "gtfs_realtime_version" character varying(20),
    "incrementality" character varying(50),
    "feed_timestamp_raw" bigint,
    "feed_time" timestamp with time zone,
    "entity_count" integer,
    "vehicle_count" integer,
    "unique_route_count" integer,
    "route_direct_match_count" integer,
    "route_trip_fallback_match_count" integer,
    "route_resolved_count" integer,
    "route_unresolved_count" integer,
    "trip_match_count" integer,
    "trip_unmatched_count" integer,
    "direction_match_count" integer,
    "direction_mismatch_count" integer,
    "direction_not_comparable_count" integer,
    "response_sha256" character varying(64),
    "duplicate_snapshot" boolean DEFAULT false NOT NULL,
    "referenced_snapshot_uid" uuid,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_route" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_id" character varying(255) NOT NULL,
    "agency_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_desc" text,
    "route_type" integer,
    "route_url" text,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_sort_order" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "network_id" character varying(255),
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_shape" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_id" character varying(255) NOT NULL,
    "is_referenced" boolean DEFAULT false NOT NULL,
    "trip_count_using_shape" integer,
    "point_count" integer,
    "geom" geometry(LineString,4326),
    "shape_length_m" double precision,
    "shape_length_km" double precision,
    "analysis_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "qc_summary" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_shape_point" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "shape_uid" uuid,
    "shape_id" character varying(255) NOT NULL,
    "shape_pt_lat" double precision,
    "shape_pt_lon" double precision,
    "geom" geometry(Point,4326),
    "shape_pt_sequence" integer NOT NULL,
    "shape_dist_traveled" double precision,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_stop" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_code" character varying(255),
    "stop_name" text,
    "tts_stop_name" text,
    "stop_desc" text,
    "stop_lat" double precision,
    "stop_lon" double precision,
    "geom" geometry(Point,4326),
    "zone_id" character varying(255),
    "stop_url" text,
    "location_type" integer,
    "parent_station" character varying(255),
    "stop_timezone" character varying(100),
    "wheelchair_boarding" integer,
    "level_id" character varying(255),
    "platform_code" character varying(255),
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_stop_time" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "trip_uid" uuid,
    "stop_uid" uuid,
    "trip_id" character varying(255) NOT NULL,
    "stop_id" character varying(255) NOT NULL,
    "stop_sequence" integer NOT NULL,
    "arrival_time_raw" character varying(20),
    "arrival_seconds" integer,
    "departure_time_raw" character varying(20),
    "departure_seconds" integer,
    "stop_headsign" text,
    "pickup_type" integer,
    "drop_off_type" integer,
    "continuous_pickup" integer,
    "continuous_drop_off" integer,
    "shape_dist_traveled" double precision,
    "timepoint" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_trip" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "static_version_uid" uuid NOT NULL,
    "route_uid" uuid,
    "shape_uid" uuid,
    "route_id" character varying(255),
    "service_id" character varying(255),
    "trip_id" character varying(255) NOT NULL,
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "direction_id" smallint,
    "block_id" character varying(255),
    "shape_id" character varying(255),
    "wheelchair_accessible" integer,
    "bikes_allowed" integer,
    "source_row" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."static_version" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "source_request_uid" uuid,
    "version_code" character varying(120),
    "static_sha256" character varying(64) NOT NULL,
    "static_object_path" text NOT NULL,
    "zip_size_bytes" bigint,
    "downloaded_at" timestamp with time zone,
    "effective_from" timestamp with time zone,
    "effective_to" timestamp with time zone,
    "is_current" boolean DEFAULT false NOT NULL,
    "routes_file_present" boolean,
    "trips_file_present" boolean,
    "stops_file_present" boolean,
    "stop_times_file_present" boolean,
    "shapes_file_present" boolean,
    "calendar_file_present" boolean,
    "calendar_dates_file_present" boolean,
    "frequencies_file_present" boolean,
    "routes_count" integer,
    "trips_count" integer,
    "stops_count" integer,
    "stop_times_count" integer,
    "shapes_count" integer,
    "shape_points_count" integer,
    "notes" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."vehicle_latest_state" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "observation_uid" uuid NOT NULL,
    "vehicle_id" character varying(255) NOT NULL,
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "trip_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "static_direction_id" smallint,
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "source_speed_raw" double precision,
    "derived_speed_kmh" double precision,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "last_seen_time" timestamp with time zone NOT NULL,
    "freshness_seconds" bigint,
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."vehicle_observation" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "feed_uid" uuid NOT NULL,
    "run_uid" uuid,
    "request_uid" uuid NOT NULL,
    "snapshot_uid" uuid NOT NULL,
    "static_version_uid" uuid,
    "static_route_uid" uuid,
    "static_trip_uid" uuid,
    "static_shape_uid" uuid,
    "static_stop_uid" uuid,
    "entity_sequence" integer NOT NULL,
    "entity_id" character varying(255),
    "vehicle_id" character varying(255),
    "vehicle_label" character varying(255),
    "license_plate" character varying(255),
    "wheelchair_accessible" character varying(50),
    "trip_id" character varying(255),
    "trip_start_time_raw" character varying(20),
    "trip_start_seconds" integer,
    "trip_start_date_raw" character varying(20),
    "trip_start_date" date,
    "realtime_schedule_relationship" character varying(50),
    "realtime_route_id" character varying(255),
    "static_route_id" character varying(255),
    "resolved_route_id" character varying(255),
    "route_short_name" character varying(255),
    "route_long_name" text,
    "route_type" integer,
    "route_color" character varying(20),
    "route_text_color" character varying(20),
    "route_resolution_method" character varying(50),
    "realtime_route_direct_matched" boolean,
    "route_resolved" boolean,
    "static_service_id" character varying(255),
    "trip_headsign" text,
    "trip_short_name" character varying(255),
    "shape_id" character varying(255),
    "trip_static_matched" boolean,
    "realtime_direction_id" smallint,
    "static_direction_id" smallint,
    "direction_comparison" character varying(30),
    "latitude" double precision,
    "longitude" double precision,
    "geom" geometry(Point,4326),
    "bearing" double precision,
    "odometer_m" double precision,
    "source_speed_raw" double precision,
    "source_speed_present" boolean DEFAULT false NOT NULL,
    "source_speed_unit_declared" character varying(50),
    "source_speed_unit_interpretation" character varying(100),
    "current_stop_sequence" integer,
    "stop_id" character varying(255),
    "current_status" character varying(50),
    "congestion_level" character varying(50),
    "occupancy_status" character varying(50),
    "occupancy_percentage" integer,
    "multi_carriage_details" jsonb,
    "vehicle_timestamp_raw" bigint,
    "vehicle_time" timestamp with time zone,
    "previous_vehicle_time" timestamp with time zone,
    "ingest_time" timestamp with time zone,
    "freshness_seconds" bigint,
    "vehicle_gap_seconds" bigint,
    "distance_from_previous_m" double precision,
    "derived_speed_kmh" double precision,
    "position_present" boolean,
    "position_wgs84_valid" boolean,
    "zero_zero_position" boolean,
    "feed_bounds_valid" boolean,
    "position_qc_status" character varying(50),
    "gps_jump_status" character varying(50),
    "jump_calculation_skipped_reason" character varying(100),
    "analysis_eligible" boolean,
    "spatial_eligible" boolean,
    "duplicate_observation" boolean DEFAULT false NOT NULL,
    "observation_key" character varying(64),
    "qc_flags" jsonb DEFAULT '[]'::jsonb NOT NULL,
    "realtime_entity" jsonb NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "melaka"."vehicle_observation_qc" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "observation_uid" uuid NOT NULL,
    "qc_code" character varying(100) NOT NULL,
    "qc_category" character varying(50),
    "severity" character varying(30),
    "qc_value_numeric" double precision,
    "qc_value_text" text,
    "details" jsonb,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."api_endpoint" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "api_code" character varying(10) NOT NULL,
    "api_name" character varying(100) NOT NULL,
    "endpoint_url" text NOT NULL,
    "schedule_type" character varying(20) NOT NULL,
    "interval_minutes" integer,
    "schedule_rule" character varying(100),
    "raw_save_enabled" boolean DEFAULT true NOT NULL,
    "db_write_enabled" boolean DEFAULT true NOT NULL,
    "enabled" boolean DEFAULT true NOT NULL,
    "description" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."collection_artifact" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "run_uid" uuid NOT NULL,
    "artifact_type" character varying(30) NOT NULL,
    "file_name" text NOT NULL,
    "file_path" text NOT NULL,
    "file_size_bytes" bigint NOT NULL,
    "sha256" character(64),
    "compression_type" character varying(20) DEFAULT 'NONE'::character varying NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."collection_page_log" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "run_uid" uuid NOT NULL,
    "page_no" integer NOT NULL,
    "skip_value" integer NOT NULL,
    "request_start_time" timestamp with time zone NOT NULL,
    "request_end_time" timestamp with time zone,
    "source_updated_time" timestamp with time zone,
    "http_status" smallint,
    "record_count" integer DEFAULT 0 NOT NULL,
    "response_bytes" bigint DEFAULT 0 NOT NULL,
    "success" boolean DEFAULT false NOT NULL,
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."collection_run" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "api_endpoint_uid" uuid NOT NULL,
    "run_mode" character varying(20) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    "scheduled_time" timestamp with time zone,
    "attempt_no" smallint DEFAULT 1 NOT NULL,
    "request_start_time" timestamp with time zone NOT NULL,
    "request_end_time" timestamp with time zone,
    "source_updated_time" timestamp with time zone,
    "http_status" smallint,
    "page_count" integer DEFAULT 0 NOT NULL,
    "record_count" integer DEFAULT 0 NOT NULL,
    "response_bytes" bigint DEFAULT 0 NOT NULL,
    "success" boolean DEFAULT false NOT NULL,
    "snapshot_complete" boolean DEFAULT false NOT NULL,
    "consistency_status" character varying(20),
    "retry_count" smallint DEFAULT 0 NOT NULL,
    "error_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."faulty_traffic_light_event" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "alarm_id" character varying(100) NOT NULL,
    "node_id" character varying(100) NOT NULL,
    "fault_type" smallint,
    "source_start_time" timestamp with time zone NOT NULL,
    "source_end_time" timestamp with time zone,
    "message" text,
    "area_uid" uuid,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "resolved_time" timestamp with time zone,
    "active" boolean DEFAULT true NOT NULL,
    "first_run_uid" uuid NOT NULL,
    "last_run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."road_opening_event" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "event_id" character varying(100) NOT NULL,
    "start_date" date NOT NULL,
    "end_date" date,
    "svc_dept" character varying(255),
    "road_name" character varying(255),
    "other" text,
    "area_uid" uuid,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "resolved_time" timestamp with time zone,
    "active" boolean DEFAULT true NOT NULL,
    "first_run_uid" uuid NOT NULL,
    "last_run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."road_work_event" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "event_id" character varying(100) NOT NULL,
    "start_date" date NOT NULL,
    "end_date" date,
    "svc_dept" character varying(255),
    "road_name" character varying(255),
    "other" text,
    "area_uid" uuid,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "resolved_time" timestamp with time zone,
    "active" boolean DEFAULT true NOT NULL,
    "first_run_uid" uuid NOT NULL,
    "last_run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."study_area" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "ciq_code" character varying(20) NOT NULL,
    "zone_code" character varying(20) NOT NULL,
    "area_name" character varying(150) NOT NULL,
    "corridor_name" character varying(150),
    "geom" geometry(MultiPolygon,4326) NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."traffic_flow_file" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "check_month" date NOT NULL,
    "run_uid" uuid NOT NULL,
    "download_url" text NOT NULL,
    "file_name" text NOT NULL,
    "file_path" text NOT NULL,
    "file_size_bytes" bigint NOT NULL,
    "sha256" character(64),
    "is_new_version" boolean DEFAULT true NOT NULL,
    "source_period" character varying(100),
    "downloaded_time" timestamp with time zone NOT NULL,
    "parse_status" character varying(20) DEFAULT 'NOT_PARSED'::character varying NOT NULL,
    "parse_message" text,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."traffic_incident_event" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "event_fingerprint" character(64) NOT NULL,
    "type" character varying(100) NOT NULL,
    "latitude" numeric(20,16) NOT NULL,
    "longitude" numeric(20,16) NOT NULL,
    "geom" geometry(Point,4326) NOT NULL,
    "message" text NOT NULL,
    "area_uid" uuid,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "resolved_time" timestamp with time zone,
    "active" boolean DEFAULT true NOT NULL,
    "first_run_uid" uuid NOT NULL,
    "last_run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."traffic_link" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "link_id" character varying(50) NOT NULL,
    "road_name" character varying(255),
    "road_category" smallint,
    "start_lon" numeric(20,16) NOT NULL,
    "start_lat" numeric(20,16) NOT NULL,
    "end_lon" numeric(20,16) NOT NULL,
    "end_lat" numeric(20,16) NOT NULL,
    "geom" geometry(LineString,4326) NOT NULL,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."traffic_link_scope" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "link_uid" uuid NOT NULL,
    "area_uid" uuid NOT NULL,
    "match_method" character varying(30) NOT NULL,
    "is_primary" boolean DEFAULT false NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."traffic_speed_observation" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "snapshot_time" timestamp with time zone NOT NULL,
    "link_uid" uuid NOT NULL,
    "speed_band" smallint NOT NULL,
    "minimum_speed" smallint,
    "maximum_speed" smallint,
    "source_updated_time" timestamp with time zone,
    "run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."travel_time_observation" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "snapshot_time" timestamp with time zone NOT NULL,
    "segment_uid" uuid NOT NULL,
    "est_time_min" smallint NOT NULL,
    "run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."travel_time_segment" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "name" character varying(20) NOT NULL,
    "direction" smallint NOT NULL,
    "far_end_point" character varying(255) NOT NULL,
    "start_point" character varying(255) NOT NULL,
    "end_point" character varying(255) NOT NULL,
    "area_uid" uuid,
    "active" boolean DEFAULT true NOT NULL,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."vms_equipment" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "equipment_id" character varying(100) NOT NULL,
    "latitude" numeric(20,16) NOT NULL,
    "longitude" numeric(20,16) NOT NULL,
    "geom" geometry(Point,4326) NOT NULL,
    "area_uid" uuid,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "lta"."vms_message_state" (
    "uid" uuid DEFAULT gen_random_uuid() NOT NULL,
    "equipment_uid" uuid NOT NULL,
    "message" text NOT NULL,
    "message_hash" character(64) NOT NULL,
    "first_seen_time" timestamp with time zone NOT NULL,
    "last_seen_time" timestamp with time zone NOT NULL,
    "ended_time" timestamp with time zone,
    "active" boolean DEFAULT true NOT NULL,
    "first_run_uid" uuid NOT NULL,
    "last_run_uid" uuid NOT NULL,
    "create_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "update_time" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE "core"."collection_run" ADD CONSTRAINT "pk_collection_run" PRIMARY KEY (uid);
ALTER TABLE "core"."collection_run" ADD CONSTRAINT "uk_collection_run_run_code" UNIQUE (run_code);
ALTER TABLE "core"."daily_archive" ADD CONSTRAINT "pk_daily_archive" PRIMARY KEY (uid);
ALTER TABLE "core"."daily_archive" ADD CONSTRAINT "uk_daily_archive_archive_date" UNIQUE (archive_date);
ALTER TABLE "core"."gtfs_feed" ADD CONSTRAINT "ck_gtfs_feed_poll_interval_positive" CHECK (poll_interval_seconds > 0);
ALTER TABLE "core"."gtfs_feed" ADD CONSTRAINT "ck_gtfs_feed_stagger_offset_nonnegative" CHECK (stagger_offset_seconds IS NULL OR stagger_offset_seconds >= 0);
ALTER TABLE "core"."gtfs_feed" ADD CONSTRAINT "pk_gtfs_feed" PRIMARY KEY (uid);
ALTER TABLE "core"."gtfs_feed" ADD CONSTRAINT "uk_gtfs_feed_feed_id" UNIQUE (feed_id);
ALTER TABLE "core"."gtfs_feed" ADD CONSTRAINT "uk_gtfs_feed_file_prefix" UNIQUE (file_prefix);
ALTER TABLE "core"."study_city" ADD CONSTRAINT "pk_study_city" PRIMARY KEY (uid);
ALTER TABLE "core"."study_city" ADD CONSTRAINT "uk_study_city_city_code" UNIQUE (city_code);
ALTER TABLE "core"."study_city" ADD CONSTRAINT "uk_study_city_schema_name" UNIQUE (schema_name);
ALTER TABLE "jb"."api_request_log" ADD CONSTRAINT "pk_api_request_log" PRIMARY KEY (uid);
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "pk_realtime_snapshot" PRIMARY KEY (uid);
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "uk_realtime_snapshot_request" UNIQUE (request_uid);
ALTER TABLE "jb"."static_route" ADD CONSTRAINT "pk_static_route" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_route" ADD CONSTRAINT "uk_static_route_version_route_id" UNIQUE (static_version_uid, route_id);
ALTER TABLE "jb"."static_shape" ADD CONSTRAINT "pk_static_shape" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_shape" ADD CONSTRAINT "uk_static_shape_version_shape_id" UNIQUE (static_version_uid, shape_id);
ALTER TABLE "jb"."static_shape_point" ADD CONSTRAINT "pk_static_shape_point" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_shape_point" ADD CONSTRAINT "uk_static_shape_point_version_shape_sequence" UNIQUE (static_version_uid, shape_id, shape_pt_sequence);
ALTER TABLE "jb"."static_stop" ADD CONSTRAINT "pk_static_stop" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_stop" ADD CONSTRAINT "uk_static_stop_version_stop_id" UNIQUE (static_version_uid, stop_id);
ALTER TABLE "jb"."static_stop_time" ADD CONSTRAINT "pk_static_stop_time" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_stop_time" ADD CONSTRAINT "uk_static_stop_time_version_trip_sequence" UNIQUE (static_version_uid, trip_id, stop_sequence);
ALTER TABLE "jb"."static_trip" ADD CONSTRAINT "pk_static_trip" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_trip" ADD CONSTRAINT "uk_static_trip_version_trip_id" UNIQUE (static_version_uid, trip_id);
ALTER TABLE "jb"."static_version" ADD CONSTRAINT "pk_static_version" PRIMARY KEY (uid);
ALTER TABLE "jb"."static_version" ADD CONSTRAINT "uk_static_version_feed_sha256" UNIQUE (feed_uid, static_sha256);
ALTER TABLE "jb"."vehicle_latest_state" ADD CONSTRAINT "pk_vehicle_latest_state" PRIMARY KEY (uid);
ALTER TABLE "jb"."vehicle_latest_state" ADD CONSTRAINT "uk_vehicle_latest_state_feed_vehicle" UNIQUE (feed_uid, vehicle_id);
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "pk_vehicle_observation" PRIMARY KEY (uid);
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "uk_vehicle_observation_snapshot_sequence" UNIQUE (snapshot_uid, entity_sequence);
ALTER TABLE "jb"."vehicle_observation_qc" ADD CONSTRAINT "pk_vehicle_observation_qc" PRIMARY KEY (uid);
ALTER TABLE "jb"."vehicle_observation_qc" ADD CONSTRAINT "uk_vehicle_observation_qc_observation_code" UNIQUE (observation_uid, qc_code);
ALTER TABLE "kuching"."api_request_log" ADD CONSTRAINT "pk_api_request_log" PRIMARY KEY (uid);
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "pk_realtime_snapshot" PRIMARY KEY (uid);
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "uk_realtime_snapshot_request" UNIQUE (request_uid);
ALTER TABLE "kuching"."static_route" ADD CONSTRAINT "pk_static_route" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_route" ADD CONSTRAINT "uk_static_route_version_route_id" UNIQUE (static_version_uid, route_id);
ALTER TABLE "kuching"."static_shape" ADD CONSTRAINT "pk_static_shape" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_shape" ADD CONSTRAINT "uk_static_shape_version_shape_id" UNIQUE (static_version_uid, shape_id);
ALTER TABLE "kuching"."static_shape_point" ADD CONSTRAINT "pk_static_shape_point" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_shape_point" ADD CONSTRAINT "uk_static_shape_point_version_shape_sequence" UNIQUE (static_version_uid, shape_id, shape_pt_sequence);
ALTER TABLE "kuching"."static_stop" ADD CONSTRAINT "pk_static_stop" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_stop" ADD CONSTRAINT "uk_static_stop_version_stop_id" UNIQUE (static_version_uid, stop_id);
ALTER TABLE "kuching"."static_stop_time" ADD CONSTRAINT "pk_static_stop_time" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_stop_time" ADD CONSTRAINT "uk_static_stop_time_version_trip_sequence" UNIQUE (static_version_uid, trip_id, stop_sequence);
ALTER TABLE "kuching"."static_trip" ADD CONSTRAINT "pk_static_trip" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_trip" ADD CONSTRAINT "uk_static_trip_version_trip_id" UNIQUE (static_version_uid, trip_id);
ALTER TABLE "kuching"."static_version" ADD CONSTRAINT "pk_static_version" PRIMARY KEY (uid);
ALTER TABLE "kuching"."static_version" ADD CONSTRAINT "uk_static_version_feed_sha256" UNIQUE (feed_uid, static_sha256);
ALTER TABLE "kuching"."vehicle_latest_state" ADD CONSTRAINT "pk_vehicle_latest_state" PRIMARY KEY (uid);
ALTER TABLE "kuching"."vehicle_latest_state" ADD CONSTRAINT "uk_vehicle_latest_state_feed_vehicle" UNIQUE (feed_uid, vehicle_id);
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "pk_vehicle_observation" PRIMARY KEY (uid);
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "uk_vehicle_observation_snapshot_sequence" UNIQUE (snapshot_uid, entity_sequence);
ALTER TABLE "kuching"."vehicle_observation_qc" ADD CONSTRAINT "pk_vehicle_observation_qc" PRIMARY KEY (uid);
ALTER TABLE "kuching"."vehicle_observation_qc" ADD CONSTRAINT "uk_vehicle_observation_qc_observation_code" UNIQUE (observation_uid, qc_code);
ALTER TABLE "kl"."api_request_log" ADD CONSTRAINT "pk_api_request_log" PRIMARY KEY (uid);
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "pk_realtime_snapshot" PRIMARY KEY (uid);
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "uk_realtime_snapshot_request" UNIQUE (request_uid);
ALTER TABLE "kl"."static_route" ADD CONSTRAINT "pk_static_route" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_route" ADD CONSTRAINT "uk_static_route_version_route_id" UNIQUE (static_version_uid, route_id);
ALTER TABLE "kl"."static_shape" ADD CONSTRAINT "pk_static_shape" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_shape" ADD CONSTRAINT "uk_static_shape_version_shape_id" UNIQUE (static_version_uid, shape_id);
ALTER TABLE "kl"."static_shape_point" ADD CONSTRAINT "pk_static_shape_point" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_shape_point" ADD CONSTRAINT "uk_static_shape_point_version_shape_sequence" UNIQUE (static_version_uid, shape_id, shape_pt_sequence);
ALTER TABLE "kl"."static_stop" ADD CONSTRAINT "pk_static_stop" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_stop" ADD CONSTRAINT "uk_static_stop_version_stop_id" UNIQUE (static_version_uid, stop_id);
ALTER TABLE "kl"."static_stop_time" ADD CONSTRAINT "pk_static_stop_time" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_stop_time" ADD CONSTRAINT "uk_static_stop_time_version_trip_sequence" UNIQUE (static_version_uid, trip_id, stop_sequence);
ALTER TABLE "kl"."static_trip" ADD CONSTRAINT "pk_static_trip" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_trip" ADD CONSTRAINT "uk_static_trip_version_trip_id" UNIQUE (static_version_uid, trip_id);
ALTER TABLE "kl"."static_version" ADD CONSTRAINT "pk_static_version" PRIMARY KEY (uid);
ALTER TABLE "kl"."static_version" ADD CONSTRAINT "uk_static_version_feed_sha256" UNIQUE (feed_uid, static_sha256);
ALTER TABLE "kl"."vehicle_latest_state" ADD CONSTRAINT "pk_vehicle_latest_state" PRIMARY KEY (uid);
ALTER TABLE "kl"."vehicle_latest_state" ADD CONSTRAINT "uk_vehicle_latest_state_feed_vehicle" UNIQUE (feed_uid, vehicle_id);
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "pk_vehicle_observation" PRIMARY KEY (uid);
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "uk_vehicle_observation_snapshot_sequence" UNIQUE (snapshot_uid, entity_sequence);
ALTER TABLE "kl"."vehicle_observation_qc" ADD CONSTRAINT "pk_vehicle_observation_qc" PRIMARY KEY (uid);
ALTER TABLE "kl"."vehicle_observation_qc" ADD CONSTRAINT "uk_vehicle_observation_qc_observation_code" UNIQUE (observation_uid, qc_code);
ALTER TABLE "melaka"."api_request_log" ADD CONSTRAINT "pk_api_request_log" PRIMARY KEY (uid);
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "pk_realtime_snapshot" PRIMARY KEY (uid);
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "uk_realtime_snapshot_request" UNIQUE (request_uid);
ALTER TABLE "melaka"."static_route" ADD CONSTRAINT "pk_static_route" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_route" ADD CONSTRAINT "uk_static_route_version_route_id" UNIQUE (static_version_uid, route_id);
ALTER TABLE "melaka"."static_shape" ADD CONSTRAINT "pk_static_shape" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_shape" ADD CONSTRAINT "uk_static_shape_version_shape_id" UNIQUE (static_version_uid, shape_id);
ALTER TABLE "melaka"."static_shape_point" ADD CONSTRAINT "pk_static_shape_point" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_shape_point" ADD CONSTRAINT "uk_static_shape_point_version_shape_sequence" UNIQUE (static_version_uid, shape_id, shape_pt_sequence);
ALTER TABLE "melaka"."static_stop" ADD CONSTRAINT "pk_static_stop" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_stop" ADD CONSTRAINT "uk_static_stop_version_stop_id" UNIQUE (static_version_uid, stop_id);
ALTER TABLE "melaka"."static_stop_time" ADD CONSTRAINT "pk_static_stop_time" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_stop_time" ADD CONSTRAINT "uk_static_stop_time_version_trip_sequence" UNIQUE (static_version_uid, trip_id, stop_sequence);
ALTER TABLE "melaka"."static_trip" ADD CONSTRAINT "pk_static_trip" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_trip" ADD CONSTRAINT "uk_static_trip_version_trip_id" UNIQUE (static_version_uid, trip_id);
ALTER TABLE "melaka"."static_version" ADD CONSTRAINT "pk_static_version" PRIMARY KEY (uid);
ALTER TABLE "melaka"."static_version" ADD CONSTRAINT "uk_static_version_feed_sha256" UNIQUE (feed_uid, static_sha256);
ALTER TABLE "melaka"."vehicle_latest_state" ADD CONSTRAINT "pk_vehicle_latest_state" PRIMARY KEY (uid);
ALTER TABLE "melaka"."vehicle_latest_state" ADD CONSTRAINT "uk_vehicle_latest_state_feed_vehicle" UNIQUE (feed_uid, vehicle_id);
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "pk_vehicle_observation" PRIMARY KEY (uid);
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "uk_vehicle_observation_snapshot_sequence" UNIQUE (snapshot_uid, entity_sequence);
ALTER TABLE "melaka"."vehicle_observation_qc" ADD CONSTRAINT "pk_vehicle_observation_qc" PRIMARY KEY (uid);
ALTER TABLE "melaka"."vehicle_observation_qc" ADD CONSTRAINT "uk_vehicle_observation_qc_observation_code" UNIQUE (observation_uid, qc_code);
ALTER TABLE "lta"."api_endpoint" ADD CONSTRAINT "pk_api_endpoint" PRIMARY KEY (uid);
ALTER TABLE "lta"."api_endpoint" ADD CONSTRAINT "uq_api_endpoint_api_code" UNIQUE (api_code);
ALTER TABLE "lta"."collection_artifact" ADD CONSTRAINT "ck_collection_artifact_size" CHECK (file_size_bytes >= 0);
ALTER TABLE "lta"."collection_artifact" ADD CONSTRAINT "ck_collection_artifact_type" CHECK (artifact_type::text = ANY (ARRAY['RAW_JSON'::character varying, 'DATA_FILE'::character varying, 'SUMMARY'::character varying]::text[]));
ALTER TABLE "lta"."collection_artifact" ADD CONSTRAINT "pk_collection_artifact" PRIMARY KEY (uid);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "ck_collection_page_log_page_no" CHECK (page_no >= 1);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "ck_collection_page_log_record_count" CHECK (record_count >= 0);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "ck_collection_page_log_response_bytes" CHECK (response_bytes >= 0);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "ck_collection_page_log_skip" CHECK (skip_value >= 0);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "pk_collection_page_log" PRIMARY KEY (uid);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "uq_collection_page_log_page" UNIQUE (run_uid, page_no);
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "uq_collection_page_log_skip" UNIQUE (run_uid, skip_value);
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "ck_collection_run_attempt" CHECK (attempt_no >= 1);
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "ck_collection_run_mode" CHECK (run_mode::text = ANY (ARRAY['SCHEDULED'::character varying, 'MANUAL_TEST'::character varying, 'BACKFILL'::character varying]::text[]));
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "ck_collection_run_page_count" CHECK (page_count >= 0);
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "ck_collection_run_record_count" CHECK (record_count >= 0);
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "ck_collection_run_response_bytes" CHECK (response_bytes >= 0);
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "ck_collection_run_retry_count" CHECK (retry_count >= 0);
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "pk_collection_run" PRIMARY KEY (uid);
ALTER TABLE "lta"."faulty_traffic_light_event" ADD CONSTRAINT "ck_faulty_traffic_light_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."faulty_traffic_light_event" ADD CONSTRAINT "pk_faulty_traffic_light_event" PRIMARY KEY (uid);
ALTER TABLE "lta"."faulty_traffic_light_event" ADD CONSTRAINT "uq_faulty_traffic_light_alarm" UNIQUE (alarm_id);
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "ck_road_opening_dates" CHECK (end_date IS NULL OR end_date >= start_date);
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "ck_road_opening_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "pk_road_opening_event" PRIMARY KEY (uid);
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "uq_road_opening_event_id" UNIQUE (event_id);
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "ck_road_work_dates" CHECK (end_date IS NULL OR end_date >= start_date);
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "ck_road_work_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "pk_road_work_event" PRIMARY KEY (uid);
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "uq_road_work_event_id" UNIQUE (event_id);
ALTER TABLE "lta"."study_area" ADD CONSTRAINT "ck_study_area_ciq" CHECK (ciq_code::text = ANY (ARRAY['WOODLANDS'::character varying, 'TUAS'::character varying]::text[]));
ALTER TABLE "lta"."study_area" ADD CONSTRAINT "ck_study_area_zone" CHECK (zone_code::text = ANY (ARRAY['CORE'::character varying, 'APPROACH'::character varying, 'CORRIDOR'::character varying]::text[]));
ALTER TABLE "lta"."study_area" ADD CONSTRAINT "pk_study_area" PRIMARY KEY (uid);
ALTER TABLE "lta"."study_area" ADD CONSTRAINT "uq_study_area_business" UNIQUE (ciq_code, zone_code, area_name);
ALTER TABLE "lta"."traffic_flow_file" ADD CONSTRAINT "ck_traffic_flow_month" CHECK (EXTRACT(day FROM check_month) = 1::numeric);
ALTER TABLE "lta"."traffic_flow_file" ADD CONSTRAINT "ck_traffic_flow_parse_status" CHECK (parse_status::text = ANY (ARRAY['NOT_PARSED'::character varying, 'PARSED'::character varying, 'FAILED'::character varying]::text[]));
ALTER TABLE "lta"."traffic_flow_file" ADD CONSTRAINT "ck_traffic_flow_size" CHECK (file_size_bytes >= 0);
ALTER TABLE "lta"."traffic_flow_file" ADD CONSTRAINT "pk_traffic_flow_file" PRIMARY KEY (uid);
ALTER TABLE "lta"."traffic_flow_file" ADD CONSTRAINT "uq_traffic_flow_check_month" UNIQUE (check_month);
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "ck_traffic_incident_lat" CHECK (latitude >= '-90'::integer::numeric AND latitude <= 90::numeric);
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "ck_traffic_incident_lon" CHECK (longitude >= '-180'::integer::numeric AND longitude <= 180::numeric);
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "ck_traffic_incident_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "pk_traffic_incident_event" PRIMARY KEY (uid);
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "uq_traffic_incident_fingerprint" UNIQUE (event_fingerprint);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "ck_traffic_link_end_lat" CHECK (end_lat >= '-90'::integer::numeric AND end_lat <= 90::numeric);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "ck_traffic_link_end_lon" CHECK (end_lon >= '-180'::integer::numeric AND end_lon <= 180::numeric);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "ck_traffic_link_seen_time" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "ck_traffic_link_start_lat" CHECK (start_lat >= '-90'::integer::numeric AND start_lat <= 90::numeric);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "ck_traffic_link_start_lon" CHECK (start_lon >= '-180'::integer::numeric AND start_lon <= 180::numeric);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "pk_traffic_link" PRIMARY KEY (uid);
ALTER TABLE "lta"."traffic_link" ADD CONSTRAINT "uq_traffic_link_link_id" UNIQUE (link_id);
ALTER TABLE "lta"."traffic_link_scope" ADD CONSTRAINT "ck_traffic_link_scope_method" CHECK (match_method::text = ANY (ARRAY['SPATIAL'::character varying, 'MANUAL'::character varying, 'ROAD_NAME'::character varying]::text[]));
ALTER TABLE "lta"."traffic_link_scope" ADD CONSTRAINT "pk_traffic_link_scope" PRIMARY KEY (uid);
ALTER TABLE "lta"."traffic_link_scope" ADD CONSTRAINT "uq_traffic_link_scope" UNIQUE (link_uid, area_uid);
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "ck_traffic_speed_band" CHECK (speed_band >= 1 AND speed_band <= 8);
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "ck_traffic_speed_max" CHECK (maximum_speed IS NULL OR maximum_speed >= 0);
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "ck_traffic_speed_min" CHECK (minimum_speed IS NULL OR minimum_speed >= 0);
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "ck_traffic_speed_range" CHECK (minimum_speed IS NULL OR maximum_speed IS NULL OR maximum_speed >= minimum_speed);
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "pk_traffic_speed_observation" PRIMARY KEY (uid);
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "uq_traffic_speed_observation" UNIQUE (snapshot_time, link_uid);
ALTER TABLE "lta"."travel_time_observation" ADD CONSTRAINT "ck_travel_time_est" CHECK (est_time_min >= 0);
ALTER TABLE "lta"."travel_time_observation" ADD CONSTRAINT "pk_travel_time_observation" PRIMARY KEY (uid);
ALTER TABLE "lta"."travel_time_observation" ADD CONSTRAINT "uq_travel_time_observation" UNIQUE (snapshot_time, segment_uid);
ALTER TABLE "lta"."travel_time_segment" ADD CONSTRAINT "ck_travel_time_segment_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."travel_time_segment" ADD CONSTRAINT "pk_travel_time_segment" PRIMARY KEY (uid);
ALTER TABLE "lta"."travel_time_segment" ADD CONSTRAINT "uq_travel_time_segment" UNIQUE (name, direction, far_end_point, start_point, end_point);
ALTER TABLE "lta"."vms_equipment" ADD CONSTRAINT "ck_vms_equipment_lat" CHECK (latitude >= '-90'::integer::numeric AND latitude <= 90::numeric);
ALTER TABLE "lta"."vms_equipment" ADD CONSTRAINT "ck_vms_equipment_lon" CHECK (longitude >= '-180'::integer::numeric AND longitude <= 180::numeric);
ALTER TABLE "lta"."vms_equipment" ADD CONSTRAINT "ck_vms_equipment_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."vms_equipment" ADD CONSTRAINT "pk_vms_equipment" PRIMARY KEY (uid);
ALTER TABLE "lta"."vms_equipment" ADD CONSTRAINT "uq_vms_equipment_id" UNIQUE (equipment_id);
ALTER TABLE "lta"."vms_message_state" ADD CONSTRAINT "ck_vms_message_seen" CHECK (last_seen_time >= first_seen_time);
ALTER TABLE "lta"."vms_message_state" ADD CONSTRAINT "pk_vms_message_state" PRIMARY KEY (uid);

ALTER TABLE "core"."gtfs_feed" ADD CONSTRAINT "fk_gtfs_feed_city" FOREIGN KEY (city_uid) REFERENCES core.study_city(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."api_request_log" ADD CONSTRAINT "fk_api_request_log_duplicate_request" FOREIGN KEY (duplicate_of_request_uid) REFERENCES jb.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."api_request_log" ADD CONSTRAINT "fk_api_request_log_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."api_request_log" ADD CONSTRAINT "fk_api_request_log_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_enrichment_static_version" FOREIGN KEY (enrichment_static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_referenced_snapshot" FOREIGN KEY (referenced_snapshot_uid) REFERENCES jb.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_request" FOREIGN KEY (request_uid) REFERENCES jb.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_route" ADD CONSTRAINT "fk_static_route_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_shape" ADD CONSTRAINT "fk_static_shape_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_shape" FOREIGN KEY (shape_uid) REFERENCES jb.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_stop" ADD CONSTRAINT "fk_static_stop_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_stop" FOREIGN KEY (stop_uid) REFERENCES jb.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_trip" FOREIGN KEY (trip_uid) REFERENCES jb.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_trip" ADD CONSTRAINT "fk_static_trip_route" FOREIGN KEY (route_uid) REFERENCES jb.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_trip" ADD CONSTRAINT "fk_static_trip_shape" FOREIGN KEY (shape_uid) REFERENCES jb.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_trip" ADD CONSTRAINT "fk_static_trip_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_version" ADD CONSTRAINT "fk_static_version_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."static_version" ADD CONSTRAINT "fk_static_version_source_request" FOREIGN KEY (source_request_uid) REFERENCES jb.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_observation" FOREIGN KEY (observation_uid) REFERENCES jb.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_request" FOREIGN KEY (request_uid) REFERENCES jb.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_snapshot" FOREIGN KEY (snapshot_uid) REFERENCES jb.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_route" FOREIGN KEY (static_route_uid) REFERENCES jb.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_shape" FOREIGN KEY (static_shape_uid) REFERENCES jb.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_stop" FOREIGN KEY (static_stop_uid) REFERENCES jb.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_trip" FOREIGN KEY (static_trip_uid) REFERENCES jb.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_version" FOREIGN KEY (static_version_uid) REFERENCES jb.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "jb"."vehicle_observation_qc" ADD CONSTRAINT "fk_vehicle_observation_qc_observation" FOREIGN KEY (observation_uid) REFERENCES jb.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."api_request_log" ADD CONSTRAINT "fk_api_request_log_duplicate_request" FOREIGN KEY (duplicate_of_request_uid) REFERENCES kuching.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."api_request_log" ADD CONSTRAINT "fk_api_request_log_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."api_request_log" ADD CONSTRAINT "fk_api_request_log_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_enrichment_static_version" FOREIGN KEY (enrichment_static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_referenced_snapshot" FOREIGN KEY (referenced_snapshot_uid) REFERENCES kuching.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_request" FOREIGN KEY (request_uid) REFERENCES kuching.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_route" ADD CONSTRAINT "fk_static_route_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_shape" ADD CONSTRAINT "fk_static_shape_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_shape" FOREIGN KEY (shape_uid) REFERENCES kuching.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_stop" ADD CONSTRAINT "fk_static_stop_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_stop" FOREIGN KEY (stop_uid) REFERENCES kuching.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_trip" FOREIGN KEY (trip_uid) REFERENCES kuching.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_trip" ADD CONSTRAINT "fk_static_trip_route" FOREIGN KEY (route_uid) REFERENCES kuching.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_trip" ADD CONSTRAINT "fk_static_trip_shape" FOREIGN KEY (shape_uid) REFERENCES kuching.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_trip" ADD CONSTRAINT "fk_static_trip_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_version" ADD CONSTRAINT "fk_static_version_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."static_version" ADD CONSTRAINT "fk_static_version_source_request" FOREIGN KEY (source_request_uid) REFERENCES kuching.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_observation" FOREIGN KEY (observation_uid) REFERENCES kuching.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_request" FOREIGN KEY (request_uid) REFERENCES kuching.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_snapshot" FOREIGN KEY (snapshot_uid) REFERENCES kuching.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_route" FOREIGN KEY (static_route_uid) REFERENCES kuching.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_shape" FOREIGN KEY (static_shape_uid) REFERENCES kuching.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_stop" FOREIGN KEY (static_stop_uid) REFERENCES kuching.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_trip" FOREIGN KEY (static_trip_uid) REFERENCES kuching.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_version" FOREIGN KEY (static_version_uid) REFERENCES kuching.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kuching"."vehicle_observation_qc" ADD CONSTRAINT "fk_vehicle_observation_qc_observation" FOREIGN KEY (observation_uid) REFERENCES kuching.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."api_request_log" ADD CONSTRAINT "fk_api_request_log_duplicate_request" FOREIGN KEY (duplicate_of_request_uid) REFERENCES kl.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."api_request_log" ADD CONSTRAINT "fk_api_request_log_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."api_request_log" ADD CONSTRAINT "fk_api_request_log_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_enrichment_static_version" FOREIGN KEY (enrichment_static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_referenced_snapshot" FOREIGN KEY (referenced_snapshot_uid) REFERENCES kl.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_request" FOREIGN KEY (request_uid) REFERENCES kl.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_route" ADD CONSTRAINT "fk_static_route_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_shape" ADD CONSTRAINT "fk_static_shape_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_shape" FOREIGN KEY (shape_uid) REFERENCES kl.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_stop" ADD CONSTRAINT "fk_static_stop_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_stop" FOREIGN KEY (stop_uid) REFERENCES kl.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_trip" FOREIGN KEY (trip_uid) REFERENCES kl.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_trip" ADD CONSTRAINT "fk_static_trip_route" FOREIGN KEY (route_uid) REFERENCES kl.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_trip" ADD CONSTRAINT "fk_static_trip_shape" FOREIGN KEY (shape_uid) REFERENCES kl.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_trip" ADD CONSTRAINT "fk_static_trip_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_version" ADD CONSTRAINT "fk_static_version_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."static_version" ADD CONSTRAINT "fk_static_version_source_request" FOREIGN KEY (source_request_uid) REFERENCES kl.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_observation" FOREIGN KEY (observation_uid) REFERENCES kl.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_request" FOREIGN KEY (request_uid) REFERENCES kl.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_snapshot" FOREIGN KEY (snapshot_uid) REFERENCES kl.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_route" FOREIGN KEY (static_route_uid) REFERENCES kl.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_shape" FOREIGN KEY (static_shape_uid) REFERENCES kl.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_stop" FOREIGN KEY (static_stop_uid) REFERENCES kl.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_trip" FOREIGN KEY (static_trip_uid) REFERENCES kl.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_version" FOREIGN KEY (static_version_uid) REFERENCES kl.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "kl"."vehicle_observation_qc" ADD CONSTRAINT "fk_vehicle_observation_qc_observation" FOREIGN KEY (observation_uid) REFERENCES kl.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."api_request_log" ADD CONSTRAINT "fk_api_request_log_duplicate_request" FOREIGN KEY (duplicate_of_request_uid) REFERENCES melaka.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."api_request_log" ADD CONSTRAINT "fk_api_request_log_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."api_request_log" ADD CONSTRAINT "fk_api_request_log_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_enrichment_static_version" FOREIGN KEY (enrichment_static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_referenced_snapshot" FOREIGN KEY (referenced_snapshot_uid) REFERENCES melaka.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_request" FOREIGN KEY (request_uid) REFERENCES melaka.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."realtime_snapshot" ADD CONSTRAINT "fk_realtime_snapshot_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_route" ADD CONSTRAINT "fk_static_route_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_shape" ADD CONSTRAINT "fk_static_shape_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_shape" FOREIGN KEY (shape_uid) REFERENCES melaka.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_shape_point" ADD CONSTRAINT "fk_static_shape_point_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_stop" ADD CONSTRAINT "fk_static_stop_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_stop" FOREIGN KEY (stop_uid) REFERENCES melaka.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_trip" FOREIGN KEY (trip_uid) REFERENCES melaka.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_stop_time" ADD CONSTRAINT "fk_static_stop_time_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_trip" ADD CONSTRAINT "fk_static_trip_route" FOREIGN KEY (route_uid) REFERENCES melaka.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_trip" ADD CONSTRAINT "fk_static_trip_shape" FOREIGN KEY (shape_uid) REFERENCES melaka.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_trip" ADD CONSTRAINT "fk_static_trip_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_version" ADD CONSTRAINT "fk_static_version_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."static_version" ADD CONSTRAINT "fk_static_version_source_request" FOREIGN KEY (source_request_uid) REFERENCES melaka.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_latest_state" ADD CONSTRAINT "fk_vehicle_latest_state_observation" FOREIGN KEY (observation_uid) REFERENCES melaka.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_feed" FOREIGN KEY (feed_uid) REFERENCES core.gtfs_feed(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_request" FOREIGN KEY (request_uid) REFERENCES melaka.api_request_log(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_run" FOREIGN KEY (run_uid) REFERENCES core.collection_run(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_snapshot" FOREIGN KEY (snapshot_uid) REFERENCES melaka.realtime_snapshot(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_route" FOREIGN KEY (static_route_uid) REFERENCES melaka.static_route(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_shape" FOREIGN KEY (static_shape_uid) REFERENCES melaka.static_shape(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_stop" FOREIGN KEY (static_stop_uid) REFERENCES melaka.static_stop(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_trip" FOREIGN KEY (static_trip_uid) REFERENCES melaka.static_trip(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation" ADD CONSTRAINT "fk_vehicle_observation_static_version" FOREIGN KEY (static_version_uid) REFERENCES melaka.static_version(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "melaka"."vehicle_observation_qc" ADD CONSTRAINT "fk_vehicle_observation_qc_observation" FOREIGN KEY (observation_uid) REFERENCES melaka.vehicle_observation(uid) ON UPDATE RESTRICT ON DELETE RESTRICT;
ALTER TABLE "lta"."collection_artifact" ADD CONSTRAINT "fk_collection_artifact_run" FOREIGN KEY (run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."collection_page_log" ADD CONSTRAINT "fk_collection_page_log_run" FOREIGN KEY (run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."collection_run" ADD CONSTRAINT "fk_collection_run_endpoint" FOREIGN KEY (api_endpoint_uid) REFERENCES lta.api_endpoint(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."faulty_traffic_light_event" ADD CONSTRAINT "fk_faulty_traffic_light_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."faulty_traffic_light_event" ADD CONSTRAINT "fk_faulty_traffic_light_first_run" FOREIGN KEY (first_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."faulty_traffic_light_event" ADD CONSTRAINT "fk_faulty_traffic_light_last_run" FOREIGN KEY (last_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "fk_road_opening_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "fk_road_opening_first_run" FOREIGN KEY (first_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."road_opening_event" ADD CONSTRAINT "fk_road_opening_last_run" FOREIGN KEY (last_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "fk_road_work_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "fk_road_work_first_run" FOREIGN KEY (first_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."road_work_event" ADD CONSTRAINT "fk_road_work_last_run" FOREIGN KEY (last_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_flow_file" ADD CONSTRAINT "fk_traffic_flow_run" FOREIGN KEY (run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "fk_traffic_incident_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "fk_traffic_incident_first_run" FOREIGN KEY (first_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_incident_event" ADD CONSTRAINT "fk_traffic_incident_last_run" FOREIGN KEY (last_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_link_scope" ADD CONSTRAINT "fk_traffic_link_scope_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_link_scope" ADD CONSTRAINT "fk_traffic_link_scope_link" FOREIGN KEY (link_uid) REFERENCES lta.traffic_link(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "fk_traffic_speed_observation_link" FOREIGN KEY (link_uid) REFERENCES lta.traffic_link(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."traffic_speed_observation" ADD CONSTRAINT "fk_traffic_speed_observation_run" FOREIGN KEY (run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."travel_time_observation" ADD CONSTRAINT "fk_travel_time_observation_run" FOREIGN KEY (run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."travel_time_observation" ADD CONSTRAINT "fk_travel_time_observation_segment" FOREIGN KEY (segment_uid) REFERENCES lta.travel_time_segment(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."travel_time_segment" ADD CONSTRAINT "fk_travel_time_segment_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."vms_equipment" ADD CONSTRAINT "fk_vms_equipment_area" FOREIGN KEY (area_uid) REFERENCES lta.study_area(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."vms_message_state" ADD CONSTRAINT "fk_vms_message_equipment" FOREIGN KEY (equipment_uid) REFERENCES lta.vms_equipment(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."vms_message_state" ADD CONSTRAINT "fk_vms_message_first_run" FOREIGN KEY (first_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;
ALTER TABLE "lta"."vms_message_state" ADD CONSTRAINT "fk_vms_message_last_run" FOREIGN KEY (last_run_uid) REFERENCES lta.collection_run(uid) ON DELETE RESTRICT;

CREATE INDEX idx_collection_run_actual_end_time ON core.collection_run USING btree (actual_end_time);
CREATE INDEX idx_collection_run_actual_start_time ON core.collection_run USING btree (actual_start_time);
CREATE INDEX idx_collection_run_create_time ON core.collection_run USING btree (create_time);
CREATE INDEX idx_collection_run_planned_start_time ON core.collection_run USING btree (planned_start_time);
CREATE INDEX idx_collection_run_run_status ON core.collection_run USING btree (run_status);
CREATE INDEX idx_collection_run_run_type ON core.collection_run USING btree (run_type);
CREATE INDEX idx_collection_run_status_start_time ON core.collection_run USING btree (run_status, actual_start_time DESC);
CREATE INDEX idx_collection_run_type_start_time ON core.collection_run USING btree (run_type, actual_start_time DESC);
CREATE INDEX idx_collection_run_update_time ON core.collection_run USING btree (update_time);
CREATE INDEX idx_daily_archive_archive_created_at ON core.daily_archive USING btree (archive_created_at);
CREATE INDEX idx_daily_archive_archive_sha256 ON core.daily_archive USING btree (archive_sha256);
CREATE INDEX idx_daily_archive_archive_status ON core.daily_archive USING btree (archive_status);
CREATE INDEX idx_daily_archive_create_time ON core.daily_archive USING btree (create_time);
CREATE INDEX idx_daily_archive_status_archive_date ON core.daily_archive USING btree (archive_status, archive_date DESC);
CREATE INDEX idx_daily_archive_update_time ON core.daily_archive USING btree (update_time);
CREATE INDEX idx_daily_archive_verified_at ON core.daily_archive USING btree (verified_at);
CREATE INDEX idx_gtfs_feed_city_enabled ON core.gtfs_feed USING btree (city_uid, enabled);
CREATE INDEX idx_gtfs_feed_city_uid ON core.gtfs_feed USING btree (city_uid);
CREATE INDEX idx_gtfs_feed_create_time ON core.gtfs_feed USING btree (create_time);
CREATE INDEX idx_gtfs_feed_enabled ON core.gtfs_feed USING btree (enabled);
CREATE INDEX idx_gtfs_feed_service_type ON core.gtfs_feed USING btree (service_type);
CREATE INDEX idx_gtfs_feed_source_platform ON core.gtfs_feed USING btree (source_platform);
CREATE INDEX idx_gtfs_feed_update_time ON core.gtfs_feed USING btree (update_time);
CREATE INDEX idx_study_city_create_time ON core.study_city USING btree (create_time);
CREATE INDEX idx_study_city_enabled ON core.study_city USING btree (enabled);
CREATE INDEX idx_study_city_update_time ON core.study_city USING btree (update_time);
CREATE INDEX idx_api_request_log_create_time_brin ON jb.api_request_log USING brin (create_time);
CREATE INDEX idx_api_request_log_duplicate_request_uid ON jb.api_request_log USING btree (duplicate_of_request_uid);
CREATE INDEX idx_api_request_log_feed_http_request_time ON jb.api_request_log USING btree (feed_uid, http_status, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_request_time ON jb.api_request_log USING btree (feed_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_uid ON jb.api_request_log USING btree (feed_uid);
CREATE INDEX idx_api_request_log_http_status ON jb.api_request_log USING btree (http_status);
CREATE INDEX idx_api_request_log_request_started_at ON jb.api_request_log USING btree (request_started_at);
CREATE INDEX idx_api_request_log_request_type ON jb.api_request_log USING btree (request_type);
CREATE INDEX idx_api_request_log_response_received_at ON jb.api_request_log USING btree (response_received_at);
CREATE INDEX idx_api_request_log_response_sha256 ON jb.api_request_log USING btree (response_sha256);
CREATE INDEX idx_api_request_log_result ON jb.api_request_log USING btree (result);
CREATE INDEX idx_api_request_log_run_request_time ON jb.api_request_log USING btree (run_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_run_uid ON jb.api_request_log USING btree (run_uid);
CREATE INDEX idx_api_request_log_scheduled_at ON jb.api_request_log USING btree (scheduled_at);
CREATE INDEX idx_realtime_snapshot_create_time_brin ON jb.realtime_snapshot USING brin (create_time);
CREATE INDEX idx_realtime_snapshot_duplicate_snapshot ON jb.realtime_snapshot USING btree (duplicate_snapshot);
CREATE INDEX idx_realtime_snapshot_enrichment_static_version_uid ON jb.realtime_snapshot USING btree (enrichment_static_version_uid);
CREATE INDEX idx_realtime_snapshot_feed_feed_time ON jb.realtime_snapshot USING btree (feed_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_feed_time ON jb.realtime_snapshot USING btree (feed_time);
CREATE INDEX idx_realtime_snapshot_feed_uid ON jb.realtime_snapshot USING btree (feed_uid);
CREATE INDEX idx_realtime_snapshot_referenced_snapshot_uid ON jb.realtime_snapshot USING btree (referenced_snapshot_uid);
CREATE INDEX idx_realtime_snapshot_response_sha256 ON jb.realtime_snapshot USING btree (response_sha256);
CREATE INDEX idx_realtime_snapshot_run_feed_time ON jb.realtime_snapshot USING btree (run_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_run_uid ON jb.realtime_snapshot USING btree (run_uid);
CREATE INDEX idx_static_route_create_time ON jb.static_route USING btree (create_time);
CREATE INDEX idx_static_route_route_id ON jb.static_route USING btree (route_id);
CREATE INDEX idx_static_route_route_short_name ON jb.static_route USING btree (route_short_name);
CREATE INDEX idx_static_route_route_type ON jb.static_route USING btree (route_type);
CREATE INDEX idx_static_shape_analysis_eligible ON jb.static_shape USING btree (analysis_eligible);
CREATE INDEX idx_static_shape_create_time ON jb.static_shape USING btree (create_time);
CREATE INDEX idx_static_shape_geom_gist ON jb.static_shape USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_is_referenced ON jb.static_shape USING btree (is_referenced);
CREATE INDEX idx_static_shape_qc_flags_gin ON jb.static_shape USING gin (qc_flags);
CREATE INDEX idx_static_shape_shape_id ON jb.static_shape USING btree (shape_id);
CREATE INDEX idx_static_shape_version_referenced ON jb.static_shape USING btree (static_version_uid, is_referenced);
CREATE INDEX idx_static_shape_point_create_time ON jb.static_shape_point USING btree (create_time);
CREATE INDEX idx_static_shape_point_geom_gist ON jb.static_shape_point USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_point_qc_flags_gin ON jb.static_shape_point USING gin (qc_flags);
CREATE INDEX idx_static_shape_point_sequence ON jb.static_shape_point USING btree (shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_id ON jb.static_shape_point USING btree (shape_id);
CREATE INDEX idx_static_shape_point_shape_sequence ON jb.static_shape_point USING btree (shape_uid, shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_uid ON jb.static_shape_point USING btree (shape_uid);
CREATE INDEX idx_static_stop_create_time ON jb.static_stop USING btree (create_time);
CREATE INDEX idx_static_stop_geom_gist ON jb.static_stop USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_stop_location_type ON jb.static_stop USING btree (location_type);
CREATE INDEX idx_static_stop_parent_station ON jb.static_stop USING btree (parent_station);
CREATE INDEX idx_static_stop_qc_flags_gin ON jb.static_stop USING gin (qc_flags);
CREATE INDEX idx_static_stop_stop_code ON jb.static_stop USING btree (stop_code);
CREATE INDEX idx_static_stop_stop_id ON jb.static_stop USING btree (stop_id);
CREATE INDEX idx_static_stop_time_arrival_seconds ON jb.static_stop_time USING btree (arrival_seconds);
CREATE INDEX idx_static_stop_time_create_time ON jb.static_stop_time USING btree (create_time);
CREATE INDEX idx_static_stop_time_departure_seconds ON jb.static_stop_time USING btree (departure_seconds);
CREATE INDEX idx_static_stop_time_stop_id ON jb.static_stop_time USING btree (stop_id);
CREATE INDEX idx_static_stop_time_stop_sequence ON jb.static_stop_time USING btree (stop_sequence);
CREATE INDEX idx_static_stop_time_stop_trip ON jb.static_stop_time USING btree (stop_uid, trip_uid);
CREATE INDEX idx_static_stop_time_stop_uid ON jb.static_stop_time USING btree (stop_uid);
CREATE INDEX idx_static_stop_time_trip_id ON jb.static_stop_time USING btree (trip_id);
CREATE INDEX idx_static_stop_time_trip_sequence ON jb.static_stop_time USING btree (trip_uid, stop_sequence);
CREATE INDEX idx_static_stop_time_trip_uid ON jb.static_stop_time USING btree (trip_uid);
CREATE INDEX idx_static_trip_create_time ON jb.static_trip USING btree (create_time);
CREATE INDEX idx_static_trip_direction_id ON jb.static_trip USING btree (direction_id);
CREATE INDEX idx_static_trip_route_direction ON jb.static_trip USING btree (route_uid, direction_id);
CREATE INDEX idx_static_trip_route_id ON jb.static_trip USING btree (route_id);
CREATE INDEX idx_static_trip_route_uid ON jb.static_trip USING btree (route_uid);
CREATE INDEX idx_static_trip_service_id ON jb.static_trip USING btree (service_id);
CREATE INDEX idx_static_trip_shape_direction ON jb.static_trip USING btree (shape_uid, direction_id);
CREATE INDEX idx_static_trip_shape_id ON jb.static_trip USING btree (shape_id);
CREATE INDEX idx_static_trip_shape_uid ON jb.static_trip USING btree (shape_uid);
CREATE INDEX idx_static_trip_trip_id ON jb.static_trip USING btree (trip_id);
CREATE INDEX idx_static_trip_version_direction ON jb.static_trip USING btree (static_version_uid, direction_id);
CREATE INDEX idx_static_trip_version_route ON jb.static_trip USING btree (static_version_uid, route_id);
CREATE INDEX idx_static_version_create_time ON jb.static_version USING btree (create_time);
CREATE INDEX idx_static_version_downloaded_at ON jb.static_version USING btree (downloaded_at);
CREATE INDEX idx_static_version_effective_from ON jb.static_version USING btree (effective_from);
CREATE INDEX idx_static_version_effective_to ON jb.static_version USING btree (effective_to);
CREATE INDEX idx_static_version_feed_downloaded_at ON jb.static_version USING btree (feed_uid, downloaded_at DESC);
CREATE INDEX idx_static_version_feed_effective_from ON jb.static_version USING btree (feed_uid, effective_from DESC);
CREATE INDEX idx_static_version_is_current ON jb.static_version USING btree (is_current);
CREATE INDEX idx_static_version_source_request_uid ON jb.static_version USING btree (source_request_uid);
CREATE INDEX idx_static_version_static_sha256 ON jb.static_version USING btree (static_sha256);
CREATE UNIQUE INDEX uk_static_version_current_feed ON jb.static_version USING btree (feed_uid) WHERE is_current;
CREATE UNIQUE INDEX uk_static_version_feed_version_code ON jb.static_version USING btree (feed_uid, version_code) WHERE (version_code IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_create_time ON jb.vehicle_latest_state USING btree (create_time);
CREATE INDEX idx_vehicle_latest_state_feed_last_seen_time ON jb.vehicle_latest_state USING btree (feed_uid, last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_feed_route ON jb.vehicle_latest_state USING btree (feed_uid, resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_feed_vehicle_time ON jb.vehicle_latest_state USING btree (feed_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_geom_gist ON jb.vehicle_latest_state USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_last_seen_time ON jb.vehicle_latest_state USING btree (last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_observation_uid ON jb.vehicle_latest_state USING btree (observation_uid);
CREATE INDEX idx_vehicle_latest_state_qc_flags_gin ON jb.vehicle_latest_state USING gin (qc_flags);
CREATE INDEX idx_vehicle_latest_state_resolved_route_id ON jb.vehicle_latest_state USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_route_vehicle_time ON jb.vehicle_latest_state USING btree (resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_trip_id ON jb.vehicle_latest_state USING btree (trip_id);
CREATE INDEX idx_vehicle_latest_state_update_time ON jb.vehicle_latest_state USING btree (update_time);
CREATE INDEX idx_vehicle_latest_state_vehicle_id ON jb.vehicle_latest_state USING btree (vehicle_id);
CREATE INDEX idx_vehicle_latest_state_vehicle_time ON jb.vehicle_latest_state USING btree (vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_analysis_eligible ON jb.vehicle_observation USING btree (analysis_eligible);
CREATE INDEX idx_vehicle_observation_create_time_brin ON jb.vehicle_observation USING brin (create_time);
CREATE INDEX idx_vehicle_observation_duplicate_observation ON jb.vehicle_observation USING btree (duplicate_observation);
CREATE INDEX idx_vehicle_observation_entity_id ON jb.vehicle_observation USING btree (entity_id);
CREATE INDEX idx_vehicle_observation_feed_route_time ON jb.vehicle_observation USING btree (feed_uid, resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_trip_time ON jb.vehicle_observation USING btree (feed_uid, trip_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_uid ON jb.vehicle_observation USING btree (feed_uid);
CREATE INDEX idx_vehicle_observation_feed_vehicle_time ON jb.vehicle_observation USING btree (feed_uid, vehicle_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_geom_gist ON jb.vehicle_observation USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_observation_gps_jump_status ON jb.vehicle_observation USING btree (gps_jump_status);
CREATE INDEX idx_vehicle_observation_ingest_time ON jb.vehicle_observation USING btree (ingest_time);
CREATE INDEX idx_vehicle_observation_observation_key ON jb.vehicle_observation USING btree (observation_key);
CREATE INDEX idx_vehicle_observation_position_qc_status ON jb.vehicle_observation USING btree (position_qc_status);
CREATE INDEX idx_vehicle_observation_previous_vehicle_time ON jb.vehicle_observation USING btree (previous_vehicle_time);
CREATE INDEX idx_vehicle_observation_qc_flags_gin ON jb.vehicle_observation USING gin (qc_flags);
CREATE INDEX idx_vehicle_observation_realtime_route_id ON jb.vehicle_observation USING btree (realtime_route_id);
CREATE INDEX idx_vehicle_observation_request_uid ON jb.vehicle_observation USING btree (request_uid);
CREATE INDEX idx_vehicle_observation_resolved_route_id ON jb.vehicle_observation USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_observation_run_uid ON jb.vehicle_observation USING btree (run_uid);
CREATE INDEX idx_vehicle_observation_run_vehicle_time ON jb.vehicle_observation USING btree (run_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_shape_id ON jb.vehicle_observation USING btree (shape_id);
CREATE INDEX idx_vehicle_observation_spatial_eligible ON jb.vehicle_observation USING btree (spatial_eligible);
CREATE INDEX idx_vehicle_observation_static_route_id ON jb.vehicle_observation USING btree (static_route_id);
CREATE INDEX idx_vehicle_observation_static_route_uid ON jb.vehicle_observation USING btree (static_route_uid);
CREATE INDEX idx_vehicle_observation_static_shape_uid ON jb.vehicle_observation USING btree (static_shape_uid);
CREATE INDEX idx_vehicle_observation_static_stop_uid ON jb.vehicle_observation USING btree (static_stop_uid);
CREATE INDEX idx_vehicle_observation_static_trip_uid ON jb.vehicle_observation USING btree (static_trip_uid);
CREATE INDEX idx_vehicle_observation_static_version_uid ON jb.vehicle_observation USING btree (static_version_uid);
CREATE INDEX idx_vehicle_observation_stop_id ON jb.vehicle_observation USING btree (stop_id);
CREATE INDEX idx_vehicle_observation_trip_id ON jb.vehicle_observation USING btree (trip_id);
CREATE INDEX idx_vehicle_observation_trip_start_date ON jb.vehicle_observation USING btree (trip_start_date);
CREATE INDEX idx_vehicle_observation_vehicle_id ON jb.vehicle_observation USING btree (vehicle_id);
CREATE INDEX idx_vehicle_observation_vehicle_time ON jb.vehicle_observation USING btree (vehicle_time DESC);
CREATE UNIQUE INDEX uk_vehicle_observation_snapshot_entity_id ON jb.vehicle_observation USING btree (snapshot_uid, entity_id) WHERE (entity_id IS NOT NULL);
CREATE INDEX idx_vehicle_observation_qc_category_create_time ON jb.vehicle_observation_qc USING btree (qc_category, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_code_create_time ON jb.vehicle_observation_qc USING btree (qc_code, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_create_time ON jb.vehicle_observation_qc USING btree (create_time);
CREATE INDEX idx_vehicle_observation_qc_severity_create_time ON jb.vehicle_observation_qc USING btree (severity, create_time DESC);
CREATE INDEX idx_api_request_log_create_time_brin ON kuching.api_request_log USING brin (create_time);
CREATE INDEX idx_api_request_log_duplicate_request_uid ON kuching.api_request_log USING btree (duplicate_of_request_uid);
CREATE INDEX idx_api_request_log_feed_http_request_time ON kuching.api_request_log USING btree (feed_uid, http_status, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_request_time ON kuching.api_request_log USING btree (feed_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_uid ON kuching.api_request_log USING btree (feed_uid);
CREATE INDEX idx_api_request_log_http_status ON kuching.api_request_log USING btree (http_status);
CREATE INDEX idx_api_request_log_request_started_at ON kuching.api_request_log USING btree (request_started_at);
CREATE INDEX idx_api_request_log_request_type ON kuching.api_request_log USING btree (request_type);
CREATE INDEX idx_api_request_log_response_received_at ON kuching.api_request_log USING btree (response_received_at);
CREATE INDEX idx_api_request_log_response_sha256 ON kuching.api_request_log USING btree (response_sha256);
CREATE INDEX idx_api_request_log_result ON kuching.api_request_log USING btree (result);
CREATE INDEX idx_api_request_log_run_request_time ON kuching.api_request_log USING btree (run_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_run_uid ON kuching.api_request_log USING btree (run_uid);
CREATE INDEX idx_api_request_log_scheduled_at ON kuching.api_request_log USING btree (scheduled_at);
CREATE INDEX idx_realtime_snapshot_create_time_brin ON kuching.realtime_snapshot USING brin (create_time);
CREATE INDEX idx_realtime_snapshot_duplicate_snapshot ON kuching.realtime_snapshot USING btree (duplicate_snapshot);
CREATE INDEX idx_realtime_snapshot_enrichment_static_version_uid ON kuching.realtime_snapshot USING btree (enrichment_static_version_uid);
CREATE INDEX idx_realtime_snapshot_feed_feed_time ON kuching.realtime_snapshot USING btree (feed_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_feed_time ON kuching.realtime_snapshot USING btree (feed_time);
CREATE INDEX idx_realtime_snapshot_feed_uid ON kuching.realtime_snapshot USING btree (feed_uid);
CREATE INDEX idx_realtime_snapshot_referenced_snapshot_uid ON kuching.realtime_snapshot USING btree (referenced_snapshot_uid);
CREATE INDEX idx_realtime_snapshot_response_sha256 ON kuching.realtime_snapshot USING btree (response_sha256);
CREATE INDEX idx_realtime_snapshot_run_feed_time ON kuching.realtime_snapshot USING btree (run_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_run_uid ON kuching.realtime_snapshot USING btree (run_uid);
CREATE INDEX idx_static_route_create_time ON kuching.static_route USING btree (create_time);
CREATE INDEX idx_static_route_route_id ON kuching.static_route USING btree (route_id);
CREATE INDEX idx_static_route_route_short_name ON kuching.static_route USING btree (route_short_name);
CREATE INDEX idx_static_route_route_type ON kuching.static_route USING btree (route_type);
CREATE INDEX idx_static_shape_analysis_eligible ON kuching.static_shape USING btree (analysis_eligible);
CREATE INDEX idx_static_shape_create_time ON kuching.static_shape USING btree (create_time);
CREATE INDEX idx_static_shape_geom_gist ON kuching.static_shape USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_is_referenced ON kuching.static_shape USING btree (is_referenced);
CREATE INDEX idx_static_shape_qc_flags_gin ON kuching.static_shape USING gin (qc_flags);
CREATE INDEX idx_static_shape_shape_id ON kuching.static_shape USING btree (shape_id);
CREATE INDEX idx_static_shape_version_referenced ON kuching.static_shape USING btree (static_version_uid, is_referenced);
CREATE INDEX idx_static_shape_point_create_time ON kuching.static_shape_point USING btree (create_time);
CREATE INDEX idx_static_shape_point_geom_gist ON kuching.static_shape_point USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_point_qc_flags_gin ON kuching.static_shape_point USING gin (qc_flags);
CREATE INDEX idx_static_shape_point_sequence ON kuching.static_shape_point USING btree (shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_id ON kuching.static_shape_point USING btree (shape_id);
CREATE INDEX idx_static_shape_point_shape_sequence ON kuching.static_shape_point USING btree (shape_uid, shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_uid ON kuching.static_shape_point USING btree (shape_uid);
CREATE INDEX idx_static_stop_create_time ON kuching.static_stop USING btree (create_time);
CREATE INDEX idx_static_stop_geom_gist ON kuching.static_stop USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_stop_location_type ON kuching.static_stop USING btree (location_type);
CREATE INDEX idx_static_stop_parent_station ON kuching.static_stop USING btree (parent_station);
CREATE INDEX idx_static_stop_qc_flags_gin ON kuching.static_stop USING gin (qc_flags);
CREATE INDEX idx_static_stop_stop_code ON kuching.static_stop USING btree (stop_code);
CREATE INDEX idx_static_stop_stop_id ON kuching.static_stop USING btree (stop_id);
CREATE INDEX idx_static_stop_time_arrival_seconds ON kuching.static_stop_time USING btree (arrival_seconds);
CREATE INDEX idx_static_stop_time_create_time ON kuching.static_stop_time USING btree (create_time);
CREATE INDEX idx_static_stop_time_departure_seconds ON kuching.static_stop_time USING btree (departure_seconds);
CREATE INDEX idx_static_stop_time_stop_id ON kuching.static_stop_time USING btree (stop_id);
CREATE INDEX idx_static_stop_time_stop_sequence ON kuching.static_stop_time USING btree (stop_sequence);
CREATE INDEX idx_static_stop_time_stop_trip ON kuching.static_stop_time USING btree (stop_uid, trip_uid);
CREATE INDEX idx_static_stop_time_stop_uid ON kuching.static_stop_time USING btree (stop_uid);
CREATE INDEX idx_static_stop_time_trip_id ON kuching.static_stop_time USING btree (trip_id);
CREATE INDEX idx_static_stop_time_trip_sequence ON kuching.static_stop_time USING btree (trip_uid, stop_sequence);
CREATE INDEX idx_static_stop_time_trip_uid ON kuching.static_stop_time USING btree (trip_uid);
CREATE INDEX idx_static_trip_create_time ON kuching.static_trip USING btree (create_time);
CREATE INDEX idx_static_trip_direction_id ON kuching.static_trip USING btree (direction_id);
CREATE INDEX idx_static_trip_route_direction ON kuching.static_trip USING btree (route_uid, direction_id);
CREATE INDEX idx_static_trip_route_id ON kuching.static_trip USING btree (route_id);
CREATE INDEX idx_static_trip_route_uid ON kuching.static_trip USING btree (route_uid);
CREATE INDEX idx_static_trip_service_id ON kuching.static_trip USING btree (service_id);
CREATE INDEX idx_static_trip_shape_direction ON kuching.static_trip USING btree (shape_uid, direction_id);
CREATE INDEX idx_static_trip_shape_id ON kuching.static_trip USING btree (shape_id);
CREATE INDEX idx_static_trip_shape_uid ON kuching.static_trip USING btree (shape_uid);
CREATE INDEX idx_static_trip_trip_id ON kuching.static_trip USING btree (trip_id);
CREATE INDEX idx_static_trip_version_direction ON kuching.static_trip USING btree (static_version_uid, direction_id);
CREATE INDEX idx_static_trip_version_route ON kuching.static_trip USING btree (static_version_uid, route_id);
CREATE INDEX idx_static_version_create_time ON kuching.static_version USING btree (create_time);
CREATE INDEX idx_static_version_downloaded_at ON kuching.static_version USING btree (downloaded_at);
CREATE INDEX idx_static_version_effective_from ON kuching.static_version USING btree (effective_from);
CREATE INDEX idx_static_version_effective_to ON kuching.static_version USING btree (effective_to);
CREATE INDEX idx_static_version_feed_downloaded_at ON kuching.static_version USING btree (feed_uid, downloaded_at DESC);
CREATE INDEX idx_static_version_feed_effective_from ON kuching.static_version USING btree (feed_uid, effective_from DESC);
CREATE INDEX idx_static_version_is_current ON kuching.static_version USING btree (is_current);
CREATE INDEX idx_static_version_source_request_uid ON kuching.static_version USING btree (source_request_uid);
CREATE INDEX idx_static_version_static_sha256 ON kuching.static_version USING btree (static_sha256);
CREATE UNIQUE INDEX uk_static_version_current_feed ON kuching.static_version USING btree (feed_uid) WHERE is_current;
CREATE UNIQUE INDEX uk_static_version_feed_version_code ON kuching.static_version USING btree (feed_uid, version_code) WHERE (version_code IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_create_time ON kuching.vehicle_latest_state USING btree (create_time);
CREATE INDEX idx_vehicle_latest_state_feed_last_seen_time ON kuching.vehicle_latest_state USING btree (feed_uid, last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_feed_route ON kuching.vehicle_latest_state USING btree (feed_uid, resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_feed_vehicle_time ON kuching.vehicle_latest_state USING btree (feed_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_geom_gist ON kuching.vehicle_latest_state USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_last_seen_time ON kuching.vehicle_latest_state USING btree (last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_observation_uid ON kuching.vehicle_latest_state USING btree (observation_uid);
CREATE INDEX idx_vehicle_latest_state_qc_flags_gin ON kuching.vehicle_latest_state USING gin (qc_flags);
CREATE INDEX idx_vehicle_latest_state_resolved_route_id ON kuching.vehicle_latest_state USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_route_vehicle_time ON kuching.vehicle_latest_state USING btree (resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_trip_id ON kuching.vehicle_latest_state USING btree (trip_id);
CREATE INDEX idx_vehicle_latest_state_update_time ON kuching.vehicle_latest_state USING btree (update_time);
CREATE INDEX idx_vehicle_latest_state_vehicle_id ON kuching.vehicle_latest_state USING btree (vehicle_id);
CREATE INDEX idx_vehicle_latest_state_vehicle_time ON kuching.vehicle_latest_state USING btree (vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_analysis_eligible ON kuching.vehicle_observation USING btree (analysis_eligible);
CREATE INDEX idx_vehicle_observation_create_time_brin ON kuching.vehicle_observation USING brin (create_time);
CREATE INDEX idx_vehicle_observation_duplicate_observation ON kuching.vehicle_observation USING btree (duplicate_observation);
CREATE INDEX idx_vehicle_observation_entity_id ON kuching.vehicle_observation USING btree (entity_id);
CREATE INDEX idx_vehicle_observation_feed_route_time ON kuching.vehicle_observation USING btree (feed_uid, resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_trip_time ON kuching.vehicle_observation USING btree (feed_uid, trip_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_uid ON kuching.vehicle_observation USING btree (feed_uid);
CREATE INDEX idx_vehicle_observation_feed_vehicle_time ON kuching.vehicle_observation USING btree (feed_uid, vehicle_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_geom_gist ON kuching.vehicle_observation USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_observation_gps_jump_status ON kuching.vehicle_observation USING btree (gps_jump_status);
CREATE INDEX idx_vehicle_observation_ingest_time ON kuching.vehicle_observation USING btree (ingest_time);
CREATE INDEX idx_vehicle_observation_observation_key ON kuching.vehicle_observation USING btree (observation_key);
CREATE INDEX idx_vehicle_observation_position_qc_status ON kuching.vehicle_observation USING btree (position_qc_status);
CREATE INDEX idx_vehicle_observation_previous_vehicle_time ON kuching.vehicle_observation USING btree (previous_vehicle_time);
CREATE INDEX idx_vehicle_observation_qc_flags_gin ON kuching.vehicle_observation USING gin (qc_flags);
CREATE INDEX idx_vehicle_observation_realtime_route_id ON kuching.vehicle_observation USING btree (realtime_route_id);
CREATE INDEX idx_vehicle_observation_request_uid ON kuching.vehicle_observation USING btree (request_uid);
CREATE INDEX idx_vehicle_observation_resolved_route_id ON kuching.vehicle_observation USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_observation_run_uid ON kuching.vehicle_observation USING btree (run_uid);
CREATE INDEX idx_vehicle_observation_run_vehicle_time ON kuching.vehicle_observation USING btree (run_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_shape_id ON kuching.vehicle_observation USING btree (shape_id);
CREATE INDEX idx_vehicle_observation_spatial_eligible ON kuching.vehicle_observation USING btree (spatial_eligible);
CREATE INDEX idx_vehicle_observation_static_route_id ON kuching.vehicle_observation USING btree (static_route_id);
CREATE INDEX idx_vehicle_observation_static_route_uid ON kuching.vehicle_observation USING btree (static_route_uid);
CREATE INDEX idx_vehicle_observation_static_shape_uid ON kuching.vehicle_observation USING btree (static_shape_uid);
CREATE INDEX idx_vehicle_observation_static_stop_uid ON kuching.vehicle_observation USING btree (static_stop_uid);
CREATE INDEX idx_vehicle_observation_static_trip_uid ON kuching.vehicle_observation USING btree (static_trip_uid);
CREATE INDEX idx_vehicle_observation_static_version_uid ON kuching.vehicle_observation USING btree (static_version_uid);
CREATE INDEX idx_vehicle_observation_stop_id ON kuching.vehicle_observation USING btree (stop_id);
CREATE INDEX idx_vehicle_observation_trip_id ON kuching.vehicle_observation USING btree (trip_id);
CREATE INDEX idx_vehicle_observation_trip_start_date ON kuching.vehicle_observation USING btree (trip_start_date);
CREATE INDEX idx_vehicle_observation_vehicle_id ON kuching.vehicle_observation USING btree (vehicle_id);
CREATE INDEX idx_vehicle_observation_vehicle_time ON kuching.vehicle_observation USING btree (vehicle_time DESC);
CREATE UNIQUE INDEX uk_vehicle_observation_snapshot_entity_id ON kuching.vehicle_observation USING btree (snapshot_uid, entity_id) WHERE (entity_id IS NOT NULL);
CREATE INDEX idx_vehicle_observation_qc_category_create_time ON kuching.vehicle_observation_qc USING btree (qc_category, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_code_create_time ON kuching.vehicle_observation_qc USING btree (qc_code, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_create_time ON kuching.vehicle_observation_qc USING btree (create_time);
CREATE INDEX idx_vehicle_observation_qc_severity_create_time ON kuching.vehicle_observation_qc USING btree (severity, create_time DESC);
CREATE INDEX idx_api_request_log_create_time_brin ON kl.api_request_log USING brin (create_time);
CREATE INDEX idx_api_request_log_duplicate_request_uid ON kl.api_request_log USING btree (duplicate_of_request_uid);
CREATE INDEX idx_api_request_log_feed_http_request_time ON kl.api_request_log USING btree (feed_uid, http_status, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_request_time ON kl.api_request_log USING btree (feed_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_uid ON kl.api_request_log USING btree (feed_uid);
CREATE INDEX idx_api_request_log_http_status ON kl.api_request_log USING btree (http_status);
CREATE INDEX idx_api_request_log_request_started_at ON kl.api_request_log USING btree (request_started_at);
CREATE INDEX idx_api_request_log_request_type ON kl.api_request_log USING btree (request_type);
CREATE INDEX idx_api_request_log_response_received_at ON kl.api_request_log USING btree (response_received_at);
CREATE INDEX idx_api_request_log_response_sha256 ON kl.api_request_log USING btree (response_sha256);
CREATE INDEX idx_api_request_log_result ON kl.api_request_log USING btree (result);
CREATE INDEX idx_api_request_log_run_request_time ON kl.api_request_log USING btree (run_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_run_uid ON kl.api_request_log USING btree (run_uid);
CREATE INDEX idx_api_request_log_scheduled_at ON kl.api_request_log USING btree (scheduled_at);
CREATE INDEX idx_realtime_snapshot_create_time_brin ON kl.realtime_snapshot USING brin (create_time);
CREATE INDEX idx_realtime_snapshot_duplicate_snapshot ON kl.realtime_snapshot USING btree (duplicate_snapshot);
CREATE INDEX idx_realtime_snapshot_enrichment_static_version_uid ON kl.realtime_snapshot USING btree (enrichment_static_version_uid);
CREATE INDEX idx_realtime_snapshot_feed_feed_time ON kl.realtime_snapshot USING btree (feed_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_feed_time ON kl.realtime_snapshot USING btree (feed_time);
CREATE INDEX idx_realtime_snapshot_feed_uid ON kl.realtime_snapshot USING btree (feed_uid);
CREATE INDEX idx_realtime_snapshot_referenced_snapshot_uid ON kl.realtime_snapshot USING btree (referenced_snapshot_uid);
CREATE INDEX idx_realtime_snapshot_response_sha256 ON kl.realtime_snapshot USING btree (response_sha256);
CREATE INDEX idx_realtime_snapshot_run_feed_time ON kl.realtime_snapshot USING btree (run_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_run_uid ON kl.realtime_snapshot USING btree (run_uid);
CREATE INDEX idx_static_route_create_time ON kl.static_route USING btree (create_time);
CREATE INDEX idx_static_route_route_id ON kl.static_route USING btree (route_id);
CREATE INDEX idx_static_route_route_short_name ON kl.static_route USING btree (route_short_name);
CREATE INDEX idx_static_route_route_type ON kl.static_route USING btree (route_type);
CREATE INDEX idx_static_shape_analysis_eligible ON kl.static_shape USING btree (analysis_eligible);
CREATE INDEX idx_static_shape_create_time ON kl.static_shape USING btree (create_time);
CREATE INDEX idx_static_shape_geom_gist ON kl.static_shape USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_is_referenced ON kl.static_shape USING btree (is_referenced);
CREATE INDEX idx_static_shape_qc_flags_gin ON kl.static_shape USING gin (qc_flags);
CREATE INDEX idx_static_shape_shape_id ON kl.static_shape USING btree (shape_id);
CREATE INDEX idx_static_shape_version_referenced ON kl.static_shape USING btree (static_version_uid, is_referenced);
CREATE INDEX idx_static_shape_point_create_time ON kl.static_shape_point USING btree (create_time);
CREATE INDEX idx_static_shape_point_geom_gist ON kl.static_shape_point USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_point_qc_flags_gin ON kl.static_shape_point USING gin (qc_flags);
CREATE INDEX idx_static_shape_point_sequence ON kl.static_shape_point USING btree (shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_id ON kl.static_shape_point USING btree (shape_id);
CREATE INDEX idx_static_shape_point_shape_sequence ON kl.static_shape_point USING btree (shape_uid, shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_uid ON kl.static_shape_point USING btree (shape_uid);
CREATE INDEX idx_static_stop_create_time ON kl.static_stop USING btree (create_time);
CREATE INDEX idx_static_stop_geom_gist ON kl.static_stop USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_stop_location_type ON kl.static_stop USING btree (location_type);
CREATE INDEX idx_static_stop_parent_station ON kl.static_stop USING btree (parent_station);
CREATE INDEX idx_static_stop_qc_flags_gin ON kl.static_stop USING gin (qc_flags);
CREATE INDEX idx_static_stop_stop_code ON kl.static_stop USING btree (stop_code);
CREATE INDEX idx_static_stop_stop_id ON kl.static_stop USING btree (stop_id);
CREATE INDEX idx_static_stop_time_arrival_seconds ON kl.static_stop_time USING btree (arrival_seconds);
CREATE INDEX idx_static_stop_time_create_time ON kl.static_stop_time USING btree (create_time);
CREATE INDEX idx_static_stop_time_departure_seconds ON kl.static_stop_time USING btree (departure_seconds);
CREATE INDEX idx_static_stop_time_stop_id ON kl.static_stop_time USING btree (stop_id);
CREATE INDEX idx_static_stop_time_stop_sequence ON kl.static_stop_time USING btree (stop_sequence);
CREATE INDEX idx_static_stop_time_stop_trip ON kl.static_stop_time USING btree (stop_uid, trip_uid);
CREATE INDEX idx_static_stop_time_stop_uid ON kl.static_stop_time USING btree (stop_uid);
CREATE INDEX idx_static_stop_time_trip_id ON kl.static_stop_time USING btree (trip_id);
CREATE INDEX idx_static_stop_time_trip_sequence ON kl.static_stop_time USING btree (trip_uid, stop_sequence);
CREATE INDEX idx_static_stop_time_trip_uid ON kl.static_stop_time USING btree (trip_uid);
CREATE INDEX idx_static_trip_create_time ON kl.static_trip USING btree (create_time);
CREATE INDEX idx_static_trip_direction_id ON kl.static_trip USING btree (direction_id);
CREATE INDEX idx_static_trip_route_direction ON kl.static_trip USING btree (route_uid, direction_id);
CREATE INDEX idx_static_trip_route_id ON kl.static_trip USING btree (route_id);
CREATE INDEX idx_static_trip_route_uid ON kl.static_trip USING btree (route_uid);
CREATE INDEX idx_static_trip_service_id ON kl.static_trip USING btree (service_id);
CREATE INDEX idx_static_trip_shape_direction ON kl.static_trip USING btree (shape_uid, direction_id);
CREATE INDEX idx_static_trip_shape_id ON kl.static_trip USING btree (shape_id);
CREATE INDEX idx_static_trip_shape_uid ON kl.static_trip USING btree (shape_uid);
CREATE INDEX idx_static_trip_trip_id ON kl.static_trip USING btree (trip_id);
CREATE INDEX idx_static_trip_version_direction ON kl.static_trip USING btree (static_version_uid, direction_id);
CREATE INDEX idx_static_trip_version_route ON kl.static_trip USING btree (static_version_uid, route_id);
CREATE INDEX idx_static_version_create_time ON kl.static_version USING btree (create_time);
CREATE INDEX idx_static_version_downloaded_at ON kl.static_version USING btree (downloaded_at);
CREATE INDEX idx_static_version_effective_from ON kl.static_version USING btree (effective_from);
CREATE INDEX idx_static_version_effective_to ON kl.static_version USING btree (effective_to);
CREATE INDEX idx_static_version_feed_downloaded_at ON kl.static_version USING btree (feed_uid, downloaded_at DESC);
CREATE INDEX idx_static_version_feed_effective_from ON kl.static_version USING btree (feed_uid, effective_from DESC);
CREATE INDEX idx_static_version_is_current ON kl.static_version USING btree (is_current);
CREATE INDEX idx_static_version_source_request_uid ON kl.static_version USING btree (source_request_uid);
CREATE INDEX idx_static_version_static_sha256 ON kl.static_version USING btree (static_sha256);
CREATE UNIQUE INDEX uk_static_version_current_feed ON kl.static_version USING btree (feed_uid) WHERE is_current;
CREATE UNIQUE INDEX uk_static_version_feed_version_code ON kl.static_version USING btree (feed_uid, version_code) WHERE (version_code IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_create_time ON kl.vehicle_latest_state USING btree (create_time);
CREATE INDEX idx_vehicle_latest_state_feed_last_seen_time ON kl.vehicle_latest_state USING btree (feed_uid, last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_feed_route ON kl.vehicle_latest_state USING btree (feed_uid, resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_feed_vehicle_time ON kl.vehicle_latest_state USING btree (feed_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_geom_gist ON kl.vehicle_latest_state USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_last_seen_time ON kl.vehicle_latest_state USING btree (last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_observation_uid ON kl.vehicle_latest_state USING btree (observation_uid);
CREATE INDEX idx_vehicle_latest_state_qc_flags_gin ON kl.vehicle_latest_state USING gin (qc_flags);
CREATE INDEX idx_vehicle_latest_state_resolved_route_id ON kl.vehicle_latest_state USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_route_vehicle_time ON kl.vehicle_latest_state USING btree (resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_trip_id ON kl.vehicle_latest_state USING btree (trip_id);
CREATE INDEX idx_vehicle_latest_state_update_time ON kl.vehicle_latest_state USING btree (update_time);
CREATE INDEX idx_vehicle_latest_state_vehicle_id ON kl.vehicle_latest_state USING btree (vehicle_id);
CREATE INDEX idx_vehicle_latest_state_vehicle_time ON kl.vehicle_latest_state USING btree (vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_analysis_eligible ON kl.vehicle_observation USING btree (analysis_eligible);
CREATE INDEX idx_vehicle_observation_create_time_brin ON kl.vehicle_observation USING brin (create_time);
CREATE INDEX idx_vehicle_observation_duplicate_observation ON kl.vehicle_observation USING btree (duplicate_observation);
CREATE INDEX idx_vehicle_observation_entity_id ON kl.vehicle_observation USING btree (entity_id);
CREATE INDEX idx_vehicle_observation_feed_route_time ON kl.vehicle_observation USING btree (feed_uid, resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_trip_time ON kl.vehicle_observation USING btree (feed_uid, trip_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_uid ON kl.vehicle_observation USING btree (feed_uid);
CREATE INDEX idx_vehicle_observation_feed_vehicle_time ON kl.vehicle_observation USING btree (feed_uid, vehicle_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_geom_gist ON kl.vehicle_observation USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_observation_gps_jump_status ON kl.vehicle_observation USING btree (gps_jump_status);
CREATE INDEX idx_vehicle_observation_ingest_time ON kl.vehicle_observation USING btree (ingest_time);
CREATE INDEX idx_vehicle_observation_observation_key ON kl.vehicle_observation USING btree (observation_key);
CREATE INDEX idx_vehicle_observation_position_qc_status ON kl.vehicle_observation USING btree (position_qc_status);
CREATE INDEX idx_vehicle_observation_previous_vehicle_time ON kl.vehicle_observation USING btree (previous_vehicle_time);
CREATE INDEX idx_vehicle_observation_qc_flags_gin ON kl.vehicle_observation USING gin (qc_flags);
CREATE INDEX idx_vehicle_observation_realtime_route_id ON kl.vehicle_observation USING btree (realtime_route_id);
CREATE INDEX idx_vehicle_observation_request_uid ON kl.vehicle_observation USING btree (request_uid);
CREATE INDEX idx_vehicle_observation_resolved_route_id ON kl.vehicle_observation USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_observation_run_uid ON kl.vehicle_observation USING btree (run_uid);
CREATE INDEX idx_vehicle_observation_run_vehicle_time ON kl.vehicle_observation USING btree (run_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_shape_id ON kl.vehicle_observation USING btree (shape_id);
CREATE INDEX idx_vehicle_observation_spatial_eligible ON kl.vehicle_observation USING btree (spatial_eligible);
CREATE INDEX idx_vehicle_observation_static_route_id ON kl.vehicle_observation USING btree (static_route_id);
CREATE INDEX idx_vehicle_observation_static_route_uid ON kl.vehicle_observation USING btree (static_route_uid);
CREATE INDEX idx_vehicle_observation_static_shape_uid ON kl.vehicle_observation USING btree (static_shape_uid);
CREATE INDEX idx_vehicle_observation_static_stop_uid ON kl.vehicle_observation USING btree (static_stop_uid);
CREATE INDEX idx_vehicle_observation_static_trip_uid ON kl.vehicle_observation USING btree (static_trip_uid);
CREATE INDEX idx_vehicle_observation_static_version_uid ON kl.vehicle_observation USING btree (static_version_uid);
CREATE INDEX idx_vehicle_observation_stop_id ON kl.vehicle_observation USING btree (stop_id);
CREATE INDEX idx_vehicle_observation_trip_id ON kl.vehicle_observation USING btree (trip_id);
CREATE INDEX idx_vehicle_observation_trip_start_date ON kl.vehicle_observation USING btree (trip_start_date);
CREATE INDEX idx_vehicle_observation_vehicle_id ON kl.vehicle_observation USING btree (vehicle_id);
CREATE INDEX idx_vehicle_observation_vehicle_time ON kl.vehicle_observation USING btree (vehicle_time DESC);
CREATE UNIQUE INDEX uk_vehicle_observation_snapshot_entity_id ON kl.vehicle_observation USING btree (snapshot_uid, entity_id) WHERE (entity_id IS NOT NULL);
CREATE INDEX idx_vehicle_observation_qc_category_create_time ON kl.vehicle_observation_qc USING btree (qc_category, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_code_create_time ON kl.vehicle_observation_qc USING btree (qc_code, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_create_time ON kl.vehicle_observation_qc USING btree (create_time);
CREATE INDEX idx_vehicle_observation_qc_severity_create_time ON kl.vehicle_observation_qc USING btree (severity, create_time DESC);
CREATE INDEX idx_api_request_log_create_time_brin ON melaka.api_request_log USING brin (create_time);
CREATE INDEX idx_api_request_log_duplicate_request_uid ON melaka.api_request_log USING btree (duplicate_of_request_uid);
CREATE INDEX idx_api_request_log_feed_http_request_time ON melaka.api_request_log USING btree (feed_uid, http_status, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_request_time ON melaka.api_request_log USING btree (feed_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_feed_uid ON melaka.api_request_log USING btree (feed_uid);
CREATE INDEX idx_api_request_log_http_status ON melaka.api_request_log USING btree (http_status);
CREATE INDEX idx_api_request_log_request_started_at ON melaka.api_request_log USING btree (request_started_at);
CREATE INDEX idx_api_request_log_request_type ON melaka.api_request_log USING btree (request_type);
CREATE INDEX idx_api_request_log_response_received_at ON melaka.api_request_log USING btree (response_received_at);
CREATE INDEX idx_api_request_log_response_sha256 ON melaka.api_request_log USING btree (response_sha256);
CREATE INDEX idx_api_request_log_result ON melaka.api_request_log USING btree (result);
CREATE INDEX idx_api_request_log_run_request_time ON melaka.api_request_log USING btree (run_uid, request_started_at DESC);
CREATE INDEX idx_api_request_log_run_uid ON melaka.api_request_log USING btree (run_uid);
CREATE INDEX idx_api_request_log_scheduled_at ON melaka.api_request_log USING btree (scheduled_at);
CREATE INDEX idx_realtime_snapshot_create_time_brin ON melaka.realtime_snapshot USING brin (create_time);
CREATE INDEX idx_realtime_snapshot_duplicate_snapshot ON melaka.realtime_snapshot USING btree (duplicate_snapshot);
CREATE INDEX idx_realtime_snapshot_enrichment_static_version_uid ON melaka.realtime_snapshot USING btree (enrichment_static_version_uid);
CREATE INDEX idx_realtime_snapshot_feed_feed_time ON melaka.realtime_snapshot USING btree (feed_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_feed_time ON melaka.realtime_snapshot USING btree (feed_time);
CREATE INDEX idx_realtime_snapshot_feed_uid ON melaka.realtime_snapshot USING btree (feed_uid);
CREATE INDEX idx_realtime_snapshot_referenced_snapshot_uid ON melaka.realtime_snapshot USING btree (referenced_snapshot_uid);
CREATE INDEX idx_realtime_snapshot_response_sha256 ON melaka.realtime_snapshot USING btree (response_sha256);
CREATE INDEX idx_realtime_snapshot_run_feed_time ON melaka.realtime_snapshot USING btree (run_uid, feed_time DESC);
CREATE INDEX idx_realtime_snapshot_run_uid ON melaka.realtime_snapshot USING btree (run_uid);
CREATE INDEX idx_static_route_create_time ON melaka.static_route USING btree (create_time);
CREATE INDEX idx_static_route_route_id ON melaka.static_route USING btree (route_id);
CREATE INDEX idx_static_route_route_short_name ON melaka.static_route USING btree (route_short_name);
CREATE INDEX idx_static_route_route_type ON melaka.static_route USING btree (route_type);
CREATE INDEX idx_static_shape_analysis_eligible ON melaka.static_shape USING btree (analysis_eligible);
CREATE INDEX idx_static_shape_create_time ON melaka.static_shape USING btree (create_time);
CREATE INDEX idx_static_shape_geom_gist ON melaka.static_shape USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_is_referenced ON melaka.static_shape USING btree (is_referenced);
CREATE INDEX idx_static_shape_qc_flags_gin ON melaka.static_shape USING gin (qc_flags);
CREATE INDEX idx_static_shape_shape_id ON melaka.static_shape USING btree (shape_id);
CREATE INDEX idx_static_shape_version_referenced ON melaka.static_shape USING btree (static_version_uid, is_referenced);
CREATE INDEX idx_static_shape_point_create_time ON melaka.static_shape_point USING btree (create_time);
CREATE INDEX idx_static_shape_point_geom_gist ON melaka.static_shape_point USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_shape_point_qc_flags_gin ON melaka.static_shape_point USING gin (qc_flags);
CREATE INDEX idx_static_shape_point_sequence ON melaka.static_shape_point USING btree (shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_id ON melaka.static_shape_point USING btree (shape_id);
CREATE INDEX idx_static_shape_point_shape_sequence ON melaka.static_shape_point USING btree (shape_uid, shape_pt_sequence);
CREATE INDEX idx_static_shape_point_shape_uid ON melaka.static_shape_point USING btree (shape_uid);
CREATE INDEX idx_static_stop_create_time ON melaka.static_stop USING btree (create_time);
CREATE INDEX idx_static_stop_geom_gist ON melaka.static_stop USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_static_stop_location_type ON melaka.static_stop USING btree (location_type);
CREATE INDEX idx_static_stop_parent_station ON melaka.static_stop USING btree (parent_station);
CREATE INDEX idx_static_stop_qc_flags_gin ON melaka.static_stop USING gin (qc_flags);
CREATE INDEX idx_static_stop_stop_code ON melaka.static_stop USING btree (stop_code);
CREATE INDEX idx_static_stop_stop_id ON melaka.static_stop USING btree (stop_id);
CREATE INDEX idx_static_stop_time_arrival_seconds ON melaka.static_stop_time USING btree (arrival_seconds);
CREATE INDEX idx_static_stop_time_create_time ON melaka.static_stop_time USING btree (create_time);
CREATE INDEX idx_static_stop_time_departure_seconds ON melaka.static_stop_time USING btree (departure_seconds);
CREATE INDEX idx_static_stop_time_stop_id ON melaka.static_stop_time USING btree (stop_id);
CREATE INDEX idx_static_stop_time_stop_sequence ON melaka.static_stop_time USING btree (stop_sequence);
CREATE INDEX idx_static_stop_time_stop_trip ON melaka.static_stop_time USING btree (stop_uid, trip_uid);
CREATE INDEX idx_static_stop_time_stop_uid ON melaka.static_stop_time USING btree (stop_uid);
CREATE INDEX idx_static_stop_time_trip_id ON melaka.static_stop_time USING btree (trip_id);
CREATE INDEX idx_static_stop_time_trip_sequence ON melaka.static_stop_time USING btree (trip_uid, stop_sequence);
CREATE INDEX idx_static_stop_time_trip_uid ON melaka.static_stop_time USING btree (trip_uid);
CREATE INDEX idx_static_trip_create_time ON melaka.static_trip USING btree (create_time);
CREATE INDEX idx_static_trip_direction_id ON melaka.static_trip USING btree (direction_id);
CREATE INDEX idx_static_trip_route_direction ON melaka.static_trip USING btree (route_uid, direction_id);
CREATE INDEX idx_static_trip_route_id ON melaka.static_trip USING btree (route_id);
CREATE INDEX idx_static_trip_route_uid ON melaka.static_trip USING btree (route_uid);
CREATE INDEX idx_static_trip_service_id ON melaka.static_trip USING btree (service_id);
CREATE INDEX idx_static_trip_shape_direction ON melaka.static_trip USING btree (shape_uid, direction_id);
CREATE INDEX idx_static_trip_shape_id ON melaka.static_trip USING btree (shape_id);
CREATE INDEX idx_static_trip_shape_uid ON melaka.static_trip USING btree (shape_uid);
CREATE INDEX idx_static_trip_trip_id ON melaka.static_trip USING btree (trip_id);
CREATE INDEX idx_static_trip_version_direction ON melaka.static_trip USING btree (static_version_uid, direction_id);
CREATE INDEX idx_static_trip_version_route ON melaka.static_trip USING btree (static_version_uid, route_id);
CREATE INDEX idx_static_version_create_time ON melaka.static_version USING btree (create_time);
CREATE INDEX idx_static_version_downloaded_at ON melaka.static_version USING btree (downloaded_at);
CREATE INDEX idx_static_version_effective_from ON melaka.static_version USING btree (effective_from);
CREATE INDEX idx_static_version_effective_to ON melaka.static_version USING btree (effective_to);
CREATE INDEX idx_static_version_feed_downloaded_at ON melaka.static_version USING btree (feed_uid, downloaded_at DESC);
CREATE INDEX idx_static_version_feed_effective_from ON melaka.static_version USING btree (feed_uid, effective_from DESC);
CREATE INDEX idx_static_version_is_current ON melaka.static_version USING btree (is_current);
CREATE INDEX idx_static_version_source_request_uid ON melaka.static_version USING btree (source_request_uid);
CREATE INDEX idx_static_version_static_sha256 ON melaka.static_version USING btree (static_sha256);
CREATE UNIQUE INDEX uk_static_version_current_feed ON melaka.static_version USING btree (feed_uid) WHERE is_current;
CREATE UNIQUE INDEX uk_static_version_feed_version_code ON melaka.static_version USING btree (feed_uid, version_code) WHERE (version_code IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_create_time ON melaka.vehicle_latest_state USING btree (create_time);
CREATE INDEX idx_vehicle_latest_state_feed_last_seen_time ON melaka.vehicle_latest_state USING btree (feed_uid, last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_feed_route ON melaka.vehicle_latest_state USING btree (feed_uid, resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_feed_vehicle_time ON melaka.vehicle_latest_state USING btree (feed_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_geom_gist ON melaka.vehicle_latest_state USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_latest_state_last_seen_time ON melaka.vehicle_latest_state USING btree (last_seen_time DESC);
CREATE INDEX idx_vehicle_latest_state_observation_uid ON melaka.vehicle_latest_state USING btree (observation_uid);
CREATE INDEX idx_vehicle_latest_state_qc_flags_gin ON melaka.vehicle_latest_state USING gin (qc_flags);
CREATE INDEX idx_vehicle_latest_state_resolved_route_id ON melaka.vehicle_latest_state USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_latest_state_route_vehicle_time ON melaka.vehicle_latest_state USING btree (resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_latest_state_trip_id ON melaka.vehicle_latest_state USING btree (trip_id);
CREATE INDEX idx_vehicle_latest_state_update_time ON melaka.vehicle_latest_state USING btree (update_time);
CREATE INDEX idx_vehicle_latest_state_vehicle_id ON melaka.vehicle_latest_state USING btree (vehicle_id);
CREATE INDEX idx_vehicle_latest_state_vehicle_time ON melaka.vehicle_latest_state USING btree (vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_analysis_eligible ON melaka.vehicle_observation USING btree (analysis_eligible);
CREATE INDEX idx_vehicle_observation_create_time_brin ON melaka.vehicle_observation USING brin (create_time);
CREATE INDEX idx_vehicle_observation_duplicate_observation ON melaka.vehicle_observation USING btree (duplicate_observation);
CREATE INDEX idx_vehicle_observation_entity_id ON melaka.vehicle_observation USING btree (entity_id);
CREATE INDEX idx_vehicle_observation_feed_route_time ON melaka.vehicle_observation USING btree (feed_uid, resolved_route_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_trip_time ON melaka.vehicle_observation USING btree (feed_uid, trip_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_feed_uid ON melaka.vehicle_observation USING btree (feed_uid);
CREATE INDEX idx_vehicle_observation_feed_vehicle_time ON melaka.vehicle_observation USING btree (feed_uid, vehicle_id, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_geom_gist ON melaka.vehicle_observation USING gist (geom) WHERE (geom IS NOT NULL);
CREATE INDEX idx_vehicle_observation_gps_jump_status ON melaka.vehicle_observation USING btree (gps_jump_status);
CREATE INDEX idx_vehicle_observation_ingest_time ON melaka.vehicle_observation USING btree (ingest_time);
CREATE INDEX idx_vehicle_observation_observation_key ON melaka.vehicle_observation USING btree (observation_key);
CREATE INDEX idx_vehicle_observation_position_qc_status ON melaka.vehicle_observation USING btree (position_qc_status);
CREATE INDEX idx_vehicle_observation_previous_vehicle_time ON melaka.vehicle_observation USING btree (previous_vehicle_time);
CREATE INDEX idx_vehicle_observation_qc_flags_gin ON melaka.vehicle_observation USING gin (qc_flags);
CREATE INDEX idx_vehicle_observation_realtime_route_id ON melaka.vehicle_observation USING btree (realtime_route_id);
CREATE INDEX idx_vehicle_observation_request_uid ON melaka.vehicle_observation USING btree (request_uid);
CREATE INDEX idx_vehicle_observation_resolved_route_id ON melaka.vehicle_observation USING btree (resolved_route_id);
CREATE INDEX idx_vehicle_observation_run_uid ON melaka.vehicle_observation USING btree (run_uid);
CREATE INDEX idx_vehicle_observation_run_vehicle_time ON melaka.vehicle_observation USING btree (run_uid, vehicle_time DESC);
CREATE INDEX idx_vehicle_observation_shape_id ON melaka.vehicle_observation USING btree (shape_id);
CREATE INDEX idx_vehicle_observation_spatial_eligible ON melaka.vehicle_observation USING btree (spatial_eligible);
CREATE INDEX idx_vehicle_observation_static_route_id ON melaka.vehicle_observation USING btree (static_route_id);
CREATE INDEX idx_vehicle_observation_static_route_uid ON melaka.vehicle_observation USING btree (static_route_uid);
CREATE INDEX idx_vehicle_observation_static_shape_uid ON melaka.vehicle_observation USING btree (static_shape_uid);
CREATE INDEX idx_vehicle_observation_static_stop_uid ON melaka.vehicle_observation USING btree (static_stop_uid);
CREATE INDEX idx_vehicle_observation_static_trip_uid ON melaka.vehicle_observation USING btree (static_trip_uid);
CREATE INDEX idx_vehicle_observation_static_version_uid ON melaka.vehicle_observation USING btree (static_version_uid);
CREATE INDEX idx_vehicle_observation_stop_id ON melaka.vehicle_observation USING btree (stop_id);
CREATE INDEX idx_vehicle_observation_trip_id ON melaka.vehicle_observation USING btree (trip_id);
CREATE INDEX idx_vehicle_observation_trip_start_date ON melaka.vehicle_observation USING btree (trip_start_date);
CREATE INDEX idx_vehicle_observation_vehicle_id ON melaka.vehicle_observation USING btree (vehicle_id);
CREATE INDEX idx_vehicle_observation_vehicle_time ON melaka.vehicle_observation USING btree (vehicle_time DESC);
CREATE UNIQUE INDEX uk_vehicle_observation_snapshot_entity_id ON melaka.vehicle_observation USING btree (snapshot_uid, entity_id) WHERE (entity_id IS NOT NULL);
CREATE INDEX idx_vehicle_observation_qc_category_create_time ON melaka.vehicle_observation_qc USING btree (qc_category, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_code_create_time ON melaka.vehicle_observation_qc USING btree (qc_code, create_time DESC);
CREATE INDEX idx_vehicle_observation_qc_create_time ON melaka.vehicle_observation_qc USING btree (create_time);
CREATE INDEX idx_vehicle_observation_qc_severity_create_time ON melaka.vehicle_observation_qc USING btree (severity, create_time DESC);
CREATE INDEX idx_collection_artifact_run ON lta.collection_artifact USING btree (run_uid);
CREATE INDEX idx_collection_page_log_run ON lta.collection_page_log USING btree (run_uid);
CREATE INDEX idx_collection_run_create_time ON lta.collection_run USING btree (create_time DESC);
CREATE INDEX idx_collection_run_endpoint ON lta.collection_run USING btree (api_endpoint_uid);
CREATE INDEX idx_collection_run_request_start ON lta.collection_run USING btree (request_start_time DESC);
CREATE INDEX idx_collection_run_scheduled_time ON lta.collection_run USING btree (scheduled_time DESC);
CREATE INDEX idx_collection_run_success ON lta.collection_run USING btree (success);
CREATE INDEX idx_faulty_light_active ON lta.faulty_traffic_light_event USING btree (active);
CREATE INDEX idx_faulty_light_area ON lta.faulty_traffic_light_event USING btree (area_uid);
CREATE INDEX idx_faulty_light_first_run ON lta.faulty_traffic_light_event USING btree (first_run_uid);
CREATE INDEX idx_faulty_light_last_run ON lta.faulty_traffic_light_event USING btree (last_run_uid);
CREATE INDEX idx_road_opening_active ON lta.road_opening_event USING btree (active);
CREATE INDEX idx_road_opening_area ON lta.road_opening_event USING btree (area_uid);
CREATE INDEX idx_road_opening_end_date ON lta.road_opening_event USING btree (end_date);
CREATE INDEX idx_road_opening_first_run ON lta.road_opening_event USING btree (first_run_uid);
CREATE INDEX idx_road_opening_last_run ON lta.road_opening_event USING btree (last_run_uid);
CREATE INDEX idx_road_opening_road_name ON lta.road_opening_event USING btree (road_name);
CREATE INDEX idx_road_opening_start_date ON lta.road_opening_event USING btree (start_date);
CREATE INDEX idx_road_work_active ON lta.road_work_event USING btree (active);
CREATE INDEX idx_road_work_area ON lta.road_work_event USING btree (area_uid);
CREATE INDEX idx_road_work_end_date ON lta.road_work_event USING btree (end_date);
CREATE INDEX idx_road_work_first_run ON lta.road_work_event USING btree (first_run_uid);
CREATE INDEX idx_road_work_last_run ON lta.road_work_event USING btree (last_run_uid);
CREATE INDEX idx_road_work_road_name ON lta.road_work_event USING btree (road_name);
CREATE INDEX idx_road_work_start_date ON lta.road_work_event USING btree (start_date);
CREATE INDEX idx_study_area_geom ON lta.study_area USING gist (geom);
CREATE INDEX idx_traffic_flow_run ON lta.traffic_flow_file USING btree (run_uid);
CREATE INDEX idx_traffic_incident_active ON lta.traffic_incident_event USING btree (active);
CREATE INDEX idx_traffic_incident_area ON lta.traffic_incident_event USING btree (area_uid);
CREATE INDEX idx_traffic_incident_first_run ON lta.traffic_incident_event USING btree (first_run_uid);
CREATE INDEX idx_traffic_incident_first_seen ON lta.traffic_incident_event USING btree (first_seen_time);
CREATE INDEX idx_traffic_incident_geom ON lta.traffic_incident_event USING gist (geom);
CREATE INDEX idx_traffic_incident_last_run ON lta.traffic_incident_event USING btree (last_run_uid);
CREATE INDEX idx_traffic_incident_type ON lta.traffic_incident_event USING btree (type);
CREATE INDEX idx_traffic_link_geom ON lta.traffic_link USING gist (geom);
CREATE INDEX idx_traffic_link_road_category ON lta.traffic_link USING btree (road_category);
CREATE INDEX idx_traffic_link_road_name ON lta.traffic_link USING btree (road_name);
CREATE INDEX idx_traffic_link_scope_area ON lta.traffic_link_scope USING btree (area_uid);
CREATE INDEX idx_traffic_link_scope_link ON lta.traffic_link_scope USING btree (link_uid);
CREATE INDEX idx_traffic_speed_link_time ON lta.traffic_speed_observation USING btree (link_uid, snapshot_time DESC);
CREATE INDEX idx_traffic_speed_run ON lta.traffic_speed_observation USING btree (run_uid);
CREATE INDEX idx_traffic_speed_snapshot_brin ON lta.traffic_speed_observation USING brin (snapshot_time);
CREATE INDEX idx_traffic_speed_snapshot_time ON lta.traffic_speed_observation USING btree (snapshot_time DESC);
CREATE INDEX idx_travel_time_run ON lta.travel_time_observation USING btree (run_uid);
CREATE INDEX idx_travel_time_segment_time ON lta.travel_time_observation USING btree (segment_uid, snapshot_time DESC);
CREATE INDEX idx_travel_time_snapshot ON lta.travel_time_observation USING btree (snapshot_time DESC);
CREATE INDEX idx_travel_time_segment_area ON lta.travel_time_segment USING btree (area_uid);
CREATE INDEX idx_vms_equipment_area ON lta.vms_equipment USING btree (area_uid);
CREATE INDEX idx_vms_equipment_geom ON lta.vms_equipment USING gist (geom);
CREATE INDEX idx_vms_message_equipment ON lta.vms_message_state USING btree (equipment_uid);
CREATE INDEX idx_vms_message_first_run ON lta.vms_message_state USING btree (first_run_uid);
CREATE INDEX idx_vms_message_last_run ON lta.vms_message_state USING btree (last_run_uid);
CREATE INDEX idx_vms_message_last_seen ON lta.vms_message_state USING btree (last_seen_time DESC);
CREATE UNIQUE INDEX uq_vms_message_one_active ON lta.vms_message_state USING btree (equipment_uid) WHERE (active = true);

CREATE OR REPLACE FUNCTION lta.set_update_time()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$function$;

CREATE TRIGGER trg_api_endpoint_update_time BEFORE UPDATE ON lta.api_endpoint FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_collection_artifact_update_time BEFORE UPDATE ON lta.collection_artifact FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_collection_page_log_update_time BEFORE UPDATE ON lta.collection_page_log FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_collection_run_update_time BEFORE UPDATE ON lta.collection_run FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_faulty_traffic_light_event_update_time BEFORE UPDATE ON lta.faulty_traffic_light_event FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_road_opening_event_update_time BEFORE UPDATE ON lta.road_opening_event FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_road_work_event_update_time BEFORE UPDATE ON lta.road_work_event FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_study_area_update_time BEFORE UPDATE ON lta.study_area FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_traffic_flow_file_update_time BEFORE UPDATE ON lta.traffic_flow_file FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_traffic_incident_event_update_time BEFORE UPDATE ON lta.traffic_incident_event FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_traffic_link_update_time BEFORE UPDATE ON lta.traffic_link FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_traffic_link_scope_update_time BEFORE UPDATE ON lta.traffic_link_scope FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_traffic_speed_observation_update_time BEFORE UPDATE ON lta.traffic_speed_observation FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_travel_time_observation_update_time BEFORE UPDATE ON lta.travel_time_observation FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_travel_time_segment_update_time BEFORE UPDATE ON lta.travel_time_segment FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_vms_equipment_update_time BEFORE UPDATE ON lta.vms_equipment FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();
CREATE TRIGGER trg_vms_message_state_update_time BEFORE UPDATE ON lta.vms_message_state FOR EACH ROW EXECUTE FUNCTION lta.set_update_time();

COMMENT ON TABLE "core"."collection_run" IS '数据采集运行任务表，用于记录一次多城市GTFS采集任务的计划、实际执行过程及请求统计结果';
COMMENT ON TABLE "core"."daily_archive" IS '每日数据归档记录表，用于记录MYTransitGPS按Malaysia自然日生成的ZIP归档、SHA-256校验、文件统计及完整性验证状态';
COMMENT ON TABLE "core"."gtfs_feed" IS 'GTFS数据源配置表，用于维护不同城市Realtime和Static GTFS接口、运营商、服务类型及采集调度参数';
COMMENT ON TABLE "core"."study_city" IS '研究城市基础信息表，用于维护MYTransitGPS项目覆盖的研究城市、对应数据库Schema和业务时区';
COMMENT ON TABLE "jb"."api_request_log" IS 'Johor Bahru GTFS API请求证据表，每次真实HTTP请求尝试均独立保留，包括成功、重定向、限流、服务器错误、超时和重复响应';
COMMENT ON TABLE "jb"."realtime_snapshot" IS 'Johor Bahru GTFS-Realtime FeedMessage快照表，仅记录成功解析的Realtime数据快照及其解析和匹配统计';
COMMENT ON TABLE "jb"."static_route" IS 'Johor Bahru GTFS routes.txt线路数据表，保留结构化线路字段及完整原始CSV行';
COMMENT ON TABLE "jb"."static_shape" IS 'Johor Bahru GTFS Shape汇总表，每个shape_id一行，保存引用状态、点数、线路几何、长度和质量控制结果';
COMMENT ON TABLE "jb"."static_shape_point" IS 'Johor Bahru GTFS shapes.txt原始Shape Point事实表，完整保留每个点的源坐标、顺序、空间对象和质量标记';
COMMENT ON TABLE "jb"."static_stop" IS 'Johor Bahru GTFS stops.txt站点数据表，保留原始坐标、有效空间对象和站点质量标记';
COMMENT ON TABLE "jb"."static_stop_time" IS 'Johor Bahru GTFS stop_times.txt到离站时刻数据表，使用原始字符串和服务日累计秒数支持超过24小时的GTFS时间';
COMMENT ON TABLE "jb"."static_trip" IS 'Johor Bahru GTFS trips.txt班次数据表，用于关联Static Route、Shape以及Realtime Trip信息';
COMMENT ON TABLE "jb"."static_version" IS 'Johor Bahru GTFS Static版本证据表，用于维护唯一Static ZIP内容、下载证据、文件存在状态、数据统计及Realtime关联有效期';
COMMENT ON TABLE "jb"."vehicle_latest_state" IS 'Johor Bahru公交车辆最新状态表，每个Feed和Vehicle只保留一行，供实时Dashboard和当前状态查询使用';
COMMENT ON TABLE "jb"."vehicle_observation" IS 'Johor Bahru公交车辆Realtime GPS历史观测事实表，每次GTFS-Realtime快照中出现的VehiclePosition均保留，用于轨迹重建、数据质量分析和长期交通研究';
COMMENT ON TABLE "jb"."vehicle_observation_qc" IS 'Johor Bahru车辆观测质量控制明细表，以规范化形式保存每条Observation的零个或多个QC标记';
COMMENT ON TABLE "kl"."api_request_log" IS 'Kuala Lumpur GTFS API请求证据表，每次真实HTTP请求尝试均独立保留，包括成功、重定向、限流、服务器错误、超时和重复响应';
COMMENT ON TABLE "kl"."realtime_snapshot" IS 'Kuala Lumpur GTFS-Realtime FeedMessage快照表，仅记录成功解析的Realtime数据快照及其解析和匹配统计';
COMMENT ON TABLE "kl"."static_route" IS 'Kuala Lumpur GTFS routes.txt线路数据表，保留结构化线路字段及完整原始CSV行';
COMMENT ON TABLE "kl"."static_shape" IS 'Kuala Lumpur GTFS Shape汇总表，每个shape_id一行，保存引用状态、点数、线路几何、长度和质量控制结果';
COMMENT ON TABLE "kl"."static_shape_point" IS 'Kuala Lumpur GTFS shapes.txt原始Shape Point事实表，完整保留每个点的源坐标、顺序、空间对象和质量标记';
COMMENT ON TABLE "kl"."static_stop" IS 'Kuala Lumpur GTFS stops.txt站点数据表，保留原始坐标、有效空间对象和站点质量标记';
COMMENT ON TABLE "kl"."static_stop_time" IS 'Kuala Lumpur GTFS stop_times.txt到离站时刻数据表，使用原始字符串和服务日累计秒数支持超过24小时的GTFS时间';
COMMENT ON TABLE "kl"."static_trip" IS 'Kuala Lumpur GTFS trips.txt班次数据表，用于关联Static Route、Shape以及Realtime Trip信息';
COMMENT ON TABLE "kl"."static_version" IS 'Kuala Lumpur GTFS Static版本证据表，用于维护唯一Static ZIP内容、下载证据、文件存在状态、数据统计及Realtime关联有效期';
COMMENT ON TABLE "kl"."vehicle_latest_state" IS 'Kuala Lumpur公交车辆最新状态表，每个Feed和Vehicle只保留一行，供实时Dashboard和当前状态查询使用';
COMMENT ON TABLE "kl"."vehicle_observation" IS 'Kuala Lumpur公交车辆Realtime GPS历史观测事实表，每次GTFS-Realtime快照中出现的VehiclePosition均保留，用于轨迹重建、数据质量分析和长期交通研究';
COMMENT ON TABLE "kl"."vehicle_observation_qc" IS 'Kuala Lumpur车辆观测质量控制明细表，以规范化形式保存每条Observation的零个或多个QC标记';
COMMENT ON TABLE "kuching"."api_request_log" IS 'Kuching GTFS API请求证据表，每次真实HTTP请求尝试均独立保留，包括成功、重定向、限流、服务器错误、超时和重复响应';
COMMENT ON TABLE "kuching"."realtime_snapshot" IS 'Kuching GTFS-Realtime FeedMessage快照表，仅记录成功解析的Realtime数据快照及其解析和匹配统计';
COMMENT ON TABLE "kuching"."static_route" IS 'Kuching GTFS routes.txt线路数据表，保留结构化线路字段及完整原始CSV行';
COMMENT ON TABLE "kuching"."static_shape" IS 'Kuching GTFS Shape汇总表，每个shape_id一行，保存引用状态、点数、线路几何、长度和质量控制结果';
COMMENT ON TABLE "kuching"."static_shape_point" IS 'Kuching GTFS shapes.txt原始Shape Point事实表，完整保留每个点的源坐标、顺序、空间对象和质量标记';
COMMENT ON TABLE "kuching"."static_stop" IS 'Kuching GTFS stops.txt站点数据表，保留原始坐标、有效空间对象和站点质量标记';
COMMENT ON TABLE "kuching"."static_stop_time" IS 'Kuching GTFS stop_times.txt到离站时刻数据表，使用原始字符串和服务日累计秒数支持超过24小时的GTFS时间';
COMMENT ON TABLE "kuching"."static_trip" IS 'Kuching GTFS trips.txt班次数据表，用于关联Static Route、Shape以及Realtime Trip信息';
COMMENT ON TABLE "kuching"."static_version" IS 'Kuching GTFS Static版本证据表，用于维护唯一Static ZIP内容、下载证据、文件存在状态、数据统计及Realtime关联有效期';
COMMENT ON TABLE "kuching"."vehicle_latest_state" IS 'Kuching公交车辆最新状态表，每个Feed和Vehicle只保留一行，供实时Dashboard和当前状态查询使用';
COMMENT ON TABLE "kuching"."vehicle_observation" IS 'Kuching公交车辆Realtime GPS历史观测事实表，每次GTFS-Realtime快照中出现的VehiclePosition均保留，用于轨迹重建、数据质量分析和长期交通研究';
COMMENT ON TABLE "kuching"."vehicle_observation_qc" IS 'Kuching车辆观测质量控制明细表，以规范化形式保存每条Observation的零个或多个QC标记';
COMMENT ON TABLE "lta"."api_endpoint" IS 'LTA DataMall接口配置主表，用于保存接口编号、名称、URL、采集频率、Raw文件保存策略以及数据库写入策略。';
COMMENT ON TABLE "lta"."collection_artifact" IS '采集原始文件与数据文件元数据登记表，仅保存路径、大小和SHA256，不直接保存大型Raw JSON。';
COMMENT ON TABLE "lta"."collection_page_log" IS '接口分页采集审计表，记录TrafficSpeedBands每个$skip请求，用于定位漏页、失败页和跨页源时间变化。';
COMMENT ON TABLE "lta"."collection_run" IS 'LTA所有接口统一采集任务主日志表，一次接口调度对应一条记录，用于记录请求、数量、完整性、重试和错误。';
COMMENT ON TABLE "lta"."faulty_traffic_light_event" IS 'LTA故障交通信号灯生命周期表，以AlarmID为源系统业务编号，以uid为数据库UUID主键。';
COMMENT ON TABLE "lta"."road_opening_event" IS 'LTA Planned Road Openings计划道路开放事件生命周期表，记录未来道路开放及道路网络调整。';
COMMENT ON TABLE "lta"."road_work_event" IS 'LTA Approved Road Works道路施工事件生命周期表，记录施工计划、道路及事件出现和结束过程。';
COMMENT ON TABLE "lta"."study_area" IS 'Woodlands CIQ与Tuas CIQ科研研究区域空间主表，用于保存CORE、APPROACH、CORRIDOR等经确认的正式研究区域。';
COMMENT ON TABLE "lta"."traffic_flow_file" IS 'Traffic Flow历史交通流量下载文件月度登记表，每月最后一天获取临时下载链接并登记实际文件及审计信息。';
COMMENT ON TABLE "lta"."traffic_incident_event" IS 'LTA实时交通事件生命周期表，保存事件位置与消息，并通过首次发现、最后发现和结束时间实现去重及生命周期管理。';
COMMENT ON TABLE "lta"."traffic_link" IS 'TrafficSpeedBands道路Link静态空间主表，保存LTA道路分段业务编号、道路属性、端点及线几何；不保存动态速度。';
COMMENT ON TABLE "lta"."traffic_link_scope" IS 'TrafficSpeedBands道路Link与CIQ研究区域多对多关联白名单，决定哪些Link动态速度允许进入科研数据库。';
COMMENT ON TABLE "lta"."traffic_speed_observation" IS 'TrafficSpeedBands动态道路速度时序观测表，每条记录表示标准采集时刻某个道路Link的速度等级。';
COMMENT ON TABLE "lta"."travel_time_observation" IS '高速公路分段预计旅行时间动态时序表，每条记录表示标准采集时刻某一分段的预计旅行时间。';
COMMENT ON TABLE "lta"."travel_time_segment" IS 'Estimated Travel Times高速公路静态旅行时间分段主表，标准化高速名称、方向、远端终点、起点和终点。';
COMMENT ON TABLE "lta"."vms_equipment" IS 'LTA VMS/EMAS电子信息板设备静态主表，保存EquipmentID及固定位置，不重复保存动态Message。';
COMMENT ON TABLE "lta"."vms_message_state" IS 'VMS电子信息板文本状态生命周期表，相同消息更新last_seen_time，消息改变时结束旧状态并创建新UUID记录。';
COMMENT ON TABLE "melaka"."api_request_log" IS 'Melaka GTFS API请求证据表，每次真实HTTP请求尝试均独立保留，包括成功、重定向、限流、服务器错误、超时和重复响应';
COMMENT ON TABLE "melaka"."realtime_snapshot" IS 'Melaka GTFS-Realtime FeedMessage快照表，仅记录成功解析的Realtime数据快照及其解析和匹配统计';
COMMENT ON TABLE "melaka"."static_route" IS 'Melaka GTFS routes.txt线路数据表，保留结构化线路字段及完整原始CSV行';
COMMENT ON TABLE "melaka"."static_shape" IS 'Melaka GTFS Shape汇总表，每个shape_id一行，保存引用状态、点数、线路几何、长度和质量控制结果';
COMMENT ON TABLE "melaka"."static_shape_point" IS 'Melaka GTFS shapes.txt原始Shape Point事实表，完整保留每个点的源坐标、顺序、空间对象和质量标记';
COMMENT ON TABLE "melaka"."static_stop" IS 'Melaka GTFS stops.txt站点数据表，保留原始坐标、有效空间对象和站点质量标记';
COMMENT ON TABLE "melaka"."static_stop_time" IS 'Melaka GTFS stop_times.txt到离站时刻数据表，使用原始字符串和服务日累计秒数支持超过24小时的GTFS时间';
COMMENT ON TABLE "melaka"."static_trip" IS 'Melaka GTFS trips.txt班次数据表，用于关联Static Route、Shape以及Realtime Trip信息';
COMMENT ON TABLE "melaka"."static_version" IS 'Melaka GTFS Static版本证据表，用于维护唯一Static ZIP内容、下载证据、文件存在状态、数据统计及Realtime关联有效期';
COMMENT ON TABLE "melaka"."vehicle_latest_state" IS 'Melaka公交车辆最新状态表，每个Feed和Vehicle只保留一行，供实时Dashboard和当前状态查询使用';
COMMENT ON TABLE "melaka"."vehicle_observation" IS 'Melaka公交车辆Realtime GPS历史观测事实表，每次GTFS-Realtime快照中出现的VehiclePosition均保留，用于轨迹重建、数据质量分析和长期交通研究';
COMMENT ON TABLE "melaka"."vehicle_observation_qc" IS 'Melaka车辆观测质量控制明细表，以规范化形式保存每条Observation的零个或多个QC标记';
COMMENT ON COLUMN "core"."collection_run"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "core"."collection_run"."run_code" IS '数据采集运行任务唯一业务编号';
COMMENT ON COLUMN "core"."collection_run"."run_type" IS '运行类型，例如DEMO、SHORT_RUN、ONE_HOUR或PRODUCTION';
COMMENT ON COLUMN "core"."collection_run"."run_status" IS '运行状态，例如RUNNING、SUCCESS、SUCCESS_WITH_WARNINGS、FAILED';
COMMENT ON COLUMN "core"."collection_run"."timezone_name" IS '本次运行用于调度和业务日期解释的IANA时区';
COMMENT ON COLUMN "core"."collection_run"."planned_start_time" IS '计划开始采集的绝对时间';
COMMENT ON COLUMN "core"."collection_run"."actual_start_time" IS '实际开始采集的绝对时间';
COMMENT ON COLUMN "core"."collection_run"."actual_end_time" IS '实际结束采集的绝对时间';
COMMENT ON COLUMN "core"."collection_run"."planned_cycle_count" IS '计划执行采集周期数';
COMMENT ON COLUMN "core"."collection_run"."actual_cycle_count" IS '实际完成采集周期数';
COMMENT ON COLUMN "core"."collection_run"."feed_count" IS '本次运行涉及的GTFS Feed数量';
COMMENT ON COLUMN "core"."collection_run"."planned_request_count" IS '计划Realtime请求总次数';
COMMENT ON COLUMN "core"."collection_run"."actual_request_count" IS '实际发起请求总次数';
COMMENT ON COLUMN "core"."collection_run"."successful_request_count" IS '成功请求次数';
COMMENT ON COLUMN "core"."collection_run"."failed_request_count" IS '失败请求次数';
COMMENT ON COLUMN "core"."collection_run"."http_429_count" IS 'HTTP 429限流响应次数';
COMMENT ON COLUMN "core"."collection_run"."http_5xx_count" IS 'HTTP 5xx服务器错误次数';
COMMENT ON COLUMN "core"."collection_run"."timeout_count" IS '请求超时次数';
COMMENT ON COLUMN "core"."collection_run"."scheduler_drift_p50_ms" IS '本次运行调度偏移的P50毫秒值';
COMMENT ON COLUMN "core"."collection_run"."scheduler_drift_p95_ms" IS '本次运行调度偏移的P95毫秒值';
COMMENT ON COLUMN "core"."collection_run"."scheduler_drift_max_ms" IS '本次运行最大调度偏移毫秒值';
COMMENT ON COLUMN "core"."collection_run"."remarks" IS '运行任务补充说明或异常说明';
COMMENT ON COLUMN "core"."collection_run"."create_time" IS '当前运行任务记录首次入库时间';
COMMENT ON COLUMN "core"."collection_run"."update_time" IS '当前运行任务最近一次更新数据库的时间';
COMMENT ON COLUMN "core"."daily_archive"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "core"."daily_archive"."archive_date" IS '被归档数据对应的Malaysia自然日期';
COMMENT ON COLUMN "core"."daily_archive"."archive_status" IS '每日归档状态，例如SUCCESS、SUCCESS_WITH_WARNINGS、NO_DATA或FAILED';
COMMENT ON COLUMN "core"."daily_archive"."timezone_name" IS '本次每日归档采用的业务时区';
COMMENT ON COLUMN "core"."daily_archive"."archive_file_name" IS 'Daily Archive ZIP文件名称';
COMMENT ON COLUMN "core"."daily_archive"."archive_path" IS 'Daily Archive ZIP文件实际存储路径';
COMMENT ON COLUMN "core"."daily_archive"."archive_size_bytes" IS 'ZIP归档文件字节大小';
COMMENT ON COLUMN "core"."daily_archive"."archive_sha256" IS 'Daily Archive ZIP完整文件SHA-256摘要';
COMMENT ON COLUMN "core"."daily_archive"."manifest_path" IS 'Daily Manifest文件在归档体系中的路径';
COMMENT ON COLUMN "core"."daily_archive"."checksum_path" IS '内部SHA-256校验文件路径';
COMMENT ON COLUMN "core"."daily_archive"."source_file_count" IS '归档前扫描到的源文件总数';
COMMENT ON COLUMN "core"."daily_archive"."archive_entry_count" IS '最终ZIP内部Entry总数量';
COMMENT ON COLUMN "core"."daily_archive"."realtime_request_count" IS '归档日期内Realtime API请求记录数量';
COMMENT ON COLUMN "core"."daily_archive"."static_request_count" IS '归档日期内Static API请求记录数量';
COMMENT ON COLUMN "core"."daily_archive"."successful_request_count" IS '归档日期内成功API请求数量';
COMMENT ON COLUMN "core"."daily_archive"."failed_request_count" IS '归档日期内失败API请求数量';
COMMENT ON COLUMN "core"."daily_archive"."parsed_json_count" IS '归档日期内parsed_full JSON文件数量';
COMMENT ON COLUMN "core"."daily_archive"."enriched_json_count" IS '归档日期内enriched_full JSON文件数量';
COMMENT ON COLUMN "core"."daily_archive"."raw_object_count" IS '归档日期内Realtime RAW protobuf对象数量';
COMMENT ON COLUMN "core"."daily_archive"."static_object_count" IS '归档日期内实际引用的唯一Static ZIP对象数量';
COMMENT ON COLUMN "core"."daily_archive"."run_report_file_count" IS '归档日期内运行质量报告文件数量';
COMMENT ON COLUMN "core"."daily_archive"."archive_created_at" IS 'Daily ZIP实际创建完成的绝对时间';
COMMENT ON COLUMN "core"."daily_archive"."verified_at" IS 'Daily ZIP完成重新打开及完整性验证的时间';
COMMENT ON COLUMN "core"."daily_archive"."warning_message" IS '归档成功但存在不完整日等警告时的说明';
COMMENT ON COLUMN "core"."daily_archive"."error_message" IS '归档失败时的错误信息';
COMMENT ON COLUMN "core"."daily_archive"."create_time" IS '当前归档记录首次写入数据库的时间';
COMMENT ON COLUMN "core"."daily_archive"."update_time" IS '当前归档记录最近一次更新数据库的时间';
COMMENT ON COLUMN "core"."gtfs_feed"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "core"."gtfs_feed"."city_uid" IS '所属研究城市UUID，关联core.study_city.uid';
COMMENT ON COLUMN "core"."gtfs_feed"."feed_id" IS 'GTFS数据源业务标识，由项目配置定义并作为跨模块稳定源标识';
COMMENT ON COLUMN "core"."gtfs_feed"."feed_name" IS 'GTFS数据源可读名称';
COMMENT ON COLUMN "core"."gtfs_feed"."operator_name" IS '公交运营机构或数据提供机构名称';
COMMENT ON COLUMN "core"."gtfs_feed"."service_type" IS '公交服务类型，例如bus、rapid_bus、mrt_feeder';
COMMENT ON COLUMN "core"."gtfs_feed"."realtime_url" IS 'GTFS-Realtime VehiclePosition接口地址';
COMMENT ON COLUMN "core"."gtfs_feed"."static_url" IS 'GTFS Static下载接口地址';
COMMENT ON COLUMN "core"."gtfs_feed"."realtime_feed_type" IS 'Realtime数据类型，本项目当前为vehicle_position';
COMMENT ON COLUMN "core"."gtfs_feed"."source_platform" IS 'GTFS接口来源平台';
COMMENT ON COLUMN "core"."gtfs_feed"."file_prefix" IS '文件系统RAW、JSON和Metadata统一文件名前缀';
COMMENT ON COLUMN "core"."gtfs_feed"."poll_interval_seconds" IS '当前Feed计划轮询间隔秒数';
COMMENT ON COLUMN "core"."gtfs_feed"."stagger_offset_seconds" IS '多Feed错峰轮询相对于周期起点的秒数';
COMMENT ON COLUMN "core"."gtfs_feed"."timezone_name" IS '数据源业务时区IANA名称';
COMMENT ON COLUMN "core"."gtfs_feed"."enabled" IS '当前Feed是否启用';
COMMENT ON COLUMN "core"."gtfs_feed"."description" IS 'Feed补充说明';
COMMENT ON COLUMN "core"."gtfs_feed"."create_time" IS '当前Feed配置首次入库时间';
COMMENT ON COLUMN "core"."gtfs_feed"."update_time" IS '当前Feed配置最近一次修改时间';
COMMENT ON COLUMN "core"."study_city"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "core"."study_city"."city_code" IS '城市业务代码，用于程序内部稳定识别研究城市';
COMMENT ON COLUMN "core"."study_city"."city_name" IS '城市英文标准名称';
COMMENT ON COLUMN "core"."study_city"."schema_name" IS '城市业务数据所在PostgreSQL Schema名称';
COMMENT ON COLUMN "core"."study_city"."country_code" IS 'ISO国家代码，本项目默认为MY';
COMMENT ON COLUMN "core"."study_city"."timezone_name" IS '城市业务时区IANA名称，本项目统一为Asia/Kuala_Lumpur';
COMMENT ON COLUMN "core"."study_city"."enabled" IS '城市是否启用数据采集及后续业务处理';
COMMENT ON COLUMN "core"."study_city"."description" IS '城市或研究区域补充说明';
COMMENT ON COLUMN "core"."study_city"."create_time" IS '当前记录首次写入数据库的时间';
COMMENT ON COLUMN "core"."study_city"."update_time" IS '当前记录最近一次更新的数据库时间';
COMMENT ON COLUMN "jb"."api_request_log"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."api_request_log"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "jb"."api_request_log"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "jb"."api_request_log"."duplicate_of_request_uid" IS '重复响应所引用的原始API请求UUID，关联jb.api_request_log.uid';
COMMENT ON COLUMN "jb"."api_request_log"."request_type" IS '请求类型，例如REALTIME或STATIC';
COMMENT ON COLUMN "jb"."api_request_log"."request_sequence" IS '本次运行中的请求顺序编号';
COMMENT ON COLUMN "jb"."api_request_log"."cycle_number" IS '本次运行中的采集周期编号';
COMMENT ON COLUMN "jb"."api_request_log"."requested_url" IS '请求发起时使用的原始URL';
COMMENT ON COLUMN "jb"."api_request_log"."final_url" IS '完成重定向后实际访问的最终URL';
COMMENT ON COLUMN "jb"."api_request_log"."scheduled_at" IS '调度器计划发起请求的绝对时间';
COMMENT ON COLUMN "jb"."api_request_log"."request_started_at" IS 'HTTP请求实际开始的绝对时间';
COMMENT ON COLUMN "jb"."api_request_log"."response_received_at" IS 'HTTP响应接收完成的绝对时间';
COMMENT ON COLUMN "jb"."api_request_log"."latency_ms" IS 'HTTP请求从开始到收到响应的耗时毫秒数';
COMMENT ON COLUMN "jb"."api_request_log"."scheduler_drift_ms" IS '实际请求开始时间相对计划时间的调度偏移毫秒数';
COMMENT ON COLUMN "jb"."api_request_log"."http_status" IS 'HTTP响应状态码，网络异常或超时时允许为空';
COMMENT ON COLUMN "jb"."api_request_log"."content_type" IS 'HTTP响应Content-Type原始值';
COMMENT ON COLUMN "jb"."api_request_log"."response_bytes" IS 'HTTP响应正文的字节数';
COMMENT ON COLUMN "jb"."api_request_log"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "jb"."api_request_log"."redirect_count" IS '本次HTTP请求经历的重定向次数';
COMMENT ON COLUMN "jb"."api_request_log"."parse_status" IS '响应正文的解析状态';
COMMENT ON COLUMN "jb"."api_request_log"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "jb"."api_request_log"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "jb"."api_request_log"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "jb"."api_request_log"."raw_object_path" IS 'Realtime原始protobuf对象的文件存储路径';
COMMENT ON COLUMN "jb"."api_request_log"."parsed_json_path" IS '完整解析JSON文件的存储路径';
COMMENT ON COLUMN "jb"."api_request_log"."enriched_json_path" IS 'Static GTFS补充后JSON文件的存储路径';
COMMENT ON COLUMN "jb"."api_request_log"."result" IS '本次API请求的最终结果状态';
COMMENT ON COLUMN "jb"."api_request_log"."error_class" IS '请求或解析失败对应的异常类名称';
COMMENT ON COLUMN "jb"."api_request_log"."error_message" IS '请求、响应或解析失败的详细错误信息';
COMMENT ON COLUMN "jb"."api_request_log"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."realtime_snapshot"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."realtime_snapshot"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "jb"."realtime_snapshot"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "jb"."realtime_snapshot"."request_uid" IS '产生当前数据的API请求UUID，关联jb.api_request_log.uid';
COMMENT ON COLUMN "jb"."realtime_snapshot"."enrichment_static_version_uid" IS '当前GTFS-Realtime快照执行Static补充和线路解析时使用的GTFS Static版本UUID，正式关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."realtime_snapshot"."batch_id" IS '当前Realtime快照所属批次业务标识';
COMMENT ON COLUMN "jb"."realtime_snapshot"."gtfs_realtime_version" IS 'GTFS-Realtime FeedHeader声明的规范版本';
COMMENT ON COLUMN "jb"."realtime_snapshot"."incrementality" IS 'GTFS-Realtime FeedHeader声明的增量模式';
COMMENT ON COLUMN "jb"."realtime_snapshot"."feed_timestamp_raw" IS 'FeedHeader提供的原始Unix秒时间戳';
COMMENT ON COLUMN "jb"."realtime_snapshot"."feed_time" IS 'FeedHeader原始时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "jb"."realtime_snapshot"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."unique_route_count" IS '当前快照解析得到的唯一线路数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."route_direct_match_count" IS '通过Realtime route_id直接匹配Static Route的数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."route_trip_fallback_match_count" IS '通过Static Trip回退匹配Route的数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."route_resolved_count" IS '成功解析到线路的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."route_unresolved_count" IS '未能解析线路的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."trip_match_count" IS '成功匹配Static Trip的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."trip_unmatched_count" IS '未匹配Static Trip的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."direction_match_count" IS 'Realtime与Static方向一致的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."direction_mismatch_count" IS 'Realtime与Static方向不一致的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."direction_not_comparable_count" IS '缺少必要字段而无法比较方向的Entity数量';
COMMENT ON COLUMN "jb"."realtime_snapshot"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "jb"."realtime_snapshot"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "jb"."realtime_snapshot"."referenced_snapshot_uid" IS '重复快照所引用的原始Realtime快照UUID';
COMMENT ON COLUMN "jb"."realtime_snapshot"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_route"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_route"."static_version_uid" IS '所属GTFS Static版本UUID，关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."static_route"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "jb"."static_route"."agency_id" IS 'GTFS routes.txt提供的官方Agency源标识';
COMMENT ON COLUMN "jb"."static_route"."route_short_name" IS 'GTFS线路短名称';
COMMENT ON COLUMN "jb"."static_route"."route_long_name" IS 'GTFS线路完整名称';
COMMENT ON COLUMN "jb"."static_route"."route_desc" IS 'GTFS线路补充描述';
COMMENT ON COLUMN "jb"."static_route"."route_type" IS 'GTFS线路交通方式类型数值';
COMMENT ON COLUMN "jb"."static_route"."route_url" IS 'GTFS线路信息网页URL';
COMMENT ON COLUMN "jb"."static_route"."route_color" IS 'GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "jb"."static_route"."route_text_color" IS 'GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "jb"."static_route"."route_sort_order" IS 'GTFS线路展示排序值';
COMMENT ON COLUMN "jb"."static_route"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "jb"."static_route"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "jb"."static_route"."network_id" IS 'GTFS线路所属网络源标识';
COMMENT ON COLUMN "jb"."static_route"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "jb"."static_route"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_shape"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_shape"."static_version_uid" IS '所属GTFS Static版本UUID，关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."static_shape"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "jb"."static_shape"."is_referenced" IS '当前Shape是否被本Static版本trips.txt中的至少一个Trip实际引用';
COMMENT ON COLUMN "jb"."static_shape"."trip_count_using_shape" IS '当前Static版本中引用该Shape的Trip数量';
COMMENT ON COLUMN "jb"."static_shape"."point_count" IS '当前Shape包含的Shape Point数量';
COMMENT ON COLUMN "jb"."static_shape"."geom" IS '当前Shape按shape_pt_sequence构造的PostGIS LineString对象';
COMMENT ON COLUMN "jb"."static_shape"."shape_length_m" IS '根据当前Shape有效点序列计算得到的几何长度，单位米，异常长度仍保留';
COMMENT ON COLUMN "jb"."static_shape"."shape_length_km" IS '根据当前Shape有效点序列计算得到的几何长度，单位km，异常长度仍保留';
COMMENT ON COLUMN "jb"."static_shape"."analysis_eligible" IS '当前Shape是否推荐进入正常线路长度和空间分析，不控制原始Shape数据保存';
COMMENT ON COLUMN "jb"."static_shape"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "jb"."static_shape"."qc_summary" IS '当前Shape质量控制结果的扩展JSON汇总';
COMMENT ON COLUMN "jb"."static_shape"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_shape_point"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_shape_point"."static_version_uid" IS '所属GTFS Static版本UUID，关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."static_shape_point"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联jb.static_shape.uid';
COMMENT ON COLUMN "jb"."static_shape_point"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "jb"."static_shape_point"."shape_pt_lat" IS 'GTFS shapes.txt提供的原始Shape Point纬度，异常值仍保留';
COMMENT ON COLUMN "jb"."static_shape_point"."shape_pt_lon" IS 'GTFS shapes.txt提供的原始Shape Point经度，异常值仍保留';
COMMENT ON COLUMN "jb"."static_shape_point"."geom" IS '有效WGS84且非0,0 Shape Point对应的PostGIS Point对象';
COMMENT ON COLUMN "jb"."static_shape_point"."shape_pt_sequence" IS 'GTFS shapes.txt中当前Shape Point的原始点序号';
COMMENT ON COLUMN "jb"."static_shape_point"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "jb"."static_shape_point"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "jb"."static_shape_point"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "jb"."static_shape_point"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "jb"."static_shape_point"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "jb"."static_shape_point"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "jb"."static_shape_point"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_stop"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_stop"."static_version_uid" IS '所属GTFS Static版本UUID，关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."static_stop"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "jb"."static_stop"."stop_code" IS 'GTFS站点面向乘客的短代码';
COMMENT ON COLUMN "jb"."static_stop"."stop_name" IS 'GTFS站点名称';
COMMENT ON COLUMN "jb"."static_stop"."tts_stop_name" IS 'GTFS站点文本转语音名称';
COMMENT ON COLUMN "jb"."static_stop"."stop_desc" IS 'GTFS站点补充描述';
COMMENT ON COLUMN "jb"."static_stop"."stop_lat" IS 'GTFS stops.txt原始纬度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "jb"."static_stop"."stop_lon" IS 'GTFS stops.txt原始经度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "jb"."static_stop"."geom" IS '由有效WGS84且非0,0站点坐标生成的PostGIS Point对象';
COMMENT ON COLUMN "jb"."static_stop"."zone_id" IS 'GTFS站点所属票价分区源标识';
COMMENT ON COLUMN "jb"."static_stop"."stop_url" IS 'GTFS站点信息网页URL';
COMMENT ON COLUMN "jb"."static_stop"."location_type" IS 'GTFS站点位置类型原始数值';
COMMENT ON COLUMN "jb"."static_stop"."parent_station" IS 'GTFS站点所属父站点的源stop_id';
COMMENT ON COLUMN "jb"."static_stop"."stop_timezone" IS 'GTFS站点时区IANA名称';
COMMENT ON COLUMN "jb"."static_stop"."wheelchair_boarding" IS 'GTFS站点无障碍上车规则原始数值';
COMMENT ON COLUMN "jb"."static_stop"."level_id" IS 'GTFS站点所属楼层源标识';
COMMENT ON COLUMN "jb"."static_stop"."platform_code" IS 'GTFS站点月台代码';
COMMENT ON COLUMN "jb"."static_stop"."position_present" IS 'GTFS原始站点行是否提供经纬度';
COMMENT ON COLUMN "jb"."static_stop"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "jb"."static_stop"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "jb"."static_stop"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "jb"."static_stop"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "jb"."static_stop"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "jb"."static_stop"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_stop_time"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_stop_time"."static_version_uid" IS '所属GTFS Static版本UUID，关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."static_stop_time"."trip_uid" IS '当前Stop Time关联的数据库Static Trip UUID，关联jb.static_trip.uid';
COMMENT ON COLUMN "jb"."static_stop_time"."stop_uid" IS '当前Stop Time关联的数据库Static Stop UUID，关联jb.static_stop.uid';
COMMENT ON COLUMN "jb"."static_stop_time"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "jb"."static_stop_time"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "jb"."static_stop_time"."stop_sequence" IS 'GTFS stop_times.txt中当前Stop在Trip内的原始顺序';
COMMENT ON COLUMN "jb"."static_stop_time"."arrival_time_raw" IS 'GTFS stop_times.txt原始到站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "jb"."static_stop_time"."arrival_seconds" IS '原始GTFS到站时间换算为从service day开始累计的秒数，用于跨24小时计算';
COMMENT ON COLUMN "jb"."static_stop_time"."departure_time_raw" IS 'GTFS stop_times.txt原始离站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "jb"."static_stop_time"."departure_seconds" IS '原始GTFS离站时间换算为从service day开始累计的秒数';
COMMENT ON COLUMN "jb"."static_stop_time"."stop_headsign" IS 'GTFS Stop Time在当前站点显示的目的地方向文字';
COMMENT ON COLUMN "jb"."static_stop_time"."pickup_type" IS 'GTFS站点上客规则原始数值';
COMMENT ON COLUMN "jb"."static_stop_time"."drop_off_type" IS 'GTFS站点下客规则原始数值';
COMMENT ON COLUMN "jb"."static_stop_time"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "jb"."static_stop_time"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "jb"."static_stop_time"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "jb"."static_stop_time"."timepoint" IS 'GTFS Stop Time是否为精确时刻点的原始数值';
COMMENT ON COLUMN "jb"."static_stop_time"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "jb"."static_stop_time"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_trip"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_trip"."static_version_uid" IS '所属GTFS Static版本UUID，关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."static_trip"."route_uid" IS '当前Trip关联的数据库Static Route UUID，关联jb.static_route.uid';
COMMENT ON COLUMN "jb"."static_trip"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联jb.static_shape.uid';
COMMENT ON COLUMN "jb"."static_trip"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "jb"."static_trip"."service_id" IS 'GTFS trips.txt提供的Service源标识';
COMMENT ON COLUMN "jb"."static_trip"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "jb"."static_trip"."trip_headsign" IS 'GTFS Trip目的地方向文字';
COMMENT ON COLUMN "jb"."static_trip"."trip_short_name" IS 'GTFS Trip短名称';
COMMENT ON COLUMN "jb"."static_trip"."direction_id" IS 'GTFS Trip方向ID原始数值';
COMMENT ON COLUMN "jb"."static_trip"."block_id" IS 'GTFS Trip所属车辆运行Block源标识';
COMMENT ON COLUMN "jb"."static_trip"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "jb"."static_trip"."wheelchair_accessible" IS 'GTFS Trip无障碍可达规则原始数值';
COMMENT ON COLUMN "jb"."static_trip"."bikes_allowed" IS 'GTFS Trip自行车携带规则原始数值';
COMMENT ON COLUMN "jb"."static_trip"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "jb"."static_trip"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."static_version"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."static_version"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "jb"."static_version"."source_request_uid" IS '下载当前Static ZIP的API请求UUID，关联jb.api_request_log.uid';
COMMENT ON COLUMN "jb"."static_version"."version_code" IS '项目或数据源提供的Static版本可读代码，同一Feed内非空值唯一';
COMMENT ON COLUMN "jb"."static_version"."static_sha256" IS '当前GTFS Static ZIP文件内容SHA-256摘要，用于Static版本唯一识别和重复内容去重';
COMMENT ON COLUMN "jb"."static_version"."static_object_path" IS '当前GTFS Static ZIP原始对象的实际存储路径';
COMMENT ON COLUMN "jb"."static_version"."zip_size_bytes" IS '当前GTFS Static ZIP文件大小，单位字节';
COMMENT ON COLUMN "jb"."static_version"."downloaded_at" IS '当前Static ZIP下载完成的绝对时间';
COMMENT ON COLUMN "jb"."static_version"."effective_from" IS '当前Static版本开始作为Realtime关联版本使用的绝对时间';
COMMENT ON COLUMN "jb"."static_version"."effective_to" IS '当前Static版本停止作为Realtime主要关联版本使用的绝对时间，当前版本允许为空';
COMMENT ON COLUMN "jb"."static_version"."is_current" IS '当前Static版本是否为所属Feed唯一的主要关联版本';
COMMENT ON COLUMN "jb"."static_version"."routes_file_present" IS 'Static ZIP中是否存在routes.txt';
COMMENT ON COLUMN "jb"."static_version"."trips_file_present" IS 'Static ZIP中是否存在trips.txt';
COMMENT ON COLUMN "jb"."static_version"."stops_file_present" IS 'Static ZIP中是否存在stops.txt';
COMMENT ON COLUMN "jb"."static_version"."stop_times_file_present" IS 'Static ZIP中是否存在stop_times.txt';
COMMENT ON COLUMN "jb"."static_version"."shapes_file_present" IS 'Static ZIP中是否存在shapes.txt';
COMMENT ON COLUMN "jb"."static_version"."calendar_file_present" IS 'Static ZIP中是否存在calendar.txt';
COMMENT ON COLUMN "jb"."static_version"."calendar_dates_file_present" IS 'Static ZIP中是否存在calendar_dates.txt';
COMMENT ON COLUMN "jb"."static_version"."frequencies_file_present" IS 'Static ZIP中是否存在frequencies.txt';
COMMENT ON COLUMN "jb"."static_version"."routes_count" IS '当前Static版本解析得到的Route记录数量';
COMMENT ON COLUMN "jb"."static_version"."trips_count" IS '当前Static版本解析得到的Trip记录数量';
COMMENT ON COLUMN "jb"."static_version"."stops_count" IS '当前Static版本解析得到的Stop记录数量';
COMMENT ON COLUMN "jb"."static_version"."stop_times_count" IS '当前Static版本解析得到的Stop Time记录数量';
COMMENT ON COLUMN "jb"."static_version"."shapes_count" IS '当前Static版本解析得到的唯一Shape数量';
COMMENT ON COLUMN "jb"."static_version"."shape_points_count" IS '当前Static版本解析得到的Shape Point记录数量';
COMMENT ON COLUMN "jb"."static_version"."notes" IS '当前Static版本的补充说明或异常说明';
COMMENT ON COLUMN "jb"."static_version"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."observation_uid" IS '关联的车辆历史观测UUID，关联jb.vehicle_observation.uid';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."last_seen_time" IS 'Collector最近一次在Realtime Feed看到该车辆的ingest绝对时间';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."vehicle_latest_state"."update_time" IS '当前Latest State记录最近一次UPSERT数据库的时间';
COMMENT ON COLUMN "jb"."vehicle_observation"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."vehicle_observation"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "jb"."vehicle_observation"."request_uid" IS '产生当前数据的API请求UUID，关联jb.api_request_log.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."snapshot_uid" IS '所属GTFS-Realtime快照UUID，关联jb.realtime_snapshot.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_version_uid" IS '当前车辆Realtime观测执行Static关联和数据补充时使用的GTFS Static版本UUID，正式关联jb.static_version.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_route_uid" IS '当前车辆Realtime观测最终匹配到的Static GTFS线路数据库UUID，正式关联jb.static_route.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_trip_uid" IS '当前车辆Realtime观测匹配到的Static GTFS Trip数据库UUID，正式关联jb.static_trip.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_shape_uid" IS '当前车辆Realtime观测通过Static Trip关联得到的Static GTFS Shape数据库UUID，正式关联jb.static_shape.uid';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_stop_uid" IS '当前车辆Realtime观测匹配到的Static GTFS站点数据库UUID，正式关联jb.static_stop.uid；Realtime未提供或无法匹配站点时允许为空';
COMMENT ON COLUMN "jb"."vehicle_observation"."entity_sequence" IS '当前FeedMessage实体列表中的0起始顺序，用于保证同一快照内Entity写入唯一性';
COMMENT ON COLUMN "jb"."vehicle_observation"."entity_id" IS '官方GTFS-Realtime FeedEntity原始ID';
COMMENT ON COLUMN "jb"."vehicle_observation"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "jb"."vehicle_observation"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "jb"."vehicle_observation"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "jb"."vehicle_observation"."wheelchair_accessible" IS '官方VehicleDescriptor无障碍可达状态原始枚举值';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_start_time_raw" IS '官方TripDescriptor start_time原始字符串，可保存超过24小时的GTFS时间';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_start_seconds" IS 'Trip开始时间换算为GTFS服务日起点后的秒数，可大于86400';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_start_date_raw" IS '官方TripDescriptor start_date原始YYYYMMDD字符串';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_start_date" IS '官方Trip开始服务日期的PostgreSQL DATE表示';
COMMENT ON COLUMN "jb"."vehicle_observation"."realtime_schedule_relationship" IS 'Realtime TripDescriptor调度关系原始枚举值';
COMMENT ON COLUMN "jb"."vehicle_observation"."realtime_route_id" IS 'Realtime消息直接提供的官方route_id';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_route_id" IS '通过Static GTFS匹配得到的官方route_id';
COMMENT ON COLUMN "jb"."vehicle_observation"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_type" IS 'Static GTFS route_type数值';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_color" IS 'Static GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_text_color" IS 'Static GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_resolution_method" IS '线路解析方法，例如DIRECT_REALTIME_ROUTE、TRIP_TO_STATIC_ROUTE或UNRESOLVED';
COMMENT ON COLUMN "jb"."vehicle_observation"."realtime_route_direct_matched" IS 'Realtime route_id是否直接匹配Static Route';
COMMENT ON COLUMN "jb"."vehicle_observation"."route_resolved" IS '当前观测是否成功解析到线路';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_service_id" IS 'Static GTFS Trip关联的service_id';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_headsign" IS 'Static GTFS Trip目的地方向文字';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_short_name" IS 'Static GTFS Trip短名称';
COMMENT ON COLUMN "jb"."vehicle_observation"."shape_id" IS '官方GTFS Shape业务标识';
COMMENT ON COLUMN "jb"."vehicle_observation"."trip_static_matched" IS '当前Realtime Trip是否匹配Static GTFS Trip';
COMMENT ON COLUMN "jb"."vehicle_observation"."realtime_direction_id" IS 'Realtime TripDescriptor提供的方向ID';
COMMENT ON COLUMN "jb"."vehicle_observation"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "jb"."vehicle_observation"."direction_comparison" IS 'Realtime与Static方向比较结果，例如MATCH、MISMATCH或NOT_COMPARABLE';
COMMENT ON COLUMN "jb"."vehicle_observation"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "jb"."vehicle_observation"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "jb"."vehicle_observation"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "jb"."vehicle_observation"."bearing" IS '官方Realtime Position方位角原始数值';
COMMENT ON COLUMN "jb"."vehicle_observation"."odometer_m" IS '官方Realtime Position里程表原始数值，单位按GTFS-Realtime规范解释';
COMMENT ON COLUMN "jb"."vehicle_observation"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "jb"."vehicle_observation"."source_speed_present" IS '官方Realtime Position是否显式提供speed字段';
COMMENT ON COLUMN "jb"."vehicle_observation"."source_speed_unit_declared" IS '数据源声明的速度单位';
COMMENT ON COLUMN "jb"."vehicle_observation"."source_speed_unit_interpretation" IS '项目对官方速度字段单位语义的解释说明';
COMMENT ON COLUMN "jb"."vehicle_observation"."current_stop_sequence" IS '官方VehiclePosition当前Stop在Trip中的顺序';
COMMENT ON COLUMN "jb"."vehicle_observation"."stop_id" IS '官方GTFS Stop业务标识';
COMMENT ON COLUMN "jb"."vehicle_observation"."current_status" IS '官方VehiclePosition当前车辆状态枚举值';
COMMENT ON COLUMN "jb"."vehicle_observation"."congestion_level" IS '官方VehiclePosition拥堵等级枚举值';
COMMENT ON COLUMN "jb"."vehicle_observation"."occupancy_status" IS '官方VehiclePosition载客状态枚举值';
COMMENT ON COLUMN "jb"."vehicle_observation"."occupancy_percentage" IS '官方VehiclePosition载客百分比原始值';
COMMENT ON COLUMN "jb"."vehicle_observation"."multi_carriage_details" IS '官方多车厢明细的完整JSON结构';
COMMENT ON COLUMN "jb"."vehicle_observation"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "jb"."vehicle_observation"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "jb"."vehicle_observation"."previous_vehicle_time" IS '计算连续移动指标时采用的上一条车辆观测绝对时间';
COMMENT ON COLUMN "jb"."vehicle_observation"."ingest_time" IS 'Java Collector接收并处理当前车辆观测的绝对时间';
COMMENT ON COLUMN "jb"."vehicle_observation"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "jb"."vehicle_observation"."vehicle_gap_seconds" IS '当前车辆观测与上一车辆观测时间差，Timestamp Regression情况下允许负值';
COMMENT ON COLUMN "jb"."vehicle_observation"."distance_from_previous_m" IS '当前有效GPS位置与上一有效位置之间的球面距离，单位米';
COMMENT ON COLUMN "jb"."vehicle_observation"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "jb"."vehicle_observation"."position_present" IS '官方Realtime是否提供位置对象';
COMMENT ON COLUMN "jb"."vehicle_observation"."position_wgs84_valid" IS '官方原始经纬度是否位于有效WGS84数值范围';
COMMENT ON COLUMN "jb"."vehicle_observation"."zero_zero_position" IS '官方原始位置是否为0,0坐标';
COMMENT ON COLUMN "jb"."vehicle_observation"."feed_bounds_valid" IS '官方原始位置是否位于当前Feed预期地理范围';
COMMENT ON COLUMN "jb"."vehicle_observation"."position_qc_status" IS '位置质量控制综合状态';
COMMENT ON COLUMN "jb"."vehicle_observation"."gps_jump_status" IS '连续车辆位置是否构成GPS跳点的质量状态';
COMMENT ON COLUMN "jb"."vehicle_observation"."jump_calculation_skipped_reason" IS '未执行GPS跳点计算的原因';
COMMENT ON COLUMN "jb"."vehicle_observation"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "jb"."vehicle_observation"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "jb"."vehicle_observation"."duplicate_observation" IS '当前车辆观测是否与之前出现的观测重复，重复Occurrence仍保留';
COMMENT ON COLUMN "jb"."vehicle_observation"."observation_key" IS '用于识别跨快照重复车辆观测的业务摘要键，只建立普通索引且不唯一';
COMMENT ON COLUMN "jb"."vehicle_observation"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "jb"."vehicle_observation"."realtime_entity" IS '当前VehiclePosition对应完整FeedEntity解析JSON，用于保存结构化字段之外的原始解析证据';
COMMENT ON COLUMN "jb"."vehicle_observation"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."observation_uid" IS '关联的车辆历史观测UUID，关联jb.vehicle_observation.uid';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."qc_code" IS '质量控制标记的稳定业务代码';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."qc_category" IS '质量控制标记所属类别';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."severity" IS '质量控制问题严重程度';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."qc_value_numeric" IS '质量控制标记对应的数值证据';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."qc_value_text" IS '质量控制标记对应的文本证据';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."details" IS '质量控制标记的扩展JSON明细';
COMMENT ON COLUMN "jb"."vehicle_observation_qc"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."api_request_log"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."api_request_log"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kl"."api_request_log"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "kl"."api_request_log"."duplicate_of_request_uid" IS '重复响应所引用的原始API请求UUID，关联kl.api_request_log.uid';
COMMENT ON COLUMN "kl"."api_request_log"."request_type" IS '请求类型，例如REALTIME或STATIC';
COMMENT ON COLUMN "kl"."api_request_log"."request_sequence" IS '本次运行中的请求顺序编号';
COMMENT ON COLUMN "kl"."api_request_log"."cycle_number" IS '本次运行中的采集周期编号';
COMMENT ON COLUMN "kl"."api_request_log"."requested_url" IS '请求发起时使用的原始URL';
COMMENT ON COLUMN "kl"."api_request_log"."final_url" IS '完成重定向后实际访问的最终URL';
COMMENT ON COLUMN "kl"."api_request_log"."scheduled_at" IS '调度器计划发起请求的绝对时间';
COMMENT ON COLUMN "kl"."api_request_log"."request_started_at" IS 'HTTP请求实际开始的绝对时间';
COMMENT ON COLUMN "kl"."api_request_log"."response_received_at" IS 'HTTP响应接收完成的绝对时间';
COMMENT ON COLUMN "kl"."api_request_log"."latency_ms" IS 'HTTP请求从开始到收到响应的耗时毫秒数';
COMMENT ON COLUMN "kl"."api_request_log"."scheduler_drift_ms" IS '实际请求开始时间相对计划时间的调度偏移毫秒数';
COMMENT ON COLUMN "kl"."api_request_log"."http_status" IS 'HTTP响应状态码，网络异常或超时时允许为空';
COMMENT ON COLUMN "kl"."api_request_log"."content_type" IS 'HTTP响应Content-Type原始值';
COMMENT ON COLUMN "kl"."api_request_log"."response_bytes" IS 'HTTP响应正文的字节数';
COMMENT ON COLUMN "kl"."api_request_log"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "kl"."api_request_log"."redirect_count" IS '本次HTTP请求经历的重定向次数';
COMMENT ON COLUMN "kl"."api_request_log"."parse_status" IS '响应正文的解析状态';
COMMENT ON COLUMN "kl"."api_request_log"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "kl"."api_request_log"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "kl"."api_request_log"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "kl"."api_request_log"."raw_object_path" IS 'Realtime原始protobuf对象的文件存储路径';
COMMENT ON COLUMN "kl"."api_request_log"."parsed_json_path" IS '完整解析JSON文件的存储路径';
COMMENT ON COLUMN "kl"."api_request_log"."enriched_json_path" IS 'Static GTFS补充后JSON文件的存储路径';
COMMENT ON COLUMN "kl"."api_request_log"."result" IS '本次API请求的最终结果状态';
COMMENT ON COLUMN "kl"."api_request_log"."error_class" IS '请求或解析失败对应的异常类名称';
COMMENT ON COLUMN "kl"."api_request_log"."error_message" IS '请求、响应或解析失败的详细错误信息';
COMMENT ON COLUMN "kl"."api_request_log"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."realtime_snapshot"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."realtime_snapshot"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kl"."realtime_snapshot"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "kl"."realtime_snapshot"."request_uid" IS '产生当前数据的API请求UUID，关联kl.api_request_log.uid';
COMMENT ON COLUMN "kl"."realtime_snapshot"."enrichment_static_version_uid" IS '当前GTFS-Realtime快照执行Static补充和线路解析时使用的GTFS Static版本UUID，正式关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."realtime_snapshot"."batch_id" IS '当前Realtime快照所属批次业务标识';
COMMENT ON COLUMN "kl"."realtime_snapshot"."gtfs_realtime_version" IS 'GTFS-Realtime FeedHeader声明的规范版本';
COMMENT ON COLUMN "kl"."realtime_snapshot"."incrementality" IS 'GTFS-Realtime FeedHeader声明的增量模式';
COMMENT ON COLUMN "kl"."realtime_snapshot"."feed_timestamp_raw" IS 'FeedHeader提供的原始Unix秒时间戳';
COMMENT ON COLUMN "kl"."realtime_snapshot"."feed_time" IS 'FeedHeader原始时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "kl"."realtime_snapshot"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."unique_route_count" IS '当前快照解析得到的唯一线路数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."route_direct_match_count" IS '通过Realtime route_id直接匹配Static Route的数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."route_trip_fallback_match_count" IS '通过Static Trip回退匹配Route的数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."route_resolved_count" IS '成功解析到线路的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."route_unresolved_count" IS '未能解析线路的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."trip_match_count" IS '成功匹配Static Trip的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."trip_unmatched_count" IS '未匹配Static Trip的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."direction_match_count" IS 'Realtime与Static方向一致的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."direction_mismatch_count" IS 'Realtime与Static方向不一致的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."direction_not_comparable_count" IS '缺少必要字段而无法比较方向的Entity数量';
COMMENT ON COLUMN "kl"."realtime_snapshot"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "kl"."realtime_snapshot"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "kl"."realtime_snapshot"."referenced_snapshot_uid" IS '重复快照所引用的原始Realtime快照UUID';
COMMENT ON COLUMN "kl"."realtime_snapshot"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_route"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_route"."static_version_uid" IS '所属GTFS Static版本UUID，关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."static_route"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "kl"."static_route"."agency_id" IS 'GTFS routes.txt提供的官方Agency源标识';
COMMENT ON COLUMN "kl"."static_route"."route_short_name" IS 'GTFS线路短名称';
COMMENT ON COLUMN "kl"."static_route"."route_long_name" IS 'GTFS线路完整名称';
COMMENT ON COLUMN "kl"."static_route"."route_desc" IS 'GTFS线路补充描述';
COMMENT ON COLUMN "kl"."static_route"."route_type" IS 'GTFS线路交通方式类型数值';
COMMENT ON COLUMN "kl"."static_route"."route_url" IS 'GTFS线路信息网页URL';
COMMENT ON COLUMN "kl"."static_route"."route_color" IS 'GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "kl"."static_route"."route_text_color" IS 'GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "kl"."static_route"."route_sort_order" IS 'GTFS线路展示排序值';
COMMENT ON COLUMN "kl"."static_route"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "kl"."static_route"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "kl"."static_route"."network_id" IS 'GTFS线路所属网络源标识';
COMMENT ON COLUMN "kl"."static_route"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kl"."static_route"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_shape"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_shape"."static_version_uid" IS '所属GTFS Static版本UUID，关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."static_shape"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "kl"."static_shape"."is_referenced" IS '当前Shape是否被本Static版本trips.txt中的至少一个Trip实际引用';
COMMENT ON COLUMN "kl"."static_shape"."trip_count_using_shape" IS '当前Static版本中引用该Shape的Trip数量';
COMMENT ON COLUMN "kl"."static_shape"."point_count" IS '当前Shape包含的Shape Point数量';
COMMENT ON COLUMN "kl"."static_shape"."geom" IS '当前Shape按shape_pt_sequence构造的PostGIS LineString对象';
COMMENT ON COLUMN "kl"."static_shape"."shape_length_m" IS '根据当前Shape有效点序列计算得到的几何长度，单位米，异常长度仍保留';
COMMENT ON COLUMN "kl"."static_shape"."shape_length_km" IS '根据当前Shape有效点序列计算得到的几何长度，单位km，异常长度仍保留';
COMMENT ON COLUMN "kl"."static_shape"."analysis_eligible" IS '当前Shape是否推荐进入正常线路长度和空间分析，不控制原始Shape数据保存';
COMMENT ON COLUMN "kl"."static_shape"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kl"."static_shape"."qc_summary" IS '当前Shape质量控制结果的扩展JSON汇总';
COMMENT ON COLUMN "kl"."static_shape"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_shape_point"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_shape_point"."static_version_uid" IS '所属GTFS Static版本UUID，关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."static_shape_point"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联kl.static_shape.uid';
COMMENT ON COLUMN "kl"."static_shape_point"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "kl"."static_shape_point"."shape_pt_lat" IS 'GTFS shapes.txt提供的原始Shape Point纬度，异常值仍保留';
COMMENT ON COLUMN "kl"."static_shape_point"."shape_pt_lon" IS 'GTFS shapes.txt提供的原始Shape Point经度，异常值仍保留';
COMMENT ON COLUMN "kl"."static_shape_point"."geom" IS '有效WGS84且非0,0 Shape Point对应的PostGIS Point对象';
COMMENT ON COLUMN "kl"."static_shape_point"."shape_pt_sequence" IS 'GTFS shapes.txt中当前Shape Point的原始点序号';
COMMENT ON COLUMN "kl"."static_shape_point"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "kl"."static_shape_point"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "kl"."static_shape_point"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "kl"."static_shape_point"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "kl"."static_shape_point"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kl"."static_shape_point"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kl"."static_shape_point"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_stop"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_stop"."static_version_uid" IS '所属GTFS Static版本UUID，关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."static_stop"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "kl"."static_stop"."stop_code" IS 'GTFS站点面向乘客的短代码';
COMMENT ON COLUMN "kl"."static_stop"."stop_name" IS 'GTFS站点名称';
COMMENT ON COLUMN "kl"."static_stop"."tts_stop_name" IS 'GTFS站点文本转语音名称';
COMMENT ON COLUMN "kl"."static_stop"."stop_desc" IS 'GTFS站点补充描述';
COMMENT ON COLUMN "kl"."static_stop"."stop_lat" IS 'GTFS stops.txt原始纬度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "kl"."static_stop"."stop_lon" IS 'GTFS stops.txt原始经度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "kl"."static_stop"."geom" IS '由有效WGS84且非0,0站点坐标生成的PostGIS Point对象';
COMMENT ON COLUMN "kl"."static_stop"."zone_id" IS 'GTFS站点所属票价分区源标识';
COMMENT ON COLUMN "kl"."static_stop"."stop_url" IS 'GTFS站点信息网页URL';
COMMENT ON COLUMN "kl"."static_stop"."location_type" IS 'GTFS站点位置类型原始数值';
COMMENT ON COLUMN "kl"."static_stop"."parent_station" IS 'GTFS站点所属父站点的源stop_id';
COMMENT ON COLUMN "kl"."static_stop"."stop_timezone" IS 'GTFS站点时区IANA名称';
COMMENT ON COLUMN "kl"."static_stop"."wheelchair_boarding" IS 'GTFS站点无障碍上车规则原始数值';
COMMENT ON COLUMN "kl"."static_stop"."level_id" IS 'GTFS站点所属楼层源标识';
COMMENT ON COLUMN "kl"."static_stop"."platform_code" IS 'GTFS站点月台代码';
COMMENT ON COLUMN "kl"."static_stop"."position_present" IS 'GTFS原始站点行是否提供经纬度';
COMMENT ON COLUMN "kl"."static_stop"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "kl"."static_stop"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "kl"."static_stop"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "kl"."static_stop"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kl"."static_stop"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kl"."static_stop"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_stop_time"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_stop_time"."static_version_uid" IS '所属GTFS Static版本UUID，关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."static_stop_time"."trip_uid" IS '当前Stop Time关联的数据库Static Trip UUID，关联kl.static_trip.uid';
COMMENT ON COLUMN "kl"."static_stop_time"."stop_uid" IS '当前Stop Time关联的数据库Static Stop UUID，关联kl.static_stop.uid';
COMMENT ON COLUMN "kl"."static_stop_time"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "kl"."static_stop_time"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "kl"."static_stop_time"."stop_sequence" IS 'GTFS stop_times.txt中当前Stop在Trip内的原始顺序';
COMMENT ON COLUMN "kl"."static_stop_time"."arrival_time_raw" IS 'GTFS stop_times.txt原始到站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "kl"."static_stop_time"."arrival_seconds" IS '原始GTFS到站时间换算为从service day开始累计的秒数，用于跨24小时计算';
COMMENT ON COLUMN "kl"."static_stop_time"."departure_time_raw" IS 'GTFS stop_times.txt原始离站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "kl"."static_stop_time"."departure_seconds" IS '原始GTFS离站时间换算为从service day开始累计的秒数';
COMMENT ON COLUMN "kl"."static_stop_time"."stop_headsign" IS 'GTFS Stop Time在当前站点显示的目的地方向文字';
COMMENT ON COLUMN "kl"."static_stop_time"."pickup_type" IS 'GTFS站点上客规则原始数值';
COMMENT ON COLUMN "kl"."static_stop_time"."drop_off_type" IS 'GTFS站点下客规则原始数值';
COMMENT ON COLUMN "kl"."static_stop_time"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "kl"."static_stop_time"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "kl"."static_stop_time"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "kl"."static_stop_time"."timepoint" IS 'GTFS Stop Time是否为精确时刻点的原始数值';
COMMENT ON COLUMN "kl"."static_stop_time"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kl"."static_stop_time"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_trip"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_trip"."static_version_uid" IS '所属GTFS Static版本UUID，关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."static_trip"."route_uid" IS '当前Trip关联的数据库Static Route UUID，关联kl.static_route.uid';
COMMENT ON COLUMN "kl"."static_trip"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联kl.static_shape.uid';
COMMENT ON COLUMN "kl"."static_trip"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "kl"."static_trip"."service_id" IS 'GTFS trips.txt提供的Service源标识';
COMMENT ON COLUMN "kl"."static_trip"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "kl"."static_trip"."trip_headsign" IS 'GTFS Trip目的地方向文字';
COMMENT ON COLUMN "kl"."static_trip"."trip_short_name" IS 'GTFS Trip短名称';
COMMENT ON COLUMN "kl"."static_trip"."direction_id" IS 'GTFS Trip方向ID原始数值';
COMMENT ON COLUMN "kl"."static_trip"."block_id" IS 'GTFS Trip所属车辆运行Block源标识';
COMMENT ON COLUMN "kl"."static_trip"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "kl"."static_trip"."wheelchair_accessible" IS 'GTFS Trip无障碍可达规则原始数值';
COMMENT ON COLUMN "kl"."static_trip"."bikes_allowed" IS 'GTFS Trip自行车携带规则原始数值';
COMMENT ON COLUMN "kl"."static_trip"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kl"."static_trip"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."static_version"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."static_version"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kl"."static_version"."source_request_uid" IS '下载当前Static ZIP的API请求UUID，关联kl.api_request_log.uid';
COMMENT ON COLUMN "kl"."static_version"."version_code" IS '项目或数据源提供的Static版本可读代码，同一Feed内非空值唯一';
COMMENT ON COLUMN "kl"."static_version"."static_sha256" IS '当前GTFS Static ZIP文件内容SHA-256摘要，用于Static版本唯一识别和重复内容去重';
COMMENT ON COLUMN "kl"."static_version"."static_object_path" IS '当前GTFS Static ZIP原始对象的实际存储路径';
COMMENT ON COLUMN "kl"."static_version"."zip_size_bytes" IS '当前GTFS Static ZIP文件大小，单位字节';
COMMENT ON COLUMN "kl"."static_version"."downloaded_at" IS '当前Static ZIP下载完成的绝对时间';
COMMENT ON COLUMN "kl"."static_version"."effective_from" IS '当前Static版本开始作为Realtime关联版本使用的绝对时间';
COMMENT ON COLUMN "kl"."static_version"."effective_to" IS '当前Static版本停止作为Realtime主要关联版本使用的绝对时间，当前版本允许为空';
COMMENT ON COLUMN "kl"."static_version"."is_current" IS '当前Static版本是否为所属Feed唯一的主要关联版本';
COMMENT ON COLUMN "kl"."static_version"."routes_file_present" IS 'Static ZIP中是否存在routes.txt';
COMMENT ON COLUMN "kl"."static_version"."trips_file_present" IS 'Static ZIP中是否存在trips.txt';
COMMENT ON COLUMN "kl"."static_version"."stops_file_present" IS 'Static ZIP中是否存在stops.txt';
COMMENT ON COLUMN "kl"."static_version"."stop_times_file_present" IS 'Static ZIP中是否存在stop_times.txt';
COMMENT ON COLUMN "kl"."static_version"."shapes_file_present" IS 'Static ZIP中是否存在shapes.txt';
COMMENT ON COLUMN "kl"."static_version"."calendar_file_present" IS 'Static ZIP中是否存在calendar.txt';
COMMENT ON COLUMN "kl"."static_version"."calendar_dates_file_present" IS 'Static ZIP中是否存在calendar_dates.txt';
COMMENT ON COLUMN "kl"."static_version"."frequencies_file_present" IS 'Static ZIP中是否存在frequencies.txt';
COMMENT ON COLUMN "kl"."static_version"."routes_count" IS '当前Static版本解析得到的Route记录数量';
COMMENT ON COLUMN "kl"."static_version"."trips_count" IS '当前Static版本解析得到的Trip记录数量';
COMMENT ON COLUMN "kl"."static_version"."stops_count" IS '当前Static版本解析得到的Stop记录数量';
COMMENT ON COLUMN "kl"."static_version"."stop_times_count" IS '当前Static版本解析得到的Stop Time记录数量';
COMMENT ON COLUMN "kl"."static_version"."shapes_count" IS '当前Static版本解析得到的唯一Shape数量';
COMMENT ON COLUMN "kl"."static_version"."shape_points_count" IS '当前Static版本解析得到的Shape Point记录数量';
COMMENT ON COLUMN "kl"."static_version"."notes" IS '当前Static版本的补充说明或异常说明';
COMMENT ON COLUMN "kl"."static_version"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."observation_uid" IS '关联的车辆历史观测UUID，关联kl.vehicle_observation.uid';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."last_seen_time" IS 'Collector最近一次在Realtime Feed看到该车辆的ingest绝对时间';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."vehicle_latest_state"."update_time" IS '当前Latest State记录最近一次UPSERT数据库的时间';
COMMENT ON COLUMN "kl"."vehicle_observation"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."vehicle_observation"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "kl"."vehicle_observation"."request_uid" IS '产生当前数据的API请求UUID，关联kl.api_request_log.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."snapshot_uid" IS '所属GTFS-Realtime快照UUID，关联kl.realtime_snapshot.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_version_uid" IS '当前车辆Realtime观测执行Static关联和数据补充时使用的GTFS Static版本UUID，正式关联kl.static_version.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_route_uid" IS '当前车辆Realtime观测最终匹配到的Static GTFS线路数据库UUID，正式关联kl.static_route.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_trip_uid" IS '当前车辆Realtime观测匹配到的Static GTFS Trip数据库UUID，正式关联kl.static_trip.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_shape_uid" IS '当前车辆Realtime观测通过Static Trip关联得到的Static GTFS Shape数据库UUID，正式关联kl.static_shape.uid';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_stop_uid" IS '当前车辆Realtime观测匹配到的Static GTFS站点数据库UUID，正式关联kl.static_stop.uid；Realtime未提供或无法匹配站点时允许为空';
COMMENT ON COLUMN "kl"."vehicle_observation"."entity_sequence" IS '当前FeedMessage实体列表中的0起始顺序，用于保证同一快照内Entity写入唯一性';
COMMENT ON COLUMN "kl"."vehicle_observation"."entity_id" IS '官方GTFS-Realtime FeedEntity原始ID';
COMMENT ON COLUMN "kl"."vehicle_observation"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "kl"."vehicle_observation"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "kl"."vehicle_observation"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "kl"."vehicle_observation"."wheelchair_accessible" IS '官方VehicleDescriptor无障碍可达状态原始枚举值';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_start_time_raw" IS '官方TripDescriptor start_time原始字符串，可保存超过24小时的GTFS时间';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_start_seconds" IS 'Trip开始时间换算为GTFS服务日起点后的秒数，可大于86400';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_start_date_raw" IS '官方TripDescriptor start_date原始YYYYMMDD字符串';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_start_date" IS '官方Trip开始服务日期的PostgreSQL DATE表示';
COMMENT ON COLUMN "kl"."vehicle_observation"."realtime_schedule_relationship" IS 'Realtime TripDescriptor调度关系原始枚举值';
COMMENT ON COLUMN "kl"."vehicle_observation"."realtime_route_id" IS 'Realtime消息直接提供的官方route_id';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_route_id" IS '通过Static GTFS匹配得到的官方route_id';
COMMENT ON COLUMN "kl"."vehicle_observation"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_type" IS 'Static GTFS route_type数值';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_color" IS 'Static GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_text_color" IS 'Static GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_resolution_method" IS '线路解析方法，例如DIRECT_REALTIME_ROUTE、TRIP_TO_STATIC_ROUTE或UNRESOLVED';
COMMENT ON COLUMN "kl"."vehicle_observation"."realtime_route_direct_matched" IS 'Realtime route_id是否直接匹配Static Route';
COMMENT ON COLUMN "kl"."vehicle_observation"."route_resolved" IS '当前观测是否成功解析到线路';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_service_id" IS 'Static GTFS Trip关联的service_id';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_headsign" IS 'Static GTFS Trip目的地方向文字';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_short_name" IS 'Static GTFS Trip短名称';
COMMENT ON COLUMN "kl"."vehicle_observation"."shape_id" IS '官方GTFS Shape业务标识';
COMMENT ON COLUMN "kl"."vehicle_observation"."trip_static_matched" IS '当前Realtime Trip是否匹配Static GTFS Trip';
COMMENT ON COLUMN "kl"."vehicle_observation"."realtime_direction_id" IS 'Realtime TripDescriptor提供的方向ID';
COMMENT ON COLUMN "kl"."vehicle_observation"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "kl"."vehicle_observation"."direction_comparison" IS 'Realtime与Static方向比较结果，例如MATCH、MISMATCH或NOT_COMPARABLE';
COMMENT ON COLUMN "kl"."vehicle_observation"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kl"."vehicle_observation"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kl"."vehicle_observation"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "kl"."vehicle_observation"."bearing" IS '官方Realtime Position方位角原始数值';
COMMENT ON COLUMN "kl"."vehicle_observation"."odometer_m" IS '官方Realtime Position里程表原始数值，单位按GTFS-Realtime规范解释';
COMMENT ON COLUMN "kl"."vehicle_observation"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "kl"."vehicle_observation"."source_speed_present" IS '官方Realtime Position是否显式提供speed字段';
COMMENT ON COLUMN "kl"."vehicle_observation"."source_speed_unit_declared" IS '数据源声明的速度单位';
COMMENT ON COLUMN "kl"."vehicle_observation"."source_speed_unit_interpretation" IS '项目对官方速度字段单位语义的解释说明';
COMMENT ON COLUMN "kl"."vehicle_observation"."current_stop_sequence" IS '官方VehiclePosition当前Stop在Trip中的顺序';
COMMENT ON COLUMN "kl"."vehicle_observation"."stop_id" IS '官方GTFS Stop业务标识';
COMMENT ON COLUMN "kl"."vehicle_observation"."current_status" IS '官方VehiclePosition当前车辆状态枚举值';
COMMENT ON COLUMN "kl"."vehicle_observation"."congestion_level" IS '官方VehiclePosition拥堵等级枚举值';
COMMENT ON COLUMN "kl"."vehicle_observation"."occupancy_status" IS '官方VehiclePosition载客状态枚举值';
COMMENT ON COLUMN "kl"."vehicle_observation"."occupancy_percentage" IS '官方VehiclePosition载客百分比原始值';
COMMENT ON COLUMN "kl"."vehicle_observation"."multi_carriage_details" IS '官方多车厢明细的完整JSON结构';
COMMENT ON COLUMN "kl"."vehicle_observation"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "kl"."vehicle_observation"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "kl"."vehicle_observation"."previous_vehicle_time" IS '计算连续移动指标时采用的上一条车辆观测绝对时间';
COMMENT ON COLUMN "kl"."vehicle_observation"."ingest_time" IS 'Java Collector接收并处理当前车辆观测的绝对时间';
COMMENT ON COLUMN "kl"."vehicle_observation"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "kl"."vehicle_observation"."vehicle_gap_seconds" IS '当前车辆观测与上一车辆观测时间差，Timestamp Regression情况下允许负值';
COMMENT ON COLUMN "kl"."vehicle_observation"."distance_from_previous_m" IS '当前有效GPS位置与上一有效位置之间的球面距离，单位米';
COMMENT ON COLUMN "kl"."vehicle_observation"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "kl"."vehicle_observation"."position_present" IS '官方Realtime是否提供位置对象';
COMMENT ON COLUMN "kl"."vehicle_observation"."position_wgs84_valid" IS '官方原始经纬度是否位于有效WGS84数值范围';
COMMENT ON COLUMN "kl"."vehicle_observation"."zero_zero_position" IS '官方原始位置是否为0,0坐标';
COMMENT ON COLUMN "kl"."vehicle_observation"."feed_bounds_valid" IS '官方原始位置是否位于当前Feed预期地理范围';
COMMENT ON COLUMN "kl"."vehicle_observation"."position_qc_status" IS '位置质量控制综合状态';
COMMENT ON COLUMN "kl"."vehicle_observation"."gps_jump_status" IS '连续车辆位置是否构成GPS跳点的质量状态';
COMMENT ON COLUMN "kl"."vehicle_observation"."jump_calculation_skipped_reason" IS '未执行GPS跳点计算的原因';
COMMENT ON COLUMN "kl"."vehicle_observation"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "kl"."vehicle_observation"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "kl"."vehicle_observation"."duplicate_observation" IS '当前车辆观测是否与之前出现的观测重复，重复Occurrence仍保留';
COMMENT ON COLUMN "kl"."vehicle_observation"."observation_key" IS '用于识别跨快照重复车辆观测的业务摘要键，只建立普通索引且不唯一';
COMMENT ON COLUMN "kl"."vehicle_observation"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kl"."vehicle_observation"."realtime_entity" IS '当前VehiclePosition对应完整FeedEntity解析JSON，用于保存结构化字段之外的原始解析证据';
COMMENT ON COLUMN "kl"."vehicle_observation"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."observation_uid" IS '关联的车辆历史观测UUID，关联kl.vehicle_observation.uid';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."qc_code" IS '质量控制标记的稳定业务代码';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."qc_category" IS '质量控制标记所属类别';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."severity" IS '质量控制问题严重程度';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."qc_value_numeric" IS '质量控制标记对应的数值证据';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."qc_value_text" IS '质量控制标记对应的文本证据';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."details" IS '质量控制标记的扩展JSON明细';
COMMENT ON COLUMN "kl"."vehicle_observation_qc"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."api_request_log"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."api_request_log"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kuching"."api_request_log"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "kuching"."api_request_log"."duplicate_of_request_uid" IS '重复响应所引用的原始API请求UUID，关联kuching.api_request_log.uid';
COMMENT ON COLUMN "kuching"."api_request_log"."request_type" IS '请求类型，例如REALTIME或STATIC';
COMMENT ON COLUMN "kuching"."api_request_log"."request_sequence" IS '本次运行中的请求顺序编号';
COMMENT ON COLUMN "kuching"."api_request_log"."cycle_number" IS '本次运行中的采集周期编号';
COMMENT ON COLUMN "kuching"."api_request_log"."requested_url" IS '请求发起时使用的原始URL';
COMMENT ON COLUMN "kuching"."api_request_log"."final_url" IS '完成重定向后实际访问的最终URL';
COMMENT ON COLUMN "kuching"."api_request_log"."scheduled_at" IS '调度器计划发起请求的绝对时间';
COMMENT ON COLUMN "kuching"."api_request_log"."request_started_at" IS 'HTTP请求实际开始的绝对时间';
COMMENT ON COLUMN "kuching"."api_request_log"."response_received_at" IS 'HTTP响应接收完成的绝对时间';
COMMENT ON COLUMN "kuching"."api_request_log"."latency_ms" IS 'HTTP请求从开始到收到响应的耗时毫秒数';
COMMENT ON COLUMN "kuching"."api_request_log"."scheduler_drift_ms" IS '实际请求开始时间相对计划时间的调度偏移毫秒数';
COMMENT ON COLUMN "kuching"."api_request_log"."http_status" IS 'HTTP响应状态码，网络异常或超时时允许为空';
COMMENT ON COLUMN "kuching"."api_request_log"."content_type" IS 'HTTP响应Content-Type原始值';
COMMENT ON COLUMN "kuching"."api_request_log"."response_bytes" IS 'HTTP响应正文的字节数';
COMMENT ON COLUMN "kuching"."api_request_log"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "kuching"."api_request_log"."redirect_count" IS '本次HTTP请求经历的重定向次数';
COMMENT ON COLUMN "kuching"."api_request_log"."parse_status" IS '响应正文的解析状态';
COMMENT ON COLUMN "kuching"."api_request_log"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "kuching"."api_request_log"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "kuching"."api_request_log"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "kuching"."api_request_log"."raw_object_path" IS 'Realtime原始protobuf对象的文件存储路径';
COMMENT ON COLUMN "kuching"."api_request_log"."parsed_json_path" IS '完整解析JSON文件的存储路径';
COMMENT ON COLUMN "kuching"."api_request_log"."enriched_json_path" IS 'Static GTFS补充后JSON文件的存储路径';
COMMENT ON COLUMN "kuching"."api_request_log"."result" IS '本次API请求的最终结果状态';
COMMENT ON COLUMN "kuching"."api_request_log"."error_class" IS '请求或解析失败对应的异常类名称';
COMMENT ON COLUMN "kuching"."api_request_log"."error_message" IS '请求、响应或解析失败的详细错误信息';
COMMENT ON COLUMN "kuching"."api_request_log"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."request_uid" IS '产生当前数据的API请求UUID，关联kuching.api_request_log.uid';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."enrichment_static_version_uid" IS '当前GTFS-Realtime快照执行Static补充和线路解析时使用的GTFS Static版本UUID，正式关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."batch_id" IS '当前Realtime快照所属批次业务标识';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."gtfs_realtime_version" IS 'GTFS-Realtime FeedHeader声明的规范版本';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."incrementality" IS 'GTFS-Realtime FeedHeader声明的增量模式';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."feed_timestamp_raw" IS 'FeedHeader提供的原始Unix秒时间戳';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."feed_time" IS 'FeedHeader原始时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."unique_route_count" IS '当前快照解析得到的唯一线路数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."route_direct_match_count" IS '通过Realtime route_id直接匹配Static Route的数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."route_trip_fallback_match_count" IS '通过Static Trip回退匹配Route的数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."route_resolved_count" IS '成功解析到线路的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."route_unresolved_count" IS '未能解析线路的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."trip_match_count" IS '成功匹配Static Trip的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."trip_unmatched_count" IS '未匹配Static Trip的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."direction_match_count" IS 'Realtime与Static方向一致的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."direction_mismatch_count" IS 'Realtime与Static方向不一致的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."direction_not_comparable_count" IS '缺少必要字段而无法比较方向的Entity数量';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."referenced_snapshot_uid" IS '重复快照所引用的原始Realtime快照UUID';
COMMENT ON COLUMN "kuching"."realtime_snapshot"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_route"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_route"."static_version_uid" IS '所属GTFS Static版本UUID，关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."static_route"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "kuching"."static_route"."agency_id" IS 'GTFS routes.txt提供的官方Agency源标识';
COMMENT ON COLUMN "kuching"."static_route"."route_short_name" IS 'GTFS线路短名称';
COMMENT ON COLUMN "kuching"."static_route"."route_long_name" IS 'GTFS线路完整名称';
COMMENT ON COLUMN "kuching"."static_route"."route_desc" IS 'GTFS线路补充描述';
COMMENT ON COLUMN "kuching"."static_route"."route_type" IS 'GTFS线路交通方式类型数值';
COMMENT ON COLUMN "kuching"."static_route"."route_url" IS 'GTFS线路信息网页URL';
COMMENT ON COLUMN "kuching"."static_route"."route_color" IS 'GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "kuching"."static_route"."route_text_color" IS 'GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "kuching"."static_route"."route_sort_order" IS 'GTFS线路展示排序值';
COMMENT ON COLUMN "kuching"."static_route"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "kuching"."static_route"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "kuching"."static_route"."network_id" IS 'GTFS线路所属网络源标识';
COMMENT ON COLUMN "kuching"."static_route"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kuching"."static_route"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_shape"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_shape"."static_version_uid" IS '所属GTFS Static版本UUID，关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."static_shape"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "kuching"."static_shape"."is_referenced" IS '当前Shape是否被本Static版本trips.txt中的至少一个Trip实际引用';
COMMENT ON COLUMN "kuching"."static_shape"."trip_count_using_shape" IS '当前Static版本中引用该Shape的Trip数量';
COMMENT ON COLUMN "kuching"."static_shape"."point_count" IS '当前Shape包含的Shape Point数量';
COMMENT ON COLUMN "kuching"."static_shape"."geom" IS '当前Shape按shape_pt_sequence构造的PostGIS LineString对象';
COMMENT ON COLUMN "kuching"."static_shape"."shape_length_m" IS '根据当前Shape有效点序列计算得到的几何长度，单位米，异常长度仍保留';
COMMENT ON COLUMN "kuching"."static_shape"."shape_length_km" IS '根据当前Shape有效点序列计算得到的几何长度，单位km，异常长度仍保留';
COMMENT ON COLUMN "kuching"."static_shape"."analysis_eligible" IS '当前Shape是否推荐进入正常线路长度和空间分析，不控制原始Shape数据保存';
COMMENT ON COLUMN "kuching"."static_shape"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kuching"."static_shape"."qc_summary" IS '当前Shape质量控制结果的扩展JSON汇总';
COMMENT ON COLUMN "kuching"."static_shape"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_shape_point"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_shape_point"."static_version_uid" IS '所属GTFS Static版本UUID，关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."static_shape_point"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联kuching.static_shape.uid';
COMMENT ON COLUMN "kuching"."static_shape_point"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "kuching"."static_shape_point"."shape_pt_lat" IS 'GTFS shapes.txt提供的原始Shape Point纬度，异常值仍保留';
COMMENT ON COLUMN "kuching"."static_shape_point"."shape_pt_lon" IS 'GTFS shapes.txt提供的原始Shape Point经度，异常值仍保留';
COMMENT ON COLUMN "kuching"."static_shape_point"."geom" IS '有效WGS84且非0,0 Shape Point对应的PostGIS Point对象';
COMMENT ON COLUMN "kuching"."static_shape_point"."shape_pt_sequence" IS 'GTFS shapes.txt中当前Shape Point的原始点序号';
COMMENT ON COLUMN "kuching"."static_shape_point"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "kuching"."static_shape_point"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "kuching"."static_shape_point"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "kuching"."static_shape_point"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "kuching"."static_shape_point"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kuching"."static_shape_point"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kuching"."static_shape_point"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_stop"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_stop"."static_version_uid" IS '所属GTFS Static版本UUID，关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."static_stop"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "kuching"."static_stop"."stop_code" IS 'GTFS站点面向乘客的短代码';
COMMENT ON COLUMN "kuching"."static_stop"."stop_name" IS 'GTFS站点名称';
COMMENT ON COLUMN "kuching"."static_stop"."tts_stop_name" IS 'GTFS站点文本转语音名称';
COMMENT ON COLUMN "kuching"."static_stop"."stop_desc" IS 'GTFS站点补充描述';
COMMENT ON COLUMN "kuching"."static_stop"."stop_lat" IS 'GTFS stops.txt原始纬度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "kuching"."static_stop"."stop_lon" IS 'GTFS stops.txt原始经度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "kuching"."static_stop"."geom" IS '由有效WGS84且非0,0站点坐标生成的PostGIS Point对象';
COMMENT ON COLUMN "kuching"."static_stop"."zone_id" IS 'GTFS站点所属票价分区源标识';
COMMENT ON COLUMN "kuching"."static_stop"."stop_url" IS 'GTFS站点信息网页URL';
COMMENT ON COLUMN "kuching"."static_stop"."location_type" IS 'GTFS站点位置类型原始数值';
COMMENT ON COLUMN "kuching"."static_stop"."parent_station" IS 'GTFS站点所属父站点的源stop_id';
COMMENT ON COLUMN "kuching"."static_stop"."stop_timezone" IS 'GTFS站点时区IANA名称';
COMMENT ON COLUMN "kuching"."static_stop"."wheelchair_boarding" IS 'GTFS站点无障碍上车规则原始数值';
COMMENT ON COLUMN "kuching"."static_stop"."level_id" IS 'GTFS站点所属楼层源标识';
COMMENT ON COLUMN "kuching"."static_stop"."platform_code" IS 'GTFS站点月台代码';
COMMENT ON COLUMN "kuching"."static_stop"."position_present" IS 'GTFS原始站点行是否提供经纬度';
COMMENT ON COLUMN "kuching"."static_stop"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "kuching"."static_stop"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "kuching"."static_stop"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "kuching"."static_stop"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kuching"."static_stop"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kuching"."static_stop"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_stop_time"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_stop_time"."static_version_uid" IS '所属GTFS Static版本UUID，关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."static_stop_time"."trip_uid" IS '当前Stop Time关联的数据库Static Trip UUID，关联kuching.static_trip.uid';
COMMENT ON COLUMN "kuching"."static_stop_time"."stop_uid" IS '当前Stop Time关联的数据库Static Stop UUID，关联kuching.static_stop.uid';
COMMENT ON COLUMN "kuching"."static_stop_time"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "kuching"."static_stop_time"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "kuching"."static_stop_time"."stop_sequence" IS 'GTFS stop_times.txt中当前Stop在Trip内的原始顺序';
COMMENT ON COLUMN "kuching"."static_stop_time"."arrival_time_raw" IS 'GTFS stop_times.txt原始到站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "kuching"."static_stop_time"."arrival_seconds" IS '原始GTFS到站时间换算为从service day开始累计的秒数，用于跨24小时计算';
COMMENT ON COLUMN "kuching"."static_stop_time"."departure_time_raw" IS 'GTFS stop_times.txt原始离站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "kuching"."static_stop_time"."departure_seconds" IS '原始GTFS离站时间换算为从service day开始累计的秒数';
COMMENT ON COLUMN "kuching"."static_stop_time"."stop_headsign" IS 'GTFS Stop Time在当前站点显示的目的地方向文字';
COMMENT ON COLUMN "kuching"."static_stop_time"."pickup_type" IS 'GTFS站点上客规则原始数值';
COMMENT ON COLUMN "kuching"."static_stop_time"."drop_off_type" IS 'GTFS站点下客规则原始数值';
COMMENT ON COLUMN "kuching"."static_stop_time"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "kuching"."static_stop_time"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "kuching"."static_stop_time"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "kuching"."static_stop_time"."timepoint" IS 'GTFS Stop Time是否为精确时刻点的原始数值';
COMMENT ON COLUMN "kuching"."static_stop_time"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kuching"."static_stop_time"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_trip"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_trip"."static_version_uid" IS '所属GTFS Static版本UUID，关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."static_trip"."route_uid" IS '当前Trip关联的数据库Static Route UUID，关联kuching.static_route.uid';
COMMENT ON COLUMN "kuching"."static_trip"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联kuching.static_shape.uid';
COMMENT ON COLUMN "kuching"."static_trip"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "kuching"."static_trip"."service_id" IS 'GTFS trips.txt提供的Service源标识';
COMMENT ON COLUMN "kuching"."static_trip"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "kuching"."static_trip"."trip_headsign" IS 'GTFS Trip目的地方向文字';
COMMENT ON COLUMN "kuching"."static_trip"."trip_short_name" IS 'GTFS Trip短名称';
COMMENT ON COLUMN "kuching"."static_trip"."direction_id" IS 'GTFS Trip方向ID原始数值';
COMMENT ON COLUMN "kuching"."static_trip"."block_id" IS 'GTFS Trip所属车辆运行Block源标识';
COMMENT ON COLUMN "kuching"."static_trip"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "kuching"."static_trip"."wheelchair_accessible" IS 'GTFS Trip无障碍可达规则原始数值';
COMMENT ON COLUMN "kuching"."static_trip"."bikes_allowed" IS 'GTFS Trip自行车携带规则原始数值';
COMMENT ON COLUMN "kuching"."static_trip"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "kuching"."static_trip"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."static_version"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."static_version"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kuching"."static_version"."source_request_uid" IS '下载当前Static ZIP的API请求UUID，关联kuching.api_request_log.uid';
COMMENT ON COLUMN "kuching"."static_version"."version_code" IS '项目或数据源提供的Static版本可读代码，同一Feed内非空值唯一';
COMMENT ON COLUMN "kuching"."static_version"."static_sha256" IS '当前GTFS Static ZIP文件内容SHA-256摘要，用于Static版本唯一识别和重复内容去重';
COMMENT ON COLUMN "kuching"."static_version"."static_object_path" IS '当前GTFS Static ZIP原始对象的实际存储路径';
COMMENT ON COLUMN "kuching"."static_version"."zip_size_bytes" IS '当前GTFS Static ZIP文件大小，单位字节';
COMMENT ON COLUMN "kuching"."static_version"."downloaded_at" IS '当前Static ZIP下载完成的绝对时间';
COMMENT ON COLUMN "kuching"."static_version"."effective_from" IS '当前Static版本开始作为Realtime关联版本使用的绝对时间';
COMMENT ON COLUMN "kuching"."static_version"."effective_to" IS '当前Static版本停止作为Realtime主要关联版本使用的绝对时间，当前版本允许为空';
COMMENT ON COLUMN "kuching"."static_version"."is_current" IS '当前Static版本是否为所属Feed唯一的主要关联版本';
COMMENT ON COLUMN "kuching"."static_version"."routes_file_present" IS 'Static ZIP中是否存在routes.txt';
COMMENT ON COLUMN "kuching"."static_version"."trips_file_present" IS 'Static ZIP中是否存在trips.txt';
COMMENT ON COLUMN "kuching"."static_version"."stops_file_present" IS 'Static ZIP中是否存在stops.txt';
COMMENT ON COLUMN "kuching"."static_version"."stop_times_file_present" IS 'Static ZIP中是否存在stop_times.txt';
COMMENT ON COLUMN "kuching"."static_version"."shapes_file_present" IS 'Static ZIP中是否存在shapes.txt';
COMMENT ON COLUMN "kuching"."static_version"."calendar_file_present" IS 'Static ZIP中是否存在calendar.txt';
COMMENT ON COLUMN "kuching"."static_version"."calendar_dates_file_present" IS 'Static ZIP中是否存在calendar_dates.txt';
COMMENT ON COLUMN "kuching"."static_version"."frequencies_file_present" IS 'Static ZIP中是否存在frequencies.txt';
COMMENT ON COLUMN "kuching"."static_version"."routes_count" IS '当前Static版本解析得到的Route记录数量';
COMMENT ON COLUMN "kuching"."static_version"."trips_count" IS '当前Static版本解析得到的Trip记录数量';
COMMENT ON COLUMN "kuching"."static_version"."stops_count" IS '当前Static版本解析得到的Stop记录数量';
COMMENT ON COLUMN "kuching"."static_version"."stop_times_count" IS '当前Static版本解析得到的Stop Time记录数量';
COMMENT ON COLUMN "kuching"."static_version"."shapes_count" IS '当前Static版本解析得到的唯一Shape数量';
COMMENT ON COLUMN "kuching"."static_version"."shape_points_count" IS '当前Static版本解析得到的Shape Point记录数量';
COMMENT ON COLUMN "kuching"."static_version"."notes" IS '当前Static版本的补充说明或异常说明';
COMMENT ON COLUMN "kuching"."static_version"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."observation_uid" IS '关联的车辆历史观测UUID，关联kuching.vehicle_observation.uid';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."last_seen_time" IS 'Collector最近一次在Realtime Feed看到该车辆的ingest绝对时间';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."vehicle_latest_state"."update_time" IS '当前Latest State记录最近一次UPSERT数据库的时间';
COMMENT ON COLUMN "kuching"."vehicle_observation"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."vehicle_observation"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "kuching"."vehicle_observation"."request_uid" IS '产生当前数据的API请求UUID，关联kuching.api_request_log.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."snapshot_uid" IS '所属GTFS-Realtime快照UUID，关联kuching.realtime_snapshot.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_version_uid" IS '当前车辆Realtime观测执行Static关联和数据补充时使用的GTFS Static版本UUID，正式关联kuching.static_version.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_route_uid" IS '当前车辆Realtime观测最终匹配到的Static GTFS线路数据库UUID，正式关联kuching.static_route.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_trip_uid" IS '当前车辆Realtime观测匹配到的Static GTFS Trip数据库UUID，正式关联kuching.static_trip.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_shape_uid" IS '当前车辆Realtime观测通过Static Trip关联得到的Static GTFS Shape数据库UUID，正式关联kuching.static_shape.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_stop_uid" IS '当前车辆Realtime观测匹配到的Static GTFS站点数据库UUID，正式关联kuching.static_stop.uid；Realtime未提供或无法匹配站点时允许为空';
COMMENT ON COLUMN "kuching"."vehicle_observation"."entity_sequence" IS '当前FeedMessage实体列表中的0起始顺序，用于保证同一快照内Entity写入唯一性';
COMMENT ON COLUMN "kuching"."vehicle_observation"."entity_id" IS '官方GTFS-Realtime FeedEntity原始ID';
COMMENT ON COLUMN "kuching"."vehicle_observation"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "kuching"."vehicle_observation"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "kuching"."vehicle_observation"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "kuching"."vehicle_observation"."wheelchair_accessible" IS '官方VehicleDescriptor无障碍可达状态原始枚举值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_start_time_raw" IS '官方TripDescriptor start_time原始字符串，可保存超过24小时的GTFS时间';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_start_seconds" IS 'Trip开始时间换算为GTFS服务日起点后的秒数，可大于86400';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_start_date_raw" IS '官方TripDescriptor start_date原始YYYYMMDD字符串';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_start_date" IS '官方Trip开始服务日期的PostgreSQL DATE表示';
COMMENT ON COLUMN "kuching"."vehicle_observation"."realtime_schedule_relationship" IS 'Realtime TripDescriptor调度关系原始枚举值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."realtime_route_id" IS 'Realtime消息直接提供的官方route_id';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_route_id" IS '通过Static GTFS匹配得到的官方route_id';
COMMENT ON COLUMN "kuching"."vehicle_observation"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_type" IS 'Static GTFS route_type数值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_color" IS 'Static GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_text_color" IS 'Static GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_resolution_method" IS '线路解析方法，例如DIRECT_REALTIME_ROUTE、TRIP_TO_STATIC_ROUTE或UNRESOLVED';
COMMENT ON COLUMN "kuching"."vehicle_observation"."realtime_route_direct_matched" IS 'Realtime route_id是否直接匹配Static Route';
COMMENT ON COLUMN "kuching"."vehicle_observation"."route_resolved" IS '当前观测是否成功解析到线路';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_service_id" IS 'Static GTFS Trip关联的service_id';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_headsign" IS 'Static GTFS Trip目的地方向文字';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_short_name" IS 'Static GTFS Trip短名称';
COMMENT ON COLUMN "kuching"."vehicle_observation"."shape_id" IS '官方GTFS Shape业务标识';
COMMENT ON COLUMN "kuching"."vehicle_observation"."trip_static_matched" IS '当前Realtime Trip是否匹配Static GTFS Trip';
COMMENT ON COLUMN "kuching"."vehicle_observation"."realtime_direction_id" IS 'Realtime TripDescriptor提供的方向ID';
COMMENT ON COLUMN "kuching"."vehicle_observation"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "kuching"."vehicle_observation"."direction_comparison" IS 'Realtime与Static方向比较结果，例如MATCH、MISMATCH或NOT_COMPARABLE';
COMMENT ON COLUMN "kuching"."vehicle_observation"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kuching"."vehicle_observation"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "kuching"."vehicle_observation"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "kuching"."vehicle_observation"."bearing" IS '官方Realtime Position方位角原始数值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."odometer_m" IS '官方Realtime Position里程表原始数值，单位按GTFS-Realtime规范解释';
COMMENT ON COLUMN "kuching"."vehicle_observation"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "kuching"."vehicle_observation"."source_speed_present" IS '官方Realtime Position是否显式提供speed字段';
COMMENT ON COLUMN "kuching"."vehicle_observation"."source_speed_unit_declared" IS '数据源声明的速度单位';
COMMENT ON COLUMN "kuching"."vehicle_observation"."source_speed_unit_interpretation" IS '项目对官方速度字段单位语义的解释说明';
COMMENT ON COLUMN "kuching"."vehicle_observation"."current_stop_sequence" IS '官方VehiclePosition当前Stop在Trip中的顺序';
COMMENT ON COLUMN "kuching"."vehicle_observation"."stop_id" IS '官方GTFS Stop业务标识';
COMMENT ON COLUMN "kuching"."vehicle_observation"."current_status" IS '官方VehiclePosition当前车辆状态枚举值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."congestion_level" IS '官方VehiclePosition拥堵等级枚举值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."occupancy_status" IS '官方VehiclePosition载客状态枚举值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."occupancy_percentage" IS '官方VehiclePosition载客百分比原始值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."multi_carriage_details" IS '官方多车厢明细的完整JSON结构';
COMMENT ON COLUMN "kuching"."vehicle_observation"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "kuching"."vehicle_observation"."previous_vehicle_time" IS '计算连续移动指标时采用的上一条车辆观测绝对时间';
COMMENT ON COLUMN "kuching"."vehicle_observation"."ingest_time" IS 'Java Collector接收并处理当前车辆观测的绝对时间';
COMMENT ON COLUMN "kuching"."vehicle_observation"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."vehicle_gap_seconds" IS '当前车辆观测与上一车辆观测时间差，Timestamp Regression情况下允许负值';
COMMENT ON COLUMN "kuching"."vehicle_observation"."distance_from_previous_m" IS '当前有效GPS位置与上一有效位置之间的球面距离，单位米';
COMMENT ON COLUMN "kuching"."vehicle_observation"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "kuching"."vehicle_observation"."position_present" IS '官方Realtime是否提供位置对象';
COMMENT ON COLUMN "kuching"."vehicle_observation"."position_wgs84_valid" IS '官方原始经纬度是否位于有效WGS84数值范围';
COMMENT ON COLUMN "kuching"."vehicle_observation"."zero_zero_position" IS '官方原始位置是否为0,0坐标';
COMMENT ON COLUMN "kuching"."vehicle_observation"."feed_bounds_valid" IS '官方原始位置是否位于当前Feed预期地理范围';
COMMENT ON COLUMN "kuching"."vehicle_observation"."position_qc_status" IS '位置质量控制综合状态';
COMMENT ON COLUMN "kuching"."vehicle_observation"."gps_jump_status" IS '连续车辆位置是否构成GPS跳点的质量状态';
COMMENT ON COLUMN "kuching"."vehicle_observation"."jump_calculation_skipped_reason" IS '未执行GPS跳点计算的原因';
COMMENT ON COLUMN "kuching"."vehicle_observation"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "kuching"."vehicle_observation"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "kuching"."vehicle_observation"."duplicate_observation" IS '当前车辆观测是否与之前出现的观测重复，重复Occurrence仍保留';
COMMENT ON COLUMN "kuching"."vehicle_observation"."observation_key" IS '用于识别跨快照重复车辆观测的业务摘要键，只建立普通索引且不唯一';
COMMENT ON COLUMN "kuching"."vehicle_observation"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "kuching"."vehicle_observation"."realtime_entity" IS '当前VehiclePosition对应完整FeedEntity解析JSON，用于保存结构化字段之外的原始解析证据';
COMMENT ON COLUMN "kuching"."vehicle_observation"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."observation_uid" IS '关联的车辆历史观测UUID，关联kuching.vehicle_observation.uid';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."qc_code" IS '质量控制标记的稳定业务代码';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."qc_category" IS '质量控制标记所属类别';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."severity" IS '质量控制问题严重程度';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."qc_value_numeric" IS '质量控制标记对应的数值证据';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."qc_value_text" IS '质量控制标记对应的文本证据';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."details" IS '质量控制标记的扩展JSON明细';
COMMENT ON COLUMN "kuching"."vehicle_observation_qc"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "lta"."api_endpoint"."uid" IS '本表UUID主键，由PostgreSQL自动生成，供其他表通过UUID建立关联。';
COMMENT ON COLUMN "lta"."api_endpoint"."api_code" IS '项目内部接口编号，例如API01；属于项目业务编号，不作为数据库主键。';
COMMENT ON COLUMN "lta"."api_endpoint"."api_name" IS 'LTA DataMall接口名称，用于任务识别与审计展示。';
COMMENT ON COLUMN "lta"."api_endpoint"."endpoint_url" IS 'LTA DataMall接口完整请求URL，用于发起数据采集请求。';
COMMENT ON COLUMN "lta"."api_endpoint"."schedule_type" IS '采集调度类型，例如MINUTES、HOURLY、DAILY或MONTH_END。';
COMMENT ON COLUMN "lta"."api_endpoint"."interval_minutes" IS '固定分钟采集周期，仅分钟或小时型任务使用。';
COMMENT ON COLUMN "lta"."api_endpoint"."schedule_rule" IS '特殊调度规则，例如每月最后一天LAST_DAY_OF_MONTH。';
COMMENT ON COLUMN "lta"."api_endpoint"."raw_save_enabled" IS '是否将LTA接口原始响应作为外部Raw文件永久保存。';
COMMENT ON COLUMN "lta"."api_endpoint"."db_write_enabled" IS '是否将清洗后的LTA业务数据写入PostgreSQL。';
COMMENT ON COLUMN "lta"."api_endpoint"."enabled" IS '当前LTA接口采集任务是否启用。';
COMMENT ON COLUMN "lta"."api_endpoint"."description" IS '接口用途、研究价值及其他补充说明。';
COMMENT ON COLUMN "lta"."api_endpoint"."create_time" IS '本条接口配置首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."api_endpoint"."update_time" IS '本条接口配置最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."collection_artifact"."uid" IS '本条采集文件登记UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."collection_artifact"."run_uid" IS '关联lta.collection_run.uid的UUID外键，表示生成该文件的采集任务。';
COMMENT ON COLUMN "lta"."collection_artifact"."artifact_type" IS '文件用途类型：RAW_JSON、DATA_FILE或SUMMARY。';
COMMENT ON COLUMN "lta"."collection_artifact"."file_name" IS '采集产物文件名称。';
COMMENT ON COLUMN "lta"."collection_artifact"."file_path" IS '采集产物在外部文件系统中的完整或受控相对路径。';
COMMENT ON COLUMN "lta"."collection_artifact"."file_size_bytes" IS '采集产物文件大小，单位为字节。';
COMMENT ON COLUMN "lta"."collection_artifact"."sha256" IS '采集产物内容SHA256校验值，用于完整性审计。';
COMMENT ON COLUMN "lta"."collection_artifact"."compression_type" IS '采集产物压缩类型，未压缩时为NONE。';
COMMENT ON COLUMN "lta"."collection_artifact"."create_time" IS '本条文件登记首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."collection_artifact"."update_time" IS '本条文件登记最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."collection_page_log"."uid" IS '本条分页审计记录UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."collection_page_log"."run_uid" IS '关联lta.collection_run.uid的UUID外键，对应本轮采集主任务。';
COMMENT ON COLUMN "lta"."collection_page_log"."page_no" IS '本轮采集内部分页序号，从1开始。';
COMMENT ON COLUMN "lta"."collection_page_log"."skip_value" IS '发送给LTA DataMall分页请求的$skip值。';
COMMENT ON COLUMN "lta"."collection_page_log"."request_start_time" IS '本页实际HTTP请求开始时间。';
COMMENT ON COLUMN "lta"."collection_page_log"."request_end_time" IS '本页实际HTTP请求结束时间。';
COMMENT ON COLUMN "lta"."collection_page_log"."source_updated_time" IS '本页LTA响应声明的源数据更新时间。';
COMMENT ON COLUMN "lta"."collection_page_log"."http_status" IS '本页LTA HTTP响应状态码。';
COMMENT ON COLUMN "lta"."collection_page_log"."record_count" IS '本页LTA响应包含的业务记录数。';
COMMENT ON COLUMN "lta"."collection_page_log"."response_bytes" IS '本页HTTP响应体字节数。';
COMMENT ON COLUMN "lta"."collection_page_log"."success" IS '本页请求及解析是否成功。';
COMMENT ON COLUMN "lta"."collection_page_log"."error_message" IS '本页失败时的错误摘要，不包含密钥。';
COMMENT ON COLUMN "lta"."collection_page_log"."create_time" IS '本条分页日志首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."collection_page_log"."update_time" IS '本条分页日志最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."collection_run"."uid" IS '本次采集任务UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."collection_run"."api_endpoint_uid" IS '关联lta.api_endpoint.uid的UUID外键，用于确定本次任务所属接口。';
COMMENT ON COLUMN "lta"."collection_run"."run_mode" IS '任务运行模式，允许SCHEDULED、MANUAL_TEST或BACKFILL。';
COMMENT ON COLUMN "lta"."collection_run"."scheduled_time" IS '系统计划执行本轮采集的时间，不是实际HTTP请求时间。';
COMMENT ON COLUMN "lta"."collection_run"."attempt_no" IS '本轮任务请求尝试序号，从1开始。';
COMMENT ON COLUMN "lta"."collection_run"."request_start_time" IS '本轮实际HTTP请求开始时间。';
COMMENT ON COLUMN "lta"."collection_run"."request_end_time" IS '本轮实际HTTP请求结束时间。';
COMMENT ON COLUMN "lta"."collection_run"."source_updated_time" IS 'LTA响应声明的源数据更新时间。';
COMMENT ON COLUMN "lta"."collection_run"."http_status" IS 'LTA HTTP响应状态码，用于请求审计。';
COMMENT ON COLUMN "lta"."collection_run"."page_count" IS '本轮成功或尝试获取的分页总数。';
COMMENT ON COLUMN "lta"."collection_run"."record_count" IS '本轮采集获得的LTA业务记录总数。';
COMMENT ON COLUMN "lta"."collection_run"."response_bytes" IS '本轮HTTP响应体总字节数。';
COMMENT ON COLUMN "lta"."collection_run"."success" IS '本轮接口采集与数据库处理是否成功。';
COMMENT ON COLUMN "lta"."collection_run"."snapshot_complete" IS '本轮数据是否形成完整科研快照。';
COMMENT ON COLUMN "lta"."collection_run"."consistency_status" IS '多页源时间一致性状态，例如SINGLE、MIXED或NA。';
COMMENT ON COLUMN "lta"."collection_run"."retry_count" IS '本轮任务发生的重试次数。';
COMMENT ON COLUMN "lta"."collection_run"."error_message" IS '本轮失败时的错误摘要，不记录API密钥等敏感信息。';
COMMENT ON COLUMN "lta"."collection_run"."create_time" IS '本条采集任务日志首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."collection_run"."update_time" IS '本条采集任务日志最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."uid" IS '故障交通灯事件UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."alarm_id" IS 'LTA源系统AlarmID故障业务编号，不作为数据库主键或外键。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."node_id" IS 'LTA源系统NodeID节点业务编号，不作为数据库主键或外键。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."fault_type" IS 'LTA返回的故障类型代码，不限制为当前已知代码。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."source_start_time" IS 'LTA源系统提供的故障开始时间。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."source_end_time" IS 'LTA源系统提供的故障结束时间。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."message" IS 'LTA返回的故障描述文本。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."area_uid" IS '可空UUID外键，关联lta.study_area.uid表示CIQ研究区域。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."first_seen_time" IS '系统第一次采集到该故障事件的时间。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."last_seen_time" IS '系统最近一次仍看到该故障事件的时间。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."resolved_time" IS '故障事件从接口消失或确认解决的时间。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."active" IS '该故障事件当前是否仍处于活动状态。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."first_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录首次发现任务。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."last_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录最近发现任务。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."create_time" IS '本条故障事件首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."faulty_traffic_light_event"."update_time" IS '本条故障事件最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."road_opening_event"."uid" IS '计划道路开放事件UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."road_opening_event"."event_id" IS 'LTA源系统道路开放EventID业务编号，不作为数据库主键或外键。';
COMMENT ON COLUMN "lta"."road_opening_event"."start_date" IS 'LTA源数据提供的计划道路开放开始日期，仅表示日期。';
COMMENT ON COLUMN "lta"."road_opening_event"."end_date" IS 'LTA源数据提供的计划道路开放结束日期，仅表示日期。';
COMMENT ON COLUMN "lta"."road_opening_event"."svc_dept" IS 'LTA返回的道路开放负责服务部门。';
COMMENT ON COLUMN "lta"."road_opening_event"."road_name" IS 'LTA返回的计划开放道路名称。';
COMMENT ON COLUMN "lta"."road_opening_event"."other" IS 'LTA返回的计划道路开放其他说明。';
COMMENT ON COLUMN "lta"."road_opening_event"."area_uid" IS '可空UUID外键，关联lta.study_area.uid表示CIQ研究区域。';
COMMENT ON COLUMN "lta"."road_opening_event"."first_seen_time" IS '系统第一次采集到该道路开放事件的时间。';
COMMENT ON COLUMN "lta"."road_opening_event"."last_seen_time" IS '系统最近一次仍看到该道路开放事件的时间。';
COMMENT ON COLUMN "lta"."road_opening_event"."resolved_time" IS '道路开放事件从接口消失或确认结束的时间。';
COMMENT ON COLUMN "lta"."road_opening_event"."active" IS '该计划道路开放事件当前是否仍处于活动状态。';
COMMENT ON COLUMN "lta"."road_opening_event"."first_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录首次发现任务。';
COMMENT ON COLUMN "lta"."road_opening_event"."last_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录最近发现任务。';
COMMENT ON COLUMN "lta"."road_opening_event"."create_time" IS '本条道路开放事件首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."road_opening_event"."update_time" IS '本条道路开放事件最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."road_work_event"."uid" IS '道路施工事件UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."road_work_event"."event_id" IS 'LTA源系统道路施工EventID业务编号，不作为数据库主键或外键。';
COMMENT ON COLUMN "lta"."road_work_event"."start_date" IS 'LTA源数据提供的施工开始日期，仅表示日期。';
COMMENT ON COLUMN "lta"."road_work_event"."end_date" IS 'LTA源数据提供的施工结束日期，仅表示日期。';
COMMENT ON COLUMN "lta"."road_work_event"."svc_dept" IS 'LTA返回的施工负责服务部门。';
COMMENT ON COLUMN "lta"."road_work_event"."road_name" IS 'LTA返回的施工道路名称。';
COMMENT ON COLUMN "lta"."road_work_event"."other" IS 'LTA返回的道路施工其他说明。';
COMMENT ON COLUMN "lta"."road_work_event"."area_uid" IS '可空UUID外键，关联lta.study_area.uid表示CIQ研究区域。';
COMMENT ON COLUMN "lta"."road_work_event"."first_seen_time" IS '系统第一次采集到该施工事件的时间。';
COMMENT ON COLUMN "lta"."road_work_event"."last_seen_time" IS '系统最近一次仍看到该施工事件的时间。';
COMMENT ON COLUMN "lta"."road_work_event"."resolved_time" IS '施工事件从接口消失或确认结束的时间。';
COMMENT ON COLUMN "lta"."road_work_event"."active" IS '该道路施工事件当前是否仍处于活动状态。';
COMMENT ON COLUMN "lta"."road_work_event"."first_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录首次发现任务。';
COMMENT ON COLUMN "lta"."road_work_event"."last_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录最近发现任务。';
COMMENT ON COLUMN "lta"."road_work_event"."create_time" IS '本条道路施工事件首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."road_work_event"."update_time" IS '本条道路施工事件最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."study_area"."uid" IS '研究区域UUID主键，由PostgreSQL自动生成，供业务表以UUID关联。';
COMMENT ON COLUMN "lta"."study_area"."ciq_code" IS 'CIQ关卡代码，允许WOODLANDS或TUAS。';
COMMENT ON COLUMN "lta"."study_area"."zone_code" IS '研究分区代码，允许CORE、APPROACH或CORRIDOR。';
COMMENT ON COLUMN "lta"."study_area"."area_name" IS '经研究人员确认的研究区域名称。';
COMMENT ON COLUMN "lta"."study_area"."corridor_name" IS '研究区域所属交通走廊名称。';
COMMENT ON COLUMN "lta"."study_area"."geom" IS '研究区域WGS84多面空间几何，PostGIS MULTIPOLYGON，SRID 4326。';
COMMENT ON COLUMN "lta"."study_area"."active" IS '该研究区域当前是否参与空间筛选。';
COMMENT ON COLUMN "lta"."study_area"."create_time" IS '本条研究区域首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."study_area"."update_time" IS '本条研究区域最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."uid" IS '交通流量文件登记UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."check_month" IS '月度检查所属月份，统一存该月第一天，例如2026-08-01。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."run_uid" IS '关联lta.collection_run.uid的UUID外键，表示获取和下载该文件的任务。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."download_url" IS 'LTA Traffic Flow接口返回的临时实际文件下载URL。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."file_name" IS '已下载交通流量文件名称。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."file_path" IS '已下载交通流量文件在外部文件系统中的路径。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."file_size_bytes" IS '已下载交通流量文件大小，单位字节。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."sha256" IS '已下载文件内容SHA256校验值，用于版本与完整性审计。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."is_new_version" IS '本月下载文件相对历史登记是否为新版本。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."source_period" IS 'LTA下载文件声明的数据覆盖时期。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."downloaded_time" IS '实际完成文件下载的时间。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."parse_status" IS '文件解析状态：NOT_PARSED、PARSED或FAILED。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."parse_message" IS '文件解析结果摘要或失败原因。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."create_time" IS '本条文件登记首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."traffic_flow_file"."update_time" IS '本条文件登记最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."uid" IS '交通事件数据库UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."event_fingerprint" IS '由标准化类型、坐标和消息计算的SHA256业务指纹，不作为数据库主键。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."type" IS 'LTA返回的交通事件类型。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."latitude" IS 'LTA返回的事件纬度，WGS84，NUMERIC(20,16)。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."longitude" IS 'LTA返回的事件经度，WGS84，NUMERIC(20,16)。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."geom" IS '按经度在前、纬度在后构造的事件WGS84 POINT，SRID 4326。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."message" IS 'LTA返回的交通事件描述文本。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."area_uid" IS '可空UUID外键，关联lta.study_area.uid表示CIQ研究区域。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."first_seen_time" IS '系统第一次从LTA采集到该事件状态的时间。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."last_seen_time" IS '系统最近一次仍从LTA看到该事件状态的时间。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."resolved_time" IS '事件从LTA接口消失或确认结束的时间。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."active" IS '该交通事件当前是否仍处于活动状态。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."first_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录首次发现该事件的任务。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."last_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录最近看到该事件的任务。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."create_time" IS '本条交通事件首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."traffic_incident_event"."update_time" IS '本条交通事件最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."traffic_link"."uid" IS '道路Link数据库UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."traffic_link"."link_id" IS 'LTA TrafficSpeedBands返回的LinkID源系统业务编号，不作为数据库主键或外键。';
COMMENT ON COLUMN "lta"."traffic_link"."road_name" IS 'LTA返回的道路名称，用于道路检索与研究分组。';
COMMENT ON COLUMN "lta"."traffic_link"."road_category" IS 'LTA返回的道路类别代码。';
COMMENT ON COLUMN "lta"."traffic_link"."start_lon" IS 'LTA道路起点经度，WGS84，NUMERIC(20,16)，用于构造线几何。';
COMMENT ON COLUMN "lta"."traffic_link"."start_lat" IS 'LTA道路起点纬度，WGS84，NUMERIC(20,16)，用于构造线几何。';
COMMENT ON COLUMN "lta"."traffic_link"."end_lon" IS 'LTA道路终点经度，WGS84，NUMERIC(20,16)，用于构造线几何。';
COMMENT ON COLUMN "lta"."traffic_link"."end_lat" IS 'LTA道路终点纬度，WGS84，NUMERIC(20,16)，用于构造线几何。';
COMMENT ON COLUMN "lta"."traffic_link"."geom" IS '由起点至终点按经度在前、纬度在后构造的WGS84 LINESTRING，SRID 4326。';
COMMENT ON COLUMN "lta"."traffic_link"."first_seen_time" IS '系统第一次从LTA采集到该道路Link的源观察时间。';
COMMENT ON COLUMN "lta"."traffic_link"."last_seen_time" IS '系统最近一次仍从LTA看到该道路Link的源观察时间。';
COMMENT ON COLUMN "lta"."traffic_link"."active" IS '该道路Link当前是否仍在LTA源数据中有效。';
COMMENT ON COLUMN "lta"."traffic_link"."create_time" IS '本条道路Link首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."traffic_link"."update_time" IS '本条道路Link最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."uid" IS '道路与研究区域关联记录UUID主键。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."link_uid" IS '关联lta.traffic_link.uid的UUID外键。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."area_uid" IS '关联lta.study_area.uid的UUID外键。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."match_method" IS '关联匹配方法：SPATIAL、MANUAL或ROAD_NAME。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."is_primary" IS '该研究区域是否为此道路Link的主要归属区域。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."active" IS '该白名单关联当前是否生效。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."create_time" IS '本条白名单关联首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."traffic_link_scope"."update_time" IS '本条白名单关联最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."uid" IS '道路速度观测UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."snapshot_time" IS '系统标准化后的本轮道路速度快照时间。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."link_uid" IS '关联lta.traffic_link.uid的UUID外键，标识被观测道路Link。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."speed_band" IS 'LTA TrafficSpeedBands返回的速度等级，范围1至8。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."minimum_speed" IS 'LTA返回的该速度等级最小速度，单位按官方接口定义。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."maximum_speed" IS 'LTA返回的该速度等级最大速度，单位按官方接口定义。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."source_updated_time" IS 'LTA源数据更新时间，不等同于入库时间。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."run_uid" IS '关联lta.collection_run.uid的UUID外键，用于追溯本次采集任务。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."create_time" IS '本条速度观测首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."traffic_speed_observation"."update_time" IS '本条速度观测最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."travel_time_observation"."uid" IS '旅行时间观测UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."travel_time_observation"."snapshot_time" IS '系统标准化后的本轮旅行时间快照时间。';
COMMENT ON COLUMN "lta"."travel_time_observation"."segment_uid" IS '关联lta.travel_time_segment.uid的UUID外键。';
COMMENT ON COLUMN "lta"."travel_time_observation"."est_time_min" IS 'LTA返回的预计旅行时间，单位为分钟。';
COMMENT ON COLUMN "lta"."travel_time_observation"."run_uid" IS '关联lta.collection_run.uid的UUID外键，用于追溯采集任务。';
COMMENT ON COLUMN "lta"."travel_time_observation"."create_time" IS '本条旅行时间观测首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."travel_time_observation"."update_time" IS '本条旅行时间观测最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."travel_time_segment"."uid" IS '旅行时间分段UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."travel_time_segment"."name" IS 'LTA返回的高速公路名称或简称。';
COMMENT ON COLUMN "lta"."travel_time_segment"."direction" IS 'LTA返回的高速公路行驶方向代码。';
COMMENT ON COLUMN "lta"."travel_time_segment"."far_end_point" IS 'LTA返回的该方向远端终点文字。';
COMMENT ON COLUMN "lta"."travel_time_segment"."start_point" IS 'LTA返回的旅行时间分段起点文字。';
COMMENT ON COLUMN "lta"."travel_time_segment"."end_point" IS 'LTA返回的旅行时间分段终点文字。';
COMMENT ON COLUMN "lta"."travel_time_segment"."area_uid" IS '可空UUID外键，关联lta.study_area.uid表示CIQ研究区域归属。';
COMMENT ON COLUMN "lta"."travel_time_segment"."active" IS '该旅行时间分段当前是否仍在LTA源数据中有效。';
COMMENT ON COLUMN "lta"."travel_time_segment"."first_seen_time" IS '系统第一次从LTA采集到该分段的源观察时间。';
COMMENT ON COLUMN "lta"."travel_time_segment"."last_seen_time" IS '系统最近一次仍从LTA看到该分段的源观察时间。';
COMMENT ON COLUMN "lta"."travel_time_segment"."create_time" IS '本条分段首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."travel_time_segment"."update_time" IS '本条分段最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."vms_equipment"."uid" IS 'VMS设备数据库UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."vms_equipment"."equipment_id" IS 'LTA源系统EquipmentID设备业务编号，不作为数据库主键或外键。';
COMMENT ON COLUMN "lta"."vms_equipment"."latitude" IS 'LTA返回的VMS设备纬度，WGS84，NUMERIC(20,16)。';
COMMENT ON COLUMN "lta"."vms_equipment"."longitude" IS 'LTA返回的VMS设备经度，WGS84，NUMERIC(20,16)。';
COMMENT ON COLUMN "lta"."vms_equipment"."geom" IS '按经度在前、纬度在后构造的设备WGS84 POINT，SRID 4326。';
COMMENT ON COLUMN "lta"."vms_equipment"."area_uid" IS '可空UUID外键，关联lta.study_area.uid表示CIQ研究区域。';
COMMENT ON COLUMN "lta"."vms_equipment"."first_seen_time" IS '系统第一次从LTA采集到该VMS设备的时间。';
COMMENT ON COLUMN "lta"."vms_equipment"."last_seen_time" IS '系统最近一次仍从LTA看到该VMS设备的时间。';
COMMENT ON COLUMN "lta"."vms_equipment"."active" IS '该VMS设备当前是否仍在LTA源数据中有效。';
COMMENT ON COLUMN "lta"."vms_equipment"."create_time" IS '本条VMS设备首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."vms_equipment"."update_time" IS '本条VMS设备最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "lta"."vms_message_state"."uid" IS 'VMS消息状态UUID主键，由PostgreSQL自动生成。';
COMMENT ON COLUMN "lta"."vms_message_state"."equipment_uid" IS '关联lta.vms_equipment.uid的UUID外键，标识显示消息的设备。';
COMMENT ON COLUMN "lta"."vms_message_state"."message" IS 'LTA VMS接口返回的当前显示文本。';
COMMENT ON COLUMN "lta"."vms_message_state"."message_hash" IS '标准化VMS消息文本的SHA256，用于识别状态变化。';
COMMENT ON COLUMN "lta"."vms_message_state"."first_seen_time" IS '系统第一次采集到该消息状态的时间。';
COMMENT ON COLUMN "lta"."vms_message_state"."last_seen_time" IS '系统最近一次仍看到相同消息状态的时间。';
COMMENT ON COLUMN "lta"."vms_message_state"."ended_time" IS '该消息被新消息替代或消失的时间。';
COMMENT ON COLUMN "lta"."vms_message_state"."active" IS '该消息状态当前是否仍在设备上显示。';
COMMENT ON COLUMN "lta"."vms_message_state"."first_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录首次发现该消息的任务。';
COMMENT ON COLUMN "lta"."vms_message_state"."last_run_uid" IS '关联lta.collection_run.uid的UUID外键，记录最近看到该消息的任务。';
COMMENT ON COLUMN "lta"."vms_message_state"."create_time" IS '本条VMS消息状态首次INSERT进入PostgreSQL的入库时间。';
COMMENT ON COLUMN "lta"."vms_message_state"."update_time" IS '本条VMS消息状态最近一次UPDATE时间，由数据库触发器自动维护。';
COMMENT ON COLUMN "melaka"."api_request_log"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."api_request_log"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "melaka"."api_request_log"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "melaka"."api_request_log"."duplicate_of_request_uid" IS '重复响应所引用的原始API请求UUID，关联melaka.api_request_log.uid';
COMMENT ON COLUMN "melaka"."api_request_log"."request_type" IS '请求类型，例如REALTIME或STATIC';
COMMENT ON COLUMN "melaka"."api_request_log"."request_sequence" IS '本次运行中的请求顺序编号';
COMMENT ON COLUMN "melaka"."api_request_log"."cycle_number" IS '本次运行中的采集周期编号';
COMMENT ON COLUMN "melaka"."api_request_log"."requested_url" IS '请求发起时使用的原始URL';
COMMENT ON COLUMN "melaka"."api_request_log"."final_url" IS '完成重定向后实际访问的最终URL';
COMMENT ON COLUMN "melaka"."api_request_log"."scheduled_at" IS '调度器计划发起请求的绝对时间';
COMMENT ON COLUMN "melaka"."api_request_log"."request_started_at" IS 'HTTP请求实际开始的绝对时间';
COMMENT ON COLUMN "melaka"."api_request_log"."response_received_at" IS 'HTTP响应接收完成的绝对时间';
COMMENT ON COLUMN "melaka"."api_request_log"."latency_ms" IS 'HTTP请求从开始到收到响应的耗时毫秒数';
COMMENT ON COLUMN "melaka"."api_request_log"."scheduler_drift_ms" IS '实际请求开始时间相对计划时间的调度偏移毫秒数';
COMMENT ON COLUMN "melaka"."api_request_log"."http_status" IS 'HTTP响应状态码，网络异常或超时时允许为空';
COMMENT ON COLUMN "melaka"."api_request_log"."content_type" IS 'HTTP响应Content-Type原始值';
COMMENT ON COLUMN "melaka"."api_request_log"."response_bytes" IS 'HTTP响应正文的字节数';
COMMENT ON COLUMN "melaka"."api_request_log"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "melaka"."api_request_log"."redirect_count" IS '本次HTTP请求经历的重定向次数';
COMMENT ON COLUMN "melaka"."api_request_log"."parse_status" IS '响应正文的解析状态';
COMMENT ON COLUMN "melaka"."api_request_log"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "melaka"."api_request_log"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "melaka"."api_request_log"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "melaka"."api_request_log"."raw_object_path" IS 'Realtime原始protobuf对象的文件存储路径';
COMMENT ON COLUMN "melaka"."api_request_log"."parsed_json_path" IS '完整解析JSON文件的存储路径';
COMMENT ON COLUMN "melaka"."api_request_log"."enriched_json_path" IS 'Static GTFS补充后JSON文件的存储路径';
COMMENT ON COLUMN "melaka"."api_request_log"."result" IS '本次API请求的最终结果状态';
COMMENT ON COLUMN "melaka"."api_request_log"."error_class" IS '请求或解析失败对应的异常类名称';
COMMENT ON COLUMN "melaka"."api_request_log"."error_message" IS '请求、响应或解析失败的详细错误信息';
COMMENT ON COLUMN "melaka"."api_request_log"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."request_uid" IS '产生当前数据的API请求UUID，关联melaka.api_request_log.uid';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."enrichment_static_version_uid" IS '当前GTFS-Realtime快照执行Static补充和线路解析时使用的GTFS Static版本UUID，正式关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."batch_id" IS '当前Realtime快照所属批次业务标识';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."gtfs_realtime_version" IS 'GTFS-Realtime FeedHeader声明的规范版本';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."incrementality" IS 'GTFS-Realtime FeedHeader声明的增量模式';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."feed_timestamp_raw" IS 'FeedHeader提供的原始Unix秒时间戳';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."feed_time" IS 'FeedHeader原始时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."entity_count" IS 'Realtime FeedMessage中的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."vehicle_count" IS 'Realtime FeedMessage中的VehiclePosition数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."unique_route_count" IS '当前快照解析得到的唯一线路数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."route_direct_match_count" IS '通过Realtime route_id直接匹配Static Route的数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."route_trip_fallback_match_count" IS '通过Static Trip回退匹配Route的数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."route_resolved_count" IS '成功解析到线路的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."route_unresolved_count" IS '未能解析线路的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."trip_match_count" IS '成功匹配Static Trip的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."trip_unmatched_count" IS '未匹配Static Trip的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."direction_match_count" IS 'Realtime与Static方向一致的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."direction_mismatch_count" IS 'Realtime与Static方向不一致的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."direction_not_comparable_count" IS '缺少必要字段而无法比较方向的Entity数量';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."response_sha256" IS 'HTTP响应正文的SHA-256摘要，重复摘要允许保存';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."duplicate_snapshot" IS '当前响应或快照是否被识别为重复内容';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."referenced_snapshot_uid" IS '重复快照所引用的原始Realtime快照UUID';
COMMENT ON COLUMN "melaka"."realtime_snapshot"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_route"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_route"."static_version_uid" IS '所属GTFS Static版本UUID，关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."static_route"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "melaka"."static_route"."agency_id" IS 'GTFS routes.txt提供的官方Agency源标识';
COMMENT ON COLUMN "melaka"."static_route"."route_short_name" IS 'GTFS线路短名称';
COMMENT ON COLUMN "melaka"."static_route"."route_long_name" IS 'GTFS线路完整名称';
COMMENT ON COLUMN "melaka"."static_route"."route_desc" IS 'GTFS线路补充描述';
COMMENT ON COLUMN "melaka"."static_route"."route_type" IS 'GTFS线路交通方式类型数值';
COMMENT ON COLUMN "melaka"."static_route"."route_url" IS 'GTFS线路信息网页URL';
COMMENT ON COLUMN "melaka"."static_route"."route_color" IS 'GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "melaka"."static_route"."route_text_color" IS 'GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "melaka"."static_route"."route_sort_order" IS 'GTFS线路展示排序值';
COMMENT ON COLUMN "melaka"."static_route"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "melaka"."static_route"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "melaka"."static_route"."network_id" IS 'GTFS线路所属网络源标识';
COMMENT ON COLUMN "melaka"."static_route"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "melaka"."static_route"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_shape"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_shape"."static_version_uid" IS '所属GTFS Static版本UUID，关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."static_shape"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "melaka"."static_shape"."is_referenced" IS '当前Shape是否被本Static版本trips.txt中的至少一个Trip实际引用';
COMMENT ON COLUMN "melaka"."static_shape"."trip_count_using_shape" IS '当前Static版本中引用该Shape的Trip数量';
COMMENT ON COLUMN "melaka"."static_shape"."point_count" IS '当前Shape包含的Shape Point数量';
COMMENT ON COLUMN "melaka"."static_shape"."geom" IS '当前Shape按shape_pt_sequence构造的PostGIS LineString对象';
COMMENT ON COLUMN "melaka"."static_shape"."shape_length_m" IS '根据当前Shape有效点序列计算得到的几何长度，单位米，异常长度仍保留';
COMMENT ON COLUMN "melaka"."static_shape"."shape_length_km" IS '根据当前Shape有效点序列计算得到的几何长度，单位km，异常长度仍保留';
COMMENT ON COLUMN "melaka"."static_shape"."analysis_eligible" IS '当前Shape是否推荐进入正常线路长度和空间分析，不控制原始Shape数据保存';
COMMENT ON COLUMN "melaka"."static_shape"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "melaka"."static_shape"."qc_summary" IS '当前Shape质量控制结果的扩展JSON汇总';
COMMENT ON COLUMN "melaka"."static_shape"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_shape_point"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_shape_point"."static_version_uid" IS '所属GTFS Static版本UUID，关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."static_shape_point"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联melaka.static_shape.uid';
COMMENT ON COLUMN "melaka"."static_shape_point"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "melaka"."static_shape_point"."shape_pt_lat" IS 'GTFS shapes.txt提供的原始Shape Point纬度，异常值仍保留';
COMMENT ON COLUMN "melaka"."static_shape_point"."shape_pt_lon" IS 'GTFS shapes.txt提供的原始Shape Point经度，异常值仍保留';
COMMENT ON COLUMN "melaka"."static_shape_point"."geom" IS '有效WGS84且非0,0 Shape Point对应的PostGIS Point对象';
COMMENT ON COLUMN "melaka"."static_shape_point"."shape_pt_sequence" IS 'GTFS shapes.txt中当前Shape Point的原始点序号';
COMMENT ON COLUMN "melaka"."static_shape_point"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "melaka"."static_shape_point"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "melaka"."static_shape_point"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "melaka"."static_shape_point"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "melaka"."static_shape_point"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "melaka"."static_shape_point"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "melaka"."static_shape_point"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_stop"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_stop"."static_version_uid" IS '所属GTFS Static版本UUID，关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."static_stop"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "melaka"."static_stop"."stop_code" IS 'GTFS站点面向乘客的短代码';
COMMENT ON COLUMN "melaka"."static_stop"."stop_name" IS 'GTFS站点名称';
COMMENT ON COLUMN "melaka"."static_stop"."tts_stop_name" IS 'GTFS站点文本转语音名称';
COMMENT ON COLUMN "melaka"."static_stop"."stop_desc" IS 'GTFS站点补充描述';
COMMENT ON COLUMN "melaka"."static_stop"."stop_lat" IS 'GTFS stops.txt原始纬度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "melaka"."static_stop"."stop_lon" IS 'GTFS stops.txt原始经度值，异常值仍保留作为Static数据质量证据';
COMMENT ON COLUMN "melaka"."static_stop"."geom" IS '由有效WGS84且非0,0站点坐标生成的PostGIS Point对象';
COMMENT ON COLUMN "melaka"."static_stop"."zone_id" IS 'GTFS站点所属票价分区源标识';
COMMENT ON COLUMN "melaka"."static_stop"."stop_url" IS 'GTFS站点信息网页URL';
COMMENT ON COLUMN "melaka"."static_stop"."location_type" IS 'GTFS站点位置类型原始数值';
COMMENT ON COLUMN "melaka"."static_stop"."parent_station" IS 'GTFS站点所属父站点的源stop_id';
COMMENT ON COLUMN "melaka"."static_stop"."stop_timezone" IS 'GTFS站点时区IANA名称';
COMMENT ON COLUMN "melaka"."static_stop"."wheelchair_boarding" IS 'GTFS站点无障碍上车规则原始数值';
COMMENT ON COLUMN "melaka"."static_stop"."level_id" IS 'GTFS站点所属楼层源标识';
COMMENT ON COLUMN "melaka"."static_stop"."platform_code" IS 'GTFS站点月台代码';
COMMENT ON COLUMN "melaka"."static_stop"."position_present" IS 'GTFS原始站点行是否提供经纬度';
COMMENT ON COLUMN "melaka"."static_stop"."position_wgs84_valid" IS 'GTFS原始坐标是否位于有效WGS84数值范围';
COMMENT ON COLUMN "melaka"."static_stop"."zero_zero_position" IS 'GTFS原始坐标是否为0,0';
COMMENT ON COLUMN "melaka"."static_stop"."spatial_eligible" IS '当前原始坐标是否推荐用于正常空间分析，不控制原始数据保存';
COMMENT ON COLUMN "melaka"."static_stop"."qc_flags" IS '当前Static实体所有质量控制标记的JSON数组';
COMMENT ON COLUMN "melaka"."static_stop"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "melaka"."static_stop"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_stop_time"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_stop_time"."static_version_uid" IS '所属GTFS Static版本UUID，关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."static_stop_time"."trip_uid" IS '当前Stop Time关联的数据库Static Trip UUID，关联melaka.static_trip.uid';
COMMENT ON COLUMN "melaka"."static_stop_time"."stop_uid" IS '当前Stop Time关联的数据库Static Stop UUID，关联melaka.static_stop.uid';
COMMENT ON COLUMN "melaka"."static_stop_time"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "melaka"."static_stop_time"."stop_id" IS 'GTFS stops.txt提供的官方Stop源标识';
COMMENT ON COLUMN "melaka"."static_stop_time"."stop_sequence" IS 'GTFS stop_times.txt中当前Stop在Trip内的原始顺序';
COMMENT ON COLUMN "melaka"."static_stop_time"."arrival_time_raw" IS 'GTFS stop_times.txt原始到站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "melaka"."static_stop_time"."arrival_seconds" IS '原始GTFS到站时间换算为从service day开始累计的秒数，用于跨24小时计算';
COMMENT ON COLUMN "melaka"."static_stop_time"."departure_time_raw" IS 'GTFS stop_times.txt原始离站服务时间字符串，支持超过24小时的GTFS时间';
COMMENT ON COLUMN "melaka"."static_stop_time"."departure_seconds" IS '原始GTFS离站时间换算为从service day开始累计的秒数';
COMMENT ON COLUMN "melaka"."static_stop_time"."stop_headsign" IS 'GTFS Stop Time在当前站点显示的目的地方向文字';
COMMENT ON COLUMN "melaka"."static_stop_time"."pickup_type" IS 'GTFS站点上客规则原始数值';
COMMENT ON COLUMN "melaka"."static_stop_time"."drop_off_type" IS 'GTFS站点下客规则原始数值';
COMMENT ON COLUMN "melaka"."static_stop_time"."continuous_pickup" IS 'GTFS连续上客规则原始数值';
COMMENT ON COLUMN "melaka"."static_stop_time"."continuous_drop_off" IS 'GTFS连续下客规则原始数值';
COMMENT ON COLUMN "melaka"."static_stop_time"."shape_dist_traveled" IS 'GTFS沿Shape累计行驶距离原始数值';
COMMENT ON COLUMN "melaka"."static_stop_time"."timepoint" IS 'GTFS Stop Time是否为精确时刻点的原始数值';
COMMENT ON COLUMN "melaka"."static_stop_time"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "melaka"."static_stop_time"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_trip"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_trip"."static_version_uid" IS '所属GTFS Static版本UUID，关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."static_trip"."route_uid" IS '当前Trip关联的数据库Static Route UUID，关联melaka.static_route.uid';
COMMENT ON COLUMN "melaka"."static_trip"."shape_uid" IS '当前记录关联的数据库Static Shape UUID，关联melaka.static_shape.uid';
COMMENT ON COLUMN "melaka"."static_trip"."route_id" IS 'GTFS routes.txt提供的官方线路源标识，不作为数据库内部UUID主键';
COMMENT ON COLUMN "melaka"."static_trip"."service_id" IS 'GTFS trips.txt提供的Service源标识';
COMMENT ON COLUMN "melaka"."static_trip"."trip_id" IS 'GTFS trips.txt提供的官方Trip源标识，用于Realtime trip_id与Static Trip关联';
COMMENT ON COLUMN "melaka"."static_trip"."trip_headsign" IS 'GTFS Trip目的地方向文字';
COMMENT ON COLUMN "melaka"."static_trip"."trip_short_name" IS 'GTFS Trip短名称';
COMMENT ON COLUMN "melaka"."static_trip"."direction_id" IS 'GTFS Trip方向ID原始数值';
COMMENT ON COLUMN "melaka"."static_trip"."block_id" IS 'GTFS Trip所属车辆运行Block源标识';
COMMENT ON COLUMN "melaka"."static_trip"."shape_id" IS 'GTFS shapes.txt提供的官方Shape源标识';
COMMENT ON COLUMN "melaka"."static_trip"."wheelchair_accessible" IS 'GTFS Trip无障碍可达规则原始数值';
COMMENT ON COLUMN "melaka"."static_trip"."bikes_allowed" IS 'GTFS Trip自行车携带规则原始数值';
COMMENT ON COLUMN "melaka"."static_trip"."source_row" IS '当前GTFS CSV原始行的完整JSON表示，用于保存未结构化扩展字段和数据审计';
COMMENT ON COLUMN "melaka"."static_trip"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."static_version"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."static_version"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "melaka"."static_version"."source_request_uid" IS '下载当前Static ZIP的API请求UUID，关联melaka.api_request_log.uid';
COMMENT ON COLUMN "melaka"."static_version"."version_code" IS '项目或数据源提供的Static版本可读代码，同一Feed内非空值唯一';
COMMENT ON COLUMN "melaka"."static_version"."static_sha256" IS '当前GTFS Static ZIP文件内容SHA-256摘要，用于Static版本唯一识别和重复内容去重';
COMMENT ON COLUMN "melaka"."static_version"."static_object_path" IS '当前GTFS Static ZIP原始对象的实际存储路径';
COMMENT ON COLUMN "melaka"."static_version"."zip_size_bytes" IS '当前GTFS Static ZIP文件大小，单位字节';
COMMENT ON COLUMN "melaka"."static_version"."downloaded_at" IS '当前Static ZIP下载完成的绝对时间';
COMMENT ON COLUMN "melaka"."static_version"."effective_from" IS '当前Static版本开始作为Realtime关联版本使用的绝对时间';
COMMENT ON COLUMN "melaka"."static_version"."effective_to" IS '当前Static版本停止作为Realtime主要关联版本使用的绝对时间，当前版本允许为空';
COMMENT ON COLUMN "melaka"."static_version"."is_current" IS '当前Static版本是否为所属Feed唯一的主要关联版本';
COMMENT ON COLUMN "melaka"."static_version"."routes_file_present" IS 'Static ZIP中是否存在routes.txt';
COMMENT ON COLUMN "melaka"."static_version"."trips_file_present" IS 'Static ZIP中是否存在trips.txt';
COMMENT ON COLUMN "melaka"."static_version"."stops_file_present" IS 'Static ZIP中是否存在stops.txt';
COMMENT ON COLUMN "melaka"."static_version"."stop_times_file_present" IS 'Static ZIP中是否存在stop_times.txt';
COMMENT ON COLUMN "melaka"."static_version"."shapes_file_present" IS 'Static ZIP中是否存在shapes.txt';
COMMENT ON COLUMN "melaka"."static_version"."calendar_file_present" IS 'Static ZIP中是否存在calendar.txt';
COMMENT ON COLUMN "melaka"."static_version"."calendar_dates_file_present" IS 'Static ZIP中是否存在calendar_dates.txt';
COMMENT ON COLUMN "melaka"."static_version"."frequencies_file_present" IS 'Static ZIP中是否存在frequencies.txt';
COMMENT ON COLUMN "melaka"."static_version"."routes_count" IS '当前Static版本解析得到的Route记录数量';
COMMENT ON COLUMN "melaka"."static_version"."trips_count" IS '当前Static版本解析得到的Trip记录数量';
COMMENT ON COLUMN "melaka"."static_version"."stops_count" IS '当前Static版本解析得到的Stop记录数量';
COMMENT ON COLUMN "melaka"."static_version"."stop_times_count" IS '当前Static版本解析得到的Stop Time记录数量';
COMMENT ON COLUMN "melaka"."static_version"."shapes_count" IS '当前Static版本解析得到的唯一Shape数量';
COMMENT ON COLUMN "melaka"."static_version"."shape_points_count" IS '当前Static版本解析得到的Shape Point记录数量';
COMMENT ON COLUMN "melaka"."static_version"."notes" IS '当前Static版本的补充说明或异常说明';
COMMENT ON COLUMN "melaka"."static_version"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."observation_uid" IS '关联的车辆历史观测UUID，关联melaka.vehicle_observation.uid';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."last_seen_time" IS 'Collector最近一次在Realtime Feed看到该车辆的ingest绝对时间';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."vehicle_latest_state"."update_time" IS '当前Latest State记录最近一次UPSERT数据库的时间';
COMMENT ON COLUMN "melaka"."vehicle_observation"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."vehicle_observation"."feed_uid" IS 'GTFS Feed数据库UUID，关联core.gtfs_feed.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."run_uid" IS '数据采集运行任务UUID，关联core.collection_run.uid，允许为空';
COMMENT ON COLUMN "melaka"."vehicle_observation"."request_uid" IS '产生当前数据的API请求UUID，关联melaka.api_request_log.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."snapshot_uid" IS '所属GTFS-Realtime快照UUID，关联melaka.realtime_snapshot.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_version_uid" IS '当前车辆Realtime观测执行Static关联和数据补充时使用的GTFS Static版本UUID，正式关联melaka.static_version.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_route_uid" IS '当前车辆Realtime观测最终匹配到的Static GTFS线路数据库UUID，正式关联melaka.static_route.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_trip_uid" IS '当前车辆Realtime观测匹配到的Static GTFS Trip数据库UUID，正式关联melaka.static_trip.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_shape_uid" IS '当前车辆Realtime观测通过Static Trip关联得到的Static GTFS Shape数据库UUID，正式关联melaka.static_shape.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_stop_uid" IS '当前车辆Realtime观测匹配到的Static GTFS站点数据库UUID，正式关联melaka.static_stop.uid；Realtime未提供或无法匹配站点时允许为空';
COMMENT ON COLUMN "melaka"."vehicle_observation"."entity_sequence" IS '当前FeedMessage实体列表中的0起始顺序，用于保证同一快照内Entity写入唯一性';
COMMENT ON COLUMN "melaka"."vehicle_observation"."entity_id" IS '官方GTFS-Realtime FeedEntity原始ID';
COMMENT ON COLUMN "melaka"."vehicle_observation"."vehicle_id" IS '官方VehicleDescriptor车辆标识';
COMMENT ON COLUMN "melaka"."vehicle_observation"."vehicle_label" IS '官方VehicleDescriptor车辆可读标签';
COMMENT ON COLUMN "melaka"."vehicle_observation"."license_plate" IS '官方VehicleDescriptor车辆牌照信息';
COMMENT ON COLUMN "melaka"."vehicle_observation"."wheelchair_accessible" IS '官方VehicleDescriptor无障碍可达状态原始枚举值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_id" IS '官方GTFS Trip业务标识';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_start_time_raw" IS '官方TripDescriptor start_time原始字符串，可保存超过24小时的GTFS时间';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_start_seconds" IS 'Trip开始时间换算为GTFS服务日起点后的秒数，可大于86400';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_start_date_raw" IS '官方TripDescriptor start_date原始YYYYMMDD字符串';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_start_date" IS '官方Trip开始服务日期的PostgreSQL DATE表示';
COMMENT ON COLUMN "melaka"."vehicle_observation"."realtime_schedule_relationship" IS 'Realtime TripDescriptor调度关系原始枚举值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."realtime_route_id" IS 'Realtime消息直接提供的官方route_id';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_route_id" IS '通过Static GTFS匹配得到的官方route_id';
COMMENT ON COLUMN "melaka"."vehicle_observation"."resolved_route_id" IS '综合Realtime直接匹配和Trip回退匹配得到的最终route_id';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_short_name" IS 'Static GTFS线路短名称';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_long_name" IS 'Static GTFS线路完整名称';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_type" IS 'Static GTFS route_type数值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_color" IS 'Static GTFS线路颜色原始十六进制字符串';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_text_color" IS 'Static GTFS线路文字颜色原始十六进制字符串';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_resolution_method" IS '线路解析方法，例如DIRECT_REALTIME_ROUTE、TRIP_TO_STATIC_ROUTE或UNRESOLVED';
COMMENT ON COLUMN "melaka"."vehicle_observation"."realtime_route_direct_matched" IS 'Realtime route_id是否直接匹配Static Route';
COMMENT ON COLUMN "melaka"."vehicle_observation"."route_resolved" IS '当前观测是否成功解析到线路';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_service_id" IS 'Static GTFS Trip关联的service_id';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_headsign" IS 'Static GTFS Trip目的地方向文字';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_short_name" IS 'Static GTFS Trip短名称';
COMMENT ON COLUMN "melaka"."vehicle_observation"."shape_id" IS '官方GTFS Shape业务标识';
COMMENT ON COLUMN "melaka"."vehicle_observation"."trip_static_matched" IS '当前Realtime Trip是否匹配Static GTFS Trip';
COMMENT ON COLUMN "melaka"."vehicle_observation"."realtime_direction_id" IS 'Realtime TripDescriptor提供的方向ID';
COMMENT ON COLUMN "melaka"."vehicle_observation"."static_direction_id" IS 'Static GTFS Trip提供的方向ID';
COMMENT ON COLUMN "melaka"."vehicle_observation"."direction_comparison" IS 'Realtime与Static方向比较结果，例如MATCH、MISMATCH或NOT_COMPARABLE';
COMMENT ON COLUMN "melaka"."vehicle_observation"."latitude" IS '官方Realtime返回的原始纬度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "melaka"."vehicle_observation"."longitude" IS '官方Realtime返回的原始经度数值，0、缺失或异常数值仍保留作为数据质量证据';
COMMENT ON COLUMN "melaka"."vehicle_observation"."geom" IS '有效WGS84车辆位置对应的PostGIS Point几何对象，0,0、缺失或非法WGS84坐标不生成有效Geometry';
COMMENT ON COLUMN "melaka"."vehicle_observation"."bearing" IS '官方Realtime Position方位角原始数值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."odometer_m" IS '官方Realtime Position里程表原始数值，单位按GTFS-Realtime规范解释';
COMMENT ON COLUMN "melaka"."vehicle_observation"."source_speed_raw" IS '官方GTFS-Realtime Position.speed字段原始数值，不对其单位进行自动换算';
COMMENT ON COLUMN "melaka"."vehicle_observation"."source_speed_present" IS '官方Realtime Position是否显式提供speed字段';
COMMENT ON COLUMN "melaka"."vehicle_observation"."source_speed_unit_declared" IS '数据源声明的速度单位';
COMMENT ON COLUMN "melaka"."vehicle_observation"."source_speed_unit_interpretation" IS '项目对官方速度字段单位语义的解释说明';
COMMENT ON COLUMN "melaka"."vehicle_observation"."current_stop_sequence" IS '官方VehiclePosition当前Stop在Trip中的顺序';
COMMENT ON COLUMN "melaka"."vehicle_observation"."stop_id" IS '官方GTFS Stop业务标识';
COMMENT ON COLUMN "melaka"."vehicle_observation"."current_status" IS '官方VehiclePosition当前车辆状态枚举值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."congestion_level" IS '官方VehiclePosition拥堵等级枚举值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."occupancy_status" IS '官方VehiclePosition载客状态枚举值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."occupancy_percentage" IS '官方VehiclePosition载客百分比原始值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."multi_carriage_details" IS '官方多车厢明细的完整JSON结构';
COMMENT ON COLUMN "melaka"."vehicle_observation"."vehicle_timestamp_raw" IS '官方VehiclePosition.timestamp原始Unix秒值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."vehicle_time" IS '官方车辆Unix时间戳转换得到的TIMESTAMPTZ绝对时间';
COMMENT ON COLUMN "melaka"."vehicle_observation"."previous_vehicle_time" IS '计算连续移动指标时采用的上一条车辆观测绝对时间';
COMMENT ON COLUMN "melaka"."vehicle_observation"."ingest_time" IS 'Java Collector接收并处理当前车辆观测的绝对时间';
COMMENT ON COLUMN "melaka"."vehicle_observation"."freshness_seconds" IS '数据处理时间与官方车辆时间的秒数差，Future Timestamp情况下允许负值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."vehicle_gap_seconds" IS '当前车辆观测与上一车辆观测时间差，Timestamp Regression情况下允许负值';
COMMENT ON COLUMN "melaka"."vehicle_observation"."distance_from_previous_m" IS '当前有效GPS位置与上一有效位置之间的球面距离，单位米';
COMMENT ON COLUMN "melaka"."vehicle_observation"."derived_speed_kmh" IS '根据连续有效GPS位置和时间差计算的派生速度，单位km/h，异常高速仍保留';
COMMENT ON COLUMN "melaka"."vehicle_observation"."position_present" IS '官方Realtime是否提供位置对象';
COMMENT ON COLUMN "melaka"."vehicle_observation"."position_wgs84_valid" IS '官方原始经纬度是否位于有效WGS84数值范围';
COMMENT ON COLUMN "melaka"."vehicle_observation"."zero_zero_position" IS '官方原始位置是否为0,0坐标';
COMMENT ON COLUMN "melaka"."vehicle_observation"."feed_bounds_valid" IS '官方原始位置是否位于当前Feed预期地理范围';
COMMENT ON COLUMN "melaka"."vehicle_observation"."position_qc_status" IS '位置质量控制综合状态';
COMMENT ON COLUMN "melaka"."vehicle_observation"."gps_jump_status" IS '连续车辆位置是否构成GPS跳点的质量状态';
COMMENT ON COLUMN "melaka"."vehicle_observation"."jump_calculation_skipped_reason" IS '未执行GPS跳点计算的原因';
COMMENT ON COLUMN "melaka"."vehicle_observation"."analysis_eligible" IS '当前观测是否推荐用于正常交通分析，不控制该记录是否写入数据库';
COMMENT ON COLUMN "melaka"."vehicle_observation"."spatial_eligible" IS '当前观测是否推荐进入正常空间轨迹分析，不控制原始经纬度保存';
COMMENT ON COLUMN "melaka"."vehicle_observation"."duplicate_observation" IS '当前车辆观测是否与之前出现的观测重复，重复Occurrence仍保留';
COMMENT ON COLUMN "melaka"."vehicle_observation"."observation_key" IS '用于识别跨快照重复车辆观测的业务摘要键，只建立普通索引且不唯一';
COMMENT ON COLUMN "melaka"."vehicle_observation"."qc_flags" IS '当前车辆观测所有质量控制标记的JSON数组';
COMMENT ON COLUMN "melaka"."vehicle_observation"."realtime_entity" IS '当前VehiclePosition对应完整FeedEntity解析JSON，用于保存结构化字段之外的原始解析证据';
COMMENT ON COLUMN "melaka"."vehicle_observation"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."uid" IS '数据库UUID主键';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."observation_uid" IS '关联的车辆历史观测UUID，关联melaka.vehicle_observation.uid';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."qc_code" IS '质量控制标记的稳定业务代码';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."qc_category" IS '质量控制标记所属类别';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."severity" IS '质量控制问题严重程度';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."qc_value_numeric" IS '质量控制标记对应的数值证据';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."qc_value_text" IS '质量控制标记对应的文本证据';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."details" IS '质量控制标记的扩展JSON明细';
COMMENT ON COLUMN "melaka"."vehicle_observation_qc"."create_time" IS '当前数据库记录实际首次写入PostgreSQL的时间';

-- End of schema-only DDL.
