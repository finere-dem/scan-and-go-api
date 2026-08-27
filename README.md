# Scan & Go API

API backend d'une plateforme B2B de distribution pour le marche ouest-africain (Mali/UEMOA) : un importateur catalogue ses produits et ses depots, vend a des grossistes et detaillants avec des politiques de prix et de credit commercial propres a chaque relation, et chaque acteur de la chaine peut scanner un produit (code-barres ou QR code) pour l'ajouter a une commande. Ce depot est le backend Spring Boot ; l'application mobile Flutter et le back-office web consomment cette API depuis des depots separes.

Projet realise en solo (backend + mobile + web-admin + deploiement), dans le cadre d'un statut national etudiant-entrepreneur (SNEE) en France, pour FINERE SOFT.

> Ce README documente une reecriture complete du projet initial (qui etait un MVP de self-checkout mono-magasin pour la grande distribution francaise). L'ancien MVP est toujours visible dans l'historique Git de ce depot.

## Sommaire

- [Ce que fait l'API](#ce-que-fait-lapi)
- [Stack technique](#stack-technique)
- [Modele de donnees](#modele-de-donnees)
- [Regles metier cles](#regles-metier-cles)
- [Endpoints](#endpoints)
- [Lancer le projet en local](#lancer-le-projet-en-local)
- [Variables d'environnement](#variables-denvironnement)
- [Deploiement](#deploiement)
- [Tests](#tests)
- [Etat du projet et limites connues](#etat-du-projet-et-limites-connues)

## Ce que fait l'API

Le systeme couvre la chaine de distribution B2B complete :

1. **Organisations et KYC** : un super-admin valide les organisations (importateur, grossiste, detaillant) avant qu'elles puissent operer.
2. **Catalogue et logistique** : un importateur cree ses produits, ses depots, et enregistre les lots recus (numero de lot, dates de peremption, quantites) pour une allocation de stock FEFO (First Expired, First Out).
3. **Tarification en cascade** : chaque vendeur fixe son propre prix et sa quantite minimum de commande selon le type de client (grossiste ou detaillant) ; un detaillant peut ensuite fixer librement son propre prix de vente au consommateur, independamment de ce qu'il a paye.
4. **Commande B2B** : l'application mobile impose l'isolation par fournisseur (un panier ne peut contenir que des produits d'un seul vendeur) et le respect des quantites minimums, avant de soumettre la commande a l'API qui alloue le stock en FEFO et verifie le credit commercial si le paiement est a credit.
5. **Credit commercial** : chaque relation acheteur-vendeur a un plafond de credit et un delai de paiement ; une tache planifiee nocturne detecte les factures en retard et verrouille automatiquement les comptes en defaut de paiement prolonge.
6. **Tracabilite QR** : generation de QR codes vectoriels et de planches d'etiquettes A4 imprimables pour les produits et les lots.
7. **Audit** : l'historique complet des comptes de credit (limite, solde, statut) est conserve via Hibernate Envers, pas seulement le dernier etat.

## Stack technique

- **Java 17**, Spring Boot 4.0.2 (Spring Web MVC, Spring Data JPA, Spring Security)
- **PostgreSQL 16+** avec migrations Flyway versionnees (le schema n'est jamais auto-genere par Hibernate)
- **JWT asymetrique (RSA)** pour l'authentification, sans etat serveur
- **Hibernate Envers** pour l'historique d'audit des comptes de credit
- **ZXing** pour la generation de QR codes, **OpenPDF** pour les planches d'etiquettes
- **Lombok**, **MapStruct**
- Deploye sur [Koyeb](https://www.koyeb.com/) via Docker, base de donnees geree sur [Aiven](https://aiven.io/)

## Modele de donnees

Douze tables couvrent l'isolation multi-tenant et la tracabilite de bout en bout :

```
organizations        -- entite legale racine (importateur / grossiste / detaillant), statut KYC
users                -- rattaches a une organisation, role determine les permissions
warehouses           -- depots physiques par organisation
products             -- catalogue de base, cree par l'importateur d'origine
product_lots         -- tracabilite par lot (peremption, quantite), rotation FEFO
pricing_policies     -- prix + quantite minimum, par vendeur / produit / type de client cible
local_retail_prices  -- prix de vente au consommateur, fixe librement par le detaillant
credit_accounts      -- plafond, solde, delai de paiement par relation acheteur-vendeur
orders / order_items -- commandes B2B, allocation de lot par ligne
invoices             -- facturation et suivi des echeances
qr_matrix_tokens     -- tokens signes (HMAC) encodes dans les QR codes
```

## Regles metier cles

- **Allocation FEFO** : `InventoryAllocationService` verrouille les lots (`SELECT ... FOR UPDATE`) et alloue en priorite les lots dont la date de peremption est la plus proche. Chaque allocation s'execute dans sa propre transaction (`REQUIRES_NEW`) pour minimiser la duree du verrou ; en cas d'echec d'une commande multi-lignes apres qu'une ligne ait deja ete allouee, une action compensatoire restitue explicitement le stock.
- **Credit avant stock** : toute validation de commande a credit (plafond, statut du compte) s'execute avant l'allocation de stock, jamais apres — une commande rejetee ne doit jamais laisser du stock consomme pour rien.
- **Isolation par organisation** : `CurrentUserService` verifie que l'utilisateur authentifie agit bien pour sa propre organisation (sauf super-admin) avant toute creation ou modification, sur chaque endpoint.

## Endpoints

Vue d'ensemble (voir les controleurs dans `controller/` pour le detail complet) :

| Domaine | Routes principales |
|---|---|
| Authentification | `POST /api/auth/register`, `/login`, `/refresh` |
| Organisations | `POST/GET /api/organizations`, `PATCH /api/organizations/{id}/status` (KYC) |
| Catalogue | `POST/GET /api/products`, `/api/product-lots`, `/api/warehouses` |
| Tarification | `POST/GET /api/pricing-policies`, `/api/local-retail-prices` |
| Credit | `POST/GET /api/credit-accounts`, `GET /api/credit-accounts/{id}/history` (audit) |
| Commandes | `POST /api/orders`, `GET /api/orders?buyerOrgId=` / `?sellerOrgId=` |
| Factures | `GET /api/invoices?buyerOrgId=` / `?sellerOrgId=` |
| QR & etiquettes | `GET /api/qr-codes/products/{id}/png`, `/shelf-poster/png`, `/label-sheet.pdf` |
| Admin | `POST /api/admin/credit/run-overdue-sweep` (declenchement manuel de la tache nocturne) |

## Lancer le projet en local

Prerequis : Java 17, Maven (ou le wrapper `mvnw` fourni), une base PostgreSQL locale.

```bash
# Creer la base
createdb scan_and_go_db

# Generer une paire de cles RSA pour signer les JWT (dev uniquement)
mkdir -p src/main/resources/keys
openssl genrsa -out src/main/resources/keys/private.pem 2048
openssl rsa -in src/main/resources/keys/private.pem -pubout -out src/main/resources/keys/public.pem

# Definir le mot de passe de la base (obligatoire, pas de valeur par defaut)
export DB_PASSWORD=votre_mot_de_passe

# Lancer l'application (Flyway applique automatiquement les migrations)
./mvnw spring-boot:run
```

L'API demarre sur `http://localhost:8080`.

## Variables d'environnement

| Variable | Obligatoire | Defaut | Usage |
|---|---|---|---|
| `PORT` | non | `8080` | Port d'ecoute du serveur |
| `DB_URL` | non | `jdbc:postgresql://localhost:5432/scan_and_go_db` | URL JDBC de la base |
| `DB_USERNAME` | non | `postgres` | Utilisateur PostgreSQL |
| `DB_PASSWORD` | **oui** | aucune | Mot de passe PostgreSQL, aucun defaut pour eviter tout mot de passe faible en dur dans le code |
| `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` | non | `classpath:keys/*.pem` | Chemin vers les cles RSA (usage local) |
| `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM` | non | aucune | Cles RSA en texte brut (PEM), prioritaires sur le chemin ci-dessus - utilise en deploiement cloud ou aucun fichier de cle n'est present dans l'image |
| `JWT_ACCESS_TTL_MIN` / `JWT_REFRESH_TTL_DAYS` | non | `60` / `14` | Duree de vie des jetons |
| `QR_HMAC_SECRET` | non (mais **fortement recommande en prod**) | valeur de dev | Secret HMAC signant les QR codes |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | non | `localhost` / `6379` / vide | Reserve pour un usage futur (cache, verrous distribues) - l'application demarre normalement sans Redis reellement joignable |
| `CORS_ALLOWED_ORIGINS` | non | `*` | Origines autorisees en CORS |

## Deploiement

Un `Dockerfile` multi-stage (build Maven + JDK 17, execution sur JRE 17) est fourni a la racine. Deploiement teste sur Koyeb (build automatique depuis ce depot GitHub) avec une base PostgreSQL geree sur Aiven (necessite `?sslmode=require` dans `DB_URL`).

## Tests

64 tests unitaires (JUnit 5 / Mockito / AssertJ) couvrent la logique metier a risque : allocation FEFO, moteur de credit (plafond, statuts, balayage nocturne des impayes), isolation par organisation, et l'ordre de validation credit-avant-stock d'une commande.

```bash
./mvnw test
```

## Etat du projet et limites connues

Honnetement, voici ce qui reste a faire avant une mise en production reelle :

- **Pas de tests automatises cote web-admin (React) ni cote mobile au-dela de la logique pure** (le panier et le decodage JWT sont testes ; les ecrans ne le sont pas).
- **CORS ouvert a toutes les origines** par defaut, a restreindre en production reelle.
- **Redis present en dependance mais non exploite fonctionnellement** pour l'instant (verrouillage distribue non necessaire en instance unique, remplace par des verrous pessimistes PostgreSQL).
- **Pas de generation de facture PDF** (seul un export CSV existe cote back-office).

Ce projet reste une base technique complete et fonctionnelle : authentification JWT asymetrique, moteur FEFO avec verrous concurrents, moteur de credit avec tache planifiee, audit trail Envers, isolation multi-tenant testee, et deploiement cloud operationnel.
