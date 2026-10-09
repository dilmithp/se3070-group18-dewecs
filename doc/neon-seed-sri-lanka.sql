-- DEWECS: Sri Lankan DUMMY data for the Neon database. Run by hand in the Neon SQL editor (Claude/automation never runs it).
--
-- What it does: INSERTs rows only (no schema change, no UPDATE/DELETE). Every statement skips rows that already
-- exist (matched by name, NIC or message), so it is safe to run again; the one exception is shelter occupants
-- and consignment items, which are skipped together with the row they belong to.
-- Run the whole file at once. It is one transaction: if any statement fails, nothing is saved.
--
-- Before running: all names, phone numbers (0710000xxx) and NICs (2000xxxxxxxx) are made up. Organisation names are
-- real national agencies used only as labels. Never run this against a database holding real citizen data you
-- are not allowed to mix with dummy rows: the app has no screen to delete these rows again.
--
-- Columns follow the JPA entities (backend/src/main/java/.../domain). Times are Sri Lanka local time, relative to
-- the moment you run the script (hours before now; a negative number means in the future).
--
-- Needs the tables to exist already (start the app once on the dev profile, or create them the usual way) and the
-- normal schema of doc/neon-schema-check.sql. If section 14 fails on a NOT NULL column named district_id in
-- relief_consignments (an old column the entity no longer has), tell Claude: the script then needs one more value.
--
-- To remove the dummy data later, a human must delete it by SQL (children first). Ask for a cleanup script.

BEGIN;

