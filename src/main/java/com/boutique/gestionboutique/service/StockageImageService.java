package com.boutique.gestionboutique.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stockage local des images de produits.
 * Dossier : app.upload.dir (application.properties), sous-dossier « produits ».
 */
@Service
public class StockageImageService {

    private static final Logger log = LoggerFactory.getLogger(StockageImageService.class);

    /** Taille maximale d'une image : 2 Mo (même valeur que spring.servlet.multipart.max-file-size). */
    public static final long TAILLE_MAX = 2L * 1024 * 1024;

    private final Path dossier;

    public StockageImageService(@Value("${app.upload.dir:}") String dossierConfigure) {
        // Propriété absente ou vide : dossier par défaut dans le répertoire de l'utilisateur
        String racine = (dossierConfigure == null || dossierConfigure.isBlank())
                ? System.getProperty("user.home") + "/boutique-uploads"
                : dossierConfigure.trim();

        this.dossier = Path.of(racine, "produits").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.dossier);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier des images : " + this.dossier
                    + " (vérifiez app.upload.dir dans application.properties)", e);
        }
        if (!Files.isWritable(this.dossier)) {
            throw new IllegalStateException("Le dossier des images n'est pas accessible en écriture : " + this.dossier
                    + " (vérifiez app.upload.dir dans application.properties)");
        }
        log.info("Dossier des images produits : {}", this.dossier);
    }

    public Path getDossier() {
        return dossier;
    }

    /** Retourne le message d'erreur à afficher, ou null si le fichier est acceptable (ou absent). */
    public String verifier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            return null;
        }
        if (fichier.getSize() > TAILLE_MAX) {
            return "L'image dépasse 2 Mo.";
        }
        if (extensionSelonContenu(fichier) == null) {
            return "Format non accepté : utilisez une image JPG, PNG ou WebP.";
        }
        return null;
    }

    /** Enregistre l'image et retourne son nom de fichier généré, ou null s'il n'y a pas de fichier. */
    public String enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            return null;
        }
        String extension = extensionSelonContenu(fichier);
        if (extension == null) {
            throw new IllegalArgumentException("Format d'image non pris en charge.");
        }
        String nom = UUID.randomUUID() + "." + extension;
        try (InputStream in = fichier.getInputStream()) {
            Files.copy(in, dossier.resolve(nom), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'enregistrement de l'image", e);
        }
        return nom;
    }

    /** Supprime un fichier du dossier des images (sans erreur s'il n'existe plus). */
    public void supprimer(String nom) {
        if (nom == null || nom.isBlank()) {
            return;
        }
        Path cible = dossier.resolve(nom).normalize();
        if (!cible.startsWith(dossier)) {
            return; // protection : on ne supprime jamais hors du dossier des images
        }
        try {
            Files.deleteIfExists(cible);
        } catch (IOException e) {
            log.warn("Impossible de supprimer l'image {}", cible, e);
        }
    }

    /** Reconnaît le format d'après les premiers octets (et non d'après le nom envoyé par le navigateur). */
    private String extensionSelonContenu(MultipartFile fichier) {
        try (InputStream in = fichier.getInputStream()) {
            byte[] e = in.readNBytes(12);
            if (e.length >= 3 && (e[0] & 0xFF) == 0xFF && (e[1] & 0xFF) == 0xD8 && (e[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if (e.length >= 4 && (e[0] & 0xFF) == 0x89 && e[1] == 'P' && e[2] == 'N' && e[3] == 'G') {
                return "png";
            }
            if (e.length >= 12 && e[0] == 'R' && e[1] == 'I' && e[2] == 'F' && e[3] == 'F'
                    && e[8] == 'W' && e[9] == 'E' && e[10] == 'B' && e[11] == 'P') {
                return "webp";
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
}