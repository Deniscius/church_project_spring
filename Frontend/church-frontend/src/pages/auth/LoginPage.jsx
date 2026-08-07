import React, { useId, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import { useAuth } from '../../hooks/useAuth';

export default function LoginPage() {
  const navigate = useNavigate();
  const { loginMultiTenant } = useAuth();
  const errorId = useId();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { user } = await loginMultiTenant({ username: username.trim(), password });
      let home = '/admin/dashboard';
      if (user?.isGlobal === true && user?.role === 'COMPTABLE') {
        home = '/admin/inscriptions-paroisse';
      } else if (user?.isGlobal === true && user?.role === 'SUPER_ADMIN') {
        home = '/admin/paroisses';
      }
      navigate(home, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Connexion impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <header className="auth-form-header">
        <h1>Espace paroisse</h1>
        <p>Connectez-vous avec les identifiants fournis par votre paroisse ou l’équipe plateforme.</p>
      </header>

      {error ? (
        <p id={errorId} className="auth-form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="form-field">
        <label htmlFor="login-username">Identifiant ou e-mail professionnel</label>
        <AppInput
          id="login-username"
          name="username"
          autoComplete="username"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="ex. jean.dupont ou jean.dupont@paroisse…"
          required
          disabled={loading}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
        />
      </div>

      <div className="form-field">
        <div className="auth-label-row">
          <label htmlFor="login-password">Mot de passe</label>
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
          id="login-password"
          name="password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          required
          disabled={loading}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
        />
      </div>

      <AppButton type="submit" className="auth-submit" disabled={loading} loading={loading}>
        {loading ? 'Connexion…' : 'Se connecter'}
      </AppButton>

      <p className="auth-form-links">
        <Link to="/">Retour à l’accueil</Link>
        <span aria-hidden="true">·</span>
        <Link to="/inscription-paroisse">Inscrire une paroisse</Link>
      </p>
    </form>
  );
}
