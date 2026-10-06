package com.boutique.gestionboutique.config;

import com.boutique.gestionboutique.Enum.Role;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Crée l'administrateur par défaut au démarrage.
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final String identifiant;
    private final String motDePasse;
    private final String nom;
    private final String prenom;
    private final String matricule;

    public AdminInitializer(UtilisateurRepository utilisateurRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.identifiant}") String identifiant,
                            @Value("${app.admin.mot-de-passe}") String motDePasse,
                            @Value("${app.admin.nom}") String nom,
                            @Value("${app.admin.prenom}") String prenom,
                            @Value("${app.admin.matricule}") String matricule) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
        this.nom = nom;
        this.prenom = prenom;
        this.matricule = matricule;
    }

    @Override
    public void run(String... args) {
        if (utilisateurRepository.existsByIdentifiant(identifiant)) {
            log.info("L'administrateur par défaut '{}' existe déjà.", identifiant);
            return;
        }
        if (utilisateurRepository.existsByMatricule(matricule)) {
            log.warn("Le matricule {} est déjà utilisé : administrateur par défaut non créé.", matricule);
            return;
        }

        Utilisateur admin = new Utilisateur();
        admin.setNom(nom);
        admin.setPrenom(prenom);
        admin.setMatricule(matricule);
        admin.setSexe('M');
        admin.setDateNaissance(LocalDate.of(2000, 1, 1));
        admin.setIdentifiant(identifiant);
        admin.setMotDePasse(passwordEncoder.encode(motDePasse));
        admin.setRole(Role.ADMIN);
        admin.setActif(true);
        utilisateurRepository.save(admin);

        log.info("Administrateur par défaut '{}' créé avec succès.", identifiant);
    }
}