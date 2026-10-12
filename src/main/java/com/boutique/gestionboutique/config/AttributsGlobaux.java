package com.boutique.gestionboutique.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Attributs disponibles dans tous les templates. */
@ControllerAdvice
public class AttributsGlobaux {

    private final String devise;

    public AttributsGlobaux(@Value("${app.boutique.devise:}") String devise) {
        this.devise = devise == null ? "" : devise.trim();
    }

    @ModelAttribute("devise")
    public String devise() {
        return devise;
    }
}