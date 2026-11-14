"""Regression for profile contact persistence using synthetic API responses only.
Build with VITE_API_BASE_URL=https://api.example.test and run Vite preview.
"""
import asyncio
import json
import os
from urllib.parse import urlparse
from playwright.async_api import async_playwright, expect

FRONT = os.environ.get('PROFILE_TEST_FRONTEND', 'http://127.0.0.1:4173')
if urlparse(FRONT).hostname not in ('localhost', '127.0.0.1'):
    raise RuntimeError('Use a disposable local frontend')

async def main():
    user = {'id': 'fixture-profile', 'publicId': 'fixture-profile', 'username': 'fixture', 'role': 'SECRETAIRE', 'isGlobal': False, 'nom': 'Fixture', 'prenom': 'Test', 'email': 'fixture@example.test', 'telephone': '+22890000000', 'permissions': ['profile:read']}
    saved = None
    async with async_playwright() as p:
        browser = await p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH', '/usr/bin/chromium'), headless=True, args=['--no-sandbox'])
        page = await browser.new_page()
        await page.add_init_script("localStorage.setItem('church_auth_session','1');localStorage.setItem('church_auth_user'," + json.dumps(json.dumps(user)) + ");")
        async def route(r):
            nonlocal saved
            if r.request.url.startswith('https://api.example.test'):
                body = {'user': user, 'paroisses': []} if '/auth/me' in r.request.url else user
                if r.request.method == 'PUT':
                    saved = r.request.post_data_json
                    body = {**user, **saved}
                await r.fulfill(content_type='application/json', headers={'Access-Control-Allow-Origin': FRONT, 'Access-Control-Allow-Credentials': 'true'}, body=json.dumps(body))
            elif r.request.url.startswith(FRONT):
                await r.continue_()
            else:
                await r.abort()
        await page.route('**/*', route)
        await page.goto(FRONT + '/admin/profil')
        await expect(page.locator('#profile-email')).to_be_disabled()
        await page.locator('#profile-telephone').fill('+22890123456')
        await page.get_by_role('button', name='Enregistrer', exact=True).click()
        await expect(page.locator('#profile-telephone')).to_have_value('+22890123456')
        await expect(page.get_by_text('Informations personnelles mises à jour.', exact=True)).to_be_visible()
        assert saved == {'nom': 'Fixture', 'prenom': 'Test', 'email': 'fixture@example.test', 'telephone': '+22890123456'}
        print('PASS: profile saves the new telephone and preserves read-only professional email')
        await browser.close()

asyncio.run(main())
