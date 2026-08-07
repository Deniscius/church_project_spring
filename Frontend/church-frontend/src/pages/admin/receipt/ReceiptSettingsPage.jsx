import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppAlert from '../../../components/ui/AppAlert';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import PhoneField from '../../../components/ui/PhoneField';
import { FieldLabel } from '../../../components/ui/HelpTip';
import { useTenant } from '../../../hooks/useTenant';
import { useAuth } from '../../../hooks/useAuth';
import { useToast } from '../../../contexts/toast.context';
import { parishService } from '../../../services/parish.service';
import { HELP } from '../../../constants/helpTips';
import {
  DEFAULT_PHONE_COUNTRY_ISO,
  parseStoredPhone,
  toE164,
  validatePhoneForCountry,
} from '../../../utils/phone';

const ARCHIDIOCESE = 'ARCHIDIOCÈSE DE LOMÉ';

/**
 * Personnalisation du reçu PDF — disponible après activation du compte paroisse.
 */
export default function ReceiptSettingsPage() {
  const toast = useToast();
  const { activeParish } = useTenant();
  const { user } = useAuth();
  const paroisseId = activeParish?.publicId || activeParish?.id;
  const canEdit = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN';

  const [parish, setParish] = useState(null);
  const [telCountryIso, setTelCountryIso] = useState(DEFAULT_PHONE_COUNTRY_ISO);
  const [telNational, setTelNational] = useState('');
  const [logoPreview, setLogoPreview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const statut = parish?.statutTenant;
  const unlocked = statut === 'ACTIVE' || statut === 'EN_TOLERANCE';
  const phoneCheck = validatePhoneForCountry(telCountryIso, telNational);
  const displayPhone = toE164(telCountryIso, telNational) || parish?.telephone || '';

  async function load() {
    if (!paroisseId) return;
    setLoading(true);
    setError(null);
    try {
      const p = await parishService.getById(paroisseId);
      setParish(p);
      const parsed = parseStoredPhone(p?.telephone || '', DEFAULT_PHONE_COUNTRY_ISO);
      setTelCountryIso(parsed.iso || DEFAULT_PHONE_COUNTRY_ISO);
      setTelNational(parsed.national || '');
      if (p?.logoPresent) {
        try {
          const blob = await parishService.fetchLogoBlob(paroisseId);
          const url = URL.createObjectURL(blob);
          setLogoPreview((prev) => {
            if (prev) URL.revokeObjectURL(prev);
            return url;
          });
        } catch {
          setLogoPreview(null);
        }
      } else {
        setLogoPreview((prev) => {
          if (prev) URL.revokeObjectURL(prev);
          return null;
        });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    return () => {
      setLogoPreview((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paroisseId]);

  async function saveContact(e) {
    e.preventDefault();
    if (!paroisseId || !canEdit) return;
    if (telNational.trim() && !phoneCheck.ok) {
      setError(phoneCheck.message || 'Numéro de téléphone invalide.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await parishService.updateCoordonnees(paroisseId, {
        email: parish?.email || '',
        telephone: toE164(telCountryIso, telNational) || telNational.trim(),
        nomBanque: parish?.nomBanque || '',
        titulaireCompte: parish?.titulaireCompte || '',
        ibanOrRib: parish?.ibanOrRib || '',
      });
      toast.success('Contact mis à jour pour l’en-tête du reçu.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enregistrement impossible');
    } finally {
      setBusy(false);
    }
  }

  async function onLogoChange(e) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !paroisseId || !canEdit) return;

    const maxBytes = 5 * 1024 * 1024;
    const allowed = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);
    const name = (file.name || '').toLowerCase();
    const extOk = /\.(jpe?g|png|webp)$/.test(name);
    if (file.size > maxBytes) {
      setError('Fichier trop volumineux (max 5 Mo).');
      return;
    }
    if (file.type && !allowed.has(file.type) && !extOk) {
      setError('Logo accepté : JPEG, PNG ou WebP.');
      return;
    }

    setBusy(true);
    setError(null);
    try {
      await parishService.uploadLogo(paroisseId, file);
      toast.success('Logo enregistré — il figurera sur les prochains reçus.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload du logo impossible');
    } finally {
      setBusy(false);
    }
  }

  async function removeLogo() {
    if (!paroisseId || !canEdit) return;
    setBusy(true);
    setError(null);
    try {
      await parishService.removeLogo(paroisseId);
      toast.success('Logo retiré.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setBusy(false);
    }
  }

  const contactLine = [parish?.email, displayPhone].filter(Boolean).join(' · ')
    || 'E-mail et téléphone de la paroisse';

  return (
    <div className="stack receipt-page">
      <PageHeader
        title="Reçu des demandes"
        subtitle={HELP.admin.recu}
      />

      {error ? <AppAlert variant="danger">{error}</AppAlert> : null}

      {!paroisseId ? (
        <AppAlert variant="info">
          Sélectionnez une paroisse active pour personnaliser le reçu.
        </AppAlert>
      ) : null}

      {loading ? <p className="muted">Chargement…</p> : null}

      {!loading && paroisseId && !unlocked ? (
        <AppAlert variant="info">
          La personnalisation du reçu sera disponible dès que le compte paroisse
          sera validé et activé par la plateforme.
        </AppAlert>
      ) : null}

      {!loading && unlocked ? (
        <div className="receipt-layout">
            <AppCard
            title="Aperçu du reçu"
            subtitle="Une page A4 : en-tête paroisse, code de suivi, intention, célébration, demandeur et paiement."
          >
            <div className="receipt-single-preview" aria-label="Aperçu reçu A4">
              <div className="receipt-preview">
                {logoPreview ? (
                  <img src={logoPreview} alt="" className="receipt-preview-logo" />
                ) : (
                  <div className="receipt-preview-logo receipt-preview-logo--placeholder" aria-hidden="true">
                    Logo
                  </div>
                )}
                <div className="receipt-preview-text">
                  <strong>{ARCHIDIOCESE}</strong>
                  <div className="receipt-preview-parish">
                    {parish?.nom || activeParish?.name || 'Nom de la paroisse'}
                  </div>
                  <div className="muted text-sm">{contactLine}</div>
                  <div className="receipt-preview-meta">
                    Code de suivi · Montant · Intention · Célébration
                  </div>
                </div>
              </div>
            </div>
          </AppCard>

          <AppCard
            title="Logo de la paroisse"
            subtitle="Optionnel. JPEG, PNG ou WebP — 5 Mo max."
          >
            {canEdit ? (
              <div className="button-row">
                <label className={`btn btn-secondary${busy ? ' is-disabled' : ''}`}>
                  {logoPreview ? 'Remplacer le logo' : 'Ajouter un logo'}
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                    hidden
                    disabled={busy}
                    onChange={onLogoChange}
                  />
                </label>
                {logoPreview ? (
                  <AppButton type="button" variant="danger" disabled={busy} onClick={removeLogo}>
                    Retirer
                  </AppButton>
                ) : null}
              </div>
            ) : (
              <p className="muted" style={{ margin: 0 }}>
                Seul l’administrateur de la paroisse peut modifier le logo.
              </p>
            )}
          </AppCard>

          <AppCard
            title="Contact affiché"
            subtitle="L’e-mail professionnel est géré par la plateforme. Vous pouvez ajuster le téléphone."
          >
            <form className="stack" onSubmit={saveContact} style={{ gap: 16 }}>
              <div className="form-field">
                <FieldLabel htmlFor="receipt-email">E-mail (reçu)</FieldLabel>
                <AppInput
                  id="receipt-email"
                  value={parish?.email || ''}
                  disabled
                  readOnly
                />
              </div>
              <div className="form-field">
                <FieldLabel htmlFor="receipt-phone" required={false}>
                  Téléphone / contact
                </FieldLabel>
                <PhoneField
                  id="receipt-phone"
                  countryIso={telCountryIso}
                  national={telNational}
                  disabled={!canEdit || busy}
                  onChange={({ countryIso, national }) => {
                    setTelCountryIso(countryIso);
                    setTelNational(national);
                  }}
                />
              </div>
              {canEdit ? (
                <div className="button-row">
                  <AppButton type="submit" disabled={busy}>
                    {busy ? 'Enregistrement…' : 'Enregistrer le contact'}
                  </AppButton>
                </div>
              ) : null}
            </form>
          </AppCard>
        </div>
      ) : null}
    </div>
  );
}
