"""Local integration regression: seed ApplicationStartupSmokeTest, run HTTPS API + Vite.
Requires Python Playwright and Chromium. Uses only disposable CI credentials on loopback.
"""
import asyncio
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
from urllib.parse import urlparse
from playwright.async_api import async_playwright, expect

FRONT = os.environ.get('AUTH_TEST_FRONTEND', 'http://127.0.0.1:5173')
API = os.environ.get('AUTH_TEST_API', 'https://localhost:18443')
CERT = Path(os.environ.get('AUTH_TEST_CERT', '/tmp/desktop-shared-tls/server.crt'))
for url in (FRONT, API):
    if urlparse(url).hostname not in ('localhost', '127.0.0.1'):
        raise RuntimeError('This fixture must run against a disposable loopback environment')

async def sign_in(page):
    await page.locator('#login-username').fill('ci-platform-admin')
    await page.locator('#login-password').fill('CiStrongPassword-2026!')
    await page.get_by_role('button', name='Se connecter', exact=True).click()

async def protected(page):
    await page.wait_for_url('**/admin/paroisses', timeout=20000)
    await expect(page.locator('#login-username')).to_have_count(0)

async def main():
    public = subprocess.check_output(['openssl','x509','-in',str(CERT),'-pubkey','-noout'])
    der = subprocess.run(['openssl','pkey','-pubin','-outform','DER'],input=public,check=True,capture_output=True).stdout
    pin=base64.b64encode(hashlib.sha256(der).digest()).decode()
    async with async_playwright() as p:
        browser=await p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH','/usr/bin/chromium'),headless=True,args=['--no-sandbox','--ignore-certificate-errors-spki-list='+pin,'--test-third-party-cookie-phaseout'])
        async def context():
            ctx=await browser.new_context()
            async def local_only(route):
                if urlparse(route.request.url).hostname in ('localhost','127.0.0.1'): await route.continue_()
                else: await route.abort()
            await ctx.route('**/*',local_only)
            page=await ctx.new_page()
            cdp=await ctx.new_cdp_session(page)
            await cdp.send('Network.enable')
            await cdp.send('Network.setCookieControls',{'enableThirdPartyCookieRestriction':True,'disableThirdPartyCookieMetadata':True,'disableThirdPartyCookieHeuristics':True})
            return ctx,page
        ctx,page=await context()
        await page.goto(FRONT+'/admin/login',wait_until='domcontentloaded')
        await sign_in(page);await protected(page)
        cookies=[c for c in await ctx.cookies() if c['name']=='MS_AT']
        assert cookies and cookies[0].get('partitionKey') and cookies[0]['secure'] and cookies[0]['httpOnly']
        await page.reload(wait_until='domcontentloaded');await protected(page)
        print('PASS: cross-site sign-in and reload with third-party cookies restricted')
        async def unavailable(route): await route.fulfill(status=503,content_type='application/json',body='{}')
        await page.route(API+'/auth/me',unavailable);await page.reload(wait_until='domcontentloaded')
        await expect(page.get_by_role('button',name='Réessayer',exact=True)).to_be_visible()
        assert await page.evaluate("localStorage.getItem('church_auth_session')")
        await page.unroute(API+'/auth/me',unavailable);await page.reload(wait_until='domcontentloaded');await protected(page)
        print('PASS: temporary bootstrap outage preserves the profile and can be retried')
        await ctx.close()
        ctx,page=await context()
        async def reject(route): await route.fulfill(status=401,content_type='application/json',body='{}')
        await page.route(API+'/auth/me',reject);await page.goto(FRONT+'/admin/login',wait_until='domcontentloaded');await sign_in(page)
        await expect(page.get_by_text('Vos identifiants sont acceptés, mais le navigateur ne confirme pas la session.',exact=False).first).to_be_visible()
        assert '/admin/login' in page.url
        assert not await page.evaluate("localStorage.getItem('church_auth_session')")
        assert await page.get_by_text('Connexion réussie.',exact=True).count()==0
        print('PASS: unconfirmed cookie session never produces a success notification')
        await ctx.close()
        for bootstrap in (True,False):
            ctx,page=await context();entered=asyncio.Event();release=asyncio.Event();count=0
            if bootstrap:
                await ctx.add_init_script("localStorage.setItem('church_auth_session','1');localStorage.setItem('church_auth_user',JSON.stringify({id:'old-id',username:'old',role:'SUPER_ADMIN',isGlobal:true,permissions:[]}));")
            async def delayed(route):
                nonlocal count
                count+=1
                if count==1:
                    entered.set();await release.wait();await route.fulfill(status=401,content_type='application/json',body='{}')
                else: await route.continue_()
            target=API+('/auth/me' if bootstrap else '/test-late-session')
            await page.route(target,delayed);await page.goto(FRONT+'/admin/login',wait_until='domcontentloaded')
            if not bootstrap:
                await page.evaluate("() => {window.late = import('/src/services/http/apiClient.js').then(m=>m.apiClient('/test-late-session',{}, {auth:true})).catch(()=>null);}")
            await asyncio.wait_for(entered.wait(),10);await sign_in(page);await protected(page)
            release.set();await page.wait_for_timeout(400)
            assert '/admin/login' not in page.url
            profile=json.loads(await page.evaluate("localStorage.getItem('church_auth_user')"))
            assert profile['username']=='ci-platform-admin'
            print('PASS: stale '+('bootstrap' if bootstrap else 'protected request')+' 401 cannot erase the new session')
            await ctx.close()
        await browser.close()

asyncio.run(main())
