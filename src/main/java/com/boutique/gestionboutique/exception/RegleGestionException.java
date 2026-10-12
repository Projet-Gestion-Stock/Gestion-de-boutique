package com.boutique.gestionboutique.exception;

// Levée quand une règle de gestion est violée
public class RegleGestionException extends RuntimeException {

    public RegleGestionException(String message) {
        super(message);
    }
}