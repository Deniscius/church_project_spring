import React, { useEffect, useId, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import { authService } from '../../services/auth.service';
import { clearResetToken, getResetToken, setResetToken } from '../../utils/sensitiveNav';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const errorId = useId();
  const [token, setToken] = useState(() => getResetToken());
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Lien e-mail ?token=… → session puis URL nettoyée (anti-fuite historique / Referer).
  useEffect(() => {
    const fromUrl = (params.get('token') || '').trim();
    if (fromUrl) {
      setResetToken(fromUrl);
      setToken(fromUrl);
      params.delete('token');
      setParams(params, { replace: true });
    } else if (!token) {
      setToken(getResetToken());
    }
  }, [params, setParams, token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const activeToken = token || getResetToken();
    if (!activeToken) {
      setError('Lien invalide : jeton manquant.');
      return;
    }
    if (password.length < 8) {
      setError('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }
    if (password !== confirm) {
      setError('Les deux mots de passe ne correspondent pas.');
      return;
    }
    setLoading(true);
    try {
      await authService.resetPassword({ token: activeToken, newPassword: password });
      clearResetToken();
      navigate('/admin/login', { replace: true, state: { resetOk: true } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Réinitialisation impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <header className="auth-form-header">
        <h1>Nouveau mot de passe</h1>
        <p>Choisissez un mot de passe d’au moins 8 caractères.</p>
      </header>

      {!token ? (
        <p className="auth-form-error" role="alert">
          Lien incomplet. Demandez une nouvelle réinitialisation.
        </p>
      ) : null}

      {error ? (
        <p id={errorId} className="auth-form-error" role="alert">{error}</p>
      ) : null}

      <div className="form-field">
        <div className="auth-label-row">
          <label htmlFor="reset-password">Nouveau mot de passe</label>
          <button
            type="button"
            className="auth-text-btn"
            onClick={() => setShowPassword((v) => !v)}
            aria-pressed={showPassword}
          >
            {showPassword ? 'Masquer' : 'Afficher'}
          </button>
        </div>
        <AppInput
          id="reset-password"
          name="password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          disabled={loading || !token}
          minLength={8}
        />
      </div>

      <div className="form-field">
        <label htmlFor="reset-confirm">Confirmer</label>
        <AppInput
          id="reset-confirm"
          name="confirm"
          type={showPassword ? 'text' : 'password'}
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          required
          disabled={loading || !token}
          minLength={8}
        />
      </div>

      <AppButton
        type="submit"
        className="auth-submit"
        disabled={loading || !token}
        loading={loading}
      >
        {loading ? 'Enregistrement…' : 'Enregistrer'}
      </AppButton>

      <p className="auth-form-links">
        <Link to="/admin/login">Retour à la connexion</Link>
        <span aria-hidden="true">·</span>
        <Link to="/admin/forgot-password">Renvoyer un lien</Link>
      </p>
    </form>
  );
}
