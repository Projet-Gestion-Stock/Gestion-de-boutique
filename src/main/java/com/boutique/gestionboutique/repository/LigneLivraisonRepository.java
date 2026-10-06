package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.LigneLivraison;
import com.boutique.gestionboutique.model.LigneLivraisonId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneLivraisonRepository extends JpaRepository<LigneLivraison, LigneLivraisonId> {
}
