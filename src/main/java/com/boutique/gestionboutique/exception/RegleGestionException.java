package com.boutique.gestionboutique.exception;

/**
 * Levée quand une règle de gestion est violée
 * (ex. stock insuffisant, désactivation de son propre compte).
 * Le message est destiné à être affiché à l'utilisateur.
 */
public class RegleGestionException extends RuntimeException {

    public RegleGestionException(String message) {
        super(message);
    }
}