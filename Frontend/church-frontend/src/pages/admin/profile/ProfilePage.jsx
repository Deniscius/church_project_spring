import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageHeader from '../../../components/ui/PageHeader';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import AppButton from '../../../components/ui/AppButton';
import { useAuth } from '../../../hooks/useAuth';
import { useTenant } from '../../../hooks/useTenant';
import { formatRole } from '../../../utils/roleMapper';
import { profileService } from '../../../services/user.service';

const EMPTY_PASSWORD_FORM = { currentPassword: '', newPassword: '', confirmPassword: '' };

export default function ProfilePage() {
  const navigate = useNavigate();
  const { user, patchCurrentUser, logout } = useAuth();
  const { activeParish } = useTenant();

  const [profile, setProfile] = useState(null);
  const [identity, setIdentity] = useState({ nom: '', prenom: '', email: '', telephone: '' });
  const [passwords, setPasswords] = useState(EMPTY_PASSWORD_FORM);
  const [loading, setLoading] = useState(true);
  const [savingIdentity, setSavingIdentity] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await profileService.get();
        if (cancelled) return;
        setProfile(data);
        setIdentity({
          nom: data.nom || '',
          prenom: data.prenom || '',
          email: data.email || '',
          telephone: data.telephone || '',
        });
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Chargement impossible');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const submitIdentity = async (e) => {
    e.preventDefault();
    setSavingIdentity(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await profileService.update(identity);
      setProfile(updated);
      patchCurrentUser({ firstName: updated.prenom, lastName: updated.nom });
      setNotice('Informations personnelles mises à jour.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Mise à jour impossible');
    } finally {
      setSavingIdentity(false);
    }
  };

  const submitPassword = async (e) => {
    e.preventDefault();
    setError(null);
    setNotice(null);

    if (passwords.newPassword !== passwords.confirmPassword) {
      setError('La confirmation ne correspond pas au nouveau mot de passe.');
      return;
    }
    if (passwords.newPassword.length < 8) {
      setError('Le nouveau mot de passe doit contenir au moins 8 caractères.');
      return;
    }

    setSavingPassword(true);
    try {
      await profileService.changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      });
      setPasswords(EMPTY_PASSWORD_FORM);
      await logout();
      navigate('/admin/login', {
        replace: true,
        state: { resetOk: true },
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Changement impossible');
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        title="Mon profil"
        subtitle="Vous êtes seul à pouvoir modifier ces informations : aucun administrateur n’y a accès."
      />

      {error ? <div className="alert-danger" role="alert">{error}</div> : null}
      {notice ? <div className="alert-success" role="status">{notice}</div> : null}
      {loading ? <p className="muted">Chargement…</p> : null}

      <div className="grid-2">
        <AppCard title="Compte" subtitle="Attributs gérés par votre administration.">
          <div className="info-list">
            <div className="info-row">
              <span>Identifiant</span>
              <strong>{profile?.username || user?.username}</strong>
            </div>
            <div className="info-row">
              <span>Rôle</span>
              <span>{formatRole(profile?.role || user?.role)}</span>
            </div>
            <div className="info-row">
              <span>Paroisse active</span>
              <span>{activeParish?.name || '—'}</span>
            </div>
            <div className="info-row">
              <span>Statut</span>
              <span>{profile?.isActive === false ? 'Inactif' : 'Actif'}</span>
            </div>
          </div>
          <small className="muted">
            L’identifiant et le rôle ne sont pas modifiables ici : ils relèvent de votre administrateur.
          </small>
        </AppCard>

        <AppCard
          title="Informations personnelles"
          subtitle="Identité affichée dans l’application et coordonnées utilisées pour vous joindre."
        >
          <form onSubmit={submitIdentity} className="stack">
            <div className="form-field">
              <label htmlFor="profile-prenom">Prénom *</label>
              <AppInput
                id="profile-prenom"
                required
                minLength={2}
                value={identity.prenom}
                onChange={(e) => setIdentity({ ...identity, prenom: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label htmlFor="profile-nom">Nom *</label>
              <AppInput
                id="profile-nom"
                required
                minLength={2}
                value={identity.nom}
                onChange={(e) => setIdentity({ ...identity, nom: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label htmlFor="profile-email">E-mail professionnel</label>
              <AppInput
                id="profile-email"
                type="email"
                autoComplete="email"
                maxLength={150}
                value={identity.email}
                readOnly
                disabled
              />
              <small className="muted">
                Adresse plateforme générée pour votre paroisse — non modifiable.
              </small>
            </div>
            <div className="form-field">
              <label htmlFor="profile-telephone">Téléphone</label>
              <AppInput
                id="profile-telephone"
                type="tel"
                autoComplete="tel"
                maxLength={50}
                value={identity.telephone}
                onChange={(e) => setIdentity({ ...identity, telephone: e.target.value })}
              />
            </div>
            <div className="button-row">
              <AppButton type="submit" disabled={savingIdentity || loading}>
                {savingIdentity ? 'Enregistrement…' : 'Enregistrer'}
              </AppButton>
            </div>
          </form>
        </AppCard>

        <AppCard title="Mot de passe" subtitle="Votre mot de passe actuel est exigé pour le changer.">
          <form onSubmit={submitPassword} className="stack">
            <div className="form-field">
              <label htmlFor="profile-current-password">Mot de passe actuel *</label>
              <AppInput
                id="profile-current-password"
                type="password"
                autoComplete="current-password"
                required
                value={passwords.currentPassword}
                onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label htmlFor="profile-new-password">Nouveau mot de passe *</label>
              <AppInput
                id="profile-new-password"
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                value={passwords.newPassword}
                onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })}
              />
              <small className="muted">8 caractères minimum.</small>
            </div>
            <div className="form-field">
              <label htmlFor="profile-confirm-password">Confirmation *</label>
              <AppInput
                id="profile-confirm-password"
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                value={passwords.confirmPassword}
                onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })}
              />
            </div>
            <div className="button-row">
              <AppButton type="submit" disabled={savingPassword || loading}>
                {savingPassword ? 'Modification…' : 'Changer le mot de passe'}
              </AppButton>
            </div>
          </form>
        </AppCard>
      </div>
    </div>
  );
}
