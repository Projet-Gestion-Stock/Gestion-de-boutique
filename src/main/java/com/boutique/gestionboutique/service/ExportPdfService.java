package com.boutique.gestionboutique.service;

import com.boutique.gestionboutique.model.Achat;
import com.boutique.gestionboutique.model.LigneLivraison;
import com.boutique.gestionboutique.model.Livraison;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Export PDF des historiques (achats et livraisons) avec OpenPDF. Aucune donnée n'est lue en base ici. */
@Service
public class ExportPdfService {

    // ---------- Palette sobre (celle de l'application) ----------
    private static final Color MARINE = new Color(0x0F, 0x27, 0x47);
    private static final Color TEXTE = new Color(0x11, 0x1C, 0x2D);
    private static final Color TEXTE_DOUX = new Color(0x64, 0x74, 0x8B);
    private static final Color TRAIT = new Color(0xE2, 0xE8, 0xF0);
    private static final Color FOND = new Color(0xF8, 0xFA, 0xFC);

    // ---------- Polices ----------
    private static final Font F_BOUTIQUE = new Font(Font.HELVETICA, 13, Font.BOLD, MARINE);
    private static final Font F_COORD = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXTE_DOUX);
    private static final Font F_TITRE = new Font(Font.HELVETICA, 15, Font.BOLD, MARINE);
    private static final Font F_EDITION = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXTE_DOUX);
    private static final Font F_LIBELLE = new Font(Font.HELVETICA, 7, Font.BOLD, TEXTE_DOUX);
    private static final Font F_VALEUR = new Font(Font.HELVETICA, 9.5f, Font.BOLD, TEXTE);
    private static final Font F_CHIFFRE = new Font(Font.HELVETICA, 14, Font.BOLD, MARINE);
    private static final Font F_ENTETE = new Font(Font.HELVETICA, 8, Font.BOLD, MARINE);
    private static final Font F_CELLULE = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTE);
    private static final Font F_TOTAL = new Font(Font.HELVETICA, 9.5f, Font.BOLD, MARINE);
    private static final Font F_PIED = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, TEXTE_DOUX);

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JOUR_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private record Critere(String libelle, String valeur) {
    }

    private record Carte(String libelle, String valeur) {
    }

    private final String boutiqueNom;
    private final String boutiqueAdresse;
    private final String boutiqueTelephone;
    private final String devise;
    private final Image logo;   // null si le fichier est introuvable : le PDF est alors produit sans logo

    public ExportPdfService(@Value("${app.boutique.nom:Ma boutique}") String boutiqueNom,
                            @Value("${app.boutique.adresse:}") String boutiqueAdresse,
                            @Value("${app.boutique.telephone:}") String boutiqueTelephone,
                            @Value("${app.boutique.devise:}") String devise) {
        this.boutiqueNom = boutiqueNom;
        this.boutiqueAdresse = boutiqueAdresse == null ? "" : boutiqueAdresse.trim();
        this.boutiqueTelephone = boutiqueTelephone == null ? "" : boutiqueTelephone.trim();
        this.devise = devise == null ? "" : devise.trim();
        this.logo = chargerLogo();
    }

    /** Le logo de l'application (static/images/logo.png), lu une seule fois. */
    private Image chargerLogo() {
        try (InputStream in = new ClassPathResource("static/images/logo.png").getInputStream()) {
            Image image = Image.getInstance(in.readAllBytes());
            image.scaleToFit(46, 46);
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    // =====================================================================
    //  Historique des achats
    // =====================================================================

    /** Les achats doivent avoir leurs lignes chargées (AchatService.listerPourExport). */
    public byte[] historiqueAchats(List<Achat> achats, LocalDate du, LocalDate au,
                                   String caissier, String generePar) {
        BigDecimal total = BigDecimal.ZERO;
        List<String[]> lignes = new ArrayList<>();
        for (Achat a : achats) {
            total = total.add(a.getTotal());
            lignes.add(new String[]{
                    String.valueOf(a.getId()),
                    a.getDateAchat().format(JOUR_HEURE),
                    a.getUtilisateur().getNomComplet(),
                    a.getNomClient() != null ? a.getNomClient() : "-",
                    String.valueOf(a.getLignes().size()),
                    String.valueOf(a.getNombreArticles()),
                    montant(a.getTotal())});
        }
        BigDecimal moyenne = achats.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(achats.size()), 2, RoundingMode.HALF_UP);

        return generer("Historique des achats", generePar,
                List.of(new Critere("PÉRIODE", periode(du, au)),
                        new Critere("CAISSIER", caissier)),
                List.of(new Carte("NOMBRE D'ACHATS", String.valueOf(achats.size())),
                        new Carte("TOTAL DES VENTES", montantAvecDevise(total)),
                        new Carte("PANIER MOYEN", montantAvecDevise(moyenne))),
                new String[]{"N°", "DATE", "CAISSIER", "CLIENT", "PRODUITS", "ARTICLES", "TOTAL" + suffixeDevise()},
                new float[]{7, 17, 21, 17, 9, 9, 20},
                new int[]{Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT,
                        Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_RIGHT},
                lignes, "TOTAL DES VENTES", montant(total), "Aucun achat ne correspond à ces critères.");
    }

    // =====================================================================
    //  Historique des livraisons
    // =====================================================================

    /** Les livraisons doivent avoir leurs lignes chargées (LivraisonService.listerPourExport). */
    public byte[] historiqueLivraisons(List<Livraison> livraisons, LocalDate du, LocalDate au,
                                       String grossiste, String generePar) {
        BigDecimal total = BigDecimal.ZERO;
        List<String[]> lignes = new ArrayList<>();
        for (Livraison l : livraisons) {
            total = total.add(l.getTotal());
            int unites = l.getLignes().stream().mapToInt(LigneLivraison::getQuantite).sum();
            lignes.add(new String[]{
                    String.valueOf(l.getId()),
                    l.getDateLivraison().format(JOUR_HEURE),
                    l.getGrossiste().getNom(),
                    l.getUtilisateur().getNomComplet(),
                    String.valueOf(l.getLignes().size()),
                    String.valueOf(unites),
                    montant(l.getTotal())});
        }
        BigDecimal moyenne = livraisons.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(livraisons.size()), 2, RoundingMode.HALF_UP);

        return generer("Historique des livraisons", generePar,
                List.of(new Critere("PÉRIODE", periode(du, au)),
                        new Critere("GROSSISTE", grossiste)),
                List.of(new Carte("NOMBRE DE LIVRAISONS", String.valueOf(livraisons.size())),
                        new Carte("COÛT D'ACHAT TOTAL", montantAvecDevise(total)),
                        new Carte("COÛT MOYEN", montantAvecDevise(moyenne))),
                new String[]{"N°", "DATE", "GROSSISTE", "ENREGISTRÉE PAR", "PRODUITS", "UNITÉS", "COÛT" + suffixeDevise()},
                new float[]{7, 17, 21, 19, 9, 8, 19},
                new int[]{Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT,
                        Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_RIGHT},
                lignes, "COÛT D'ACHAT TOTAL", montant(total), "Aucune livraison ne correspond à ces critères.");
    }

    // =====================================================================
    //  Construction du document
    // =====================================================================

    private byte[] generer(String titre, String generePar, List<Critere> criteres, List<Carte> cartes,
                           String[] entetes, float[] largeurs, int[] alignements,
                           List<String[]> lignes, String libelleTotal, String valeurTotal, String messageVide) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 36, 52);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, sortie);
            writer.setPageEvent(new PiedDePage(boutiqueNom + "  ·  " + titre));
            document.addTitle(titre + " - " + boutiqueNom);
            document.addAuthor(boutiqueNom);
            document.addCreator("Gestion de boutique");
            document.open();

            document.add(entete(titre, generePar));
            document.add(blocCriteres(criteres));
            document.add(blocCartes(cartes));
            document.add(tableau(entetes, largeurs, alignements, lignes, libelleTotal, valeurTotal, messageVide));
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le PDF.", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return sortie.toByteArray();
    }

    /** En-tête : logo et coordonnées de la boutique à gauche, titre du rapport à droite, filet marine dessous. */
    private PdfPTable entete(String titre, String generePar) throws DocumentException {
        PdfPTable table = logo != null
                ? new PdfPTable(new float[]{11, 47, 42})
                : new PdfPTable(new float[]{58, 42});
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);

        if (logo != null) {
            PdfPCell celluleLogo = new PdfPCell(logo, false);
            styleEntete(celluleLogo);
            celluleLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(celluleLogo);
        }

        PdfPCell boutique = new PdfPCell();
        styleEntete(boutique);
        boutique.setVerticalAlignment(Element.ALIGN_MIDDLE);
        boutique.addElement(new Paragraph(boutiqueNom, F_BOUTIQUE));
        String coordonnees = boutiqueAdresse
                + (boutiqueAdresse.isEmpty() || boutiqueTelephone.isEmpty() ? "" : "  ·  ")
                + (boutiqueTelephone.isEmpty() ? "" : "Tél. " + boutiqueTelephone);
        if (!coordonnees.isEmpty()) {
            Paragraph p = new Paragraph(coordonnees, F_COORD);
            p.setSpacingBefore(2);
            boutique.addElement(p);
        }
        table.addCell(boutique);

        PdfPCell rapport = new PdfPCell();
        styleEntete(rapport);
        rapport.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph intitule = new Paragraph(titre, F_TITRE);
        intitule.setAlignment(Element.ALIGN_RIGHT);
        Paragraph edition = new Paragraph(
                "Édité le " + LocalDateTime.now().format(JOUR_HEURE) + " par " + generePar, F_EDITION);
        edition.setAlignment(Element.ALIGN_RIGHT);
        edition.setSpacingBefore(3);
        rapport.addElement(intitule);
        rapport.addElement(edition);
        table.addCell(rapport);
        return table;
    }

    private void styleEntete(PdfPCell cellule) {
        cellule.setBorder(Rectangle.BOTTOM);
        cellule.setBorderColor(MARINE);
        cellule.setBorderWidth(1.2f);
        cellule.setPaddingLeft(0);
        cellule.setPaddingRight(8);
        cellule.setPaddingTop(0);
        cellule.setPaddingBottom(10);
    }

    /** Critères du filtre : un bandeau gris très pâle, sans bordure. */
    private PdfPTable blocCriteres(List<Critere> criteres) throws DocumentException {
        PdfPTable table = new PdfPTable(criteres.size());
        table.setWidthPercentage(100);
        table.setSpacingAfter(16);
        for (Critere c : criteres) {
            PdfPCell cellule = new PdfPCell();
            cellule.setBackgroundColor(FOND);
            cellule.setBorder(Rectangle.NO_BORDER);
            cellule.setPadding(9);
            cellule.addElement(new Paragraph(c.libelle(), F_LIBELLE));
            Paragraph valeur = new Paragraph(c.valeur(), F_VALEUR);
            valeur.setSpacingBefore(2);
            cellule.addElement(valeur);
            table.addCell(cellule);
        }
        return table;
    }

    /** Trois chiffres clés côte à côte, séparés par un filet vertical. */
    private PdfPTable blocCartes(List<Carte> cartes) throws DocumentException {
        PdfPTable table = new PdfPTable(cartes.size());
        table.setWidthPercentage(100);
        table.setSpacingAfter(18);
        for (int i = 0; i < cartes.size(); i++) {
            Carte carte = cartes.get(i);
            PdfPCell cellule = new PdfPCell();
            cellule.setBorder(i == 0 ? Rectangle.NO_BORDER : Rectangle.LEFT);
            cellule.setBorderColor(TRAIT);
            cellule.setBorderWidth(0.8f);
            cellule.setPaddingLeft(i == 0 ? 0 : 14);
            cellule.setPaddingTop(2);
            cellule.setPaddingBottom(2);
            cellule.addElement(new Paragraph(carte.libelle(), F_LIBELLE));
            Paragraph valeur = new Paragraph(carte.valeur(), F_CHIFFRE);
            valeur.setSpacingBefore(3);
            cellule.addElement(valeur);
            table.addCell(cellule);
        }
        return table;
    }

    /** Tableau : en-tête répété à chaque page, filets fins, ligne de total encadrée en haut. */
    private PdfPTable tableau(String[] entetes, float[] largeurs, int[] alignements, List<String[]> lignes,
                              String libelleTotal, String valeurTotal, String messageVide) throws DocumentException {
        PdfPTable table = new PdfPTable(largeurs);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        for (int i = 0; i < entetes.length; i++) {
            PdfPCell c = cellule(entetes[i], F_ENTETE, alignements[i]);
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(MARINE);
            c.setBorderWidth(1f);
            c.setPaddingTop(4);
            c.setPaddingBottom(7);
            table.addCell(c);
        }

        if (lignes.isEmpty()) {
            PdfPCell vide = cellule(messageVide, F_COORD, Element.ALIGN_CENTER);
            vide.setColspan(entetes.length);
            vide.setPadding(26);
            table.addCell(vide);
            return table;
        }

        for (String[] ligne : lignes) {
            for (int i = 0; i < ligne.length; i++) {
                PdfPCell c = cellule(ligne[i], F_CELLULE, alignements[i]);
                c.setBorder(Rectangle.BOTTOM);
                c.setBorderColor(TRAIT);
                c.setBorderWidth(0.5f);
                table.addCell(c);
            }
        }

        int dernier = entetes.length - 1;
        PdfPCell etiquette = cellule(libelleTotal, F_TOTAL, Element.ALIGN_RIGHT);
        etiquette.setColspan(dernier);
        etiquette.setBorder(Rectangle.TOP);
        etiquette.setBorderColor(MARINE);
        etiquette.setBorderWidth(1f);
        etiquette.setPaddingTop(10);
        table.addCell(etiquette);

        PdfPCell somme = cellule(valeurTotal, F_TOTAL, Element.ALIGN_RIGHT);
        somme.setBorder(Rectangle.TOP);
        somme.setBorderColor(MARINE);
        somme.setBorderWidth(1f);
        somme.setPaddingTop(10);
        table.addCell(somme);
        return table;
    }

    private PdfPCell cellule(String texte, Font police, int alignement) {
        PdfPCell c = new PdfPCell(new Phrase(texte, police));
        c.setHorizontalAlignment(alignement);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingLeft(4);
        c.setPaddingRight(4);
        c.setPaddingTop(7);
        c.setPaddingBottom(7);
        return c;
    }

    // =====================================================================
    //  Formats
    // =====================================================================

    private String periode(LocalDate du, LocalDate au) {
        if (du != null && au != null) return "Du " + du.format(JOUR) + " au " + au.format(JOUR);
        if (du != null) return "À partir du " + du.format(JOUR);
        if (au != null) return "Jusqu'au " + au.format(JOUR);
        return "Toutes les dates";
    }

    /** 1 234 567,50 (espace insécable comme séparateur de milliers, virgule décimale). */
    private String montant(BigDecimal valeur) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator('\u00A0');
        symboles.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symboles).format(valeur);
    }

    private String montantAvecDevise(BigDecimal valeur) {
        return montant(valeur) + (devise.isEmpty() ? "" : " " + devise);
    }

    private String suffixeDevise() {
        return devise.isEmpty() ? "" : " (" + devise + ")";
    }

    // =====================================================================
    //  Pied de chaque page : filet, boutique et titre à gauche, « Page x / y » à droite
    // =====================================================================

    private static class PiedDePage extends PdfPageEventHelper {

        private final String texteGauche;
        private PdfTemplate total;

        PiedDePage(String texteGauche) {
            this.texteGauche = texteGauche;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(24, 14);   // le nombre total de pages est écrit à la fermeture
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float gauche = document.leftMargin();
            float droite = document.getPageSize().getWidth() - document.rightMargin();

            cb.saveState();
            cb.setColorStroke(TRAIT);
            cb.setLineWidth(0.6f);
            cb.moveTo(gauche, 38);
            cb.lineTo(droite, 38);
            cb.stroke();
            cb.restoreState();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(texteGauche, F_PIED), gauche, 26, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber() + " / ", F_PIED), droite - 14, 26, 0);
            cb.addTemplate(total, droite - 14, 24);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1), F_PIED), 0, 2, 0);
        }
    }
}