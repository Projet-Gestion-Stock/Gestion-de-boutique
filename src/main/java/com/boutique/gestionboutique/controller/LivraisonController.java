package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.LigneLivraisonDTO;
import com.boutique.gestionboutique.dto.LivraisonDTO;
import com.boutique.gestionboutique.dto.ProduitDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.model.Livraison;
import com.boutique.gestionboutique.service.CategorieService;
import com.boutique.gestionboutique.service.GrossisteService;
import com.boutique.gestionboutique.service.LivraisonService;
import com.boutique.gestionboutique.service.ProduitService;
import com.boutique.gestionboutique.util.Filtres;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/livraisons")
public class LivraisonController {

    private static final int TAILLE_PAGE = 15;

    private final LivraisonService livraisonService;
    private final GrossisteService grossisteService;
    private final ProduitService produitService;
    private final CategorieService categorieService;
    private final Validator validator;

    public LivraisonController(LivraisonService livraisonService,
                               GrossisteService grossisteService,
                               ProduitService produitService,
                               CategorieService categorieService,
                               Validator validator) {
        this.livraisonService = livraisonService;
        this.grossisteService = grossisteService;
        this.produitService = produitService;
        this.categorieService = categorieService;
        this.validator = validator;
    }

    // ---------- Historique ----------

    @GetMapping
    public String liste(@RequestParam(name = "du", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                        @RequestParam(name = "au", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                        @RequestParam(name = "grossisteId", required = false) Integer grossisteId,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        Model model) {

        Page<Livraison> resultat = Page.empty();
        BigDecimal total = BigDecimal.ZERO;
        if (du != null && au != null && du.isAfter(au)) {
            // Règle FI3
            model.addAttribute("erreurFiltre",
                    "La date de fin doit être postérieure ou égale à la date de début.");
        } else {
            resultat = livraisonService.rechercher(du, au, grossisteId, page, TAILLE_PAGE);
            total = livraisonService.totalFiltre(du, au, grossisteId);
        }

        LocalDate aujourdhui = LocalDate.now();
        model.addAttribute("livraisons", resultat.getContent());
        model.addAttribute("pagination", resultat);
        model.addAttribute("filtresUrl", Filtres.enUrl(du, au, "grossisteId", grossisteId));
        model.addAttribute("totalAffiche", total);
        model.addAttribute("grossistes", grossisteService.lister());

        // Valeurs du filtre, pour réafficher le formulaire tel qu'il a été rempli
        model.addAttribute("du", du);
        model.addAttribute("au", au);
        model.addAttribute("grossisteId", grossisteId);
        model.addAttribute("filtreActif", du != null || au != null || grossisteId != null);

        // Raccourcis (règle FI6)
        model.addAttribute("aujourdhui", aujourdhui);
        model.addAttribute("il7Jours", aujourdhui.minusDays(6));
        model.addAttribute("debutMois", aujourdhui.withDayOfMonth(1));
        return "livraisons/liste";
    }

    // ---------- Détail ----------

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("livraison", livraisonService.trouverDetaillee(id));
        return "livraisons/detail";
    }

    // ---------- Saisie ----------

    @GetMapping("/nouvelle")
    public String nouvelle(Model model) {
        model.addAttribute("livraisonForm", LivraisonDTO.vide());
        preparerFormulaire(model);
        return "livraisons/formulaire";
    }

    @PostMapping
    public String enregistrer(@Valid @ModelAttribute("livraisonForm") LivraisonDTO form,
                              BindingResult result,
                              Model model,
                              Principal principal,
                              RedirectAttributes redirect) {
        verifierLignes(form, result);
        if (result.hasErrors()) {
            preparerFormulaire(model);
            return "livraisons/formulaire";
        }
        try {
            Livraison livraison = livraisonService.enregistrer(form, principal.getName());
            redirect.addFlashAttribute("succes",
                    "Livraison n° " + livraison.getId() + " enregistrée : le stock a été mis à jour.");
            return "redirect:/livraisons/" + livraison.getId();
        } catch (RegleGestionException e) {
            result.reject("regle.gestion", e.getMessage());
            preparerFormulaire(model);
            return "livraisons/formulaire";
        }
    }

    // ---------- Outils ----------

    private void preparerFormulaire(Model model) {
        model.addAttribute("grossistes", grossisteService.lister());
        // Règle L4 : seuls les produits actifs sont proposés
        model.addAttribute("produits", produitService.rechercher(null, false));
        // Solution B : catégories pour les nouveaux produits
        model.addAttribute("categories", categorieService.listerToutes());
    }

    /**
     * Contrôles dépendant du mode de chaque ligne :
     * - produit existant : un produit choisi, une seule fois par livraison (règle L3) ;
     * - nouveau produit : champs valides (règles P1 à P4) et nom unique, en base ET dans la livraison.
     * Chaque erreur est placée sur le champ de la ligne concernée.
     */
    private void verifierLignes(LivraisonDTO form, BindingResult result) {
        if (form.lignes() == null) {
            return; // déjà signalé par @NotEmpty
        }
        Map<Integer, Integer> produitsVus = new HashMap<>();
        Map<String, Integer> nomsVus = new HashMap<>();

        for (int i = 0; i < form.lignes().size(); i++) {
            LigneLivraisonDTO ligne = form.lignes().get(i);
            if (ligne == null) {
                result.reject("ligne.invalide", "Une ligne de la livraison est invalide.");
                continue;
            }
            String chemin = "lignes[" + i + "]";

            if (!ligne.nouveau()) {
                if (ligne.produitId() == null) {
                    result.rejectValue(chemin + ".produitId", "obligatoire", "Choisissez un produit.");
                    continue;
                }
                Integer dejaVu = produitsVus.putIfAbsent(ligne.produitId(), i);
                if (dejaVu != null) {
                    result.rejectValue(chemin + ".produitId", "doublon",
                            "Ce produit figure déjà à la ligne " + (dejaVu + 1)
                                    + " : regroupez les quantités sur une seule ligne.");
                }
                continue;
            }

            // Mode « nouveau produit »
            String base = chemin + ".nouveauProduit.";
            ProduitDTO np = ligne.nouveauProduit();
            for (ConstraintViolation<ProduitDTO> v : validator.validate(np)) {
                result.rejectValue(base + v.getPropertyPath(), "invalide", v.getMessage());
            }
            if (np.nom() != null && !np.nom().isBlank() && !result.hasFieldErrors(base + "nom")) {
                String nom = np.nom().trim();
                Integer dejaVu = nomsVus.putIfAbsent(nom.toLowerCase(Locale.ROOT), i);
                if (dejaVu != null) {
                    result.rejectValue(base + "nom", "doublon",
                            "Ce nom est déjà utilisé à la ligne " + (dejaVu + 1) + " de cette livraison.");
                } else if (produitService.nomDejaUtilise(nom, null)) {
                    result.rejectValue(base + "nom", "unique", "Un produit porte déjà ce nom.");
                }
            }
        }
    }
}