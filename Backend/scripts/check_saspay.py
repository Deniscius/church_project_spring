#!/usr/bin/env python3
"""Read-only configuration check. Uses environment credentials; never initiates a payment."""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from urllib.parse import urlparse

parser = argparse.ArgumentParser()
parser.add_argument('--offline', action='store_true', help='Validate environment only')
parser.add_argument('--allow-live', action='store_true', help='Explicitly allow read-only checks with a live key')
args = parser.parse_args()
key = os.environ.get('SASPAY_API_KEY', '')
secret = os.environ.get('SASPAY_WEBHOOK_SECRET', '')
app = os.environ.get('APP_URL', '')
base = os.environ.get('SASPAY_API_BASE_URL', 'https://api.saspay.me/api/v1').rstrip('/')
missing = [name for name, value in [('SASPAY_API_KEY', key), ('SASPAY_WEBHOOK_SECRET', secret), ('APP_URL', app)] if not value]
if missing:
    print('Configuration incomplète : ' + ', '.join(missing)); sys.exit(2)
if not key.startswith('sk_test_') and not (args.allow_live and key.startswith('sk_live_')):
    print('Une clé sk_test_ est requise. Le mode live exige --allow-live.'); sys.exit(2)
api_url, app_url = urlparse(base), urlparse(app)
if (api_url.scheme != 'https' or api_url.hostname not in {'api.saspay.me', 'sandbox-api.saspay.me'}
        or api_url.username or api_url.password or api_url.query or api_url.fragment
        or app_url.scheme != 'https' or not app_url.hostname or app_url.username or app_url.password
        or app_url.query or app_url.fragment):
    print('Les URLs API SasPay et APP_URL doivent être des URLs HTTPS valides.'); sys.exit(2)
print('Variables présentes ; aucun secret affiché. URL de retour : ' + app + '. Le webhook /webhooks/saspay doit être configuré sur le backend.')
if args.offline:
    sys.exit(0)
try:
    for path, authenticated in [('countries/', False), ('pricing/my-rates/', True)]:
        headers = {'Accept': 'application/json'}
        if authenticated:
            headers['Authorization'] = 'Bearer ' + key
        with urllib.request.urlopen(urllib.request.Request(base + '/' + path, headers=headers), timeout=15) as response:
            data = json.load(response)
        if isinstance(data, dict) and data.get('success') is False:
            raise ValueError()
        print('Lecture API ' + path + ' : OK')
except urllib.error.HTTPError as error:
    print('Vérification SasPay refusée (HTTP ' + str(error.code) + '). Aucun nouvel essai automatique.'); sys.exit(1)
except Exception:
    print('Vérification SasPay indisponible. Aucun paiement n’a été créé.'); sys.exit(1)
print('Accès API vérifié. Le paiement sandbox et le webhook restent à tester depuis le tableau de bord.')
