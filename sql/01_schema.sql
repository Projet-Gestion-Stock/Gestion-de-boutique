-- =====================================================================
--  Logiciel de gestion de boutique - Script DDL (PostgreSQL)
--  Base : boutique_db

-- Création d'un utilisateur "boutique_user" avec son mot de passe ayant tout les droits sur la base boutique_db :

--      CREATE USER boutique_user WITH PASSWORD 'boutique_pwd';

--  Création de la base (à exécuter une fois, connecté en postgres) :

--      CREATE DATABASE boutique_db ENCODING 'UTF8' OWNER boutique_user;

--  Puis se connecter à "boutique_db" et exécuter ce script.
--  Arrêter l'application avant de l'exécuter.
-- =====================================================================

DROP TABLE IF EXISTS contient_livraison CASCADE;
DROP TABLE IF EXISTS contient_achat     CASCADE;
DROP TABLE IF EXISTS livraison          CASCADE;
DROP TABLE IF EXISTS achat              CASCADE;
DROP TABLE IF EXISTS produit            CASCADE;
DROP TABLE IF EXISTS grossiste          CASCADE;
DROP TABLE IF EXISTS utilisateur        CASCADE;
DROP TABLE IF EXISTS categorie          CASCADE;

-- ---------------------------------------------------------------------
-- CATEGORIE
-- ---------------------------------------------------------------------
CREATE TABLE categorie (
    id_categorie  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL,
    CONSTRAINT uq_categorie_nom UNIQUE (nom)
);

-- ---------------------------------------------------------------------
-- UTILISATEUR
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
    id_utilisateur    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom               VARCHAR(100) NOT NULL,
    prenom            VARCHAR(100) NOT NULL,
    matricule         VARCHAR(30)  NOT NULL,
    sexe              CHAR(1)      NOT NULL,
    date_naissance    DATE         NOT NULL,
    identifiant       VARCHAR(50)  NOT NULL,
    mot_de_passe      VARCHAR(100) NOT NULL,          -- hash BCrypt (60 car.)
    role              VARCHAR(20)  NOT NULL DEFAULT 'CAISSIER',
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    doit_changer_mdp  BOOLEAN      NOT NULL DEFAULT TRUE,   -- vrai tant que l'utilisateur n'a pas défini son mot de passe
    CONSTRAINT uq_utilisateur_matricule   UNIQUE (matricule),
    CONSTRAINT uq_utilisateur_identifiant UNIQUE (identifiant),
    CONSTRAINT ck_utilisateur_sexe        CHECK (sexe IN ('M', 'F')),
    CONSTRAINT ck_utilisateur_role        CHECK (role IN ('ADMIN', 'CAISSIER')),
    CONSTRAINT ck_utilisateur_naissance   CHECK (date_naissance < CURRENT_DATE)
);

-- ---------------------------------------------------------------------
-- GROSSISTE
-- ---------------------------------------------------------------------
CREATE TABLE grossiste (
    id_grossiste  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom           VARCHAR(150) NOT NULL,
    telephone     VARCHAR(30),
    adresse       VARCHAR(255),
    email         VARCHAR(150)
);

