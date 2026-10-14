# Gestion de boutique

Application web de gestion d'une boutique de produits alimentaires.

Elle permet d'enregistrer les **achats** des clients (avec ticket de caisse imprimable) et les **livraisons** des grossistes, de gérer le **stock** (dates de péremption par lot, alertes de stock faible), de consulter l'**historique** (avec filtres et export PDF), de rechercher un produit par son nom et de gérer les utilisateurs selon leur rôle (administrateur ou caissier).

## Technologies

Java 17 · Spring Boot 4.1.1 (MVC, Security, Data JPA) · Thymeleaf · Bootstrap 5 · PostgreSQL · OpenPDF · Maven

## Prérequis

- Java 17 ou supérieur
- PostgreSQL (testé avec la version 16)
- Maven (uniquement pour compiler le projet à partir des sources)

## Installation

### 1. Créer la base de données

Dans `psql` ou pgAdmin, connecté en administrateur (`postgres`) :

```sql
CREATE USER boutique_user WITH PASSWORD 'boutique_pwd';
CREATE DATABASE boutique_db ENCODING 'UTF8' OWNER boutique_user;
```

`boutique_pwd` est un mot de passe de démonstration, propre à ce projet. Vous pouvez en choisir un autre (voir l'étape 2).

### 2. Renommer le fichier de configuration (étape obligatoire)

Le vrai fichier de configuration n'est pas versionné : il est ignoré par git pour ne jamais publier d'informations sensibles. Le dépôt contient un modèle à renommer.

Dans `src/main/resources/`, **renommer `application.properties.exemple` en `application.properties`** :

```
# Linux / macOS
cp src/main/resources/application.properties.exemple src/main/resources/application.properties

# Windows (PowerShell)
Copy-Item src\main\resources\application.properties.exemple src\main\resources\application.properties
```

Puis ouvrir `application.properties` et remplacer la valeur de `spring.datasource.password` par le mot de passe choisi à l'étape 1 (`boutique_pwd` si vous n'avez rien changé) :

```
spring.datasource.password=boutique_pwd
```

### 3. Créer les tables et charger le jeu de test

Depuis le dossier `sql/` :

```
psql -U boutique_user -d boutique_db -f 01_schema.sql
psql -U boutique_user -d boutique_db -f 02_donnees_test.sql
```

> `01_schema.sql` supprime puis recrée les tables : à n'exécuter que sur une base vide ou à réinitialiser.

### 4. Lancer l'application

```
mvn spring-boot:run
```

ou, pour fabriquer puis lancer le JAR exécutable :

```
mvn clean package
java -jar target/gestion-boutique-0.0.1-SNAPSHOT.jar
```

Ouvrir ensuite **http://localhost:8081**.

## Comptes de test

| Identifiant | Mot de passe | Rôle | Particularité |
|---|---|---|---|
| `admin` | `admin123` | Administrateur | Doit changer son mot de passe à la première connexion |
| `caissier1` | `caissier123` | Caissier | Accès direct |
| `caissier2` | `caissier123` | Caissier | Doit changer son mot de passe à la première connexion |
| `caissier3` | `caissier123` | Caissier | Compte désactivé : connexion refusée |

## Vérifier la cohérence du stock

Après des ventes, des livraisons ou des retraits de lots, le script `sql/03_verification_stock.sql` vérifie que le stock de chaque produit correspond à la somme des unités restantes de ses lots. Les trois premiers contrôles ne doivent afficher aucune ligne.

```
psql -U boutique_user -d boutique_db -f 03_verification_stock.sql
```

## Architecture

Application en couches, selon le modèle MVC :

```
src/main/java/com/boutique/gestionboutique/
├── controller/   Contrôleurs (reçoivent les requêtes, choisissent la vue)
├── service/      Règles de gestion et transactions
├── repository/   Accès aux données (Spring Data JPA)
├── model/        Entités persistantes
├── security/     Authentification, droits par rôle
├── form/ dto/    Objets de saisie et de lecture
└── config/       Configuration

src/main/resources/
├── templates/    Vues Thymeleaf
└── static/       Scripts et styles

sql/              Scripts de la base (schéma, jeu de test, vérification)
```
