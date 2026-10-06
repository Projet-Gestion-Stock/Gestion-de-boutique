package com.boutique.gestionboutique.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grossiste")
@Getter
@Setter
@NoArgsConstructor
public class Grossiste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grossiste")
    private Integer id;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "email", length = 150)
    private String email;
}

