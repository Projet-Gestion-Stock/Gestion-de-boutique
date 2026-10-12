(function () {
    var modalEl = document.getElementById('modalUtilisateur');
    var form = document.getElementById('formUtilisateur');
    var modal = bootstrap.Modal.getOrCreateInstance(modalEl);

    // Formulaire propre à chaque ouverture : champs vides, rôle CAISSIER, aucune erreur affichée
    function viderFormulaire() {
        form.querySelectorAll('input').forEach(function (c) {
            if (c.type === 'radio') { c.checked = false; }
            else if (c.type !== 'hidden') { c.value = ''; }
            c.classList.remove('is-invalid');
        });
        form.querySelectorAll('.is-invalid').forEach(function (c) { c.classList.remove('is-invalid'); });
        document.getElementById('role').value = 'CAISSIER';
        form.querySelectorAll('.champ-mdp input').forEach(function (c) { c.type = 'password'; });
    }

    document.getElementById('btnAjouter').addEventListener('click', function () {
        viderFormulaire();
        modal.show();
    });

    modalEl.addEventListener('shown.bs.modal', function () { document.getElementById('nom').focus(); });

    // Afficher / masquer un mot de passe
    form.addEventListener('click', function (e) {
        var oeil = e.target.closest('.btn-oeil');
        if (!oeil) { return; }
        var champ = oeil.parentElement.querySelector('input');
        champ.type = champ.type === 'password' ? 'text' : 'password';
    });

    // Confirmations
    var modalReinit = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalReinit'));
    var formReinit = document.getElementById('formReinit');
    var baseReinit = formReinit.getAttribute('action');
    var modalDesactiver = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalDesactiver'));
    var formDesactiver = document.getElementById('formDesactiver');
    var baseDesactiver = formDesactiver.getAttribute('action');

    document.addEventListener('click', function (e) {
        var reinit = e.target.closest('.btn-reinit');
        if (reinit) {
            formReinit.action = baseReinit + '/' + reinit.dataset.id + '/reinitialiser-mot-de-passe';
            document.getElementById('nomAReinit').textContent = reinit.dataset.nom;
            modalReinit.show();
            return;
        }
        var desactiver = e.target.closest('.btn-desactiver');
        if (desactiver) {
            formDesactiver.action = baseDesactiver + '/' + desactiver.dataset.id + '/desactiver';
            document.getElementById('nomADesactiver').textContent = desactiver.dataset.nom;
            modalDesactiver.show();
        }
    });

    // Après un refus du serveur : la fenêtre se rouvre avec les messages d'erreur
    if (modalEl.dataset.ouvrir === 'true') { modal.show(); }
})();