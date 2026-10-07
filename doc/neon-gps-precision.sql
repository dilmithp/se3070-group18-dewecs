-- DEWECS: GPS coordinate precision fix for the shared Neon database.
-- Run by hand in the Neon SQL editor AFTER telling your teammates. Claude/automation must never run this file.
--
-- Why: before Phase 7 the entities declared gps_lat / gps_lng without precision, so Hibernate created
-- numeric(38,2) and every coordinate was rounded to 2 decimals (about 1 km). The entities now declare
-- numeric(10,7) (about 1 cm); Hibernate's ddl-auto=validate does not compare precision, so the app starts
-- either way, but Neon keeps its old columns until you run step 2.
--
-- Until step 2 runs, Neon keeps storing 2 decimals: a POST response still shows 7 decimals because it returns
-- the saved object, but a later GET or an idempotent replay shows 2.
-- Coordinates that were already rounded to 2 decimals cannot be recovered.

-- 1. Read-only check: current definition of the four columns.
SELECT table_name, column_name, data_type, numeric_precision, numeric_scale
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('ground_reports', 'rescue_requests')
  AND column_name IN ('gps_lat', 'gps_lng')
ORDER BY table_name, column_name;

-- 2. The change (widening scale; existing values keep their rounded 2-decimal value).
ALTER TABLE ground_reports  ALTER COLUMN gps_lat TYPE numeric(10,7);
ALTER TABLE ground_reports  ALTER COLUMN gps_lng TYPE numeric(10,7);
ALTER TABLE rescue_requests ALTER COLUMN gps_lat TYPE numeric(10,7);
ALTER TABLE rescue_requests ALTER COLUMN gps_lng TYPE numeric(10,7);
