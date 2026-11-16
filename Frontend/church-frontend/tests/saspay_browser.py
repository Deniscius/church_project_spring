"""SasPay payment UI on a disposable build; synthetic API only, no real payment."""
import json
import os
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect
FRONT = os.environ.get('SASPAY_TEST_FRONTEND', 'http://127.0.0.1:4173')
if urlparse(FRONT).hostname not in ('localhost', '127.0.0.1'):
    raise RuntimeError('Use a loopback frontend with synthetic data')
CODE = 'MS-SP-ABCDEFGHJK'
with sync_playwright() as p:
    browser = p.chromium.launch(executable_path='/usr/bin/chromium', headless=True, args=['--no-sandbox'])
    for width in (1280, 390):
        context = browser.new_context(viewport={'width': width, 'height': 900})
        context.add_init_script("sessionStorage.setItem('ms_nav_payment_code','" + CODE + "')")
        page = context.new_page()
        posted = []
        def route(request):
            url = request.request.url
            if url.startswith('https://api.example.test'):
                headers = {'Access-Control-Allow-Origin': FRONT, 'Access-Control-Allow-Credentials': 'true', 'Access-Control-Allow-Headers': 'content-type,x-requested-with', 'Access-Control-Allow-Methods': 'GET,POST,OPTIONS'}
                if request.request.method == 'OPTIONS':
                    request.fulfill(status=204, headers=headers); return
                if '/paiements/quote/' in url:
                    data = {'montantFacture': 2000, 'montantCharge': 2100, 'montantFraisPlateforme': 100, 'montantFraisAgregeateur': 0, 'providerFeesPending': True}
                elif '/paiements/checkout/' in url:
                    posted.append(json.loads(request.request.post_data or '{}'))
                    request.fulfill(status=409, content_type='application/json', headers=headers, body=json.dumps({'message': 'SasPay simulé : paiement non initié'})); return
                elif '/demandes/' in url:
                    data = {'publicId': 'fixture-demand', 'codeSuivie': CODE, 'modePaiement': 'FLOOZ', 'statutPaiement': 'NON_PAYE', 'statutDemande': 'EN_ATTENTE', 'intention': 'Intention synthétique pour le test SasPay', 'montant': 2000, 'paroisseNom': 'Paroisse de test'}
                elif '/auth/me' in url:
                    request.fulfill(status=401, content_type='application/json', headers=headers, body='{}'); return
                else:
                    data = []
                request.fulfill(content_type='application/json', headers=headers, body=json.dumps(data))
            elif url.startswith(FRONT):
                request.continue_()
            else:
                request.abort()
        page.route('**/*', route)
        page.goto(FRONT + '/paiement')
        expect(page.get_by_text('Sous-total hors frais SasPay', exact=True)).to_be_visible()
        expect(page.get_by_text('Confirmés sur SasPay', exact=True)).to_be_visible()
        email = page.get_by_role('textbox', name='E-mail pour le paiement')
        email.fill('payeur@example.test')
        page.get_by_role('button', name='Payer avec SasPay', exact=True).click()
        expect(page.get_by_text('SasPay simulé : paiement non initié', exact=True)).to_be_visible()
        assert posted == [{'email': 'payeur@example.test'}]
        assert page.url.endswith('/paiement')
        assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth')
        page.screenshot(path=f'/workspace/work/saspay-payment-{width}.png', full_page=True)
        context.close()
    browser.close()
print('PASS: desktop/mobile SasPay fee disclosure, payer email, safe failure and no horizontal overflow')
