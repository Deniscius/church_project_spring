import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import { ROLES, ROLE_LABELS } from '../../../constants/roles';
import { useAuth } from '../../../hooks/useAuth';
import { useTenant } from '../../../hooks/useTenant';
import { userService } from '../../../services/user.service';

const TEAM_ROLES = [ROLES.SECRETAIRE, ROLES.CURE, ROLES.COMPTABLE_LOCAL, ROLES.ADMIN];

const ROLE_PAROISSE_OPTIONS = [
  { value: 'ADMIN', label: 'Administrateur paroissial' },
  { value: 'GESTIONNAIRE', label: 'Gestionnaire' },
  { value: 'SECRETAIRE', label: 'Secrétaire' },
  { value: 'CONSULTATION', label: 'Consultation' },
];

const INITIAL_VALUE = {
  nom: '',
  prenom: '',
  username: '',
  email: '',
  telephone: '',
  password: '',
  role: ROLES.SECRETAIRE,
  isActive: true,
  roleParoisse: 'SECRETAIRE',
};

export default function TeamMemberForm({ userId = null }) {
  const navigate = useNavigate();
  const { activeParish } = useTenant();
  const { user: currentUser } = useAuth();
  const [form, setForm] = useState(INITIAL_VALUE);
  const [loading, setLoading] = useState(Boolean(userId));
  const [error, setError] = useState(null);
  const isEdit = Boolean(userId);
  const currentUserId = currentUser?.id || currentUser?.publicId;
  const isSelf = isEdit && userId === currentUserId;

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    (async () => {
      try {
        const data = await userService.getById(userId);
        if (cancelled) return;
        setForm({
          nom: data.nom || '',
          prenom: data.prenom || '',
          username: data.username || '',
          email: data.email || '',
          telephone: data.telephone || '',
          password: '',
          role: data.role || ROLES.SECRETAIRE,
          isActive: data.isActive !== false,
          roleParoisse: 'SECRETAIRE',
        });
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Membre introuvable');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const field = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  const submit = async (event) => {
    event.preventDefault();
    if (!activeParish?.id) {
      setError('Aucune paroisse active. Reconnectez-vous avant de gérer l’équipe.');
      return;
    }
    if (isSelf && form.isActive === false) {
      setError('Vous ne pouvez pas désactiver votre propre compte');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const payload = {
        nom: form.nom.trim(),
        prenom: form.prenom.trim(),
        username: form.username.trim(),
        email: form.email.trim(),
        telephone: form.telephone.trim(),
        role: form.role,
        isActive: isSelf ? true : form.isActive,
        isGlobal: false,
      };

      if (isEdit) {
        // Nom, prénom et mot de passe relèvent du seul titulaire du compte :
        // ce formulaire ne pilote que le rôle et l'activation.
        await userService.update(userId, payload);
      } else {
        payload.password = form.password;
        payload.paroisses = [
          {
            paroisseId: activeParish.id,
            roleParoisse: form.roleParoisse,
          },
        ];
        await userService.create(payload);
      }

      navigate('/admin/equipe');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Enregistrement impossible');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppCard
      title={isEdit ? 'Modifier le membre' : 'Nouveau membre'}
      subtitle={
        activeParish?.name
          ? `Paroisse : ${activeParish.name}`
          : 'La paroisse active sera utilisée pour l’affectation.'
      }
    >
      {error ? <p className="text-red-600">{error}</p> : null}

      {isEdit ? (
        <div className="alert-info" role="status" style={{ marginBottom: 16 }}>
          {isSelf
            ? 'Vos nom, prénom, coordonnées et mot de passe se modifient depuis « Mon profil ».'
            : 'Identité, coordonnées et mot de passe appartiennent au titulaire du compte : lui seul peut les modifier, depuis « Mon profil ». Ce formulaire pilote le rôle et l’activation.'}
        </div>
      ) : null}

      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="form-field">
            <label htmlFor="team-nom">Nom *</label>
            <AppInput
              id="team-nom"
              required
              minLength={2}
              maxLength={100}
              value={form.nom}
              disabled={loading || isEdit}
              readOnly={isEdit}
              onChange={(e) => field('nom', e.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="team-prenom">Prénom *</label>
            <AppInput
              id="team-prenom"
              required
              minLength={2}
              maxLength={150}
              value={form.prenom}
              disabled={loading || isEdit}
              readOnly={isEdit}
              onChange={(e) => field('prenom', e.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="team-username">Identifiant *</label>
            <AppInput
              id="team-username"
              required
              minLength={3}
              maxLength={100}
              pattern="[A-Za-z0-9._-]+"
              title="Lettres, chiffres, point, tiret et underscore uniquement"
              value={form.username}
              disabled={loading || isEdit}
              readOnly={isEdit}
              onChange={(e) => field('username', e.target.value)}
            />
            {isEdit ? (
              <small className="muted">L’identifiant ne peut plus être modifié après création.</small>
            ) : null}
          </div>

          <div className="form-field">
            <label htmlFor="team-email">E-mail professionnel</label>
            <AppInput
              id="team-email"
              type="email"
              maxLength={150}
              value={form.email}
              disabled
              readOnly
            />
            <small className="muted">
              {isEdit
                ? 'Adresse professionnelle immuable.'
                : 'Généré automatiquement (prenom.nom@paroisse…) à la création.'}
            </small>
          </div>

          <div className="form-field">
            <label htmlFor="team-telephone">Téléphone</label>
            <AppInput
              id="team-telephone"
              type="tel"
              maxLength={50}
              value={form.telephone}
              disabled={loading || isEdit}
              readOnly={isEdit}
              onChange={(e) => field('telephone', e.target.value)}
            />
          </div>

          {!isEdit ? (
            <div className="form-field">
              <label htmlFor="team-password">Mot de passe *</label>
              <AppInput
                id="team-password"
                type="password"
                required
                minLength={8}
                maxLength={200}
                value={form.password}
                disabled={loading}
                onChange={(e) => field('password', e.target.value)}
              />
              <small className="muted">
                Minimum 8 caractères. Le membre pourra le changer depuis son profil.
              </small>
            </div>
          ) : null}

          <div className="form-field">
            <label htmlFor="team-role">Rôle applicatif *</label>
            <select
              id="team-role"
              className="select"
              required
              value={form.role}
              disabled={loading}
              onChange={(e) => field('role', e.target.value)}
            >
              {TEAM_ROLES.map((role) => (
                <option key={role} value={role}>
                  {ROLE_LABELS[role] || role}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="team-active">État *</label>
            <select
              id="team-active"
              className="select"
              value={String(form.isActive)}
              disabled={loading || isSelf}
              onChange={(e) => field('isActive', e.target.value === 'true')}
            >
              <option value="true">Actif</option>
              <option value="false">Inactif</option>
            </select>
            {isSelf ? (
              <small className="muted">Vous ne pouvez pas désactiver votre propre compte.</small>
            ) : null}
          </div>

          {!isEdit ? (
            <>
              <div className="form-field">
                <label htmlFor="team-parish">Paroisse</label>
                <AppInput
                  id="team-parish"
                  value={activeParish?.name || 'Aucune paroisse active'}
                  disabled
                  readOnly
                />
              </div>

              <div className="form-field">
                <label htmlFor="team-role-paroisse">Rôle dans la paroisse *</label>
                <select
                  id="team-role-paroisse"
                  className="select"
                  required
                  value={form.roleParoisse}
                  disabled={loading}
                  onChange={(e) => field('roleParoisse', e.target.value)}
                >
                  {ROLE_PAROISSE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </>
          ) : (
            <div className="form-field full">
              <p className="muted">
                L’affectation paroissiale reste inchangée pendant cette modification.
              </p>
            </div>
          )}
        </div>

        <div className="button-row" style={{ marginTop: 18 }}>
          <AppButton type="submit" loading={loading} disabled={!activeParish?.id}>
            {isEdit ? 'Enregistrer' : 'Créer le membre'}
          </AppButton>
          <AppButton variant="secondary" onClick={() => navigate('/admin/equipe')} disabled={loading}>
            Annuler
          </AppButton>
        </div>
      </form>
    </AppCard>
  );
}
