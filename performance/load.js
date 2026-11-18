import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL;
if (__ENV.PERF_ISOLATED !== 'yes' || BASE !== 'http://app:8080') {
  throw new Error('This benchmark only targets http://app:8080 on the isolated Compose network.');
}
const fixture = JSON.parse(open('./fixture.json'));
if (fixture.marker !== 'isolated-synthetic-only') throw new Error('Synthetic fixture marker missing');
const level = Number(__ENV.LEVEL || 5);
if (!Number.isInteger(level) || level < 1 || level > 100) throw new Error('LEVEL must be 1..100');
const runId = __ENV.RUN_ID || 'baseline';
if (!/^[a-zA-Z0-9_-]{1,60}$/.test(runId)) throw new Error('Invalid RUN_ID');
const workload = __ENV.WORKLOAD || 'mixed';
const duration = __ENV.DURATION || '45s';
if (!/^[1-9][0-9]{0,2}s$/.test(duration) || Number(duration.slice(0,-1)) > 120) throw new Error('DURATION must be 1s..120s');
const latency = new Trend('endpoint_ms', true);
const unexpected = new Counter('unexpected_responses');
export const options = {
  scenarios: { journeys: { executor: 'constant-vus', vus: level, duration, gracefulStop: '15s' } },
  setupTimeout: '120s',
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    'endpoint_ms{endpoint:public}': ['p(95)<500'],
    'endpoint_ms{endpoint:list}': ['p(95)<500'],
    'endpoint_ms{endpoint:stats}': ['p(95)<500'],
    'endpoint_ms{endpoint:revision}': ['p(95)<500'],
    'endpoint_ms{endpoint:delta}': ['p(95)<500'],
    'endpoint_ms{endpoint:create}': ['p(95)<1000'],
  },
};

function request(method, path, body, endpoint, cookie, parish) {
  const result = http.request(method, BASE + path, body == null ? null : JSON.stringify(body), {
    redirects: 0,
    timeout: '10s',
    headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest', ...(cookie ? { Cookie: cookie } : {}) },
    // A fixed name prevents high cardinality from UUIDs and search terms.
    tags: { name: endpoint, endpoint, level: String(level), workload },
  });
  latency.add(result.timings.duration, { endpoint });
  const valid = check(result, { [`${endpoint} status`]: r => r.status === (endpoint === 'create' ? 201 : 200) });
  if (valid) {
    check(result, { [`${endpoint} payload`]: r => {
      try {
        const data = r.json();
        if (endpoint === 'public') return Array.isArray(data);
        if (endpoint === 'revision') return /^[a-f0-9]{64}$/.test(data.revision);
        if (endpoint === 'list') return Array.isArray(data.content) && data.content.every(row => row.paroissePublicId === parish);
        if (endpoint === 'delta') return Array.isArray(data.upserts) && data.upserts.every(row => row.paroissePublicId === parish);
        if (endpoint === 'create') return Boolean(data.codeSuivie) && data.paroissePublicId === body.paroissePublicId;
        return typeof data === 'object';
      } catch { return false; }
    } });
  }
  if (!valid) unexpected.add(1, { endpoint, status: String(result.status) });
  return result;
}

export function setup() {
  const users = fixture.users.map(user => {
    http.cookieJar().clear(BASE);
    const login = http.post(BASE + '/auth/login', JSON.stringify({ username: user.username, password: fixture.password }), {
      headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }, redirects: 0, tags: { name: 'setup_login', endpoint: 'setup_login' },
    });
    if (login.status !== 200 || !login.cookies.MS_AT?.length) fail(`Synthetic account login failed: ${login.status}`);
    return { ...user, cookie: `MS_AT=${login.cookies.MS_AT[0].value}` };
  });
  return users;
}

export default function (users) {
  const user = users[(exec.vu.idInTest - 1) % users.length];
  const iteration = exec.scenario.iterationInTest;
  const bucket = iteration % 20;
  if (workload === 'public' || (workload === 'mixed' && bucket < 8)) {
    request('GET', '/horaires/public/paroisses-actives', null, 'public');
  } else if (workload === 'desktop' || (workload === 'mixed' && bucket >= 15 && bucket < 19)) {
    request('GET', `/desktop/paroisses/${user.parish}/revision`, null, 'revision', user.cookie, user.parish);
    request('GET', `/desktop/paroisses/${user.parish}/sync/changes?after=${user.cursor}`, null, 'delta', user.cookie, user.parish);
  } else if (workload === 'create' || (workload === 'mixed' && bucket === 19)) {
    request('POST', '/demandes', { ...user.payload, nomFidele: 'Performance', prenomFidele: 'Fictif', intention: `Intention fictive de performance ${iteration}`, emailFidele: 'perf@example.test' }, 'create');
  } else {
    // Most admins stay on page one; some navigate to later pages.
    const page = iteration % 5 === 0 ? 1 + (iteration % 9) : 0;
    request('GET', `/demandes/paroisse/${user.parish}?page=${page}&size=20`, null, 'list', user.cookie, user.parish);
    request('GET', `/demandes/paroisse/${user.parish}/stats`, null, 'stats', user.cookie, user.parish);
  }
  sleep(1);
}

export function handleSummary(data) {
  // Setup data (including cookies) is never exported.
  return { [`results/${runId}.json`]: JSON.stringify({ ...data, test: { level, workload, runId, duration, synthetic: true } }, null, 2) };
}
