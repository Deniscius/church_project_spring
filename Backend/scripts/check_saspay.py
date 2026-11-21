#!/usr/bin/env python3
"""Read-only SasPay checks. Never prints credentials or initiates a payment."""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from urllib.parse import urlparse

SANDBOX = 'https://sandbox-api.saspay.me/api/v1'
LIVE = 'https://api.saspay.me/api/v1'


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Never forward the Authorization header to a redirected endpoint.
        return None


def configuration(env, allow_live=False):
    key = env.get('SASPAY_API_KEY', '').strip()
    secret = env.get('SASPAY_WEBHOOK_SECRET', '')
    app = env.get('APP_URL', '').strip()
    missing = [name for name, value in [('SASPAY_API_KEY', key),
               ('SASPAY_WEBHOOK_SECRET', secret), ('APP_URL', app)] if not value]
    if missing:
        raise ValueError('Configuration incomplète : ' + ', '.join(missing))
    if (not (key.startswith('sk_test_') or key.startswith('sk_live_')) or len(key) <= 8
            or any(c.isspace() or ord(c) < 32 or ord(c) == 127 or c in "\"'" for c in key)):
        raise ValueError('Format de clé SasPay invalide : vérifier la clé sans guillemets ni espaces internes.')
    sandbox = key.startswith('sk_test_')
    if not sandbox and not allow_live:
        raise ValueError('Le mode live exige explicitement --allow-live ; aucune requête envoyée.')
    base = env.get('SASPAY_API_BASE_URL', '').strip()
    if not base or base.lower() == 'auto':
        base = SANDBOX if sandbox else LIVE
    base = base.rstrip('/')
    try:
        api, front = urlparse(base), urlparse(app)
        if (api.scheme != 'https' or api.hostname not in {'api.saspay.me', 'sandbox-api.saspay.me'}
                or api.username or api.password or api.query or api.fragment
                or api.port not in (None, 443) or api.path != '/api/v1'
                or front.scheme != 'https' or not front.hostname or front.username or front.password
                or front.query or front.fragment or any(c.isspace() for c in app)):
            raise ValueError()
    except ValueError:
        raise ValueError('URLs SasPay et APP_URL invalides : HTTPS et URL API officielle requis.') from None
    if sandbox != (api.hostname == 'sandbox-api.saspay.me'):
        raise ValueError('Environnement incohérent : SASPAY_API_BASE_URL doit correspondre à la clé sandbox/live (ou auto).')
    return key, base, 'sandbox' if sandbox else 'live'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline', action='store_true', help='Validate configuration without network access')
    parser.add_argument('--allow-live', action='store_true', help='Explicitly allow read-only checks with a live key')
    args = parser.parse_args()
    try:
        key, base, mode = configuration(os.environ, args.allow_live)
    except ValueError as error:
        print(str(error))
        return 2
    print('Configuration valide ; mode=' + mode + ', API=' + urlparse(base).hostname + '. Aucun secret affiché.')
    if args.offline:
        return 0
    opener = urllib.request.build_opener(NoRedirect())
    try:
        for path, authenticated in [('countries/', False), ('pricing/my-rates/', True)]:
            headers = {'Accept': 'application/json'}
            if authenticated:
                headers['Authorization'] = 'Bearer ' + key
            request = urllib.request.Request(base + '/' + path, headers=headers)
            with opener.open(request, timeout=15) as response:
                data = json.load(response)
            if isinstance(data, dict) and data.get('success') is False:
                raise ValueError()
            print('Lecture API ' + path + ' : OK')
    except urllib.error.HTTPError as error:
        if error.code == 401:
            print('Authentification SasPay refusée (HTTP 401) : vérifier clé et environnement.')
        elif error.code == 403:
            print('Accès SasPay refusé (HTTP 403) : vérifier scope PAYIN/BOTH, droits et état du marchand.')
        else:
            print('Vérification SasPay refusée (HTTP ' + str(error.code) + ').')
        print('Aucun nouvel essai automatique, aucun paiement créé.')
        return 1
    except Exception:
        print('Vérification SasPay indisponible. Aucun paiement créé ni nouvel essai automatique.')
        return 1
    print('Accès API vérifié. Le paiement sandbox et le webhook restent à tester.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
