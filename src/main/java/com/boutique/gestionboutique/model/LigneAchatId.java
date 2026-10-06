package com.boutique.gestionboutique.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LigneAchatId implements Serializable {

    @Column(name = "id_achat")
    private Integer idAchat;

    @Column(name = "id_produit")
    private Integer idProduit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LigneAchatId that)) return false;
        return Objects.equals(idAchat, that.idAchat) && Objects.equals(idProduit, that.idProduit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAchat, idProduit);
    }
}
