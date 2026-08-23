import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import FormError from '../../components/ui/FormError';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../contexts/toast.context';
import { useScrollToError } from '../../hooks/useScrollToError';
import {
  normalizeFormErrors,
  sanitizeAuthPasswordEdges,
  sanitizeAuthUsernameInput,
} from '../../utils/formErrors';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const { loginMultiTenant } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const resetOk = Boolean(location.state?.resetOk);
  const sessionExpired = Boolean(location.state?.sessionExpired);
  const errorRef = useScrollToError(error);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const cleanUser = sanitizeAuthUsernameInput(username);
    const cleanPass = sanitizeAuthPasswordEdges(password);
    setUsername(cleanUser);
    setPassword(cleanPass);
    if (!cleanUser || !cleanPass) {
      const msg = 'Identifiant et mot de passe sont obligatoires.';
      setError(msg);
      toast.error(msg);
      return;
    }
    setLoading(true);
    try {
      const { user } = await loginMultiTenant({ username: cleanUser, password: cleanPass });
      toast.success('Connexion réussie.');
      let home = '/admin/dashboard';
      if (user?.isGlobal === true && user?.role === 'COMPTABLE') {
        home = '/admin/inscriptions-paroisse';
      } else if (user?.isGlobal === true && user?.role === 'SUPER_ADMIN') {
        home = '/admin/paroisses';
      }
      const requestedPath = location.state?.from;
      const safeDestination = typeof requestedPath === 'string'
        && requestedPath.startsWith('/admin/')
        && requestedPath !== '/admin/login'
        ? requestedPath
        : home;
      navigate(safeDestination, { replace: true });
    } catch (err) {
      const messages = normalizeFormErrors(err);
      setError(messages.join(' ; '));
      toast.error(messages.length === 1 ? messages[0] : `${messages.length} erreurs`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <header className="auth-form-header">
        <p className="auth-form-kicker">Espace paroisse</p>
        <h1>Connexion</h1>
        <p>Accédez à la gestion des demandes, horaires et finances.</p>
      </header>

      {resetOk ? (
        <p className="auth-form-success" role="status">
          Mot de passe mis à jour. Vous pouvez vous connecter.
        </p>
      ) : null}

      {sessionExpired ? (
        <p className="auth-form-info" role="status">
          Votre session a expiré. Reconnectez-vous pour reprendre votre activité.
        </p>
      ) : null}

      <FormError error={error} errorRef={errorRef} title="Connexion impossible" />

      <div className="form-field">
        <label htmlFor="login-username">Identifiant ou e-mail</label>
        <AppInput
          id="login-username"
          name="username"
          autoComplete="username"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          value={username}
          onChange={(e) => setUsername(sanitizeAuthUsernameInput(e.target.value))}
          onBlur={() => setUsername((v) => sanitizeAuthUsernameInput(v))}
          placeholder="ex. admin.saint-joseph"
          required
          disabled={loading}
          aria-invalid={error ? true : undefined}
        />
      </div>

      <div className="form-field">
        <label htmlFor="login-password">Mot de passe</label>
        <div className="auth-password-field">
          <AppInput
            id="login-password"
            name="password"
            className="auth-password-input"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={() => setPassword((v) => sanitizeAuthPasswordEdges(v))}
            placeholder="Votre mot de passe"
            required
            disabled={loading}
            aria-invalid={error ? true : undefined}
          />
          <button
            type="button"
            className="auth-password-toggle"
            onClick={() => setShowPassword((v) => !v)}
            aria-pressed={showPassword}
            aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
            disabled={loading}
          >
            {showPassword ? 'Masquer' : 'Voir'}
          </button>
        </div>
        <div className="auth-field-meta">
          <Link to="/admin/forgot-password" className="auth-text-link">
            Mot de passe oublié ?
          </Link>
        </div>
      </div>

      <AppButton type="submit" className="auth-submit" disabled={loading} loading={loading}>
        {loading ? 'Connexion…' : 'Se connecter'}
      </AppButton>

      <nav className="auth-form-links" aria-label="Autres actions">
        <Link to="/">Accueil public</Link>
        <span className="auth-form-links-sep" aria-hidden="true" />
        <Link to="/inscription-paroisse">Inscrire une paroisse</Link>
      </nav>
    </form>
  );
}
