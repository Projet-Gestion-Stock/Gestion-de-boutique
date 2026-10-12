
package com.boutique.gestionboutique.controller;

import com.boutique.gestionboutique.service.TableauDeBordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TableauDeBordController {

    private final TableauDeBordService tableauDeBordService;
    private final String devise;

    public TableauDeBordController(
            TableauDeBordService tableauDeBordService,
            @Value("app.boutique.devise") String devise) {
        this.tableauDeBordService = tableauDeBordService;
        this.devise = devise;
    }

    @GetMapping("/")
    public String accueil(
            @RequestParam(name = "jours", defaultValue = "30") int jours,
            Authentication authentication,
            Model model) {

        boolean admin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!admin) {
            return "redirect:/achats/nouveau";
        }

        int periode = TableauDeBordService.PERIODES_AUTORISEES.contains(jours)
                ? jours
                : TableauDeBordService.PERIODE_PAR_DEFAUT;

        model.addAttribute(
                "tableau",
                tableauDeBordService.construire(periode)
        );

        model.addAttribute("devise", devise);

        return "tableau-de-bord/index";
    }
}
