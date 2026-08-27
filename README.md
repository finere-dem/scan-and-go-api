# Scan & Go API

API backend d'une application de self-checkout mobile : le client scanne le code-barres d'un produit en rayon avec son telephone, l'ajoute a son panier, et finalise ses achats sans passer par une caisse traditionnelle. Ce depot est le backend Spring Boot ; l'application mobile Flutter cliente vit dans un depot separe.

Projet realise en solo (backend + mobile + deploiement), dans le cadre d'un statut national etudiant-entrepreneur (SNEE) en France.

## Sommaire

- [Ce que fait l'API](#ce-que-fait-lapi)
- [Stack technique](#stack-technique)
- [Modele de donnees](#modele-de-donnees)
- [Endpoints](#endpoints)
- [Lancer le projet en local](#lancer-le-projet-en-local)
- [Variables d'environnement](#variables-denvironnement)
- [Etat du projet et limites connues](#etat-du-projet-et-limites-connues)
- [Ce que j'ai appris](#ce-que-jai-appris)

## Ce que fait l'API

Trois operations couvrent le parcours complet d'un self-checkout :

1. **Scanner un produit** : identifier un produit par son code-barres, dans le contexte d'un magasin donne (le meme produit peut avoir un prix different d'un magasin a l'autre).
2. **Passer en caisse** : calculer le total d'un panier (liste de produits) pour un magasin, et enregistrer la commande.
3. **Lister les magasins** disponibles, pour que l'app mobile propose un choix a l'utilisateur avant de commencer a scanner.

## Stack technique

- **Java 17**, Spring Boot 4.0.2 (Spring Web MVC, Spring Data JPA)
- **PostgreSQL** comme base de donnees
- **Lombok** pour reduire le boilerplate
- Deploye en production sur [Koyeb](https://www.koyeb.com/)

## Modele de donnees

```
Store (magasin)
  id, nom, adresse

Product (produit)
  id, code-barres, nom, description

StoreProduct (association magasin <-> produit)
  id, store_id, product_id, prix
  -> permet a un meme produit d'avoir un prix different selon le magasin

Order (commande)
  id, total, statut, date de creation, store_id
```

`DataSeeder` peuple automatiquement deux magasins de demo (Leclerc Bondy, Carrefour Paris) et deux produits (Coca-Cola, Nutella) avec des prix differents selon le magasin, au premier demarrage si la base est vide.

## Endpoints

| Methode | Route | Description |
|---|---|---|
| `GET` | `/api/scan?barcode={code}&storeId={id}` | Cherche un produit par code-barres dans un magasin donne. Retourne 404 si absent. |
| `POST` | `/api/checkout` | Body JSON `{ "storeId": 1, "productIds": [1, 2] }`. Calcule le total et cree la commande. Retourne l'id de la commande. |
| `GET` | `/api/stores` | Liste tous les magasins disponibles. |

## Lancer le projet en local

Prerequis : Java 17, Maven (ou le wrapper `mvnw` fourni), une base PostgreSQL locale.

```bash
# Creer la base
createdb scan_and_go_db

# Definir le mot de passe de la base (obligatoire, pas de valeur par defaut)
export DB_PASSWORD=votre_mot_de_passe

# Lancer l'application
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
| `JPA_DDL_AUTO` | non | `update` | Strategie Hibernate de synchronisation du schema |
| `JPA_SHOW_SQL` | non | `false` | Affiche les requetes SQL dans les logs |
| `CORS_ALLOWED_ORIGINS` | non | `*` | Origines autorisees en CORS |

## Etat du projet et limites connues

Ce depot est un MVP fonctionnel, pas un produit pret pour la production. Honnetement, voici ce qui manque avant qu'il puisse gerer de vrais paiements en magasin :

- **Pas de paiement reel** : le checkout enregistre une commande marquee "COMPLETED" mais ne debite personne. Une integration type Stripe serait necessaire.
- **Pas d'authentification** : aucun compte utilisateur, aucune protection des routes.
- **Pas de prevention anti-vol** : les solutions de self-checkout en production (Carrefour, Auchan, ou le fournisseur europeen shopreme) reposent sur des verifications de sortie, des algorithmes de detection de comportement suspect, et des applications staff dediees. Rien de tout ca ici.
- **CORS ouvert a toutes les origines** par defaut, a restreindre en production reelle.

Ce projet a ete mis en pause cote commercial apres etude du marche francais du self-checkout, deja bien equipe par les grandes enseignes et par des fournisseurs specialises (shopreme notamment, qui propose deja une offre marque blanche avec gestion anti-vol integree). Il reste ici comme preuve technique complete : conception d'un modele de donnees multi-magasins, API REST, et application mobile bout en bout.

## Ce que j'ai appris

Concevoir un modele de donnees ou le meme produit a des prix differents selon le magasin (via la table d'association `StoreProduct` plutot qu'un prix fixe sur `Product`), deployer une API Spring Boot en production sur Koyeb, et faire le lien complet avec une application Flutter consommant cette API en conditions reelles.
