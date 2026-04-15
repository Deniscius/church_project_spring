import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../components/ui/PageHeader';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import { useAuth } from '../../hooks/useAuth';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { user } = await login({ username: username.trim(), password });
      const isGlobalAdmin = ['ADMIN', 'SUPER_ADMIN'].includes(user?.role);
      navigate(isGlobalAdmin ? '/admin/paroisses' : '/admin/dashboard', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Connexion impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="stack" onSubmit={handleSubmit}>
      <PageHeader title="Connexion paroisse" subtitle="Écran d'authentification pour accéder au dashboard privé de chaque paroisse." />
      {error ? <p className="text-red-600 text-sm">{error}</p> : null}
      <div className="form-field">
        <label htmlFor="login-username">Nom d&apos;utilisateur</label>
        <AppInput
          id="login-username"
          name="username"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="admin"
          required
        />
      </div>
      <div className="form-field">
        <label htmlFor="login-password">Mot de passe</label>
        <AppInput
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          required
        />
      </div>
      <div className="button-row">
        <AppButton type="submit" disabled={loading}>
          {loading ? 'Connexion…' : 'Se connecter'}
        </AppButton>
        <AppButton type="button" variant="secondary">
          Mot de passe oublié
        </AppButton>
      </div>
    </form>
  );
}
