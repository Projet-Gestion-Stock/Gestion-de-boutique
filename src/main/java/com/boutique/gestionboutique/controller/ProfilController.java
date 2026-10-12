package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.config.UtilisateurDetails;
import com.boutique.gestionboutique.dto.MotDePasseDTO;
import com.boutique.gestionboutique.dto.ProfilDTO;
import com.boutique.gestionboutique.model.Utilisateur;
import com.boutique.gestionboutique.service.UtilisateurService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Profil de l'utilisateur connecté. Aucun identifiant n'est reçu dans l'URL ni dans le formulaire :
 * l'utilisateur concerné est toujours celui de la session.
 */
@Controller
@RequestMapping("/profil")
public class ProfilController {

    private final UtilisateurService utilisateurService;

    public ProfilController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    // ---------- Informations personnelles ----------

    @GetMapping
    public String voir(@AuthenticationPrincipal UtilisateurDetails user, Model model) {
        Utilisateur u = utilisateurService.trouverParIdentifiant(user.getUsername());
        model.addAttribute("profilForm", ProfilDTO.depuis(u));
        model.addAttribute("utilisateur", u);
        return "profil/profil";
    }

    @PostMapping
    public String enregistrer(@AuthenticationPrincipal UtilisateurDetails user,
                              @Valid @ModelAttribute("profilForm") ProfilDTO form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("utilisateur", utilisateurService.trouverParIdentifiant(user.getUsername()));
            return "profil/profil";
        }
        utilisateurService.modifierProfil(user.getUsername(), form);
        redirect.addFlashAttribute("succes", "Votre profil a été mis à jour.");
        return "redirect:/profil";
    }

    // ---------- Mot de passe ----------

    private static String pageMotDePasse(boolean premiereConnexion) {
        return premiereConnexion ? "profil/premiere-connexion" : "profil/mot-de-passe";
    }

    @GetMapping("/mot-de-passe")
    public String formulaireMotDePasse(@AuthenticationPrincipal UtilisateurDetails user, Model model) {
        model.addAttribute("motDePasseForm", MotDePasseDTO.vide());
        model.addAttribute("premiereConnexion", user.isDoitChangerMotDePasse());
        return pageMotDePasse(user.isDoitChangerMotDePasse());
    }

    @PostMapping("/mot-de-passe")
    public String changerMotDePasse(@AuthenticationPrincipal UtilisateurDetails user,
                                    Authentication authentication,
                                    @Valid @ModelAttribute("motDePasseForm") MotDePasseDTO form,
                                    BindingResult result,
                                    Model model,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    RedirectAttributes redirect) {
        boolean premiereConnexion = user.isDoitChangerMotDePasse();
        String identifiant = user.getUsername();

        // Hors première connexion, le mot de passe actuel est exigé
        if (!premiereConnexion
                && (form.motDePasseActuel() == null
                || !utilisateurService.motDePasseCorrespond(identifiant, form.motDePasseActuel()))) {
            result.rejectValue("motDePasseActuel", "incorrect", "Le mot de passe actuel est incorrect.");
        }

        if (!result.hasFieldErrors("nouveauMotDePasse")) {
            if (!form.nouveauMotDePasse().equals(form.confirmation())) {
                result.rejectValue("confirmation", "mismatch", "Les deux mots de passe ne correspondent pas.");
            } else if (utilisateurService.motDePasseCorrespond(identifiant, form.nouveauMotDePasse())) {
                result.rejectValue("nouveauMotDePasse", "identique",
                        "Le nouveau mot de passe doit être différent de l'actuel.");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("premiereConnexion", premiereConnexion);
            return pageMotDePasse(premiereConnexion);
        }

        utilisateurService.definirMotDePasse(identifiant, form.nouveauMotDePasse());

        if (premiereConnexion) {
            // Nouvelle session : on déconnecte, l'utilisateur se reconnecte avec son mot de passe
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/login?mdp";
        }
        redirect.addFlashAttribute("succes", "Votre mot de passe a été modifié.");
        return "redirect:/profil";
    }
}
