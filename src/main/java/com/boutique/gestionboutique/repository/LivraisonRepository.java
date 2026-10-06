package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LivraisonRepository extends JpaRepository<Livraison, Integer> {

    List<Livraison> findAllByOrderByDateLivraisonDesc();
}
