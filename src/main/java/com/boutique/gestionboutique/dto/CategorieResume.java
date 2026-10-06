package com.boutique.gestionboutique.dto;

/** Ligne de la liste des catégories : la catégorie et le nombre de produits qui lui sont rattachés. */
public record CategorieResume(Integer id, String nom, long nbProduits) {
}