import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import AppInput from '../../components/ui/AppInput';
import AppButton from '../../components/ui/AppButton';
import FormError from '../../components/ui/FormError';
import { useToast } from '../../contexts/toast.context';
import { useScrollToError } from '../../hooks/useScrollToError';
import { authService } from '../../services/auth.service';
import { normalizeFormErrors, sanitizeAuthUsernameInput } from '../../utils/formErrors';

export default function ForgotPasswordPage() {
  const toast = useToast();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);
  const errorRef = useScrollToError(error);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    const clean = sanitizeAuthUsernameInput(usernameOrEmail);
    setUsernameOrEmail(clean);
    if (!clean) {
      const msg = 'Indiquez un identifiant ou un e-mail.';
      setError(msg);
      toast.error(msg);
      return;
    }
    setLoading(true);
    try {
      const res = await authService.forgotPassword(clean);
      setInfo(res?.message || 'Si un compte correspond, un e-mail a été envoyé.');
      toast.success('Demande enregistrée.');
    } catch (err) {
      const messages = normalizeFormErrors(err);
      setError(messages.join(' ; '));
      toast.error(messages[0]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <header className="auth-form-header">
        <p className="auth-form-kicker">Sécurité</p>
        <h1>Mot de passe oublié</h1>
        <p>
          Indiquez votre identifiant ou e-mail. Si un compte existe, un lien
          valable 30 minutes vous sera envoyé.
        </p>
      </header>

      <FormError error={error} errorRef={errorRef} title="Demande impossible" />
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
          onChange={(e) => setUsernameOrEmail(sanitizeAuthUsernameInput(e.target.value))}
          onBlur={() => setUsernameOrEmail((v) => sanitizeAuthUsernameInput(v))}
          placeholder="ex. jean.dupont"
          required
          disabled={loading}
          aria-invalid={error ? true : undefined}
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
