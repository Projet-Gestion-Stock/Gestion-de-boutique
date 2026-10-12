package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.security.UtilisateurDetails;
import com.boutique.gestionboutique.form.MotDePasseForm;
import com.boutique.gestionboutique.form.ProfilForm;
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

// Profil de l'utilisateur connecté. Aucun identifiant n'est reçu dans l'URL ni dans le formulaire
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
        model.addAttribute("profilForm", ProfilForm.depuis(u));
        model.addAttribute("utilisateur", u);
        return "profil/mon-profil";
    }

    @PostMapping
    public String enregistrer(@AuthenticationPrincipal UtilisateurDetails user,
                              @Valid @ModelAttribute("profilForm") ProfilForm form,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("utilisateur", utilisateurService.trouverParIdentifiant(user.getUsername()));
            return "profil/mon-profil";
        }
        utilisateurService.modifierProfil(user.getUsername(), form);
        redirect.addFlashAttribute("succes", "Votre profil a été mis à jour.");
        return "redirect:/profil/mon-profil";
    }

    // ---------- Mot de passe ----------

    private static String pageMotDePasse(boolean premiereConnexion) {
        return premiereConnexion ? "auth/premiere-connexion" : "profil/mot-de-passe";
    }

    @GetMapping("/mot-de-passe")
    public String formulaireMotDePasse(@AuthenticationPrincipal UtilisateurDetails user, Model model) {
        model.addAttribute("motDePasseForm", MotDePasseForm.vide());
        model.addAttribute("premiereConnexion", user.isDoitChangerMotDePasse());
        return pageMotDePasse(user.isDoitChangerMotDePasse());
    }

    @PostMapping("/mot-de-passe")
    public String changerMotDePasse(@AuthenticationPrincipal UtilisateurDetails user,
                                    Authentication authentication,
                                    @Valid @ModelAttribute("motDePasseForm") MotDePasseForm form,
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
        return "redirect:/profil/mon-profil";
    }
}
