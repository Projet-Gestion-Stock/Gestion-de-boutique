package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.dto.AchatDTO;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.model.Achat;
import com.boutique.gestionboutique.service.AchatService;
import com.boutique.gestionboutique.service.ExportPdfService;
import com.boutique.gestionboutique.service.ProduitService;
import com.boutique.gestionboutique.util.ReponsePdf;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.boutique.gestionboutique.util.Filtres;
import org.springframework.data.domain.Page;
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
import java.util.List;

@Controller
@RequestMapping("/achats")
public class AchatController {

    private final AchatService achatService;
    private final ProduitService produitService;
    private final String boutiqueNom;
    private final String boutiqueAdresse;
    private final String boutiqueTelephone;
    private final String boutiqueDevise;
    private final ExportPdfService exportPdfService;

    public AchatController(AchatService achatService,
                           ProduitService produitService,
                           @Value("${app.boutique.nom:Ma boutique}") String boutiqueNom,
                           @Value("${app.boutique.adresse:}") String boutiqueAdresse,
                           @Value("${app.boutique.telephone:}") String boutiqueTelephone,
                           @Value("${app.boutique.devise:}") String boutiqueDevise, ExportPdfService exportPdfService) {
        this.achatService = achatService;
        this.produitService = produitService;
        this.boutiqueNom = boutiqueNom;
        this.boutiqueAdresse = boutiqueAdresse;
        this.boutiqueTelephone = boutiqueTelephone;
        this.boutiqueDevise = boutiqueDevise;
        this.exportPdfService = exportPdfService;
    }

    private static final int TAILLE_PAGE = 15;

    // ---------- Historique ----------

    @GetMapping
    public String liste(@RequestParam(name = "du", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                        @RequestParam(name = "au", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                        @RequestParam(name = "caissierId", required = false) Integer caissierId,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        Model model) {

        Page<Achat> resultat = Page.empty();
        BigDecimal total = BigDecimal.ZERO;
        if (du != null && au != null && du.isAfter(au)) {
            // Règle FI3
            model.addAttribute("erreurFiltre",
                    "La date de fin doit être postérieure ou égale à la date de début.");
        } else {
            resultat = achatService.rechercher(du, au, caissierId, page, TAILLE_PAGE);
            total = achatService.totalFiltre(du, au, caissierId);
        }

        LocalDate aujourdhui = LocalDate.now();
        model.addAttribute("achats", resultat.getContent());
        model.addAttribute("pagination", resultat);
        model.addAttribute("filtresUrl", Filtres.enUrl(du, au, "caissierId", caissierId));
        model.addAttribute("totalAffiche", total);
        model.addAttribute("caissiers", achatService.listerUtilisateurs());

        // Valeurs du filtre, pour réafficher le formulaire tel qu'il a été rempli
        model.addAttribute("du", du);
        model.addAttribute("au", au);
        model.addAttribute("caissierId", caissierId);
        model.addAttribute("filtreActif", du != null || au != null || caissierId != null);

        // Raccourcis (règle FI6)
        model.addAttribute("aujourdhui", aujourdhui);
        model.addAttribute("il7Jours", aujourdhui.minusDays(6));
        model.addAttribute("debutMois", aujourdhui.withDayOfMonth(1));
        return "achats/liste";
    }

    // ---------- Export PDF de l'historique (mêmes filtres que la liste, sans pagination) ----------

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exporterPdf(@RequestParam(name = "du", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                                              @RequestParam(name = "au", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                                              @RequestParam(name = "caissierId", required = false) Integer caissierId,
                                              Principal principal) {
        if (du != null && au != null && du.isAfter(au)) {
            throw new RegleGestionException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        List<Achat> achats = achatService.listerPourExport(du, au, caissierId);
        byte[] pdf = exportPdfService.historiqueAchats(
                achats, du, au, achatService.libelleCaissier(caissierId), principal.getName());
        return ReponsePdf.telechargement(pdf, "historique-achats");
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
        model.addAttribute("devise", boutiqueDevise);
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