(function () {
    var modalEl = document.getElementById('modalProduit');
    var form = document.getElementById('formProduit');
    var modalProduit = bootstrap.Modal.getOrCreateInstance(modalEl);
    var urlBase = form.dataset.actionCreation;   // « /produits »
    var imageDefaut = modalEl.dataset.imageDefaut;

    var champs = {
        nom: document.getElementById('nom'),
        categorie: document.getElementById('categorieId'),
        prix: document.getElementById('prixVente'),
        seuil: document.getElementById('seuilAlerte')
    };
    var stockActuel = document.getElementById('stockActuel');
    var apercu = document.getElementById('apercu');
    var fichier = document.getElementById('imageFichier');
    var supprimer = document.getElementById('supprimerImage');
    var groupeSupprimer = document.getElementById('groupeSupprimerImage');
    var libelleChoix = document.getElementById('libelleChoixImage');

    function reinitialiserImage(src, personnalisee) {
        apercu.src = src;
        apercu.dataset.origine = src;
        fichier.value = '';
        supprimer.checked = false;
        groupeSupprimer.classList.toggle('d-none', !personnalisee);
        libelleChoix.textContent = personnalisee ? "Changer l'image" : 'Choisir une image';
    }

    function effacerErreurs() {
        form.querySelectorAll('.is-invalid').forEach(function (e) { e.classList.remove('is-invalid'); });
        var erreurImage = document.getElementById('erreurImage');
        if (erreurImage) { erreurImage.remove(); }
    }

    function ouvrirModification(d) {
        form.action = urlBase + '/' + d.id;
        champs.nom.value = d.nom;
        champs.categorie.value = d.categorie;
        champs.prix.value = d.prix;
        champs.seuil.value = d.seuil;
        stockActuel.value = d.stock;
        reinitialiserImage(d.image, d.imagePerso === 'true');
        effacerErreurs();
        modalProduit.show();
    }

    // Aperçu immédiat de l'image choisie
    fichier.addEventListener('change', function () {
        var f = fichier.files[0];
        if (f) { apercu.src = URL.createObjectURL(f); supprimer.checked = false; }
        else { apercu.src = apercu.dataset.origine; }
    });
    supprimer.addEventListener('change', function () {
        if (supprimer.checked) { apercu.src = imageDefaut; fichier.value = ''; }
        else { apercu.src = apercu.dataset.origine; }
    });

    modalEl.addEventListener('shown.bs.modal', function () { champs.nom.focus(); });

    // Confirmation de désactivation
    var modalDesactiver = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalDesactiver'));
    var formDesactiver = document.getElementById('formDesactiver');
    var baseDesactiver = formDesactiver.getAttribute('action');

    document.addEventListener('click', function (e) {
        var modifier = e.target.closest('.btn-modifier');
        if (modifier) { ouvrirModification(modifier.dataset); return; }
        var desactiver = e.target.closest('.btn-desactiver');
        if (desactiver) {
            formDesactiver.action = baseDesactiver + '/' + desactiver.dataset.id + '/desactiver';
            document.getElementById('nomADesactiver').textContent = desactiver.dataset.nom;
            modalDesactiver.show();
        }
    });

    // Après un refus du serveur : la fenêtre se rouvre avec les messages d'erreur
    if (modalEl.dataset.ouvrir === 'true') {
        apercu.dataset.origine = apercu.getAttribute('src');
        var personnalisee = modalEl.dataset.imagePerso === 'true';
        groupeSupprimer.classList.toggle('d-none', !personnalisee);
        libelleChoix.textContent = personnalisee ? "Changer l'image" : 'Choisir une image';
        modalProduit.show();
    }
})();