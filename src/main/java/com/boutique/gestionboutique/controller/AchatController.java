package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.AchatDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.model.Achat;
import com.boutique.gestionboutique.service.AchatService;
import com.boutique.gestionboutique.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/achats")
public class AchatController {

    private final AchatService achatService;
    private final ProduitService produitService;
    private final String boutiqueNom;
    private final String boutiqueAdresse;
    private final String boutiqueTelephone;
    private final String boutiqueDevise;

    public AchatController(AchatService achatService,
                           ProduitService produitService,
                           @Value("${app.boutique.nom:Ma boutique}") String boutiqueNom,
                           @Value("${app.boutique.adresse:}") String boutiqueAdresse,
                           @Value("${app.boutique.telephone:}") String boutiqueTelephone,
                           @Value("${app.boutique.devise:}") String boutiqueDevise) {
        this.achatService = achatService;
        this.produitService = produitService;
        this.boutiqueNom = boutiqueNom;
        this.boutiqueAdresse = boutiqueAdresse;
        this.boutiqueTelephone = boutiqueTelephone;
        this.boutiqueDevise = boutiqueDevise;
    }

    // ---------- Historique ----------

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("achats", achatService.lister());
        return "achats/liste";
    }

    // ---------- Détail ----------

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("achat", achatService.trouverDetaille(id));
        return "achats/detail";
    }

    // ---------- Ticket de caisse ----------

    /** Ticket d'un achat enregistré : généré à la demande, donc identique à chaque (ré)impression. */
    @GetMapping("/{id}/ticket")
    public String ticket(@PathVariable Integer id, Model model) {
        model.addAttribute("achat", achatService.trouverDetaille(id));
        model.addAttribute("boutiqueNom", boutiqueNom);
        model.addAttribute("boutiqueAdresse", boutiqueAdresse);
        model.addAttribute("boutiqueTelephone", boutiqueTelephone);
        model.addAttribute("boutiqueDevise", boutiqueDevise);
        return "achats/ticket";
    }

    // ---------- Saisie ----------

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("achatForm", AchatDTO.vide());
        preparerFormulaire(model);
        return "achats/formulaire";
    }

    @PostMapping
    public String enregistrer(@Valid @ModelAttribute("achatForm") AchatDTO form,
                              BindingResult result,
                              Model model,
                              Principal principal,
                              RedirectAttributes redirect) {
        verifierLignes(form, result);
        if (result.hasErrors()) {
            preparerFormulaire(model);
            return "achats/formulaire";
        }
        try {
            Achat achat = achatService.enregistrer(form, principal.getName());
            redirect.addFlashAttribute("succes",
                    "Achat n° " + achat.getId() + " enregistré : le stock a été mis à jour.");
            return "redirect:/achats/" + achat.getId();
        } catch (RegleGestionException e) {
            // Le service renvoie un problème par ligne (stock insuffisant, produit indisponible...)
            for (String message : e.getMessage().split("\n")) {
                result.reject("regle.gestion", message);
            }
            preparerFormulaire(model);
            return "achats/formulaire";
        }
    }

    // ---------- Outils ----------

    /** Règle AC4 : seuls les produits actifs sont proposés (ceux en rupture sont affichés mais non sélectionnables). */
    private void preparerFormulaire(Model model) {
        model.addAttribute("produits", produitService.rechercher(null, false));
    }

    private void verifierLignes(AchatDTO form, BindingResult result) {
        if (form.lignes() == null) {
            return; // déjà signalé par @NotEmpty
        }
        if (form.lignes().stream().anyMatch(ligne -> ligne == null)) {
            result.reject("ligne.invalide", "Une ligne de l'achat est invalide.");
        }
    }
}