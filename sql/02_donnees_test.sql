-- =====================================================================
--  Logiciel de gestion de boutique - Jeu de données de test (PostgreSQL)
--
--  À exécuter APRÈS 01_schema.sql, sur une base vide, application arrêtée :
--      psql -U boutique_user -d boutique_db -f 02_donnees_test.sql
--
--  Encodage : ce fichier est en UTF-8 (sans BOM) et la base est en UTF-8.
--  Le script force lui-même l'encodage du client (voir SET client_encoding plus bas), car
--  sous Windows psql lit par défaut en WIN1252 et abîme les accents (« concentrÃ© »).
--
--  Comptes créés (mot de passe stocké en BCrypt) :
--      admin      / admin123      ADMIN     (doit changer son mot de passe à la 1re connexion)
--      caissier1  / caissier123   CAISSIER  (peut se connecter directement)
--      caissier2  / caissier123   CAISSIER  (doit changer son mot de passe à la 1re connexion)
--      caissier3  / caissier123   CAISSIER  (compte DÉSACTIVÉ : connexion refusée)
--  Le compte 'admin' est le même que celui créé automatiquement par l'application
--  (matricule ADM-001) : l'application le détecte et ne le recrée pas.
--
--  Les dates sont RELATIVES à la date du jour (CURRENT_DATE) : le jeu reste cohérent
--  quel que soit le jour d'exécution.
--
--  Situations couvertes par les données :
--      - lots avec péremption, dont un lot qui périme dans quelques jours (alerte)
--      - 2 lots périmés non retirés + 1 lot partiellement retiré (traçabilité)
--      - produits à stock faible (<= seuil d'alerte) et un produit en rupture (stock 0)
--      - produit sans péremption (sel), produit désactivé
--      - 2 lots d'un même produit (eau, yaourt) : vente dans l'ordre de péremption
--      - prix de vente modifié depuis certaines ventes (prix figé dans les lignes d'achat)
--      - achats répartis sur ~55 jours, avec et sans nom de client, dont un achat aujourd'hui
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENCODAGE : le client doit envoyer et lire de l'UTF-8, comme la base.
-- ---------------------------------------------------------------------
SET client_encoding = 'UTF8';

-- ---------------------------------------------------------------------
-- UTILISATEURS
-- ---------------------------------------------------------------------
INSERT INTO utilisateur (nom, prenom, matricule, sexe, date_naissance, identifiant, mot_de_passe, role, actif, doit_changer_mdp) VALUES
('ADMIN',     'Super',   'ADM-001', 'M', DATE '2000-01-01', 'admin',     '$2a$10$Fbl9G1ehXLo6yIqTSeidWOfdbDmI8vkCFzsUBlOl/bGLt9XGdrgXG', 'ADMIN',    TRUE,  TRUE),
('Ouédraogo', 'Aminata', 'CAI-001', 'F', DATE '1998-05-14', 'caissier1', '$2a$10$0jFS8BSXvdPotipIupLKQ.nxFT6becOj/u4EPdNofi.q3EDbeHoBK',   'CAISSIER', TRUE,  FALSE),
('Sawadogo',  'Ibrahim', 'CAI-002', 'M', DATE '1995-11-02', 'caissier2', '$2a$10$0jFS8BSXvdPotipIupLKQ.nxFT6becOj/u4EPdNofi.q3EDbeHoBK',   'CAISSIER', TRUE,  TRUE),
('Compaoré',  'Salif',   'CAI-003', 'M', DATE '1990-03-21', 'caissier3', '$2a$10$0jFS8BSXvdPotipIupLKQ.nxFT6becOj/u4EPdNofi.q3EDbeHoBK',   'CAISSIER', FALSE, FALSE);

-- ---------------------------------------------------------------------
-- CATEGORIES
-- ---------------------------------------------------------------------
INSERT INTO categorie (nom) VALUES
('Boissons'),
('Produits laitiers'),
('Céréales et féculents'),
('Conserves'),
('Condiments et épices'),
('Biscuits et confiseries');

-- ---------------------------------------------------------------------
-- GROSSISTES
-- ---------------------------------------------------------------------
INSERT INTO grossiste (nom, telephone, adresse, email) VALUES
('Sahel Distribution',     '+226 25 00 00 01', 'Zone industrielle, Ouagadougou', 'contact@sahel-distribution.example'),
('Laiterie du Faso',       '+226 25 00 00 02', 'Secteur 15, Ouagadougou',        'commandes@laiterie-faso.example'),
('Comptoir des Conserves', '+226 25 00 00 03', 'Avenue Kwame Nkrumah, Ouagadougou', NULL);

-- ---------------------------------------------------------------------
-- PRODUITS
-- quantite_stock est mis à 0 ici : il est recalculé plus bas à partir des lots
-- (stock du produit = somme des quantite_restante de ses lots).
-- ---------------------------------------------------------------------
INSERT INTO produit (nom, prix_vente, quantite_stock, seuil_alerte, actif, id_categorie)
SELECT v.nom, v.prix, 0, v.seuil, v.actif, c.id_categorie
FROM (VALUES
    ('Eau minérale 1,5 L',       500.00, 20, TRUE,  'Boissons'),
    ('Jus de mangue 1 L',       1200.00, 10, TRUE,  'Boissons'),
    ('Lait en poudre 400 g',    2500.00,  8, TRUE,  'Produits laitiers'),
    ('Yaourt nature 125 g',      300.00, 15, TRUE,  'Produits laitiers'),
    ('Lait concentré sucré 400 g', 600.00, 10, FALSE, 'Produits laitiers'),
    ('Riz parfumé 5 kg',        4500.00, 10, TRUE,  'Céréales et féculents'),
    ('Spaghetti 500 g',          600.00, 15, TRUE,  'Céréales et féculents'),
    ('Farine de blé 1 kg',       750.00, 10, TRUE,  'Céréales et féculents'),
    ('Tomate concentrée 400 g',  800.00, 10, TRUE,  'Conserves'),
    ('Sardines à l''huile 125 g', 650.00, 12, TRUE, 'Conserves'),
    ('Sel fin 1 kg',             250.00, 10, TRUE,  'Condiments et épices'),
    ('Bouillon cube 100 g',      450.00, 10, TRUE,  'Condiments et épices'),
    ('Huile végétale 1 L',      1500.00, 10, TRUE,  'Condiments et épices'),
    ('Biscuits au beurre 200 g', 700.00, 10, TRUE,  'Biscuits et confiseries'),
    ('Chocolat en tablette 100 g', 900.00, 10, TRUE, 'Biscuits et confiseries')
) AS v(nom, prix, seuil, actif, categorie)
JOIN categorie c ON c.nom = v.categorie;

-- ---------------------------------------------------------------------
-- LIVRAISONS (5 approvisionnements, enregistrés par l'admin)
-- ---------------------------------------------------------------------
INSERT INTO livraison (date_livraison, id_grossiste, id_utilisateur)
SELECT CURRENT_DATE - v.jours + TIME '08:30', g.id_grossiste, u.id_utilisateur
FROM (VALUES
    (60, 'Sahel Distribution'),
    (45, 'Laiterie du Faso'),
    (20, 'Laiterie du Faso'),
    (15, 'Comptoir des Conserves'),
    ( 5, 'Sahel Distribution')
) AS v(jours, grossiste)
JOIN grossiste   g ON g.nom = v.grossiste
JOIN utilisateur u ON u.identifiant = 'admin'
ORDER BY v.jours DESC;   -- numéros attribués dans l'ordre chronologique

-- ---------------------------------------------------------------------
-- LOTS (lignes de livraison)
-- Colonnes : livraison (jours avant aujourd'hui), produit, quantité reçue, prix d'achat,
--            date de péremption, quantité restante, quantité retirée.
-- Pour chaque lot : quantité reçue = vendues + retirées + restantes.
-- ---------------------------------------------------------------------
INSERT INTO contient_livraison (id_livraison, id_produit, quantite, prix_achat, date_peremption, quantite_restante, quantite_retiree)
SELECT l.id_livraison, p.id_produit, v.quantite, v.prix_achat, v.date_peremption, v.restante, v.retiree
FROM (VALUES
    -- Livraison d'il y a 60 jours (Sahel Distribution)
    (60, 'Eau minérale 1,5 L',        200, 350.00, CURRENT_DATE + 400, 50, 0),
    (60, 'Riz parfumé 5 kg',           60, 3500.00, CURRENT_DATE + 500, 38, 0),
    (60, 'Spaghetti 500 g',           100, 400.00, CURRENT_DATE + 300, 65, 0),
    (60, 'Farine de blé 1 kg',         80, 500.00, CURRENT_DATE + 200, 62, 0),
    (60, 'Sel fin 1 kg',              100, 150.00, NULL::date,         60, 0),
    (60, 'Biscuits au beurre 200 g',   50, 450.00, CURRENT_DATE - 7,   12, 0),   -- lot PÉRIMÉ, non retiré
    -- Livraison d'il y a 45 jours (Laiterie du Faso)
    (45, 'Lait en poudre 400 g',       40, 1900.00, CURRENT_DATE + 180,  7, 0),  -- stock faible
    (45, 'Yaourt nature 125 g',       100, 180.00, CURRENT_DATE + 6,    20, 0),  -- périme dans 6 jours
    (45, 'Chocolat en tablette 100 g', 40, 600.00, CURRENT_DATE - 3,     5, 10), -- PÉRIMÉ, 10 unités déjà retirées
    -- Livraison d'il y a 20 jours (Laiterie du Faso)
    (20, 'Yaourt nature 125 g',        80, 190.00, CURRENT_DATE + 25,   80, 0),  -- 2e lot de yaourt
    -- Livraison d'il y a 15 jours (Comptoir des Conserves)
    (15, 'Tomate concentrée 400 g',    60, 550.00, CURRENT_DATE + 400,  40, 0),
    (15, 'Sardines à l''huile 125 g',  48, 450.00, CURRENT_DATE + 300,   8, 0),  -- stock faible
    (15, 'Bouillon cube 100 g',        60, 300.00, CURRENT_DATE + 350,  45, 0),
    -- Livraison d'il y a 5 jours (Sahel Distribution)
    ( 5, 'Jus de mangue 1 L',          72, 800.00, CURRENT_DATE + 120,  48, 0),
    ( 5, 'Eau minérale 1,5 L',        100, 360.00, CURRENT_DATE + 420, 100, 0)   -- 2e lot d'eau
) AS v(jours, produit, quantite, prix_achat, date_peremption, restante, retiree)
JOIN livraison l ON l.date_livraison::date = CURRENT_DATE - v.jours
JOIN produit   p ON p.nom = v.produit;

-- Stock des produits = somme des unités restantes de leurs lots (jamais saisi à la main).
UPDATE produit p
SET quantite_stock = COALESCE((SELECT SUM(cl.quantite_restante)
                               FROM contient_livraison cl
                               WHERE cl.id_produit = p.id_produit), 0);

-- ---------------------------------------------------------------------
-- ACHATS (12 tickets)
-- ---------------------------------------------------------------------
INSERT INTO achat (date_achat, nom_client, id_utilisateur)
SELECT CURRENT_DATE - v.jours + v.heure, v.client, u.id_utilisateur
FROM (VALUES
    (55, TIME '10:15', NULL::varchar,   'caissier1'),
    (50, TIME '16:40', 'Mme Traoré',    'caissier2'),
    (44, TIME '09:05', NULL,            'caissier1'),
    (38, TIME '11:30', 'M. Ouédraogo',  'caissier2'),
    (30, TIME '17:20', NULL,            'caissier1'),
    (22, TIME '12:00', 'Mme Sawadogo',  'caissier2'),
    (14, TIME '15:45', NULL,            'caissier1'),
    (12, TIME '10:10', 'M. Kaboré',     'caissier2'),
    ( 8, TIME '14:25', NULL,            'admin'),
    ( 3, TIME '09:50', NULL,            'caissier2'),
    ( 1, TIME '16:05', 'Mme Compaoré',  'caissier1'),
    ( 0, TIME '08:45', NULL,            'caissier1')
) AS v(jours, heure, client, caissier)
JOIN utilisateur u ON u.identifiant = v.caissier
ORDER BY v.jours DESC;   -- numéros de ticket attribués dans l'ordre chronologique

-- ---------------------------------------------------------------------
-- LIGNES D'ACHAT (prix figé au moment de la vente)
-- Riz et lait en poudre : vendus à un ancien prix lors des premiers achats.
-- ---------------------------------------------------------------------
INSERT INTO contient_achat (id_achat, id_produit, quantite, prix_unitaire)
SELECT a.id_achat, p.id_produit, v.quantite, v.prix_unitaire
FROM (VALUES
    (55, 'Eau minérale 1,5 L',        30,  500.00),
    (55, 'Riz parfumé 5 kg',           5, 4300.00),
    (55, 'Sel fin 1 kg',              10,  250.00),
    (55, 'Spaghetti 500 g',           10,  600.00),
    (55, 'Biscuits au beurre 200 g',   8,  700.00),

    (50, 'Eau minérale 1,5 L',        24,  500.00),
    (50, 'Farine de blé 1 kg',         6,  750.00),
    (50, 'Spaghetti 500 g',           10,  600.00),
    (50, 'Biscuits au beurre 200 g',  10,  700.00),
    (50, 'Sel fin 1 kg',               8,  250.00),

    (44, 'Lait en poudre 400 g',      12, 2400.00),
    (44, 'Yaourt nature 125 g',       20,  300.00),
    (44, 'Eau minérale 1,5 L',        20,  500.00),
    (44, 'Riz parfumé 5 kg',           6, 4300.00),
    (44, 'Biscuits au beurre 200 g',  10,  700.00),

    (38, 'Chocolat en tablette 100 g',10,  900.00),
    (38, 'Yaourt nature 125 g',       25,  300.00),
    (38, 'Lait en poudre 400 g',      10, 2400.00),
    (38, 'Farine de blé 1 kg',         6,  750.00),
    (38, 'Sel fin 1 kg',              10,  250.00),

    (30, 'Eau minérale 1,5 L',        36,  500.00),
    (30, 'Spaghetti 500 g',           10,  600.00),
    (30, 'Riz parfumé 5 kg',           5, 4500.00),
    (30, 'Biscuits au beurre 200 g',  10,  700.00),
    (30, 'Chocolat en tablette 100 g', 8,  900.00),

    (22, 'Yaourt nature 125 g',       20,  300.00),
    (22, 'Lait en poudre 400 g',      11, 2500.00),
    (22, 'Farine de blé 1 kg',         6,  750.00),
    (22, 'Sel fin 1 kg',              12,  250.00),
    (22, 'Riz parfumé 5 kg',           6, 4500.00),
    (22, 'Chocolat en tablette 100 g', 7,  900.00),

    (14, 'Eau minérale 1,5 L',        40,  500.00),
    (14, 'Spaghetti 500 g',            5,  600.00),
    (14, 'Yaourt nature 125 g',       15,  300.00),

    (12, 'Tomate concentrée 400 g',    8,  800.00),
    (12, 'Sardines à l''huile 125 g', 15,  650.00),
    (12, 'Bouillon cube 100 g',        5,  450.00),

    ( 8, 'Sardines à l''huile 125 g', 15,  650.00),
    ( 8, 'Bouillon cube 100 g',        5,  450.00),

    ( 3, 'Tomate concentrée 400 g',   12,  800.00),
    ( 3, 'Jus de mangue 1 L',         10, 1200.00),

    ( 1, 'Sardines à l''huile 125 g', 10,  650.00),
    ( 1, 'Jus de mangue 1 L',          8, 1200.00),

    ( 0, 'Bouillon cube 100 g',        5,  450.00),
    ( 0, 'Jus de mangue 1 L',          6, 1200.00)
) AS v(jours, produit, quantite, prix_unitaire)
JOIN achat   a ON a.date_achat::date = CURRENT_DATE - v.jours
JOIN produit p ON p.nom = v.produit;
