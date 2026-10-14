-- =====================================================================
--  Logiciel de gestion de boutique - Vérification de l'intégrité du stock
--
--      psql -U boutique_user -d boutique_db -f 03_verification_stock.sql
--
--  Ce script ne modifie RIEN : il ne fait que lire.
--  Les contrôles 1 à 3 doivent renvoyer AUCUNE LIGNE. Toute ligne affichée est une anomalie.
-- =====================================================================

\echo '--- 1. Produits dont le stock enregistré <> somme des unités restantes de leurs lots'
SELECT p.id_produit,
       p.nom,
       p.quantite_stock                              AS stock_enregistre,
       COALESCE(SUM(cl.quantite_restante), 0)        AS somme_lots,
       p.quantite_stock - COALESCE(SUM(cl.quantite_restante), 0) AS ecart
FROM produit p
LEFT JOIN contient_livraison cl ON cl.id_produit = p.id_produit
GROUP BY p.id_produit, p.nom, p.quantite_stock
HAVING p.quantite_stock <> COALESCE(SUM(cl.quantite_restante), 0)
ORDER BY p.nom;

\echo '--- 2. Lots incohérents : restantes + retirées > quantité reçue (unités vendues négatives)'
SELECT id_livraison, id_produit, quantite, quantite_restante, quantite_retiree
FROM contient_livraison
WHERE quantite_restante + quantite_retiree > quantite;

\echo '--- 3. Produits dont les unités vendues d''après les lots <> total des lignes d''achat'
SELECT p.id_produit,
       p.nom,
       COALESCE(l.vendues_lots, 0)   AS vendues_selon_lots,
       COALESCE(a.vendues_achats, 0) AS vendues_selon_achats
FROM produit p
LEFT JOIN (SELECT id_produit, SUM(quantite - quantite_restante - quantite_retiree) AS vendues_lots
           FROM contient_livraison GROUP BY id_produit) l ON l.id_produit = p.id_produit
LEFT JOIN (SELECT id_produit, SUM(quantite) AS vendues_achats
           FROM contient_achat GROUP BY id_produit) a ON a.id_produit = p.id_produit
WHERE COALESCE(l.vendues_lots, 0) <> COALESCE(a.vendues_achats, 0)
ORDER BY p.nom;

\echo '--- Information : stock total et stock vendable (lots non périmés) par produit'
SELECT p.nom,
       p.quantite_stock                                        AS stock_total,
       COALESCE(SUM(cl.quantite_restante)
                FILTER (WHERE cl.date_peremption IS NULL
                           OR cl.date_peremption >= CURRENT_DATE), 0) AS stock_vendable,
       p.seuil_alerte,
       p.actif
FROM produit p
LEFT JOIN contient_livraison cl ON cl.id_produit = p.id_produit
GROUP BY p.id_produit, p.nom, p.quantite_stock, p.seuil_alerte, p.actif
ORDER BY p.nom;
