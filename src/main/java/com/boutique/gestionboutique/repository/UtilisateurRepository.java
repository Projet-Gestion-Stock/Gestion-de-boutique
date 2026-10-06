package com.boutique.gestionboutique.repository;


import com.boutique.gestionboutique.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByIdentifiant(String identifiant);

    boolean existsByIdentifiant(String identifiant);

    boolean existsByMatricule(String matricule);

    boolean existsByIdentifiantAndIdNot(String identifiant, Integer id);

    boolean existsByMatriculeAndIdNot(String matricule, Integer id);

    List<Utilisateur> findAllByOrderByNomAscPrenomAsc();
}