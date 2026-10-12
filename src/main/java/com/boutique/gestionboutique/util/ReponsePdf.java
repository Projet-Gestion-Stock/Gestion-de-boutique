package com.boutique.gestionboutique.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

/** Réponse HTTP pour un PDF : téléchargement, ou affichage dans la fenêtre d'aperçu. */
public final class ReponsePdf {

    private ReponsePdf() {
    }

    /** Téléchargement (nom du fichier : prefixe-AAAA-MM-JJ.pdf). */
    public static ResponseEntity<byte[]> telechargement(byte[] pdf, String prefixe) {
        return reponse(pdf, prefixe, false);
    }

    /** apercu = true : le navigateur affiche le PDF (dans l'iframe de la fenêtre d'aperçu) au lieu de le télécharger. */
    public static ResponseEntity<byte[]> telechargement(byte[] pdf, String prefixe, boolean apercu) {
        return reponse(pdf, prefixe, apercu);
    }

    private static ResponseEntity<byte[]> reponse(byte[] pdf, String prefixe, boolean apercu) {
        String nom = prefixe + "-" + LocalDate.now() + ".pdf";
        ContentDisposition disposition = (apercu ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(nom).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}