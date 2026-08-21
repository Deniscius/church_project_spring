import http from 'k6/http';
import { check, fail, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const API_URL = __ENV.API_URL || 'https://missanye-api.onrender.com';
const WEB_ORIGIN = __ENV.WEB_ORIGIN || 'https://missanye-web.onrender.com';
const USERNAME = __ENV.K6_USERNAME;
const PASSWORD = __ENV.K6_PASSWORD;

const authenticatedDuration = new Trend('authenticated_endpoint_duration', true);

export const options = {
  // Le JWT est stocké dans un cookie HttpOnly. K6 réinitialise normalement
  // les cookies entre deux itérations, contrairement à un navigateur réel.
  noCookiesReset: true,
  scenarios: {
    authenticated_reads: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 1 },
        { duration: '20s', target: 3 },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    authenticated_endpoint_duration: ['p(95)<2000'],
  },
};

export function setup() {
  if (!USERNAME || !PASSWORD) {
    fail('K6_USERNAME et K6_PASSWORD sont obligatoires.');
  }
  http.get(`${API_URL}/actuator/health`, { timeout: '45s' });
}

function login() {
  const response = http.post(
    `${API_URL}/auth/login-multi-tenant`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    {
      headers: {
        'Content-Type': 'application/json',
        Origin: WEB_ORIGIN,
      },
      tags: { endpoint: 'login' },
      timeout: '45s',
    },
  );

  const ok = check(response, {
    'connexion: HTTP 200': (r) => r.status === 200,
    'connexion: cookie HttpOnly reçu': (r) =>
      String(r.headers['Set-Cookie'] || '').includes('HttpOnly'),
    'connexion: jeton absent du JSON': (r) => {
      try {
        return !r.json('token');
      } catch (_) {
        return false;
      }
    },
  });

  if (!ok) {
    fail(`Connexion impossible (HTTP ${response.status}).`);
  }

  return response.json('selectedParoisse.id');
}

let parishId;

function getAndCheck(path, label) {
  const response = http.get(`${API_URL}${path}`, {
    headers: { Origin: WEB_ORIGIN },
    tags: { endpoint: label },
    timeout: '30s',
  });

  authenticatedDuration.add(response.timings.duration, { endpoint: label });
  check(response, {
    [`${label}: HTTP 200`]: (r) => r.status === 200,
    [`${label}: réponse JSON`]: (r) =>
      String(r.headers['Content-Type'] || '').includes('application/json'),
  });
}

export default function () {
  if (!parishId) {
    parishId = login();
    if (!parishId) fail('La paroisse sélectionnée est absente de la réponse.');
  }

  group('session et profil', () => {
    getAndCheck('/auth/me', 'session');
    getAndCheck('/admin/profile', 'profil');
  });

  group('activité paroissiale', () => {
    getAndCheck(`/demandes/paroisse/${parishId}?page=0&size=20`, 'demandes');
    getAndCheck(`/demandes/paroisse/${parishId}/stats`, 'statistiques');
    getAndCheck(`/dashboard/paroisses/${parishId}/programmations?jours=14`, 'programmations');
  });

  sleep(1);
}
