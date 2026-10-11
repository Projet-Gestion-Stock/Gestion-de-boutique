package com.boutique.gestionboutique.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

/** Réponse HTTP de téléchargement d'un PDF (nom du fichier : prefixe-AAAA-MM-JJ.pdf). */
public final class ReponsePdf {

    private ReponsePdf() {
    }

    public static ResponseEntity<byte[]> telechargement(byte[] pdf, String prefixe) {
        String nom = prefixe + "-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nom).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}