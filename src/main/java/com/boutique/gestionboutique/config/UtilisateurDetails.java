package com.boutique.gestionboutique.config;

import com.boutique.gestionboutique.model.Utilisateur;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/** Utilisateur connecté a l'obligation de changer son mot de passe. */
public class UtilisateurDetails extends User {

    private final boolean doitChangerMotDePasse;

    public UtilisateurDetails(Utilisateur u) {
        super(u.getIdentifiant(),
                u.getMotDePasse(),
                u.isActif(),          // compte désactivé : connexion refusée
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        this.doitChangerMotDePasse = u.isDoitChangerMotDePasse();
    }

    public boolean isDoitChangerMotDePasse() {
        return doitChangerMotDePasse;
    }
}
