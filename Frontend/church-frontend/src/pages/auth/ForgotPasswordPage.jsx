import React, { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import { authService } from '../../services/auth.service';

export default function ForgotPasswordPage() {
  const errorId = useId();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    setLoading(true);
    try {
      const res = await authService.forgotPassword(usernameOrEmail.trim());
      setInfo(res?.message || 'Si un compte correspond, un e-mail a été envoyé.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Demande impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <header className="auth-form-header">
        <h1>Mot de passe oublié</h1>
        <p>
          Indiquez votre identifiant ou e-mail professionnel. Si un compte existe,
          vous recevrez un lien de réinitialisation (valable 30 minutes).
        </p>
      </header>

      {error ? (
        <p id={errorId} className="auth-form-error" role="alert">{error}</p>
      ) : null}
      {info ? (
        <p className="auth-form-success" role="status">{info}</p>
      ) : null}

      <div className="form-field">
        <label htmlFor="forgot-id">Identifiant ou e-mail</label>
        <AppInput
          id="forgot-id"
          name="usernameOrEmail"
          autoComplete="username"
          autoCapitalize="none"
          value={usernameOrEmail}
          onChange={(e) => setUsernameOrEmail(e.target.value)}
          placeholder="ex. jean.dupont ou jean.dupont@missanye.com"
          required
          disabled={loading}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
        />
      </div>

      <AppButton type="submit" className="auth-submit" disabled={loading} loading={loading}>
        {loading ? 'Envoi…' : 'Envoyer le lien'}
      </AppButton>

      <p className="auth-form-links">
        <Link to="/admin/login">Retour à la connexion</Link>
      </p>
    </form>
  );
}
