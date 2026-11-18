"""Summarize selected completed runs; ignores exploratory/discarded measurements."""
import argparse
import csv
import json
from pathlib import Path
import re


def cache_counts(path):
    output={}
    for line in path.read_text().splitlines():
        if not line.startswith('cache_gets_total'):continue
        cache=re.search(r'cache="([^"]+)"',line);result=re.search(r'result="([^"]+)"',line)
        if cache and result:output[(cache.group(1),result.group(1))]=float(line.rsplit(' ',1)[1])
    return output


def summarize(path):
    data=json.loads(path.read_text());metrics=data['metrics'];stem=path.stem
    control=json.loads(path.with_name(stem+'-controls.json').read_text())
    ticks_per_second=control.get('cpu_ticks_per_second',100)
    samples=[json.loads(line) for line in path.with_name(stem+'-telemetry.jsonl').read_text().splitlines()]
    cpu=[]
    for before,after in zip(samples,samples[1:]):
        if before['elapsed_s']<10 or 'jvm_cpu_ticks' not in before or 'jvm_cpu_ticks' not in after:continue
        cpu.append((after['jvm_cpu_ticks']-before['jvm_cpu_ticks'])*100/ticks_per_second/(after['elapsed_s']-before['elapsed_s']))
    endpoints={key.split('endpoint:')[1].rstrip('}'):value['values'] for key,value in metrics.items() if key.startswith('endpoint_ms{')}
    before=cache_counts(path.with_name(stem+'-before.prom'));after=cache_counts(path.with_name(stem+'-after.prom'))
    caches={}
    for name in sorted({key[0] for key in after}):
        hit=after.get((name,'hit'),0)-before.get((name,'hit'),0)
        miss=after.get((name,'miss'),0)-before.get((name,'miss'),0)
        if hit+miss:caches[name]={'hits':hit,'misses':miss,'hit_ratio':hit/(hit+miss)}
    def maximum(prefix):
        return max([value for sample in samples for name,value in sample.get('metrics',{}).items() if name.startswith(prefix)] or [0])
    return {
        'run':stem, 'fixture_demands':control.get('fixture_demands',100000 if stem.startswith('volume100k') else 20000), 'level':data['test']['level'], 'duration':data['test']['duration'],
        'http_requests':metrics['http_reqs']['values']['count'], 'http_rps_including_setup':metrics['http_reqs']['values']['rate'],
        'http_error_rate':metrics['http_req_failed']['values']['rate'], 'check_success_rate':metrics['checks']['values']['rate'],
        'max_read_p95_ms':max(value['p(95)'] for key,value in endpoints.items() if key!='create'),
        'create_p95_ms':endpoints.get('create',{}).get('p(95)'),
        'rss_peak_mib':max(int(sample['VmRSS'].split()[0])/1024 for sample in samples if 'VmRSS' in sample),
        'process_cpu_mean_pct_after_10s':sum(cpu)/len(cpu) if cpu else None,
        'process_cpu_max_pct_after_10s':max(cpu) if cpu else None,
        'hikari_pending_max_observed':maximum('hikaricp_connections_pending'),
        'hikari_active_max_observed':maximum('hikaricp_connections_active'),
        'db_active_max_observed':max([sample.get('db_activity',{}).get('active',0) for sample in samples] or [0]),
        'sampling_errors':sum('sampling_error' in sample for sample in samples),
        'thresholds_pass':all(threshold['ok'] for metric in metrics.values() for threshold in metric.get('thresholds',{}).values()),
        'endpoints':endpoints, 'caches':caches,
    }


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',default='performance/results/summary.json');parser.add_argument('--prefixes',default='heap384,heap256,volume100k,baseline');args=parser.parse_args()
    root=Path(__file__).resolve().parent
    prefixes=set(args.prefixes.split(','))
    files=[p for p in (root/'results').glob('*.json') if '-' in p.stem and p.stem.rsplit('-',1)[0] in prefixes and p.stem.rsplit('-',1)[1].isdigit()]
    rows=[summarize(path) for path in sorted(files,key=lambda p:(p.stem.rsplit('-',1)[0],int(p.stem.rsplit('-',1)[1])))]
    output=Path(args.output);output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(rows,indent=2))
    fields=['run','level','http_requests','http_rps_including_setup','http_error_rate','max_read_p95_ms','create_p95_ms','rss_peak_mib','process_cpu_mean_pct_after_10s','hikari_pending_max_observed','thresholds_pass']
    with output.with_suffix('.csv').open('w') as target:
        writer=csv.DictWriter(target,fieldnames=fields,extrasaction='ignore',lineterminator='\n');writer.writeheader();writer.writerows(rows)
    for row in rows:
        print(row['run'],'p95 read=',round(row['max_read_p95_ms']), 'create=',round(row['create_p95_ms'] or 0), 'RSS MiB=',round(row['rss_peak_mib'],1),'errors=',row['http_error_rate'])
