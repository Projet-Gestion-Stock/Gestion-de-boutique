package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.ProfilDTO;
import com.boutique.gestionboutique.dto.UtilisateurDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UtilisateurService {

    /** Mot de passe appliqué lors d'une réinitialisation par l'administrateur. */
    public static final String MOT_DE_PASSE_PAR_DEFAUT = "123456";

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Consultation ----------

    @Transactional(readOnly = true)
    public List<Utilisateur> lister() {
        return utilisateurRepository.findAllByOrderByNomAscPrenomAsc();
    }

    @Transactional(readOnly = true)
    public Utilisateur trouver(Integer id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Utilisateur trouverParIdentifiant(String identifiant) {
        return utilisateurRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + identifiant));
    }

    @Transactional(readOnly = true)
    public boolean identifiantDejaUtilise(String identifiant) {
        return utilisateurRepository.existsByIdentifiant(identifiant.trim());
    }

    @Transactional(readOnly = true)
    public boolean matriculeDejaUtilise(String matricule) {
        return utilisateurRepository.existsByMatricule(matricule.trim());
    }

    // ---------- Administration (rôle ADMIN) ----------

    /** Crée un compte avec un mot de passe PROVISOIRE : l'utilisateur devra le changer à sa première connexion. */
    @Transactional
    public Utilisateur creer(UtilisateurDTO form) {
        Utilisateur u = new Utilisateur();
        u.setNom(form.nom().trim());
        u.setPrenom(form.prenom().trim());
        u.setMatricule(form.matricule().trim());
        u.setSexe(form.sexe().charAt(0));
        u.setDateNaissance(form.dateNaissance());
        u.setIdentifiant(form.identifiant().trim());
        u.setRole(form.role());
        u.setMotDePasse(passwordEncoder.encode(form.motDePasse()));
        u.setDoitChangerMotDePasse(true);
        u.setActif(true);
        return utilisateurRepository.save(u);
    }

    /** Active ou désactive un compte (on ne supprime jamais, pour garder l'historique). */
    @Transactional
    public void changerStatut(Integer id, boolean actif, String identifiantConnecte) {
        Utilisateur u = trouver(id);
        if (!actif && u.getIdentifiant().equals(identifiantConnecte)) {
            throw new RegleGestionException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        u.setActif(actif);
    }

    /**
     * Remet le mot de passe de l'utilisateur à la valeur par défaut et l'oblige à en définir
     * un nouveau à sa prochaine connexion. Un administrateur ne peut pas réinitialiser
     * son propre mot de passe ici (il utilise « Mon profil », qui exige l'ancien).
     */
    @Transactional
    public void reinitialiserMotDePasse(Integer id, String identifiantConnecte) {
        Utilisateur u = trouver(id);
        if (u.getIdentifiant().equals(identifiantConnecte)) {
            throw new RegleGestionException(
                    "Pour changer votre propre mot de passe, utilisez « Mon profil ».");
        }
        u.setMotDePasse(passwordEncoder.encode(MOT_DE_PASSE_PAR_DEFAUT));
        u.setDoitChangerMotDePasse(true);
    }

    // ---------- Profil personnel (l'utilisateur connecté uniquement) ----------

    /** Met à jour les informations modifiables du profil. Matricule, identifiant et rôle ne changent jamais ici. */
    @Transactional
    public Utilisateur modifierProfil(String identifiant, ProfilDTO form) {
        Utilisateur u = trouverParIdentifiant(identifiant);
        u.setNom(form.nom().trim());
        u.setPrenom(form.prenom().trim());
        u.setSexe(form.sexe().charAt(0));
        u.setDateNaissance(form.dateNaissance());
        return utilisateurRepository.save(u);
    }

    /** Vrai si le mot de passe fourni est celui actuellement enregistré pour cet utilisateur. */
    @Transactional(readOnly = true)
    public boolean motDePasseCorrespond(String identifiant, String motDePasse) {
        return passwordEncoder.matches(motDePasse, trouverParIdentifiant(identifiant).getMotDePasse());
    }

    /** Définit le mot de passe choisi par l'utilisateur et lève l'obligation de le changer. */
    @Transactional
    public void definirMotDePasse(String identifiant, String nouveauMotDePasse) {
        Utilisateur u = trouverParIdentifiant(identifiant);
        u.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        u.setDoitChangerMotDePasse(false);
    }
}