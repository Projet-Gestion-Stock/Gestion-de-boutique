(function () {
    var modalEl = document.getElementById('modalCategorie');
    var form = document.getElementById('formCategorie');
    var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    var urlBase = form.dataset.actionCreation;   // « /categories »
    var champNom = document.getElementById('nom');

    function effacerErreurs() {
        form.querySelectorAll('.is-invalid').forEach(function (e) { e.classList.remove('is-invalid'); });
    }

    function ouvrir(edition, d) {
        form.action = edition ? urlBase + '/' + d.id : urlBase;
        document.getElementById('modalCategorieTitre').textContent =
            edition ? 'Renommer la catégorie' : 'Ajouter une catégorie';
        champNom.value = edition ? d.nom : '';
        effacerErreurs();
        modal.show();
    }

    modalEl.addEventListener('shown.bs.modal', function () { champNom.focus(); champNom.select(); });

    var modalSupprimer = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalSupprimer'));
    var formSupprimer = document.getElementById('formSupprimer');
    var baseSupprimer = formSupprimer.getAttribute('action');

    document.addEventListener('click', function (e) {
        if (e.target.closest('.btn-ajouter')) { ouvrir(false, null); return; }
        var modifier = e.target.closest('.btn-modifier');
        if (modifier) { ouvrir(true, modifier.dataset); return; }
        var supprimer = e.target.closest('.btn-supprimer');
        if (supprimer) {
            formSupprimer.action = baseSupprimer + '/' + supprimer.dataset.id + '/supprimer';
            document.getElementById('nomASupprimer').textContent = supprimer.dataset.nom;
            modalSupprimer.show();
        }
    });

    // Après un refus du serveur : la fenêtre se rouvre avec les messages d'erreur
    if (modalEl.dataset.ouvrir === 'true') { modal.show(); }
})();