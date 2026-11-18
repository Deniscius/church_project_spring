"""Browser regression: run Vite on 5174 with VITE_API_BASE_URL=http://127.0.0.1:5174/mock-api.
Requires Python Playwright and Chromium; uses only synthetic data and mocked API/streams.
"""
import asyncio
import json
from playwright.async_api import async_playwright, expect

PARISH = '11111111-1111-4111-8111-111111111111'
DEMAND = '22222222-2222-4222-8222-222222222222'

async def main():
    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch(executable_path='/usr/bin/chromium', headless=True, args=['--no-sandbox'])
        page = await browser.new_page()
        errors = []
        page.on('pageerror', lambda error: errors.append(str(error)))
        await page.add_init_script('''
            localStorage.setItem('church_auth_session','1');
            window.testStreams=[];
            window.EventSource=class {
              constructor(url,options){this.url=url;this.options=options;this.listeners={};this.closed=false;window.testStreams.push(this);setTimeout(()=>this.emit('revision','a'.repeat(64)),30);}
              addEventListener(name,listener){this.listeners[name]=listener;}
              emit(name,data){if(!this.closed)this.listeners[name]?.({data});}
              close(){this.closed=true;}
            };
        ''')
        user = {'id': 'user-test', 'nom': 'Test', 'prenom': 'User', 'username': 'tester', 'role': 'SECRETAIRE', 'isGlobal': False, 'permissions': ['demand:read', 'demand:edit', 'parish:read']}
        await page.add_init_script("localStorage.setItem('church_auth_user'," + json.dumps(json.dumps(user)) + ");")
        parish = {'id': PARISH, 'publicId': PARISH, 'nom': 'Paroisse test', 'adresse': 'Test', 'active': True, 'statutTenant': 'ACTIVE'}
        await page.add_init_script("localStorage.setItem('paroisses'," + json.dumps(json.dumps([parish])) + ");localStorage.setItem('selectedParoisse'," + json.dumps(json.dumps(parish)) + ");")
        demand = {'publicId': DEMAND, 'paroissePublicId': PARISH, 'paroisseNom': 'Paroisse test', 'intention': 'Intention initiale', 'codeSuivie': 'MS-TEST', 'statutDemande': 'VALIDEE', 'statutPaiement': 'NON_PAYE', 'statutValidation': 'VALIDEE', 'montant': 5000, 'datesCelebration': [], 'celebrationSlots': []}
        calls = []
        async def route(request):
            path = request.request.url.split('/mock-api')[-1]
            calls.append(path)
            if path == '/auth/me': body = {'user': user, 'paroisses': [parish], 'selectedParoisse': parish}
            elif path.startswith('/paroisses'): body = parish if path.endswith(PARISH) else [parish]
            elif path == '/demandes/' + DEMAND: body = dict(demand)
            elif path.endswith('/revision'): body = {'revision': 'd' * 64}
            else: body = []
            await request.fulfill(content_type='application/json', body=json.dumps(body))
        await page.route('**/mock-api/**', route)
        await page.goto('http://127.0.0.1:5174/admin/demandes/' + DEMAND)
        await page.wait_for_timeout(800)
        await expect(page.get_by_text('Intention initiale', exact=True)).to_be_visible()
        await page.wait_for_timeout(300)
        assert await page.evaluate('window.testStreams.filter(s=>!s.closed).length') == 1
        assert await page.evaluate('window.testStreams.find(s=>!s.closed).options.withCredentials') is True
        demand['intention'] = 'Intention modifiée sur un autre poste'
        await page.evaluate("window.testStreams.find(s=>!s.closed).emit('revision','b'.repeat(64))")
        await expect(page.get_by_text(demand['intention'], exact=True)).to_be_visible()
        await page.get_by_role('button', name='Corriger l’intention').click()
        field = page.get_by_role('dialog').get_by_role('textbox')
        await field.fill('Saisie locale à conserver')
        demand['intention'] = 'Nouvelle modification distante'
        await page.evaluate("window.testStreams.find(s=>!s.closed).emit('revision','c'.repeat(64))")
        await page.wait_for_timeout(200)
        await expect(field).to_have_value('Saisie locale à conserver')
        await page.get_by_role('button', name='Annuler', exact=True).click()
        await expect(page.get_by_text(demand['intention'], exact=True)).to_be_visible()
        before = len(calls)
        await page.evaluate("window.testStreams.find(s=>!s.closed).emit('revision','c'.repeat(64))")
        await page.wait_for_timeout(200)
        assert len(calls) == before, 'Unchanged revisions must not reload data'
        await page.evaluate("window.dispatchEvent(new Event('offline'))")
        # Simulate EventSource failure: the fallback checks only the revision endpoint.
        await page.evaluate("window.testStreams.find(s=>!s.closed).onerror()")
        await page.wait_for_timeout(300)
        assert any(path.endswith('/revision') for path in calls)
        await page.goto('http://127.0.0.1:5174/')
        await page.wait_for_timeout(200)
        assert not errors, errors
        print('PASS: shared credentialed stream, remote refresh, draft preserved, unchanged revision ignored, fallback')
        await browser.close()

asyncio.run(main())