-- 0. Resync the id counters. Rows that were added by hand with an explicit id leave a table's counter behind its
--    real maximum, and the next INSERT then fails with "duplicate key ... _pkey". This sets each counter to
--    max(id) + 1 (or 1 for an empty table). It changes no rows.
SELECT setval(pg_get_serial_sequence('districts', 'id'),            COALESCE((SELECT MAX(id) FROM districts), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('organizations', 'id'),        COALESCE((SELECT MAX(id) FROM organizations), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('users', 'id'),                COALESCE((SELECT MAX(id) FROM users), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('rescue_teams', 'id'),         COALESCE((SELECT MAX(id) FROM rescue_teams), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('shelters', 'id'),             COALESCE((SELECT MAX(id) FROM shelters), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('shelter_occupants', 'id'),    COALESCE((SELECT MAX(id) FROM shelter_occupants), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('resources', 'id'),            COALESCE((SELECT MAX(id) FROM resources), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('hazard_events', 'id'),        COALESCE((SELECT MAX(id) FROM hazard_events), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('warnings', 'id'),             COALESCE((SELECT MAX(id) FROM warnings), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('rescue_requests', 'id'),      COALESCE((SELECT MAX(id) FROM rescue_requests), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('ground_reports', 'id'),       COALESCE((SELECT MAX(id) FROM ground_reports), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('relief_consignments', 'id'),  COALESCE((SELECT MAX(id) FROM relief_consignments), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('consignment_items', 'id'),    COALESCE((SELECT MAX(id) FROM consignment_items), 0) + 1, false);

-- 1. Districts (Galle usually exists already) ---------------------------------------------------------------
INSERT INTO districts (name)
SELECT v.name
FROM (VALUES ('Colombo'), ('Gampaha'), ('Kalutara'), ('Kandy'), ('Galle'), ('Matara'), ('Jaffna'),
             ('Ratnapura'), ('Badulla'), ('Batticaloa')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM districts d WHERE d.name = v.name);

-- 2. Organisations -------------------------------------------------------------------------------------------
INSERT INTO organizations (name, type)
SELECT v.name, v.type
FROM (VALUES
    ('Disaster Management Centre',     'GOVERNMENT'),
    ('Sri Lanka Army',                 'ARMED_FORCES'),
    ('Sri Lanka Navy',                 'ARMED_FORCES'),
    ('Sri Lanka Air Force',            'ARMED_FORCES'),
    ('Sri Lanka Red Cross Society',    'NGO'),
    ('Sarvodaya Shramadana Society',   'NGO'),
    ('World Vision Lanka',             'NGO'),
    ('Ceylon Relief Foundation',       'PRIVATE_DONOR')
) AS v(name, type)
WHERE NOT EXISTS (SELECT 1 FROM organizations o WHERE o.name = v.name);

-- 3. Officers (staff users) ----------------------------------------------------------------------------------
INSERT INTO users (full_name, phone, address, district_id)
SELECT v.n, v.p, v.a, d.id
FROM (VALUES
    ('Nadeesha Fernando',        '0710000001', 'DMC, Vidya Mawatha, Colombo 07',   'Colombo'),
    ('Kasun Wickramasinghe',     '0710000002', 'District Secretariat, Galle',      'Galle'),
    ('Tharindu Bandara',         '0710000003', 'District Secretariat, Kandy',      'Kandy'),
    ('Kirushanthan Selvaraj',    '0710000004', 'District Secretariat, Jaffna',     'Jaffna'),
    ('Nirmala Senanayake',       '0710000005', 'District Secretariat, Ratnapura',  'Ratnapura'),
    ('Mohamed Fazil',            '0710000006', 'District Secretariat, Batticaloa', 'Batticaloa')
) AS v(n, p, a, district)
JOIN districts d ON d.name = v.district
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.full_name = v.n);

-- 4. Citizens (users + citizens, joined inheritance) -----------------------------------------------------------
WITH v(n, p, a, district, nic) AS (VALUES
    ('Amaya Silva',            '0710000101', '12 Temple Road, Galle',             'Galle',      '200000000101'),
    ('Kasun Perera',           '0710000102', '45 Matara Road, Galle',             'Galle',      '200000000102'),
    ('Dilani Jayasinghe',      '0710000103', '8 Lake View, Kandy',                'Kandy',      '200000000103'),
    ('Ruwan Rathnayake',       '0710000104', '3 Peradeniya Road, Kandy',          'Kandy',      '200000000104'),
    ('Sanduni Gunawardena',    '0710000105', '77 Galle Road, Colombo 03',         'Colombo',    '200000000105'),
    ('Chamara Dissanayake',    '0710000106', '21 Baseline Road, Colombo 09',      'Colombo',    '200000000106'),
    ('Thevarajah Sinnathurai', '0710000107', '5 Hospital Road, Jaffna',           'Jaffna',     '200000000107'),
    ('Priya Nadarajah',        '0710000108', '14 Stanley Road, Jaffna',           'Jaffna',     '200000000108'),
    ('Nuwan Herath',           '0710000109', '9 Rakwana Road, Ratnapura',         'Ratnapura',  '200000000109'),
    ('Ishara Kumari',          '0710000110', '33 Badulla Road, Bandarawela',      'Badulla',    '200000000110'),
    ('Aslam Rahuman',          '0710000111', '6 Main Street, Batticaloa',         'Batticaloa', '200000000111'),
    ('Lakmal Weerasinghe',     '0710000112', '18 Kalutara South, Kalutara',       'Kalutara',   '200000000112')
), ins AS (
    INSERT INTO users (full_name, phone, address, district_id)
    SELECT v.n, v.p, v.a, d.id
    FROM v JOIN districts d ON d.name = v.district
    WHERE NOT EXISTS (SELECT 1 FROM citizens c WHERE c.nic = v.nic)
    RETURNING id, full_name
)
INSERT INTO citizens (id, nic)
SELECT ins.id, v.nic FROM ins JOIN v ON v.n = ins.full_name;

-- 5. Rescue teams ----------------------------------------------------------------------------------------------
INSERT INTO rescue_teams (name, district_id, status, organization_id)
SELECT v.name, d.id, v.status, o.id
FROM (VALUES
    ('Colombo Rapid Response Alpha',  'Colombo',    'AVAILABLE',  'Sri Lanka Army'),
    ('Colombo Water Rescue Bravo',    'Colombo',    'AVAILABLE',  'Sri Lanka Navy'),
    ('Galle Flood Team One',          'Galle',      'DISPATCHED', 'Sri Lanka Navy'),
    ('Galle Red Cross Volunteers',    'Galle',      'AVAILABLE',  'Sri Lanka Red Cross Society'),
    ('Kandy Landslide Response',      'Kandy',      'DISPATCHED', 'Sri Lanka Army'),
    ('Kandy Mountain Rescue',         'Kandy',      'AVAILABLE',  'Sri Lanka Air Force'),
    ('Jaffna Coastal Unit',           'Jaffna',     'AVAILABLE',  'Sri Lanka Navy'),
    ('Ratnapura Search Team',         'Ratnapura',  'AVAILABLE',  'Sri Lanka Army'),
    ('Badulla Hill Country Team',     'Badulla',    'NEEDS_SUPPORT', 'Sarvodaya Shramadana Society'),
    ('Batticaloa Relief Squad',       'Batticaloa', 'AVAILABLE',  'World Vision Lanka'),
    ('Matara Sea Rescue',             'Matara',     'AVAILABLE',  'Sri Lanka Navy'),
    ('Kalutara River Patrol',         'Kalutara',   'FULL',       'Sri Lanka Army')
) AS v(name, district, status, org)
JOIN districts d ON d.name = v.district
JOIN organizations o ON o.name = v.org
WHERE NOT EXISTS (SELECT 1 FROM rescue_teams t WHERE t.name = v.name);

-- 6. Shelters (occupancy = capacity means FULL) ------------------------------------------------------------------
INSERT INTO shelters (name, district_id, capacity, current_occupancy, status, organization_id)
SELECT v.name, d.id, v.cap, v.occ, v.status, o.id
FROM (VALUES
    ('Galle Municipal Hall',                 'Galle',      120, 120, 'FULL',   'Disaster Management Centre'),
    ('Richmond College Hall, Galle',         'Galle',      200,  64, 'OPEN',   'Sri Lanka Red Cross Society'),
    ('Kandy Girls High School Hall',         'Kandy',      150,   4, 'OPEN',   'Disaster Management Centre'),
    ('Peradeniya Temple Pavilion',           'Kandy',       80,  80, 'FULL',   'Sarvodaya Shramadana Society'),
    ('Colombo Sports Complex',               'Colombo',    400, 150, 'OPEN',   'Disaster Management Centre'),
    ('Kollupitiya Community Centre',         'Colombo',    100,   0, 'CLOSED', 'Ceylon Relief Foundation'),
    ('Jaffna Central College Hall',          'Jaffna',     180,  35, 'OPEN',   'World Vision Lanka'),
    ('Ratnapura Maha Vidyalaya Hall',        'Ratnapura',  160,  90, 'OPEN',   'Sri Lanka Army'),
    ('Bandarawela Town Hall',                'Badulla',    120,  50, 'OPEN',   'Sarvodaya Shramadana Society'),
    ('Batticaloa Teachers Training Hall',    'Batticaloa', 140,  25, 'OPEN',   'World Vision Lanka'),
    ('Matara Rahula College Hall',           'Matara',     130,   0, 'OPEN',   'Sri Lanka Red Cross Society'),
    ('Kalutara Bodhiya Meeting Hall',        'Kalutara',   110, 110, 'FULL',   'Disaster Management Centre')
) AS v(name, district, cap, occ, status, org)
JOIN districts d ON d.name = v.district
JOIN organizations o ON o.name = v.org
WHERE NOT EXISTS (SELECT 1 FROM shelters s WHERE s.name = v.name);

-- 7. Occupants for one small shelter (Kandy Girls High School Hall has 4) -----------------------------------------
INSERT INTO shelter_occupants (shelter_id, full_name, nic, check_in_time)
SELECT s.id, v.n, v.nic, (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => v.hrs)
FROM (VALUES
    ('Dilani Jayasinghe',  '200000000103', 5),
    ('Ruwan Rathnayake',   '200000000104', 5),
    ('Sumudu Karunaratne', '200000000201', 4),
    ('Anoma Ekanayake',    '200000000202', 3)
) AS v(n, nic, hrs)
JOIN shelters s ON s.name = 'Kandy Girls High School Hall'
WHERE NOT EXISTS (SELECT 1 FROM shelter_occupants o WHERE o.shelter_id = s.id AND o.nic = v.nic);

-- 8. Relief supplies (two rows are below the low-stock threshold of 10) ---------------------------------------
INSERT INTO resources (name, type, quantity, unit, district_id, organization_id)
SELECT v.name, v.type, v.qty, v.unit, d.id, o.id
FROM (VALUES
    ('Rice',                 'FOOD',              2400, 'kg',     'Colombo',    'Disaster Management Centre'),
    ('Bottled water',        'WATER',              900, 'cases',  'Colombo',    'Ceylon Relief Foundation'),
    ('Rice',                 'FOOD',              1200, 'kg',     'Galle',      'Disaster Management Centre'),
    ('Dry ration packs',     'FOOD',               350, 'packs',  'Galle',      'Sri Lanka Red Cross Society'),
    ('First-aid kits',       'MEDICAL_SUPPLIES',     6, 'kits',   'Galle',      'Sri Lanka Red Cross Society'),
    ('Tarpaulins',           'SHELTER_MATERIALS',  120, 'sheets', 'Kandy',      'Sarvodaya Shramadana Society'),
    ('Blankets',             'OTHER',              260, 'pieces', 'Kandy',      'Disaster Management Centre'),
    ('Life jackets',         'EQUIPMENT',           40, 'pieces', 'Kalutara',   'Sri Lanka Navy'),
    ('Diesel',               'FUEL',               800, 'litres', 'Ratnapura',  'Sri Lanka Army'),
    ('Drinking water tanks', 'WATER',                8, 'tanks',  'Ratnapura',  'Disaster Management Centre'),
    ('Rice',                 'FOOD',               600, 'kg',     'Jaffna',     'World Vision Lanka'),
    ('Medicine boxes',       'MEDICAL_SUPPLIES',    55, 'boxes',  'Jaffna',     'World Vision Lanka'),
    ('Tents',                'SHELTER_MATERIALS',   30, 'tents',  'Badulla',    'Sri Lanka Army'),
    ('Dry ration packs',     'FOOD',               420, 'packs',  'Batticaloa', 'World Vision Lanka'),
    ('Rice',                 'FOOD',               700, 'kg',     'Matara',     'Sri Lanka Red Cross Society'),
    ('Water purification tablets', 'MEDICAL_SUPPLIES', 1500, 'strips', 'Gampaha', 'Ceylon Relief Foundation')
) AS v(name, type, qty, unit, district, org)
JOIN districts d ON d.name = v.district
JOIN organizations o ON o.name = v.org
WHERE NOT EXISTS (SELECT 1 FROM resources r WHERE r.name = v.name AND r.district_id = d.id);

-- 9. Hazard events (one per type and district) --------------------------------------------------------------------
INSERT INTO hazard_events (hazard_type, severity_level, status, district_id, occurred_at)
SELECT v.type, v.sev, v.status, d.id, (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => v.hrs)
FROM (VALUES
    ('FLOOD',     'HIGH',     'ACTIVE',    'Galle',        8),
    ('LANDSLIDE', 'CRITICAL', 'ACTIVE',    'Kandy',        5),
    ('FLOOD',     'HIGH',     'CONTAINED', 'Ratnapura',   50),
    ('CYCLONE',   'MODERATE', 'RESOLVED',  'Jaffna',     150),
    ('LANDSLIDE', 'HIGH',     'ACTIVE',    'Badulla',     26),
    ('FLOOD',     'MODERATE', 'RESOLVED',  'Kalutara',   240),
    ('DROUGHT',   'LOW',      'CONTAINED', 'Batticaloa', 400),
    ('FLOOD',     'HIGH',     'CONTAINED', 'Colombo',     70)
) AS v(type, sev, status, district, hrs)
JOIN districts d ON d.name = v.district
WHERE NOT EXISTS (SELECT 1 FROM hazard_events e WHERE e.district_id = d.id AND e.hazard_type = v.type);

-- 10. Warnings (issued, expired, draft, retracted) ------------------------------------------------------------------
INSERT INTO warnings (hazard_event_id, severity, status, message, issued_at, expires_at, issued_by_user_id)
SELECT e.id, w.sev, w.status, w.msg,
       CASE WHEN w.issued_hrs IS NULL THEN NULL
            ELSE (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => w.issued_hrs) END,
       CASE WHEN w.expires_hrs IS NULL THEN NULL
            ELSE (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => w.expires_hrs) END,
       u.id
FROM (VALUES
    ('FLOOD',     'Galle',      'HIGH',     'ISSUED',    'Gin Ganga flood warning: water level rising at Baddegama. Move to higher ground and avoid riverbanks.',       6,  -12, 'Kasun Wickramasinghe'),
    ('FLOOD',     'Galle',      'MODERATE', 'EXPIRED',   'Gin Ganga early notice: heavy rain expected over the next 24 hours in the Galle district.',                   30,   6, 'Kasun Wickramasinghe'),
    ('LANDSLIDE', 'Kandy',      'CRITICAL', 'ISSUED',    'Landslide warning for Kandy: evacuate the slopes above Peradeniya Road and Galaha immediately.',               4,  -24, 'Tharindu Bandara'),
    ('LANDSLIDE', 'Badulla',    'HIGH',     'ISSUED',    'Landslide alert for Badulla: NBRO has marked Bandarawela and Haputale slopes as danger zones.',               20,  -10, 'Nadeesha Fernando'),
    ('FLOOD',     'Ratnapura',  'HIGH',     'EXPIRED',   'Kalu Ganga flood warning: low-lying areas of Ratnapura town may be under water.',                             60,  20, 'Nirmala Senanayake'),
    ('CYCLONE',   'Jaffna',     'MODERATE', 'EXPIRED',   'Cyclone watch: strong winds and rough seas expected off the northern coast. Fishermen must stay ashore.',    160, 110, 'Kirushanthan Selvaraj'),
    ('FLOOD',     'Colombo',    'HIGH',     'CANCELLED', 'Kelani river flood warning (withdrawn after the water level fell below the alert mark).',                     80,  40, 'Nadeesha Fernando'),
    ('FLOOD',     'Colombo',    'MODERATE', 'DRAFT',     'Draft: Kelani river levels are being monitored. Prepare to publish if rain continues tonight.',              NULL, NULL, 'Nadeesha Fernando'),
    ('DROUGHT',   'Batticaloa', 'LOW',      'ISSUED',    'Drought advisory for Batticaloa: conserve drinking water, bowsers will be scheduled by the District Secretariat.', 300, -100, 'Mohamed Fazil')
) AS w(ev_type, ev_district, sev, status, msg, issued_hrs, expires_hrs, officer)
JOIN districts d ON d.name = w.ev_district
JOIN hazard_events e ON e.district_id = d.id AND e.hazard_type = w.ev_type
JOIN users u ON u.full_name = w.officer
WHERE NOT EXISTS (SELECT 1 FROM warnings x WHERE x.message = w.msg);

-- 11. Broadcast channels of the warnings (matched by the start of the message) -------------------------------------
INSERT INTO warning_broadcast_channels (warning_id, broadcast_channels)
SELECT w.id, v.channel
FROM (VALUES
    ('Gin Ganga flood warning',      'SMS'), ('Gin Ganga flood warning',      'RADIO'), ('Gin Ganga flood warning',     'SIREN'),
    ('Gin Ganga early notice',       'SMS'),
    ('Landslide warning for Kandy',  'SMS'), ('Landslide warning for Kandy',  'TV'),    ('Landslide warning for Kandy', 'APP_PUSH'),
    ('Landslide alert for Badulla',  'SMS'), ('Landslide alert for Badulla',  'RADIO'),
    ('Kalu Ganga flood warning',     'SMS'), ('Kalu Ganga flood warning',     'RADIO'),
    ('Cyclone watch',                'RADIO'), ('Cyclone watch',              'TV'),
    ('Kelani river flood warning',   'SMS'),
    ('Drought advisory',             'RADIO'), ('Drought advisory',           'SOCIAL_MEDIA')
) AS v(tag, channel)
JOIN warnings w ON w.message LIKE v.tag || '%'
WHERE NOT EXISTS (SELECT 1 FROM warning_broadcast_channels c WHERE c.warning_id = w.id AND c.broadcast_channels = v.channel);

-- 12. Rescue requests ------------------------------------------------------------------------------------------------
INSERT INTO rescue_requests (requester_name, requester_phone, district_id, gps_lat, gps_lng, description, priority,
                             status, assigned_team_id, submitted_at, assigned_at, completed_at)
SELECT v.n, v.p, d.id, v.lat, v.lng, v.descr, v.prio, v.status, t.id,
       (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => v.sub_hrs),
       CASE WHEN v.asg_hrs  IS NULL THEN NULL ELSE (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => v.asg_hrs)  END,
       CASE WHEN v.done_hrs IS NULL THEN NULL ELSE (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => v.done_hrs) END
FROM (VALUES
    ('Amaya Silva',            '0710000101', 'Galle',      6.0361000::numeric,  80.2167000::numeric, 'Family of five trapped on the upper floor, water at the first floor.',      'CRITICAL', 'ASSIGNED',  'Galle Flood Team One',     5,    4, NULL::int),
    ('Kasun Perera',           '0710000102', 'Galle',      6.0535000::numeric,  80.2210000::numeric, 'Elderly woman cannot walk, needs a boat to reach the Municipal Hall.',    'HIGH',     'PENDING',   NULL,                       3, NULL, NULL),
    ('Dilani Jayasinghe',      '0710000103', 'Kandy',      7.2570000::numeric,  80.5970000::numeric, 'House damaged by a landslide near Peradeniya Road, two people injured.',  'CRITICAL', 'ASSIGNED',  'Kandy Landslide Response', 4,    3, NULL),
    ('Ruwan Rathnayake',       '0710000104', 'Kandy',      7.2906000::numeric,  80.6337000::numeric, 'Road blocked by debris, a bus with passengers is stranded.',              'HIGH',     'COMPLETED', 'Kandy Mountain Rescue',    20,  19,   16),
    ('Sanduni Gunawardena',    '0710000105', 'Colombo',    6.9271000::numeric,  79.8612000::numeric, 'Basement flooded, need pumps and help moving two disabled residents.',    'MODERATE', 'COMPLETED', 'Colombo Water Rescue Bravo', 66, 65,   60),
    ('Thevarajah Sinnathurai', '0710000107', 'Jaffna',     9.6615000::numeric,  80.0255000::numeric, 'Fishing boat overdue, three crew missing since last night.',             'HIGH',     'COMPLETED', 'Jaffna Coastal Unit',      150, 149,  140),
    ('Nuwan Herath',           '0710000109', 'Ratnapura',  6.6828000::numeric,  80.3992000::numeric, 'Village cut off by flood water, food and drinking water needed.',        'HIGH',     'PENDING',   NULL,                       2, NULL, NULL),
    ('Ishara Kumari',          '0710000110', 'Badulla',    6.9934000::numeric,  81.0550000::numeric, 'Cracks on the slope above our house, family needs to be moved.',         'MODERATE', 'PENDING',   NULL,                       7, NULL, NULL),
    ('Aslam Rahuman',          '0710000111', 'Batticaloa', 7.7310000::numeric,  81.6747000::numeric, 'Well has dried up, forty families need drinking water.',                 'LOW',      'CANCELLED', NULL,                       200, NULL, NULL),
    ('Lakmal Weerasinghe',     '0710000112', 'Kalutara',   6.5854000::numeric,  79.9607000::numeric, 'Riverbank erosion threatening three houses.',                            'MODERATE', 'PENDING',   NULL,                       10, NULL, NULL)
) AS v(n, p, district, lat, lng, descr, prio, status, team, sub_hrs, asg_hrs, done_hrs)
JOIN districts d ON d.name = v.district
LEFT JOIN rescue_teams t ON t.name = v.team
WHERE NOT EXISTS (SELECT 1 FROM rescue_requests r WHERE r.description = v.descr);

-- 13. Ground reports from citizens (the Flutter app sends these) ---------------------------------------------------
INSERT INTO ground_reports (reported_by_citizen_id, district_id, category, photo_url, gps_lat, gps_lng, description,
                            status, verified_by_user_id, action_note, submitted_at)
SELECT c.id, d.id, v.cat, NULL, v.lat, v.lng, v.descr, v.status, o.id, v.note,
       (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => v.hrs)
FROM (VALUES
    ('Amaya Silva',            'Galle',      'FLOOD',     6.0300000::numeric,  80.2200000::numeric, 'Gin Ganga overflowing at Baddegama bridge, water entering houses.',               'PENDING_REVIEW', NULL,                   NULL,                                         2),
    ('Kasun Perera',           'Galle',      'FLOOD',     6.0561000::numeric,  80.2142000::numeric, 'Main road near Galle fort flooded about knee deep, vehicles turning back.',       'VERIFIED',       'Kasun Wickramasinghe', NULL,                                         5),
    ('Dilani Jayasinghe',      'Kandy',      'LANDSLIDE', 7.2540000::numeric,  80.5920000::numeric, 'Earth slip across Peradeniya Road, one lane blocked.',                           'ACTIONED',       'Tharindu Bandara',     'Road Development Authority team cleared one lane.', 4),
    ('Ruwan Rathnayake',       'Kandy',      'LANDSLIDE', 7.3010000::numeric,  80.6450000::numeric, 'Cracks in the ground behind the temple wall, trees leaning.',                   'PENDING_REVIEW', NULL,                   NULL,                                         1),
    ('Sanduni Gunawardena',    'Colombo',    'FLOOD',     6.9330000::numeric,  79.8500000::numeric, 'Drain overflowing at Galle Road Dehiwala, shops closing.',                       'ACTIONED',       'Nadeesha Fernando',    'Municipal council pumping the drain, notified the CMC control room.', 68),
    ('Chamara Dissanayake',    'Colombo',    'FLOOD',     6.9190000::numeric,  79.8780000::numeric, 'Kelani river bank at Kolonnawa is close to the road level.',                     'VERIFIED',       'Nadeesha Fernando',    NULL,                                         72),
    ('Thevarajah Sinnathurai', 'Jaffna',     'CYCLONE',   9.6700000::numeric,  80.0100000::numeric, 'Very strong wind, several roofs damaged near the lagoon.',                       'ACTIONED',       'Kirushanthan Selvaraj','Relief tarpaulins issued to four families.',  152),
    ('Priya Nadarajah',        'Jaffna',     'CYCLONE',   9.6500000::numeric,  80.0300000::numeric, 'Fishing boats pulled ashore, sea is rough and waves reach the road.',            'NEEDS_INFO',     'Kirushanthan Selvaraj','Please send an exact location.',             155),
    ('Nuwan Herath',           'Ratnapura',  'FLOOD',     6.6850000::numeric,  80.4010000::numeric, 'Kalu Ganga has covered the old bridge, village on the far side is cut off.',        'PENDING_REVIEW', NULL,                   NULL,                                         3),
    ('Ishara Kumari',          'Badulla',    'LANDSLIDE', 6.9800000::numeric,  80.9900000::numeric, 'Small landslide near the Haputale road, soil still moving.',                     'VERIFIED',       'Nadeesha Fernando',    NULL,                                         22),
    ('Aslam Rahuman',          'Batticaloa', 'DROUGHT',   7.7300000::numeric,  81.6700000::numeric, 'Village well is dry, people walking two kilometres for water.',                 'REJECTED',       'Mohamed Fazil',        'Duplicate of an earlier report from the same village.', 260),
    ('Lakmal Weerasinghe',     'Kalutara',   'FLOOD',     6.5900000::numeric,  79.9700000::numeric, 'Water level rising on the Kalu Ganga at Kalutara South, low houses affected.',    'PENDING_REVIEW', NULL,                   NULL,                                         9)
) AS v(citizen, district, cat, lat, lng, descr, status, officer, note, hrs)
JOIN users u ON u.full_name = v.citizen
JOIN citizens c ON c.id = u.id
JOIN districts d ON d.name = v.district
LEFT JOIN users o ON o.full_name = v.officer
WHERE NOT EXISTS (SELECT 1 FROM ground_reports g WHERE g.description = v.descr);

-- 14. Relief distributions (consignments and their items) ------------------------------------------------------------
WITH c(org, shelter, status, disp_hrs, deliv_hrs) AS (VALUES
    ('Disaster Management Centre',   'Galle Municipal Hall',          'DISPATCHED',  3, NULL::int),
    ('Sri Lanka Red Cross Society',  'Richmond College Hall, Galle',  'DELIVERED',  20, 16),
    ('Sarvodaya Shramadana Society', 'Peradeniya Temple Pavilion',    'DELIVERED',  12, 9),
    ('Disaster Management Centre',   'Colombo Sports Complex',        'DELIVERED',  60, 55),
    ('World Vision Lanka',           'Jaffna Central College Hall',   'CANCELLED',  100, NULL),
    ('Sri Lanka Army',               'Ratnapura Maha Vidyalaya Hall', 'DISPATCHED',  6, NULL)
), items(shelter, status, resource, qty) AS (VALUES
    ('Galle Municipal Hall',          'DISPATCHED', 'Rice',             300),
    ('Galle Municipal Hall',          'DISPATCHED', 'Dry ration packs', 60),
    ('Richmond College Hall, Galle',  'DELIVERED',  'Dry ration packs', 90),
    ('Peradeniya Temple Pavilion',    'DELIVERED',  'Blankets',         80),
    ('Peradeniya Temple Pavilion',    'DELIVERED',  'Tarpaulins',       25),
    ('Colombo Sports Complex',        'DELIVERED',  'Rice',             500),
    ('Colombo Sports Complex',        'DELIVERED',  'Bottled water',    200),
    ('Jaffna Central College Hall',   'CANCELLED',  'Medicine boxes',   15),
    ('Ratnapura Maha Vidyalaya Hall', 'DISPATCHED', 'Diesel',           150)
), ins AS (
    INSERT INTO relief_consignments (organization_id, shelter_id, status, dispatched_at, delivered_at)
    SELECT o.id, s.id, c.status,
           (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => c.disp_hrs),
           CASE WHEN c.deliv_hrs IS NULL THEN NULL
                ELSE (now() AT TIME ZONE 'Asia/Colombo') - make_interval(hours => c.deliv_hrs) END
    FROM c
    JOIN organizations o ON o.name = c.org
    JOIN shelters s ON s.name = c.shelter
    WHERE NOT EXISTS (SELECT 1 FROM relief_consignments x
                      WHERE x.shelter_id = s.id AND x.organization_id = o.id AND x.status = c.status)
    RETURNING id, shelter_id, status
)
INSERT INTO consignment_items (consignment_id, resource_id, quantity)
SELECT ins.id, r.id, i.qty
FROM ins
JOIN shelters s ON s.id = ins.shelter_id
JOIN items i ON i.shelter = s.name AND i.status = ins.status
JOIN resources r ON r.name = i.resource AND r.district_id = s.district_id;

COMMIT;

-- Check the result (read-only):
SELECT 'districts' AS what, count(*) FROM districts
UNION ALL SELECT 'organizations', count(*) FROM organizations
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'citizens', count(*) FROM citizens
UNION ALL SELECT 'rescue_teams', count(*) FROM rescue_teams
UNION ALL SELECT 'shelters', count(*) FROM shelters
UNION ALL SELECT 'shelter_occupants', count(*) FROM shelter_occupants
UNION ALL SELECT 'resources', count(*) FROM resources
UNION ALL SELECT 'hazard_events', count(*) FROM hazard_events
UNION ALL SELECT 'warnings', count(*) FROM warnings
UNION ALL SELECT 'warning_broadcast_channels', count(*) FROM warning_broadcast_channels
UNION ALL SELECT 'rescue_requests', count(*) FROM rescue_requests
UNION ALL SELECT 'ground_reports', count(*) FROM ground_reports
UNION ALL SELECT 'relief_consignments', count(*) FROM relief_consignments
UNION ALL SELECT 'consignment_items', count(*) FROM consignment_items;
