"""Synthetic SSE fan-out probe; run only on the internal Compose network."""
import concurrent.futures
import http.cookies
import json
import os
from pathlib import Path
import statistics
import threading
import time
import urllib.request

BASE = 'http://app:8080'
if os.environ.get('PERF_ISOLATED') != 'yes': raise RuntimeError('Isolated mode required')
fixture = json.loads(Path('/work/fixture.json').read_text())
if fixture['marker'] != 'isolated-synthetic-only': raise RuntimeError('Wrong fixture')
lock = threading.Lock()
stop = threading.Event()
results = []


def account(user):
    request = urllib.request.Request(BASE+'/auth/login', data=json.dumps({'username':user['username'],'password':fixture['password']}).encode(), headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(request,timeout=15) as response:
        cookie=http.cookies.SimpleCookie();cookie.load(response.headers.get('Set-Cookie',''))
        return 'MS_AT='+cookie['MS_AT'].value


def listen(user):
    result={'connected':False,'events':0,'updates':0,'expected_update':user['parish']==fixture['users'][0]['parish']}
    try:
        cookie=account(user)
        request=urllib.request.Request(BASE+'/desktop/paroisses/'+user['parish']+'/events',headers={'Cookie':cookie,'Accept':'text/event-stream'})
        started=time.monotonic()
        with urllib.request.urlopen(request,timeout=20) as response:
            result['http_status']=response.status
            result['connected']=response.status==200
            result['connect_ms']=round((time.monotonic()-started)*1000,2)
            with lock: results.append(result)
            previous=None
            while not stop.is_set():
                line=response.readline().decode()
                if not line:break
                if line.startswith('data:'):
                    value=line[5:].strip()
                    if len(value)!=64:raise RuntimeError('Invalid revision')
                    with lock:
                        result['events']+=1
                        if previous is not None and value!=previous:
                            result['updates']+=1
                            result['update_after_mutation_s']=round(time.monotonic()-mutation_at,3)
                    previous=value
    except Exception as error:
        result['error']=type(error).__name__
        with lock:
            if result not in results:results.append(result)


if __name__=='__main__':
    users=fixture['users']
    started=time.monotonic()
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(users)) as pool:
        futures=[pool.submit(listen,user) for user in users]
        deadline=time.monotonic()+20
        while time.monotonic()<deadline:
            with lock: active=sum(result['events']>0 for result in results)
            if active==len(users):break
            time.sleep(.1)
        if active!=len(users):stop.set();raise RuntimeError(f'Only {active} streams became ready')
        user=users[0]
        request=urllib.request.Request(BASE+'/demandes',data=json.dumps(user['payload']).encode(),headers={'Content-Type':'application/json'})
        mutation_at=time.monotonic()
        with urllib.request.urlopen(request,timeout=15) as response: mutation_status=response.status
        time.sleep(18)
        stop.set()
        for future in futures:future.result(timeout=25)
    output={'clients':len(users),'duration_s':round(time.monotonic()-started,2),'mutation_status':mutation_status,'connected':sum(result['connected'] for result in results),'expected_changed_clients':sum(result['expected_update'] for result in results),'changed_clients':sum(result['updates']>0 for result in results),'errors':[result['error'] for result in results if 'error' in result],'results':results}
    Path('/work/results/sse.json').write_text(json.dumps(output,indent=2))
    assert output['connected']==len(users) and not output['errors'],output
    assert output['changed_clients']==output['expected_changed_clients'],output
    print(json.dumps({key:value for key,value in output.items() if key!='results'}))
