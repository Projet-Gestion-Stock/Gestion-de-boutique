package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Integer id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "matricule", nullable = false, length = 30, unique = true)
    private String matricule;

    /** 'M' ou 'F' **/
    @Column(name = "sexe", nullable = false, length = 1)
    private Character sexe;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "identifiant", nullable = false, length = 50, unique = true)
    private String identifiant;

    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.CAISSIER;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    // Vrai tant que l'utilisateur n'a pas défini son propre mot de passe. */
    @Column(name = "doit_changer_mdp", nullable = false)
    private boolean doitChangerMotDePasse = true;

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
