#!/usr/bin/env python3
"""Operate only the named, internal Docker test project. Never accepts a remote URL."""
import argparse
import hashlib
import json
import math
import os
import re
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
PERF = ROOT / 'performance'
RESULTS = PERF / 'results'
COMPOSE = ['docker', 'compose', '-f', str(PERF / 'compose.yaml')]
LOCAL = 'http://127.0.0.1:8080'

def command(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, text=True, **kwargs)

def sql(statement):
    return command(COMPOSE + ['exec', '-T', 'db', 'psql', '-X', '-U', 'perf', '-d', 'missanye_perf', '-At', '-v', 'ON_ERROR_STOP=1', '-c', statement], capture_output=True).stdout.strip()

def wait_ready():
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            response=command(COMPOSE + ['exec','-T','app','curl','-fsS','--max-time','3',LOCAL+'/actuator/health'], capture_output=True)
            if json.loads(response.stdout).get('status') == 'UP': return
        except Exception:
            pass
        time.sleep(2)
    raise RuntimeError('Local benchmark backend did not become ready')

def authenticate_observer():
    response=command(COMPOSE + ['exec','-T','app','curl','-fsS','--max-time','15','-c','/tmp/perf-observer.cookies','-H','Content-Type: application/json','-d',json.dumps({'username':'perf-observer','password':'PerfOnly-2026!'}),LOCAL+'/auth/login'], capture_output=True)
    if not json.loads(response.stdout).get('publicId'): raise RuntimeError('Observer login failed')
    return None

def prometheus(opener):
    return command(COMPOSE + ['exec','-T','app','curl','-fsS','--max-time','10','-b','/tmp/perf-observer.cookies',LOCAL+'/actuator/prometheus'], capture_output=True).stdout

def selected_metrics(raw):
    keys = ('process_cpu_usage', 'jvm_memory_used_bytes', 'jvm_gc_pause_seconds_count', 'hikaricp_connections_active', 'hikaricp_connections_pending', 'hikaricp_connections_max', 'tomcat_threads_busy_threads')
    output = {}
    for line in raw.splitlines():
        if not line.startswith(keys): continue
        name, value = line.rsplit(' ', 1)
        try:
            number = float(value)
            if math.isfinite(number): output[name] = number
        except ValueError: pass
    return output

def resource_check(allow_shared=False):
    raw=command(['docker','info','--format','{{json .Warnings}}'],capture_output=True).stdout
    warnings=json.loads(raw) or []
    missing=any('without cgroups' in warning for warning in warnings)
    if missing and not allow_shared:
        raise RuntimeError('Docker quotas are not enforced. Pass --allow-shared-host explicitly; results cannot establish container capacity.')
    return warnings

def prepare():
    command(COMPOSE + ['up', '-d', 'db', 'app'])
    wait_ready()
    if sql("SELECT to_regclass('public.perf_fixture') IS NOT NULL") == 't':
        raise RuntimeError('Fixture already exists; use down before preparing a fresh benchmark')
    with (PERF / 'seed.sql').open() as source:
        command(COMPOSE + ['exec', '-T', 'db', 'psql', '-X', '-U', 'perf', '-d', 'missanye_perf', '-v', 'ON_ERROR_STOP=1'], stdin=source, stdout=(RESULTS / 'seed.log').open('w'))
    with (PERF / 'export-fixture.sql').open() as source:
        fixture = command(COMPOSE + ['exec', '-T', 'db', 'psql', '-X', '-U', 'perf', '-d', 'missanye_perf', '-At', '-v', 'ON_ERROR_STOP=1'], stdin=source, capture_output=True).stdout
    data = json.loads(fixture)
    assert data['marker'] == 'isolated-synthetic-only' and len(data['users']) == 20
    (PERF / 'fixture.json').write_text(json.dumps(data))
    jar = ROOT / 'Backend/target/church_project_spring-0.0.1-SNAPSHOT.jar'
    metadata = {
        'commit': command(['git', 'rev-parse', 'HEAD'], capture_output=True).stdout.strip(),
        'backend_dirty': subprocess.run(['git', 'diff', '--quiet', '--', 'Backend'], cwd=ROOT).returncode != 0,
        'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
        'requested_resources': {'app_cpu': 1, 'app_memory_mib': 512, 'db_cpu': 0.5, 'db_memory_mib': 512, 'hikari_max': 8, 'tomcat_max': 40},
        'docker_warnings': resource_check(args.allow_shared_host),
        'initial_demands': int(sql("SELECT count(*) FROM demande WHERE code_suivie LIKE 'PERF-%'")),
        'database': sql('SELECT version()'),
        'created_utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
    }
    (RESULTS / 'environment.json').write_text(json.dumps(metadata, indent=2))
    print('Isolated synthetic fixture ready:', metadata['initial_demands'], 'demands', flush=True)

