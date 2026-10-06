package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.dto.GrossisteDTO;
import com.boutique.gestionboutique.exception.RessourceIntrouvableException;
import com.boutique.gestionboutique.model.Grossiste;
import com.boutique.gestionboutique.repository.GrossisteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrossisteService {

    private final GrossisteRepository grossisteRepository;

    public GrossisteService(GrossisteRepository grossisteRepository) {
        this.grossisteRepository = grossisteRepository;
    }

    @Transactional(readOnly = true)
    public List<Grossiste> lister() {
        return grossisteRepository.findAllByOrderByNomAsc();
    }

    @Transactional(readOnly = true)
    public Grossiste trouver(Integer id) {
        return grossisteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Grossiste introuvable : " + id));
    }

    /** idExclu = id du grossiste modifié (null à la création). */
    @Transactional(readOnly = true)
    public boolean nomDejaUtilise(String nom, Integer idExclu) {
        String valeur = nom.trim();
        return idExclu == null
                ? grossisteRepository.existsByNomIgnoreCase(valeur)
                : grossisteRepository.existsByNomIgnoreCaseAndIdNot(valeur, idExclu);
    }

    @Transactional
    public Grossiste creer(GrossisteDTO form) {
        Grossiste g = new Grossiste();
        remplir(g, form);
        return grossisteRepository.save(g);
    }

    @Transactional
    public Grossiste modifier(Integer id, GrossisteDTO form) {
        Grossiste g = trouver(id);
        remplir(g, form);
        return grossisteRepository.save(g);
    }

    private void remplir(Grossiste g, GrossisteDTO form) {
        g.setNom(form.nom().trim());
        g.setTelephone(videEnNull(form.telephone()));
        g.setAdresse(videEnNull(form.adresse()));
        g.setEmail(videEnNull(form.email()));
    }

    /** Un champ facultatif laissé vide est enregistré NULL (et non une chaîne vide). */
    private String videEnNull(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }
}