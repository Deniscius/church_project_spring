import { useCallback, useEffect, useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import AppButton from '../../../components/ui/AppButton';
import AppCard from '../../../components/ui/AppCard';
import AppInput from '../../../components/ui/AppInput';
import { useTenant } from '../../../hooks/useTenant';
import { useToast } from '../../../contexts/toast.context';
import { choirService } from '../../../services/choir.service';
import { copyText } from '../../../utils/clipboard';

export default function ChoirsPage() {
  const { activeParish } = useTenant();
  const toast = useToast();
  const [choirs, setChoirs] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [busy, setBusy] = useState(false);
  const [invitation, setInvitation] = useState(null);

  const load = useCallback(async () => {
    if (!activeParish?.id) return;
    try {
      setChoirs(await choirService.list(activeParish.id));
    } catch (error) {
      toast.error(error?.message || 'Impossible de charger les chorales.');
    }
  }, [activeParish?.id, toast]);

  useEffect(() => { load(); }, [load]);

  async function create(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await choirService.create(activeParish.id, { nom: name.trim(), description: description.trim() || null });
      setName(''); setDescription('');
      toast.success('La chorale a été ajoutée à la paroisse.');
      await load();
    } catch (error) {
      toast.error(error?.message || 'Impossible d’ajouter la chorale.');
    } finally { setBusy(false); }
  }

  async function generate(choir) {
    try {
      const result = await choirService.invite(choir.publicId);
      setInvitation({ ...result, choirName: choir.nom });
      toast.success('Invitation générée. Copiez-la maintenant.');
    } catch (error) {
      toast.error(error?.message || 'Impossible de générer l’invitation.');
    }
  }

  async function remove(choir) {
    if (!window.confirm(`Désactiver la chorale « ${choir.nom} » ?`)) return;
    try {
      await choirService.remove(choir.publicId);
      toast.success('La chorale a été désactivée.');
      await load();
    } catch (error) { toast.error(error?.message || 'Suppression impossible.'); }
  }

  return (
    <div>
      <PageHeader title="Chorales" subtitle={`Chorales rattachées à ${activeParish?.name || 'la paroisse'}.`} />
      <AppCard title="Ajouter une chorale" subtitle="Elle restera rattachée au doyenné et à la paroisse sélectionnés.">
        <form onSubmit={create} className="form-grid">
          <label className="form-group">Nom de la chorale
            <AppInput value={name} onChange={(e) => setName(e.target.value)} minLength={2} maxLength={150} required />
          </label>
          <label className="form-group">Description facultative
            <AppInput value={description} onChange={(e) => setDescription(e.target.value)} maxLength={500} />
          </label>
          <AppButton type="submit" loading={busy}>Ajouter la chorale</AppButton>
        </form>
      </AppCard>

      {invitation && (
        <AppCard title={`Invitation — ${invitation.choirName}`} subtitle={`Valable jusqu’au ${new Date(invitation.expiresAt).toLocaleString('fr-FR')}. Usage unique.`}>
          <div className="form-row">
            <AppInput value={invitation.token} readOnly aria-label="Invitation Chorale-Management" />
            <AppButton onClick={async () => {
              const copied = await copyText(invitation.token);
              copied ? toast.success('Invitation copiée.') : toast.error('Copie impossible.');
            }}>Copier</AppButton>
          </div>
          <p className="muted">Transmettez cette invitation uniquement au responsable de la chorale, dans un canal privé.</p>
        </AppCard>
      )}

      <AppCard title="Chorales enregistrées">
        {choirs.length === 0 ? <p className="muted">Aucune chorale enregistrée.</p> : (
          <div className="table-responsive"><table className="table"><thead><tr><th>Nom</th><th>Description</th><th>Actions</th></tr></thead><tbody>
            {choirs.map((choir) => <tr key={choir.publicId}><td>{choir.nom}</td><td>{choir.description || '—'}</td><td className="form-row">
              <AppButton size="sm" onClick={() => generate(choir)}>Générer une invitation</AppButton>
              <AppButton size="sm" variant="danger" onClick={() => remove(choir)}>Désactiver</AppButton>
            </td></tr>)}
          </tbody></table></div>
        )}
      </AppCard>
    </div>
  );
}