def run_level(level, duration, run_id, workload):
    if sql('SELECT marker FROM perf_fixture') != 'isolated-synthetic-only': raise RuntimeError('Wrong database')
    warnings=resource_check(args.allow_shared_host)
    control={'docker_warnings':warnings,'entrypoint':json.loads(command(['docker','inspect',command(COMPOSE+['ps','-q','app'],capture_output=True).stdout.strip(),'--format','{{json .Config.Entrypoint}}'],capture_output=True).stdout),'actual_cgroup':command(COMPOSE+['exec','-T','app','sh','-c','cat /sys/fs/cgroup/cpu.max /sys/fs/cgroup/memory.max'],capture_output=True).stdout}
    control['cpu_ticks_per_second']=os.sysconf('SC_CLK_TCK')
    control['fixture_demands']=int(sql("SELECT count(*) FROM demande WHERE code_suivie LIKE 'PERF-%'"))
    (RESULTS/f'{run_id}-controls.json').write_text(json.dumps(control,indent=2))
    opener = authenticate_observer()
    (RESULTS / f'{run_id}-before.prom').write_text(prometheus(opener))
    sql('SELECT pg_stat_statements_reset()')
    env = {**os.environ, 'LEVEL': str(level), 'DURATION': duration, 'RUN_ID': run_id, 'WORKLOAD': workload}
    app = command(COMPOSE + ['ps', '-q', 'app'], capture_output=True).stdout.strip()
    db = command(COMPOSE + ['ps', '-q', 'db'], capture_output=True).stdout.strip()
    started = time.monotonic()
    with (RESULTS / f'{run_id}.log').open('w') as log, (RESULTS / f'{run_id}-telemetry.jsonl').open('w') as samples:
        process = subprocess.Popen(COMPOSE + ['--profile', 'load', 'run', '--rm', '-T', 'k6'], cwd=ROOT, env=env, text=True, stdout=log, stderr=subprocess.STDOUT)
        while process.poll() is None:
            sample = {'elapsed_s': round(time.monotonic() - started, 2)}
            try:
                if not any('without cgroups' in warning for warning in warnings):
                    stats = command(['docker', 'stats', '--no-stream', '--format', '{{json .}}', app, db], capture_output=True).stdout
                    sample['containers'] = [json.loads(line) for line in stats.splitlines() if line.strip()]
                proc=command(COMPOSE+['exec','-T','app','sh','-c',"cat /proc/1/stat; grep -E 'VmRSS|VmHWM|Cpus_allowed_list' /proc/1/status"],capture_output=True).stdout.splitlines()
                fields=proc[0].split();sample['jvm_cpu_ticks']=int(fields[13])+int(fields[14])
                for line in proc[1:]:
                    key,value=line.split(':',1);sample[key]=value.strip()
                sample['metrics'] = selected_metrics(prometheus(opener))
                sample['db_activity'] = json.loads(sql("SELECT json_build_object('active',count(*) FILTER(WHERE state='active'),'waiting',count(*) FILTER(WHERE wait_event_type='Lock'),'connections',count(*)) FROM pg_stat_activity WHERE datname=current_database() AND backend_type='client backend' AND pid<>pg_backend_pid()"))
            except Exception as error:
                sample['sampling_error'] = type(error).__name__
            samples.write(json.dumps(sample) + '\n'); samples.flush()
            time.sleep(3)
        code = process.wait()
    (RESULTS / f'{run_id}-after.prom').write_text(prometheus(opener))
    with (PERF / 'top-sql.sql').open() as source:
        output = command(COMPOSE + ['exec', '-T', 'db', 'psql', '-X', '-U', 'perf', '-d', 'missanye_perf', '--csv'], stdin=source, capture_output=True).stdout
    (RESULTS / f'{run_id}-sql.csv').write_text(output)
    print(f'{run_id}: k6 exit={code} (99 means a performance threshold failed)', flush=True)
    if code not in (0, 99): raise RuntimeError(f'Benchmark infrastructure failed: {code}; see results/{run_id}.log')
    return code

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['prepare', 'run', 'down'])
    parser.add_argument('--allow-shared-host', action='store_true')
    parser.add_argument('--levels', default='5,15,30,60')
    parser.add_argument('--duration', default='45s')
    parser.add_argument('--workload', choices=['mixed', 'public', 'desktop', 'admin', 'create'], default='mixed')
    parser.add_argument('--prefix', default='baseline')
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,40}', args.prefix): parser.error('prefix must be 1..40 safe filename characters')
    if not re.fullmatch(r'[1-9][0-9]{0,2}s', args.duration) or int(args.duration[:-1])>120: parser.error('duration must be 1s..120s')
    RESULTS.mkdir(exist_ok=True)
    if args.action == 'prepare':
        resource_check(args.allow_shared_host);prepare()
    elif args.action == 'down': command(COMPOSE + ['down', '-v'])
    else:
        wait_ready()
        levels = [int(value) for value in args.levels.split(',')]
        if not levels or any(value < 1 or value > 100 for value in levels): raise ValueError('Levels must be 1..100')
        for level in levels:
            run_level(level, args.duration, f'{args.prefix}-{level}', args.workload)
