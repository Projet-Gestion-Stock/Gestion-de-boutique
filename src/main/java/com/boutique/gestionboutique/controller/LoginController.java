package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

@Controller
public class LoginController {

    private final DashboardService dashboardService;
    private final String devise;

    public LoginController(DashboardService dashboardService,
                           @Value("${app.boutique.devise:}") String devise) {
        this.dashboardService = dashboardService;
        this.devise = devise;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String accueil(@RequestParam(name = "jours", defaultValue = "30") int jours,
                          Authentication authentication,
                          Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Règle TB7 : le caissier n'a pas de tableau de bord, il arrive directement sur la saisie d'un achat
        if (!admin) {
            return "redirect:/achats/nouveau";
        }

        // Une période inattendue (ex. ?jours=999) retombe sur la valeur par défaut
        int periode = DashboardService.PERIODES_AUTORISEES.contains(jours)
                ? jours : DashboardService.PERIODE_PAR_DEFAUT;

        model.addAttribute("tableau", dashboardService.construire(periode));
        model.addAttribute("devise", devise);
        return "dashboard/index";
    }
}