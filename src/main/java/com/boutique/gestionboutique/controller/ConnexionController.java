package com.boutique.gestionboutique.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConnexionController {

    @GetMapping("/login")
    public String login() {
        return "auth/connexion";
    }

}