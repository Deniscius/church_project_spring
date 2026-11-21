"""No real credentials, external requests or payment creation."""
import contextlib
import io
import os
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
import check_saspay


class SasPayDiagnosticTest(unittest.TestCase):
    def env(self, mode='test', **extra):
        return {'SASPAY_API_KEY': 'sk_' + mode + '_synthetic_fixture',
                'SASPAY_WEBHOOK_SECRET': 'synthetic-signing-fixture',
                'APP_URL': 'https://frontend.example', **extra}

    def test_automatic_server_tracks_key_environment(self):
        self.assertEqual(check_saspay.configuration(self.env())[1], check_saspay.SANDBOX)
        self.assertEqual(check_saspay.configuration(self.env('live'), True)[1], check_saspay.LIVE)
        self.assertEqual(check_saspay.configuration(self.env(SASPAY_API_BASE_URL='auto'))[1], check_saspay.SANDBOX)

    def test_explicit_environment_mismatch_is_rejected_before_network(self):
        for env in [self.env(SASPAY_API_BASE_URL=check_saspay.LIVE),
                    self.env('live', SASPAY_API_BASE_URL=check_saspay.SANDBOX)]:
            with self.assertRaisesRegex(ValueError, 'Environnement incohérent'):
                check_saspay.configuration(env, True)

    def test_live_requires_explicit_opt_in(self):
        with self.assertRaisesRegex(ValueError, '--allow-live'):
            check_saspay.configuration(self.env('live'))

    def test_padding_is_trimmed_but_embedded_whitespace_and_quotes_are_refused(self):
        env = self.env()
        key = env['SASPAY_API_KEY']
        env['SASPAY_API_KEY'] = ' \n' + key + '\n '
        self.assertEqual(check_saspay.configuration(env)[0], key)
        for invalid in ['"' + key + '"', key + ' embedded', key[:10] + '\n' + key[10:]]:
            with self.assertRaises(ValueError):
                check_saspay.configuration(self.env(SASPAY_API_KEY=invalid))

    def test_urls_cannot_leak_credentials_or_redirect_authorization(self):
        for base in ['https://evil.test/api/v1', 'https://sandbox-api.saspay.me.evil.test/api/v1',
                     'https://user:password@sandbox-api.saspay.me/api/v1',
                     check_saspay.SANDBOX + '?token=private', check_saspay.SANDBOX + '#private',
                     'https://sandbox-api.saspay.me:444/api/v1', 'https://sandbox-api.saspay.me/other']:
            with self.assertRaises(ValueError):
                check_saspay.configuration(self.env(SASPAY_API_BASE_URL=base))
        self.assertIsNone(check_saspay.NoRedirect().redirect_request(None, None, 302, '', {}, 'https://evil.test'))

    def test_offline_has_no_requests_or_secret_output(self):
        env = self.env()
        output = io.StringIO()
        with patch.dict(os.environ, env, clear=True), patch('sys.argv', ['check_saspay.py', '--offline']), \
                patch('urllib.request.build_opener') as opener, contextlib.redirect_stdout(output):
            self.assertEqual(check_saspay.main(), 0)
            opener.assert_not_called()
        for secret in [env['SASPAY_API_KEY'], env['SASPAY_WEBHOOK_SECRET']]:
            self.assertNotIn(secret, output.getvalue())

    def test_http_errors_are_distinguished_and_never_retried(self):
        for status, phrase in [(401, 'Authentification'), (403, 'scope'), (503, '503')]:
            output = io.StringIO()
            with patch.dict(os.environ, self.env(), clear=True), patch('sys.argv', ['check_saspay.py']), \
                    patch('urllib.request.build_opener') as opener, contextlib.redirect_stdout(output):
                opener.return_value.open.side_effect = HTTPError('https://sandbox-api.saspay.me', status,
                                                                'private response body', None, None)
                self.assertEqual(check_saspay.main(), 1)
                self.assertEqual(opener.return_value.open.call_count, 1)
            self.assertIn(phrase, output.getvalue())
            self.assertNotIn('private response', output.getvalue())


if __name__ == '__main__':
    unittest.main()
