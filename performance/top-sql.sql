SELECT calls,round(total_exec_time::numeric,2) AS total_ms,round(mean_exec_time::numeric,2) AS mean_ms,
 rows,shared_blks_hit,shared_blks_read,temp_blks_written,left(query,4000) AS query
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%' AND query NOT LIKE '%pg_stat_activity%'
ORDER BY total_exec_time DESC LIMIT 15;
