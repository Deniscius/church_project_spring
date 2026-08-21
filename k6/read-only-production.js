import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const WEB_URL = __ENV.WEB_URL || 'https://missanye-web.onrender.com';
const API_URL = __ENV.API_URL || 'https://missanye-api.onrender.com';

const businessDuration = new Trend('business_endpoint_duration', true);
const functionalFailures = new Rate('functional_failures');

export const options = {
  discardResponseBodies: false,
  scenarios: {
    production_read_only: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 2 },
        { duration: '30s', target: 5 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    business_endpoint_duration: ['p(95)<3000'],
  },
};

export function setup() {
  http.get(`${API_URL}/actuator/health`, {
    tags: { endpoint: 'warmup_health' },
    timeout: '45s',
  });
  http.get(WEB_URL, {
    tags: { endpoint: 'warmup_frontend' },
    timeout: '45s',
  });
}

function validate(response, label) {
  const ok = check(response, {
    [`${label}: HTTP 200`]: (r) => r.status === 200,
    [`${label}: réponse non vide`]: (r) => Boolean(r.body && r.body.length > 0),
  });
  functionalFailures.add(!ok, { endpoint: label });
  businessDuration.add(response.timings.duration, { endpoint: label });
}

export default function () {
  group('site public', () => {
    validate(http.get(WEB_URL, {
      tags: { endpoint: 'frontend_home' },
      timeout: '30s',
    }), 'frontend');
  });

  group('API publique', () => {
    const responses = http.batch([
      ['GET', `${API_URL}/actuator/health`, null, {
        tags: { endpoint: 'health' }, timeout: '30s',
      }],
      ['GET', `${API_URL}/paroisses/public`, null, {
        tags: { endpoint: 'public_parishes' }, timeout: '30s',
      }],
      ['GET', `${API_URL}/doyennes`, null, {
        tags: { endpoint: 'deaneries' }, timeout: '30s',
      }],
      ['GET', `${API_URL}/type-paiement`, null, {
        tags: { endpoint: 'payment_types' }, timeout: '30s',
      }],
      ['GET', `${API_URL}/plans-saas/public`, null, {
        tags: { endpoint: 'public_plans' }, timeout: '30s',
      }],
    ]);

    validate(responses[0], 'health');
    validate(responses[1], 'paroisses');
    validate(responses[2], 'doyennes');
    validate(responses[3], 'types de paiement');
    validate(responses[4], 'plans SaaS');
  });

  sleep(1);
}
