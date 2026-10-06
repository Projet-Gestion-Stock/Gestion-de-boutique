package com.boutique.gestionboutique.repository;

import com.boutique.gestionboutique.model.LigneAchat;
import com.boutique.gestionboutique.model.LigneAchatId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneAchatRepository extends JpaRepository<LigneAchat, LigneAchatId> {
}
