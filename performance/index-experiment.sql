\set ON_ERROR_STOP on
DO $$ BEGIN IF current_database()<>'missanye_perf' THEN RAISE EXCEPTION 'Only the isolated database is allowed'; END IF; END $$;
BEGIN;
CREATE INDEX perf_candidate_demande_recent ON demande(paroisse_id,created_at DESC) WHERE status_del=false;
SELECT 'CANDIDATE_PAGE_0';
EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON)
SELECT d.* FROM demande d WHERE d.paroisse_id=(SELECT id FROM paroisse WHERE public_id=md5('perf-parish-1')::uuid)
AND NOT d.status_del ORDER BY d.created_at DESC LIMIT 20;
ROLLBACK;