-- ---------------------------------------------------------------------
-- PRODUIT
-- La date de péremption n'est plus portée par le produit : elle est portée
-- par chaque LOT (voir contient_livraison).
-- quantite_stock = total des unités en stock = somme des quantite_restante
-- de ses lots (périmés compris), tenu à jour dans chaque transaction.
-- ---------------------------------------------------------------------
CREATE TABLE produit (
    id_produit       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom              VARCHAR(150)  NOT NULL,
    prix_vente       NUMERIC(12,2) NOT NULL,
    quantite_stock   INTEGER       NOT NULL DEFAULT 0,
    seuil_alerte     INTEGER       NOT NULL DEFAULT 5,
    image            VARCHAR(255),                   -- nom du fichier image (facultatif)
    actif            BOOLEAN       NOT NULL DEFAULT TRUE,
    id_categorie     INTEGER       NOT NULL,
    CONSTRAINT ck_produit_prix   CHECK (prix_vente >= 0),
    CONSTRAINT ck_produit_stock  CHECK (quantite_stock >= 0),   -- stock jamais négatif
    CONSTRAINT ck_produit_seuil  CHECK (seuil_alerte >= 0),
    CONSTRAINT fk_produit_categorie FOREIGN KEY (id_categorie)
        REFERENCES categorie (id_categorie) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- ACHAT (vente à un client)  -  le total est calculé, non stocké
-- ---------------------------------------------------------------------
CREATE TABLE achat (
    id_achat        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date_achat      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nom_client      VARCHAR(100),                    -- facultatif
    id_utilisateur  INTEGER      NOT NULL,
    CONSTRAINT fk_achat_utilisateur FOREIGN KEY (id_utilisateur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- LIVRAISON (approvisionnement par un grossiste)  -  total calculé
-- ---------------------------------------------------------------------
CREATE TABLE livraison (
    id_livraison    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date_livraison  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_grossiste    INTEGER   NOT NULL,
    id_utilisateur  INTEGER   NOT NULL,
    CONSTRAINT fk_livraison_grossiste FOREIGN KEY (id_grossiste)
        REFERENCES grossiste (id_grossiste) ON DELETE RESTRICT,
    CONSTRAINT fk_livraison_utilisateur FOREIGN KEY (id_utilisateur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- CONTIENT_ACHAT (lignes d'achat)
-- ---------------------------------------------------------------------
CREATE TABLE contient_achat (
    id_achat       INTEGER       NOT NULL,
    id_produit     INTEGER       NOT NULL,
    quantite       INTEGER       NOT NULL,
    prix_unitaire  NUMERIC(12,2) NOT NULL,           -- prix figé au moment de la vente
    CONSTRAINT pk_contient_achat PRIMARY KEY (id_achat, id_produit),
    CONSTRAINT ck_contient_achat_quantite CHECK (quantite > 0),
    CONSTRAINT ck_contient_achat_prix     CHECK (prix_unitaire >= 0),
    CONSTRAINT fk_contient_achat_achat FOREIGN KEY (id_achat)
        REFERENCES achat (id_achat) ON DELETE CASCADE,
    CONSTRAINT fk_contient_achat_produit FOREIGN KEY (id_produit)
        REFERENCES produit (id_produit) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- CONTIENT_LIVRAISON (lignes de livraison) = LOTS
-- Une ligne de livraison est un lot : les unités d'un produit reçues dans une livraison.
--   date_peremption   : date du lot (NULL = ne périme pas)
--   quantite_restante : unités du lot encore en stock (périmées comprises)
--   quantite_retiree  : unités retirées du stock car périmées (traçabilité)
--   quantite = vendues + retirées + restantes
-- ---------------------------------------------------------------------
CREATE TABLE contient_livraison (
    id_livraison       INTEGER       NOT NULL,
    id_produit         INTEGER       NOT NULL,
    quantite           INTEGER       NOT NULL,
    prix_achat         NUMERIC(12,2) NOT NULL,       -- prix payé au grossiste
    date_peremption    DATE,
    quantite_restante  INTEGER       NOT NULL,
    quantite_retiree   INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT pk_contient_livraison PRIMARY KEY (id_livraison, id_produit),
    CONSTRAINT ck_contient_livraison_quantite CHECK (quantite > 0),
    CONSTRAINT ck_contient_livraison_prix     CHECK (prix_achat >= 0),
    CONSTRAINT ck_lot_restante  CHECK (quantite_restante >= 0),
    CONSTRAINT ck_lot_retiree   CHECK (quantite_retiree >= 0),
    CONSTRAINT ck_lot_coherence CHECK (quantite_restante + quantite_retiree <= quantite),
    CONSTRAINT fk_contient_livraison_livraison FOREIGN KEY (id_livraison)
        REFERENCES livraison (id_livraison) ON DELETE CASCADE,
    CONSTRAINT fk_contient_livraison_produit FOREIGN KEY (id_produit)
        REFERENCES produit (id_produit) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- INDEX (recherche, historique, tableau de bord, clés étrangères)
-- ---------------------------------------------------------------------
CREATE UNIQUE INDEX uq_produit_nom          ON produit (LOWER(nom));      -- unicité du nom (sans tenir compte de la casse) + recherche
CREATE INDEX idx_produit_categorie          ON produit (id_categorie);
CREATE INDEX idx_achat_date                 ON achat (date_achat);        -- filtres + CA du jour
CREATE INDEX idx_achat_utilisateur          ON achat (id_utilisateur);
CREATE INDEX idx_livraison_date             ON livraison (date_livraison);
CREATE INDEX idx_livraison_grossiste        ON livraison (id_grossiste);
CREATE INDEX idx_livraison_utilisateur      ON livraison (id_utilisateur);
CREATE INDEX idx_contient_achat_produit     ON contient_achat (id_produit);      -- produits les plus vendus
CREATE INDEX idx_contient_livraison_produit ON contient_livraison (id_produit);
-- Lots encore en stock et datés : alertes de péremption, vente dans l'ordre de péremption
CREATE INDEX idx_lot_peremption ON contient_livraison (date_peremption) WHERE quantite_restante > 0;
