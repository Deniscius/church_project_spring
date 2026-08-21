import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const API_URL = __ENV.API_URL || 'https://missanye-api.onrender.com';
const WEB_ORIGIN = __ENV.WEB_ORIGIN || 'https://missanye-web.onrender.com';
const USERNAME = __ENV.K6_USERNAME;
const PASSWORD = __ENV.K6_PASSWORD;

const sessionDuration = new Trend('endpoint_session_duration', true);
const profileDuration = new Trend('endpoint_profile_duration', true);
const demandsDuration = new Trend('endpoint_demands_duration', true);
const statsDuration = new Trend('endpoint_stats_duration', true);
const scheduleDuration = new Trend('endpoint_schedule_duration', true);

export const options = {
  scenarios: {
    capacity_read_only: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 5 },
        { duration: '15s', target: 10 },
        { duration: '15s', target: 15 },
        { duration: '15s', target: 20 },
        { duration: '15s', target: 25 },
        { duration: '20s', target: 25 },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: [
      { threshold: 'rate<0.02', abortOnFail: true, delayAbortEval: '15s' },
    ],
    checks: [
      { threshold: 'rate>0.95', abortOnFail: true, delayAbortEval: '15s' },
    ],
    http_req_duration: [
      { threshold: 'p(95)<1500', abortOnFail: true, delayAbortEval: '20s' },
    ],
  },
};

function cookiePair(setCookie) {
  return String(setCookie || '').split(';', 1)[0];
}

export function setup() {
  if (!USERNAME || !PASSWORD) {
    fail('K6_USERNAME et K6_PASSWORD sont obligatoires.');
  }

  http.get(`${API_URL}/actuator/health`, { timeout: '45s' });

  const loginResponse = http.post(
    `${API_URL}/auth/login-multi-tenant`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    {
      headers: { 'Content-Type': 'application/json', Origin: WEB_ORIGIN },
      timeout: '45s',
      tags: { endpoint: 'setup_login' },
    },
  );

  const cookie = cookiePair(loginResponse.headers['Set-Cookie']);
  const parishId = loginResponse.json('selectedParoisse.id');
  if (loginResponse.status !== 200 || !cookie || !parishId) {
    fail(`Initialisation impossible (HTTP ${loginResponse.status}).`);
  }

  return { cookie, parishId };
}

function record(response, label, trend) {
  trend.add(response.timings.duration, { endpoint: label });
  check(response, {
    [`${label}: HTTP 200`]: (r) => r.status === 200,
    [`${label}: JSON`]: (r) =>
      String(r.headers['Content-Type'] || '').includes('application/json'),
  });
}

export default function (data) {
  const params = {
    headers: { Cookie: data.cookie, Origin: WEB_ORIGIN },
    timeout: '20s',
  };

  const responses = http.batch([
    ['GET', `${API_URL}/auth/me`, null,
      { ...params, tags: { endpoint: 'session' } }],
    ['GET', `${API_URL}/admin/profile`, null,
      { ...params, tags: { endpoint: 'profile' } }],
    ['GET', `${API_URL}/demandes/paroisse/${data.parishId}?page=0&size=20`, null,
      { ...params, tags: { endpoint: 'demands' } }],
    ['GET', `${API_URL}/demandes/paroisse/${data.parishId}/stats`, null,
      { ...params, tags: { endpoint: 'stats' } }],
    ['GET', `${API_URL}/dashboard/paroisses/${data.parishId}/programmations?jours=14`, null,
      { ...params, tags: { endpoint: 'schedule' } }],
  ]);

  record(responses[0], 'session', sessionDuration);
  record(responses[1], 'profil', profileDuration);
  record(responses[2], 'demandes', demandsDuration);
  record(responses[3], 'statistiques', statsDuration);
  record(responses[4], 'programmations', scheduleDuration);

  sleep(1);
}
