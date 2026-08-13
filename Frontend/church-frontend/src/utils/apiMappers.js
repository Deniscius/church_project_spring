export function mapParoisseToTenant(p) {
  if (!p) return null;
  return {
    id: p.publicId || p.id,
    name: p.nom,
    city: p.doyenneNom || '',
    email: p.email || '',
    phone: p.telephone || '',
    subscriptionExpiresAt: p.subscriptionExpiresAt || null,
    isSystem: Boolean(p.isSystem),
    statutTenant: p.statutTenant || null,
    raw: p,
  };
}

export function mapParoisseToTableRow(p) {
  return {
    id: p.publicId,
    name: p.nom,
    address: p.adresse || '',
    city: p.doyenneNom || '—',
    deaneryId: p.doyennePublicId,
    email: p.email || '—',
    phone: p.telephone || '—',
    active: p.statutTenant,
    subscriptionExpiresAt: p.subscriptionExpiresAt || null,
    nomBanque: p.nomBanque || '',
    titulaireCompte: p.titulaireCompte || '',
    ibanOrRib: p.ibanOrRib || '',
  };
}

export function mapDemandeToRequestRow(d) {
  const celebrationDates = Array.isArray(d.datesCelebration) && d.datesCelebration.length
    ? d.datesCelebration.map((date) => formatDateShort(date) || '—').join(' · ')
    : '—';
  return {
    id: d.publicId,
    trackingCode: d.codeSuivie,
    applicant: formatFideleName(d.prenomFidele, d.nomFidele),
    requestType: d.typeDemandeLibelle || '—',
    requestStatus: d.statutDemande,
    validationStatus: d.statutValidation,
    paymentStatus: d.statutPaiement,
    amount: d.montant != null ? Number(d.montant) : null,
    createdAt: d.createdAt,
    celebrationDates,
    statusDel: Boolean(d.statusDel),
    deletedAt: d.deletedAt || null,
    deletedByNom: d.deletedByNom || null,
    _raw: d,
  };
}

export function mapDemandeToPaymentRow(d) {
  return {
    id: d.publicId,
    trackingCode: d.codeSuivie,
    applicant: formatFideleName(d.prenomFidele, d.nomFidele),
    type: d.typePaiementLibelle || d.modePaiement || '—',
    amount: d.montant != null ? Number(d.montant) : null,
    status: d.statutPaiement,
    paidAt: d.dateDetailsPaiement,
    transactionId: d.idTransaction || '—',
  };
}

export function mapFactureToInvoiceRow(f) {
  const reglement = f.reglement || null;
  const objet = [f.typeDemandeLibelle, f.forfaitNom].filter(Boolean).join(' — ');

  return {
    id: f.publicId,
    number: f.refFacture,
    trackingCode: f.codeSuivieDemande || '—',
    applicant: formatFideleName(f.prenomFidele, f.nomFidele),
    contact: f.telFidele || f.emailFidele || '—',
    intention: f.intention || '—',
    object: objet || '—',
    amount: f.montant != null ? Number(f.montant) : null,
    // Net réellement crédité à la paroisse : montant facturé moins frais agrégateur.
    netAmount: reglement?.montantNet != null ? Number(reglement.montantNet) : null,
    fees: reglement?.montantFrais != null ? Number(reglement.montantFrais) : null,
    status: f.statutPaiement,
    issuedAt: f.dateEmission || null,
    paidAt: f.datePaiement || reglement?.datePaiement || null,
    paymentMode: f.modePaiement || null,
    paymentLabel: f.typePaiementLibelle || null,
    provider: reglement?.provider || null,
    transactionId: reglement?.idTransaction || null,
    demandeId: f.demandePublicId || null,
    _raw: f,
  };
}

