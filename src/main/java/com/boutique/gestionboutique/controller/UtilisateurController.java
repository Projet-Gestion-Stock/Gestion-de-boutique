package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.model.Role;
import com.boutique.gestionboutique.form.UtilisateurForm;
import com.boutique.gestionboutique.exception.RegleGestionException;
import com.boutique.gestionboutique.service.UtilisateurService;
import jakarta.validation.Valid;
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
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    // ---------- Liste  ----------

    @GetMapping
    public String liste(Model model) {
        model.addAttribute("utilisateurForm", UtilisateurForm.vide());
        preparerListe(model, false);
        return "utilisateurs/liste";
    }

    // ---------- Création ----------

    @PostMapping
    public String creer(@Valid @ModelAttribute("utilisateurForm") UtilisateurForm form,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        verifierSaisie(form, result);
        if (result.hasErrors()) {
            preparerListe(model, true);
            return "utilisateurs/liste";
        }
        utilisateurService.creer(form);
        redirect.addFlashAttribute("succes",
                "Utilisateur « " + form.identifiant().trim() + " » créé. Il devra définir son mot de passe à sa première connexion.");
        return "redirect:/utilisateurs";
    }

    // ---------- Activation / désactivation ----------

    @PostMapping("/{id}/desactiver")
    public String desactiver(@PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        return changerStatut(id, false, principal, redirect, "Utilisateur désactivé.");
    }

    @PostMapping("/{id}/activer")
    public String activer(@PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        return changerStatut(id, true, principal, redirect, "Utilisateur réactivé.");
    }

    private String changerStatut(Integer id, boolean actif, Principal principal,
                                 RedirectAttributes redirect, String messageSucces) {
        try {
            utilisateurService.changerStatut(id, actif, principal.getName());
            redirect.addFlashAttribute("succes", messageSucces);
        } catch (RegleGestionException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/utilisateurs";
    }

    // ---------- Réinitialisation du mot de passe ----------

    @PostMapping("/{id}/reinitialiser-mot-de-passe")
    public String reinitialiserMotDePasse(@PathVariable Integer id,
                                          Principal principal,
                                          RedirectAttributes redirect) {
        try {
            String identifiant = utilisateurService.trouver(id).getIdentifiant();
            utilisateurService.reinitialiserMotDePasse(id, principal.getName());
            redirect.addFlashAttribute("succes",
                    "Le mot de passe de « " + identifiant + " » a été réinitialisé à « "
                            + UtilisateurService.MOT_DE_PASSE_PAR_DEFAUT
                            + " ». Il devra le changer à sa prochaine connexion.");
        } catch (RegleGestionException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/utilisateurs";
    }

    // ---------- Outils ----------

    private void preparerListe(Model model, boolean ouvrir) {
        model.addAttribute("utilisateurs", utilisateurService.lister());
        model.addAttribute("roles", Role.values());
        model.addAttribute("ouvrirModal", ouvrir);
    }

    private void verifierSaisie(UtilisateurForm form, BindingResult result) {
        if (!result.hasFieldErrors("identifiant")
                && utilisateurService.identifiantDejaUtilise(form.identifiant())) {
            result.rejectValue("identifiant", "unique", "Cet identifiant est déjà utilisé.");
        }
        if (!result.hasFieldErrors("matricule")
                && utilisateurService.matriculeDejaUtilise(form.matricule())) {
            result.rejectValue("matricule", "unique", "Ce matricule est déjà utilisé.");
        }
        if (!result.hasFieldErrors("motDePasse")
                && !form.motDePasse().equals(form.confirmationMotDePasse())) {
            result.rejectValue("confirmationMotDePasse", "mismatch",
                    "Les deux mots de passe ne correspondent pas.");
        }
    }
}