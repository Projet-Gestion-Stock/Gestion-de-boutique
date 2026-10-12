(function () {
    var fenetre = document.getElementById('modalPdf');
    var cadre = document.getElementById('pdfCadre');
    var modal = bootstrap.Modal.getOrCreateInstance(fenetre);

    document.addEventListener('click', function (e) {
        var lien = e.target.closest('[data-pdf]');
        if (!lien) { return; }
        e.preventDefault();
        document.getElementById('modalPdfTitre').textContent = lien.dataset.pdfTitre || 'Aperçu du PDF';
        document.getElementById('pdfTelecharger').href = lien.getAttribute('href');   // téléchargement
        document.getElementById('pdfOuvrir').href = lien.dataset.pdf;                 // affichage dans un onglet
        cadre.src = lien.dataset.pdf;
        modal.show();
    });

    fenetre.addEventListener('hidden.bs.modal', function () { cadre.removeAttribute('src'); });
})();