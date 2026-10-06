package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Grossiste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrossisteRepository extends JpaRepository<Grossiste, Integer> {

    List<Grossiste> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);

    /** Pour la modification : on ignore le grossiste modifié. */
    boolean existsByNomIgnoreCaseAndIdNot(String nom, Integer id);
}
