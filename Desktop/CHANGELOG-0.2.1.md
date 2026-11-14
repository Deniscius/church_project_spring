# Missanye Desktop 0.2.1

- Paramètres paroissiaux : liste et formulaire séparés, cartes lisibles, actions fixes et disposition adaptée à la largeur de la fenêtre. Les informations d’identité d’un membre sont présentées en lecture seule ; le rôle et l’activation restent modifiables selon les permissions.
- Contrôles visuels harmonisés sur les écrans de connexion, demandes, tableaux et paramètres.
- Le choix explicite d’un forfait, notamment Dominicale, reste sélectionné après un changement de date ou une actualisation. Les horaires et jours autorisés continuent de s’appliquer ; un message signale une date incompatible.
- En ligne, vérification conditionnelle de la révision paroissiale toutes les dix secondes. Les données sont rechargées lorsqu’elles changent. Une saisie administrative en cours reste intacte avec une invitation à actualiser après enregistrement.
- Réconciliation complète au moins chaque minute, compatibilité avec un backend plus ancien, reprise après interruption et bouton d’actualisation immédiate.
- Le nouvel endpoint de révision exige une session et contrôle l’accès à la paroisse. Il renvoie une empreinte opaque et utilise ETag/304 ; chaque lecture et écriture métier conserve ses contrôles habituels.

Le backend doit être déployé avec cette version pour bénéficier de la vérification conditionnelle. L’installation Windows conserve le dossier Program Files, les droits administrateur et les paramètres existants.
