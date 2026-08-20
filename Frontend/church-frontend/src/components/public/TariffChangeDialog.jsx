import React from 'react';
import AppDialog from '../ui/AppDialog';
import { formatCurrency } from '../../utils/formatCurrency';

function formatAmount(value) {
  if (value == null || value === '') return '—';
  const amount = Number(value);
  return Number.isFinite(amount) ? formatCurrency(amount) : '—';
}

export default function TariffChangeDialog({ open, notice, onClose }) {
  if (!notice) return null;

  return (
    <AppDialog
      open={open}
      title="Le tarif change pour cette date"
      variant="info"
      confirmLabel="J’ai compris"
      hideCancel
      onConfirm={onClose}
      onCancel={onClose}
    >
      <div className="stack" style={{ gap: 14 }}>
        <p style={{ margin: 0 }}>
          La date choisie, <strong>{notice.formattedDate}</strong>, applique un tarif différent
          selon le jour de célébration.
        </p>

        <div className="info-list">
          <div className="info-row">
            <span>Tarif précédent</span>
            <strong>
              {notice.previousLabel} · {formatAmount(notice.previousAmount)}
            </strong>
          </div>
          <div className="info-row">
            <span>Nouveau tarif</span>
            <strong>
              {notice.nextLabel} · {formatAmount(notice.nextAmount)}
            </strong>
          </div>
        </div>

        <p className="muted" style={{ margin: 0 }}>
          Le nouveau montant a été appliqué automatiquement à votre demande et apparaît dans le résumé.
        </p>
      </div>
    </AppDialog>
  );
}
