"""Receipt preview regression on the production build with synthetic API data only.
Build with VITE_API_BASE_URL=https://api.example.test, then run Vite preview.
Requires Python Playwright + Chromium. No production account or data is accessed.
"""
import asyncio
import json
import os
from urllib.parse import urlparse
from playwright.async_api import async_playwright, expect

FRONT = os.environ.get('RECEIPT_TEST_FRONTEND', 'http://127.0.0.1:4173')
API = 'https://api.example.test'
if urlparse(FRONT).hostname not in ('localhost', '127.0.0.1'):
    raise RuntimeError('Run this fixture on a disposable loopback frontend')


def receipt_pdf():
    stream = b'BT /F1 18 Tf 50 750 Td (Receipt preview regression) Tj ET'
    objects = [b'<< /Type /Catalog /Pages 2 0 R >>',
               b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
               b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
               b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
               b'<< /Length ' + str(len(stream)).encode() + b' >>\nstream\n' + stream + b'\nendstream']
    result = b'%PDF-1.4\n'
    offsets = [0]
    for number, obj in enumerate(objects, 1):
        offsets.append(len(result))
        result += f'{number} 0 obj\n'.encode() + obj + b'\nendobj\n'
    start = len(result)
    result += b'xref\n0 6\n0000000000 65535 f \n'
    for offset in offsets[1:]:
        result += f'{offset:010} 00000 n \n'.encode()
    return result + f'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n{start}\n%%EOF\n'.encode()


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH', '/usr/bin/chromium'), headless=True, args=['--no-sandbox'])
        for legacy in (False, True):
            for width in (1280, 390):
                ctx = await browser.new_context(viewport={'width': width, 'height': 900}, accept_downloads=True)
                user = {'id': 'fixture-admin', 'username': 'fixture', 'role': 'SECRETAIRE', 'isGlobal': False, 'permissions': ['demand:read', 'invoice:read']}
                await ctx.add_init_script("localStorage.setItem('church_auth_session','1');localStorage.setItem('church_auth_user'," + json.dumps(json.dumps(user)) + ");localStorage.setItem('paroisses',JSON.stringify([{id:'fixture-parish',publicId:'fixture-parish',nom:'Fixture parish'}]));sessionStorage.setItem('ms_nav_invoice_code','MS-SP-ABCDEFGHJK');" + ('Promise.withResolvers = undefined;' if legacy else ''))
                page = await ctx.new_page()
                calls = 0
                fail_pdf = False
                async def route(r):
                    nonlocal calls
                    url = r.request.url
                    if url.startswith(API):
                        headers = {'Access-Control-Allow-Origin': FRONT, 'Access-Control-Allow-Credentials': 'true'}
                        if '/recu' in url:
                            calls += 1
                            if fail_pdf:
                                await r.fulfill(status=503, content_type='application/json', headers=headers, body='{}')
                            else:
                                await r.fulfill(content_type='application/pdf', headers=headers, body=receipt_pdf())
                        else:
                            if '/auth/me' in url:
                                body = {'user': user, 'paroisses': [{'id': 'fixture-parish', 'nom': 'Fixture parish'}], 'selectedParoisse': None}
                            elif '/demandes/' in url:
                                body = {'publicId': 'fixture-demand', 'codeSuivie': 'MS-SP-ABCDEFGHJK', 'intention': 'Synthetic test intention', 'statutDemande': 'TERMINEE', 'statutPaiement': 'PAYE', 'montant': 2000}
                            else:
                                body = []
                            await r.fulfill(content_type='application/json', headers=headers, body=json.dumps(body))
                    elif url.startswith(FRONT):
                        await r.continue_()
                    else:
                        await r.abort()
                await page.route('**/*', route)
                await page.goto(FRONT + '/admin/demandes/fixture-demand')
                button = page.get_by_role('button', name='Aperçu / imprimer le reçu')
                await button.click()
                canvas = page.locator('[role=dialog] canvas')
                await expect(canvas).to_be_visible()
                assert await canvas.evaluate("c => Array.from(c.getContext('2d').getImageData(0,0,c.width,c.height).data).some((v,i)=>i%4!==3 && v<200)")
                await page.get_by_role('button', name='Fermer', exact=True).click()
                await button.click()
                await expect(canvas).to_be_visible()
                assert calls == 1, 'Reopening the same receipt must use the cached PDF'
                async with page.expect_download() as download:
                    await page.get_by_role('button', name='Télécharger', exact=True).click()
                assert (await download.value).suggested_filename == 'recu-MS-SP-ABCDEFGHJK.pdf'
                await page.get_by_role('button', name='Fermer', exact=True).click()
                await page.reload()
                fail_pdf = True
                await button.click()
                await expect(page.locator('[role=dialog] [role=alert]')).to_be_visible()
                await page.get_by_role('button', name='Fermer', exact=True).click()
                fail_pdf = False
                await button.click()
                await expect(canvas).to_be_visible()
                print(f'PASS admin receipt: legacy={legacy}, width={width}; render, reopen, download, error recovery')
                await ctx.close()
        await browser.close()

asyncio.run(main())