export function mapUserToRow(u) {
  const isActive = Boolean(u.isActive);

  return {
    id: u.publicId,
    firstName: u.prenom,
    lastName: u.nom,
    username: u.username,
    email: u.email || '',
    telephone: u.telephone || '',
    contact: [u.email, u.telephone].filter(Boolean).join(' · ') || '—',
    role: u.role,
    active: isActive ? 'ACTIVE' : 'INACTIVE',
    isActive,
    isGlobal: Boolean(u.isGlobal),
  };
}

export function mapParoisseAccessToRow(a) {
  const userLabel = [a.userPrenom, a.userNom].filter(Boolean).join(' ') || a.username || '—';
  return {
    id: a.publicId,
    user: userLabel,
    username: a.username || '',
    userEmail: a.userEmail || '',
    userTelephone: a.userTelephone || '',
    userRole: a.userRole || '',
    userActive: a.userActive !== false,
    parish: a.paroisseNom || '—',
    parishId: a.paroissePublicId,
    parishEmail: a.paroisseEmail || '',
    parishTelephone: a.paroisseTelephone || '',
    deanery: a.doyenneNom || '',
    parishActive: a.paroisseActive !== false,
    subscriptionExpiresAt: a.paroisseSubscriptionExpiresAt || null,
    role: a.roleParoisse,
    active: a.active ? 'ACTIVE' : 'INACTIVE',
    isActive: Boolean(a.active),
  };
}

export function mapDoyenneToRow(l) {
  return {
    id: l.publicId,
    label: l.nom || '—',
    name: l.nom || '—',
    rang: l.rang ?? null,
    description: l.description || '—',
  };
}

export function mapTypePaiementToRow(t) {
  return {
    id: t.publicId,
    label: t.libelle,
    mode: t.mode,
  };
}

import { formatAllowedDays } from './schedulingUtils';
import { formatFideleName } from './personName';
import { formatDateShort } from './formatDate';
import {
  NATURE_FORFAIT_LABELS,
  PRIMARY_REQUEST_TYPE_LABELS,
  WEEK_DAY_LABELS,
  getForfaitDureeLabel,
} from '../constants/enums';

export function mapTypeDemandeToRow(t) {
  return {
    id: t.publicId,
    label: t.libelle,
    category: PRIMARY_REQUEST_TYPE_LABELS[t.typeDemandeEnum] || t.typeDemandeEnum,
    allowedDays: formatAllowedDays(t.joursCelebrationAutorises),
    leadTime: `${t.delaiMinimumHeures ?? 24} h`,
    active: t.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapHoraireToRow(h) {
  const dateLabel = h.dateSpecifique
    ? h.dateSpecifique
    : (WEEK_DAY_LABELS[h.jourSemaine] || h.jourSemaine || '—');
  const nature = h.natureHonoraire
    ? (NATURE_FORFAIT_LABELS[h.natureHonoraire] || h.natureHonoraire)
    : null;
  return {
    id: h.publicId,
    label: h.libelle || (h.dateSpecifique ? 'Événement solennel' : h.jourSemaine) || '—',
    day: dateLabel,
    hour: h.heureCelebration,
    flags: [
      h.uniqueSurParoisse ? 'Unique' : null,
      h.dateSpecifique ? (nature ? `Solennel · ${nature}` : 'Date précise') : 'Hebdo',
    ].filter(Boolean).join(' · '),
    active: h.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapForfaitToRow(f) {
  return {
    id: f.publicId,
    code: f.codeForfait,
    label: f.libelle || f.nomForfait || f.codeForfait,
    nature: NATURE_FORFAIT_LABELS[f.natureForfait] || f.natureForfait || '—',
    amount: f.montantForfait != null ? Number(f.montantForfait) : null,
    celebrations: f.nombreCelebration != null
      ? `${f.nombreCelebration} (${getForfaitDureeLabel(f.nombreCelebration)})`
      : '—',
    allowedDays: formatAllowedDays(f.joursCelebrationAutorises),
    customHour: Boolean(f.heurePersonnalise),
    active: f.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}
