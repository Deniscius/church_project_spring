# Super Admin, tenants et intervention

Missanye sépare clairement la **plateforme** (exploitation SaaS) et les **paroisses** (travail quotidien local).

## Qui fait quoi

| Acteur | Portée | Usage normal |
|--------|--------|--------------|
| **SUPER_ADMIN** | Tous les tenants + paramètres plateforme | Annuaire, inscriptions, utilisateurs, doyennés, accès, finances (lecture), catalogue modèle |
| **COMPTABLE** (plateforme) | Finances SaaS + catalogue | Abonnements, reversements, catalogue |
| **ADMIN / SECRÉTAIRE / CURÉ / COMPTABLE_LOCAL** | **Une** paroisse | Demandes, horaires, équipe, trésorerie locale, etc. |

Le Super Admin **n’est pas** l’opérateur quotidien d’une paroisse. L’équipe locale (ADMIN, etc.) gère le jour à jour.

## Mode plateforme (défaut Super Admin)

- Menu **Plateforme** uniquement.
- Aucun tenant « opérationnel » sélectionné (ou catalogue modèle uniquement).
- Écrans : paroisses, inscriptions, utilisateurs, accès, doyennés, etc.

## Mode intervention (ponctuel)

Quand un souci l’exige (données bloquées, accès, config, paiement, etc.) :

1. Depuis **Paroisses** → **Intervenir**, ou via le sélecteur / recherche du topbar.
2. Un bandeau **Intervention** rappelle le contexte.
3. Un menu **Contrôle** expose les outils de diagnostic / correction (pas la feuille d’intentions ni le quotidien pastoral).
4. **Quitter** ramène à la plateforme.

Les actions en intervention **impactent réellement** le tenant choisi (mêmes droits API d’intervention que pour le support).

## Densité UI admin

L’espace admin applique `--scale: 0.95` (−5 %) via `.admin-shell`. Le site public reste à 16px.

## Rôles applicatifs vs libellés paroisse

- **UserRole** (`users.role`) : droits réels (menus, API).
- **RoleParoisse** (`paroisse_access.role_paroisse`) : libellé d’équipe dans une paroisse — affichage, pas la source d’autorisation applicative actuelle.

## Règle produit

> Super Admin = contrôle de la plateforme + **intervention** si besoin.  
> Tenant local = **exploitation** de la paroisse.
