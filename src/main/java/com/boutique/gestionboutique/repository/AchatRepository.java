package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.Achat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchatRepository extends JpaRepository<Achat, Integer> {

    List<Achat> findAllByOrderByDateAchatDesc();
}