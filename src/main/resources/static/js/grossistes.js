(function () {
    var modalEl = document.getElementById('modalGrossiste');
    var form = document.getElementById('formGrossiste');
    var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    var urlBase = form.dataset.actionCreation;   // « /grossistes »
    var champs = {
        nom: document.getElementById('nom'),
        telephone: document.getElementById('telephone'),
        email: document.getElementById('email'),
        adresse: document.getElementById('adresse')
    };

    function effacerErreurs() {
        form.querySelectorAll('.is-invalid').forEach(function (e) { e.classList.remove('is-invalid'); });
    }

    function ouvrir(edition, d) {
        form.action = edition ? urlBase + '/' + d.id : urlBase;
        document.getElementById('modalGrossisteTitre').textContent =
            edition ? 'Modifier le grossiste' : 'Ajouter un grossiste';
        champs.nom.value = edition ? d.nom : '';
        champs.telephone.value = edition ? (d.telephone || '') : '';
        champs.email.value = edition ? (d.email || '') : '';
        champs.adresse.value = edition ? (d.adresse || '') : '';
        effacerErreurs();
        modal.show();
    }

    modalEl.addEventListener('shown.bs.modal', function () { champs.nom.focus(); });

    document.addEventListener('click', function (e) {
        if (e.target.closest('.btn-ajouter')) { ouvrir(false, null); return; }
        var modifier = e.target.closest('.btn-modifier');
        if (modifier) { ouvrir(true, modifier.dataset); }
    });

    // Après un refus du serveur : la fenêtre se rouvre avec les messages d'erreur
    if (modalEl.dataset.ouvrir === 'true') { modal.show(); }
})();