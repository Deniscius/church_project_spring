# Interface desktop et adaptation aux écrans

Les FXML fournis (`home.fxml`, `home-view.fxml`, `hello-view.fxml`,
`register.fxml`, `categories.fxml`, `products.fxml`) ont servi de références
visuelles : navigation latérale, accueil synthétique, identité bleue, titres
hiérarchisés, cartes et tableaux accompagnés d'actions. Les vues Missanye sont
implémentées pour JavaFX 17 et pilotées par les contrôleurs métier existants.
Les icônes de navigation sont des tracés vectoriels embarqués dans le FXML.

## Organisation

- **Tableau de bord** : paroisse sélectionnée, compteurs issus des données locales,
  accès rapides et aperçu des cinq premières demandes disponibles. Entrée ou un
  double-clic ouvre la demande sélectionnée ; une recherche précédente est effacée
  pour rendre cette demande visible.
- **Nouvelle demande** : quatre groupes explicites — forfait, coordonnées,
  calendrier, paiement et intention. Le formulaire conserve sa saisie pendant la
  navigation entre les pages.
- **Demandes et paiements** : recherche au-dessus du tableau, panneau de détail et
  actions d'encaissement, de reçu, de correction et de validation.
- **Mon activité** : filtre des opérations, états et actions de reprise.

Le tableau de bord s'affiche après l'ouverture de l'espace local. Les compteurs
portent sur les données consultables du poste et les opérations de la paroisse
courante. Les demandes du tableau de bord reprennent l'ordre des données
chargées. Les cartes ne déclenchent aucun envoi automatique.

## Dimensionnement

Les dimensions JavaFX sont logiques : Windows applique ensuite la mise à
l'échelle de l'écran. La fenêtre principale et les dialogues utilisent la zone
visible du moniteur. Les conteneurs redimensionnent leurs enfants ; les pages
longues restent accessibles par défilement.

| Largeur logique | Navigation | Contenu |
|---|---|---|
| Moins de 1180 | Rail d'icônes de 76 unités, infobulles et noms accessibles | Plus de place pour les formulaires et les tableaux |
| À partir de 1180 | Menu de 228 unités avec libellés | Tableau de bord et listes étendus |

Les cartes de synthèse se répartissent sur une à trois colonnes selon leur
largeur disponible. Pour une hauteur inférieure à 800 unités, les marges sont
réduites. Les colonnes de tableau ont une largeur minimale lisible ; en dessous
de 780 unités disponibles pour le tableau, le défilement horizontal permet de
consulter les colonnes restantes. Les actions utilisent des conteneurs qui
passent à la ligne.

## Vérification

Les tests JavaFX vérifient les fenêtres en 900 × 600, 1024 × 768, 1366 × 768,
1600 × 1000 et 1920 × 1080 unités logiques. Ils contrôlent la navigation, la
conservation d'une saisie, l'ouverture d'une demande depuis l'accueil et l'accès
aux actions après défilement. Les captures sont produites dans
`Desktop/target/desktop-*.png` et publiées par le workflow Windows dans
**Missanye-Interface-Previews**.

Le passage de validation de cette refonte a réussi 44 tests ; les deux tests
optionnels avec backend réel ne sont pas exécutés par défaut. Les tests
headless ne remplacent pas une recette manuelle Windows à 100 %, 125 % et 150 %,
notamment pour les polices et le comportement du système de fenêtres.
