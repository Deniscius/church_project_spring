/**
 * Textes des bulles d’aide (parcours public + admin).
 * Centralisé pour rester cohérent et facile à ajuster.
 */
export const HELP = {
  demande: {
    step1:
      'Indiquez l’intention et un téléphone. Nom et e-mail sont optionnels.',
    step2:
      'Paroisse, puis type de demande, puis tarif — dans cet ordre.',
    step3:
      'Choisissez la date, puis l’heure proposée par la paroisse.',
    step4:
      'Choisissez comment vous réglerez. Le code de suivi permettra de payer ensuite.',
    prenom: `Optionnel. Sans prénom ni nom, le dossier affiche « Un(e) chrétien(ne) ».`,
    nom: `Optionnel. Sans prénom ni nom, le dossier affiche « Un(e) chrétien(ne) ».`,
    email: 'Optionnel — utile pour recevoir des infos sur la demande.',
    telephone:
      'Obligatoire. Choisissez le pays, puis le numéro sans l’indicatif. Ce même numéro permet ensuite de retrouver vos demandes sur la page Suivi.',
    intention:
      'Formulez clairement pour qui ou pour quoi (min. 10 caractères). Les suggestions ci-dessous aident à démarrer.',
    paroisse: 'Tapez le nom pour filtrer les paroisses actives.',
    typeDemande: 'Détermine les tarifs et le délai avant la célébration.',
    nature: 'Le montant s’affiche dans la liste (normale, dominicale, spéciale…).',
    date: 'Choisissez parmi les jours autorisés pour ce tarif.',
    paiement: 'Le paiement se finalise ensuite avec le code de suivi.',
    codeSuivi:
      'Saisissez le code reçu lors du dépôt, ou utilisez le téléphone du dépôt pour retrouver vos demandes.',
    guideNom:
      'Si vous ne renseignez pas de nom, le demandeur apparaîtra comme « Un(e) chrétien(ne) » sur le reçu et la feuille d’intentions.',
    guideTelephone:
      'Avec le numéro utilisé au dépôt, vous pouvez retrouver vos demandes sur Suivi, même sans le code sous la main.',
  },
  inscription: {
    doyenne: 'Sélectionnez le doyenné de l’archidiocèse pour charger l’annuaire des paroisses.',
    paroisse: 'Tapez le nom de la paroisse. Si elle n’apparaît pas, utilisez la saisie manuelle.',
    adresse: 'Indiquez le lieu précis (quartier, rue, ville) pour localiser la paroisse.',
    telephone: 'Choisissez le pays, puis le numéro national sans l’indicatif.',
    emailParoisse: 'Optionnel. Adresse de contact de la paroisse (secrétariat). Ce n’est pas l’adresse qui reçoit le code OTP.',
    adminEmail: 'Obligatoire. Adresse personnelle de l’administrateur : le code OTP et les identifiants y sont envoyés.',
    documents: 'Scannez ou photographiez le mandat du curé et la pièce d’identité du premier administrateur (PDF, JPEG ou PNG, max 5 Mo).',
    otp: 'Le code à 6 chiffres part uniquement vers l’e-mail personnel de l’administrateur (pas l’e-mail paroisse). Après validation, Missanye génère vos identifiants de connexion.',
  },
  admin: {
    recu: 'Personnalisez le reçu PDF (logo et téléphone). Le document tient sur une page A4 avec les informations essentielles : code de suivi, intention, célébration et paiement.',
    tresorerie: 'Le solde en ligne correspond aux intentions payées via la plateforme, distinct de la caisse espèces locale.',
  },
};
