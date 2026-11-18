"""Verify per-IP throttling on a newly restarted isolated app, not under load."""
import collections
import json
import os
from pathlib import Path
import time
import urllib.request
import urllib.error

BASE='http://app:8080'
if os.environ.get('PERF_ISOLATED')!='yes':raise RuntimeError('Isolated mode required')

def status(request):
    try:
        with urllib.request.urlopen(request,timeout=10) as response:return response.status,dict(response.headers)
    except urllib.error.HTTPError as error:return error.code,dict(error.headers)

if __name__=='__main__':
    for _ in range(90):
        try:
            if status(BASE+'/actuator/health')[0]==200:break
        except Exception:pass
        time.sleep(2)
    reads=[status(BASE+'/horaires/public/paroisses-actives') for _ in range(61)]
    logins=[status(urllib.request.Request(BASE+'/auth/login',data=json.dumps({'username':'perf-missing-user','password':'Deliberately-invalid'}).encode(),headers={'Content-Type':'application/json'})) for _ in range(9)]
    output={'public_statuses':dict(collections.Counter(code for code,_ in reads)),'login_statuses':dict(collections.Counter(code for code,_ in logins)),'public_retry_after':reads[-1][1].get('Retry-After'),'login_retry_after':logins[-1][1].get('Retry-After')}
    Path('/work/results/rate-limit.json').write_text(json.dumps(output,indent=2))
    assert reads[-1][0]==429 and logins[-1][0]==429,output
    assert sum(code==200 for code,_ in reads)==60 and sum(code==401 for code,_ in logins)==8,output
    print(json.dumps(output))
