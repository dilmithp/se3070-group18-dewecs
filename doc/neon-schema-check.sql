-- DEWECS: READ-ONLY schema drift checks for the shared Neon database.
-- Run these by hand in the Neon SQL editor. Every statement is a SELECT: nothing here changes data or schema.
-- Claude/automation must never run this file. Source: PROGRESS_NOTES.md section 8, step 1.

-- 1. Do the new columns exist, and are they nullable the way the code expects?
--    Expected: warnings.issued_at = YES; relief_consignments.district_id absent or YES.
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (table_name, column_name) IN (
    ('warnings','issued_at'), ('warnings','message'), ('warnings','expires_at'),
    ('ground_reports','district_id'), ('ground_reports','category'), ('ground_reports','action_note'),
    ('shelters','status'),
    ('resources','unit'), ('resources','district_id'),
    ('relief_consignments','district_id'), ('relief_consignments','shelter_id'), ('relief_consignments','delivered_at'))
ORDER BY table_name, column_name;

-- 2. Does the rescue_requests table exist? (NULL means no.)
SELECT to_regclass('public.rescue_requests');

-- 3. Enum CHECK constraints that ddl-auto=update will not widen.
--    warnings.status must allow DRAFT; ground_reports.status must allow ACTIONED.
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE contype = 'c'
  AND conrelid::regclass::text IN ('warnings','ground_reports','shelters','rescue_requests','resources','relief_consignments')
ORDER BY 1, 2;
