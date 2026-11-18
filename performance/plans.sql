-- Diagnostic only; the app uses additional entity joins. No production connection is accepted.
SELECT 'LIST_PAGE_0';
EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON)
SELECT d.* FROM demande d WHERE d.paroisse_id=(SELECT id FROM paroisse WHERE public_id=md5('perf-parish-1')::uuid)
AND NOT d.status_del ORDER BY d.created_at DESC LIMIT 20;
SELECT 'LIST_PAGE_9';
EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON)
SELECT d.* FROM demande d WHERE d.paroisse_id=(SELECT id FROM paroisse WHERE public_id=md5('perf-parish-1')::uuid)
AND NOT d.status_del ORDER BY d.created_at DESC LIMIT 20 OFFSET 180;
SELECT 'SYNC_DELTA_EMPTY';
EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON)
SELECT demand_public_id,revision FROM desktop_demand_change
WHERE parish_id=(SELECT id FROM paroisse WHERE public_id=md5('perf-parish-1')::uuid)
AND revision>(SELECT revision FROM desktop_sync_clock WHERE parish_id=(SELECT id FROM paroisse WHERE public_id=md5('perf-parish-1')::uuid))
ORDER BY revision LIMIT 101;
