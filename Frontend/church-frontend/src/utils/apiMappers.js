export function mapParoisseToTenant(p) {
  if (!p) return null;
  return {
    id: p.publicId,
    name: p.nom,
    city: p.localiteVille || '',
    email: p.email || '',
    phone: p.telephone || '',
    raw: p,
  };
}

export function mapParoisseToTableRow(p) {
  return {
    id: p.publicId,
    name: p.nom,
    city: p.localiteVille || '—',
    email: p.email || '—',
    phone: p.telephone || '—',
    active: p.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapDemandeToRequestRow(d) {
  return {
    id: d.publicId,
    trackingCode: d.codeSuivie,
    applicant: [d.prenomFidele, d.nomFidele].filter(Boolean).join(' ') || '—',
    requestType: d.typeDemandeLibelle || '—',
    requestStatus: d.statutDemande,
    validationStatus: d.statutValidation,
    paymentStatus: d.statutPaiement,
    amount: d.montant != null ? Number(d.montant) : null,
    createdAt: d.createdAt,
    _raw: d,
  };
}

export function mapDemandeToPaymentRow(d) {
  return {
    id: d.publicId,
    trackingCode: d.codeSuivie,
    applicant: [d.prenomFidele, d.nomFidele].filter(Boolean).join(' ') || '—',
    type: d.typePaiementLibelle || d.modePaiement || '—',
    amount: d.montant != null ? Number(d.montant) : null,
    status: d.statutPaiement,
    paidAt: d.dateDetailsPaiement,
    transactionId: d.idTransaction || '—',
  };
}

export function mapFactureToInvoiceRow(f) {
  return {
    id: f.publicId,
    number: f.refFacture,
    trackingCode: f.codeSuivieDemande || '—',
    applicant: [f.prenomFidele, f.nomFidele].filter(Boolean).join(' ') || '—',
    amount: f.montant != null ? Number(f.montant) : null,
    status: f.statutPaiement,
    _raw: f,
  };
}

export function mapUserToRow(u) {
  return {
    id: u.publicId,
    firstName: u.prenom,
    lastName: u.nom,
    username: u.username,
    role: u.role,
    active: u.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapParoisseAccessToRow(a) {
  const userLabel = [a.userPrenom, a.userNom].filter(Boolean).join(' ') || a.username || '—';
  return {
    id: a.publicId,
    user: userLabel,
    parish: a.paroisseNom || '—',
    role: a.roleParoisse,
    active: a.active ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapLocaliteToRow(l) {
  return {
    id: l.publicId,
    label: [l.ville, l.quartier].filter(Boolean).join(' — ') || '—',
  };
}

export function mapTypePaiementToRow(t) {
  return {
    id: t.publicId,
    label: t.libelle,
    mode: t.mode,
  };
}

export function mapTypeDemandeToRow(t) {
  return {
    id: t.publicId,
    label: t.libelle,
    category: t.typeDemandeEnum,
    active: t.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapHoraireToRow(h) {
  return {
    id: h.publicId,
    label: h.libelle || h.jourSemaine || '—',
    day: h.jourSemaine,
    hour: h.heureCelebration,
    active: h.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}

export function mapForfaitToRow(f) {
  return {
    id: f.publicId,
    label: f.libelle || f.nomForfait || f.codeForfait,
    amount: f.montantForfait != null ? Number(f.montantForfait) : null,
    celebrations: f.nombreCelebration ?? '—',
    customHour: Boolean(f.heurePersonnalise),
    active: f.isActive ? 'ACTIVE' : 'INACTIVE',
  };
}
