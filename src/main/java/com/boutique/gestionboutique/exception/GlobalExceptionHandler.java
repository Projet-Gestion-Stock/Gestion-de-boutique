package com.boutique.gestionboutique.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Point unique de traitement des exceptions connues : elles produisent toutes la même page d'erreur. */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Ressource absente : 404. */
    @ExceptionHandler(RessourceIntrouvableException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleIntrouvable(RessourceIntrouvableException ex, Model model) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return page(model, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Règle de gestion violée : 409. Le message est écrit pour l'utilisateur. */
    @ExceptionHandler(RegleGestionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleRegleGestion(RegleGestionException ex, Model model) {
        log.warn("Règle de gestion violée : {}", ex.getMessage());
        return page(model, HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Contrainte de la base violée (doublon, élément encore référencé...).
     * On n'affiche PAS le message technique : il est seulement journalisé.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleIntegrite(DataIntegrityViolationException ex, Model model) {
        log.error("Violation d'une contrainte de la base", ex);
        return page(model, HttpStatus.CONFLICT,
                "Opération impossible : elle entre en conflit avec des données existantes "
                        + "(doublon ou élément déjà utilisé ailleurs).");
    }

    private String page(Model model, HttpStatus status, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("message", message);
        return "error";
    }
}