import React, { useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppAlert from '../../../components/ui/AppAlert';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import PdfPreviewModal from '../../../components/ui/PdfPreviewModal';
import PhoneField from '../../../components/ui/PhoneField';
import { FieldLabel } from '../../../components/ui/HelpTip';
import { useTenant } from '../../../hooks/useTenant';
import { usePermissions } from '../../../hooks/usePermissions';
import { useToast } from '../../../contexts/toast.context';
import { parishService } from '../../../services/parish.service';
import { PERMISSIONS } from '../../../constants/roles';
import {
  DEFAULT_PHONE_COUNTRY_ISO,
  parseStoredPhone,
  toE164,
  validatePhoneForCountry,
} from '../../../utils/phone';

const ARCHIDIOCESE = 'ARCHIDIOCÈSE DE LOMÉ';

export default function ReceiptSettingsPage() {
  const toast = useToast();
  const { activeParish } = useTenant();
  const { has } = usePermissions();
  const paroisseId = activeParish?.publicId || activeParish?.id;
  const canEdit = has(PERMISSIONS.RECEIPT_MANAGE);

  const [parish, setParish] = useState(null);
  const [telCountryIso, setTelCountryIso] = useState(DEFAULT_PHONE_COUNTRY_ISO);
  const [telNational, setTelNational] = useState('');
  const [logoPreview, setLogoPreview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [pdfOpen, setPdfOpen] = useState(false);
  const [pdfBlob, setPdfBlob] = useState(null);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [pdfError, setPdfError] = useState(null);

  const statut = parish?.statutTenant;
  const unlocked = statut === 'ACTIVE' || statut === 'EN_TOLERANCE';
  const phoneCheck = validatePhoneForCountry(telCountryIso, telNational);
  const displayPhone = toE164(telCountryIso, telNational) || parish?.telephone || '';
  const contactLine = [parish?.email, displayPhone].filter(Boolean).join(' · ') || 'Contact paroisse';

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
    setPdfOpen(false);
    setPdfBlob(null);
    setPdfError(null);
    load();
    return () => {
      setLogoPreview((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paroisseId]);

  async function openPdfSample() {
    if (!paroisseId) return;
    setPdfLoading(true);
    setPdfError(null);
    setPdfBlob(null);
    setPdfOpen(true);
    try {
      const blob = await parishService.fetchReceiptSamplePdf(paroisseId);
      setPdfBlob(blob);
    } catch (e) {
      const raw = e instanceof Error ? e.message : String(e || '');
      setPdfError(
        /failed to fetch|networkerror|load failed/i.test(raw)
          ? 'Impossible de charger le reçu (réseau / API). Réessayez ou ouvrez-le dans un onglet.'
          : (raw || 'Aperçu PDF impossible')
      );
      setPdfBlob(null);
    } finally {
      setPdfLoading(false);
    }
  }

  function downloadPdfSample() {
    if (!pdfBlob) return;
    const url = URL.createObjectURL(pdfBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'recu-modele.pdf';
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  function openPdfSampleInTab() {
    if (!pdfBlob) return;
    const url = URL.createObjectURL(pdfBlob);
    window.open(url, '_blank', 'noopener,noreferrer');
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

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
    if (file.size > 2 * 1024 * 1024) {
      toast.error('Logo trop volumineux (max 2 Mo).');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await parishService.uploadLogo(paroisseId, file);
      toast.success('Logo enregistré.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload impossible');
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

  return (
    <div className="stack">
      <PageHeader
        title="Personnalisation du reçu"
        subtitle="Logo et contact affichés sur le reçu PDF A4 (ORIGINAL + DUPLICATA)."
        actions={(
          unlocked && paroisseId ? (
            <AppButton type="button" variant="primary" onClick={openPdfSample} disabled={pdfLoading}>
              {pdfLoading ? 'Préparation…' : 'Simuler le reçu PDF'}
            </AppButton>
          ) : null
        )}
      />

      {error ? <AppAlert variant="danger">{error}</AppAlert> : null}
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
            title="Aperçu rapide"
            subtitle="En-tête tel qu’il apparaîtra. Utilisez « Simuler le reçu PDF » pour le rendu exact."
          >
            <div className="receipt-single-preview" aria-label="Aperçu reçu">
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
                  <p className="receipt-preview-contact">{contactLine}</p>
                  <div className="receipt-preview-meta">
                    Code · Montant · Dépôt · Intention
                  </div>
                </div>
              </div>
            </div>
            <div className="button-row" style={{ marginTop: 14 }}>
              <AppButton type="button" variant="secondary" onClick={openPdfSample} disabled={pdfLoading}>
                Simuler le reçu PDF
              </AppButton>
            </div>
          </AppCard>

          <AppCard title="Logo de la paroisse" subtitle="Optionnel. JPEG, PNG ou WebP — 2 Mo max (optimisé côté serveur).">
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
                Votre compte ne peut pas modifier le logo.
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
                <AppInput id="receipt-email" value={parish?.email || ''} disabled readOnly />
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

      <PdfPreviewModal
        open={pdfOpen}
        title="Simulation du reçu PDF"
        pdfBlob={pdfBlob}
        fileName="recu-modele.pdf"
        loading={pdfLoading}
        error={pdfError}
        onClose={() => setPdfOpen(false)}
        onDownload={downloadPdfSample}
        onOpenInTab={openPdfSampleInTab}
      />
    </div>
  );
}
