(function () {
    var fenetre = document.getElementById('modalTicket');
    var cadre = document.getElementById('ticketCadre');
    var modal = bootstrap.Modal.getOrCreateInstance(fenetre);

    document.addEventListener('click', function (e) {
        var lien = e.target.closest('[data-ticket]');
        if (!lien) { return; }
        e.preventDefault();
        document.getElementById('modalTicketTitre').textContent = 'Ticket n° ' + lien.dataset.numero;
        cadre.src = lien.dataset.ticket;
        modal.show();
    });

    // Hauteur du cadre = hauteur du ticket
    cadre.addEventListener('load', function () {
        try {
            var hauteur = cadre.contentDocument.documentElement.scrollHeight;
            cadre.style.height = Math.min(Math.max(hauteur, 320), 560) + 'px';
        } catch (e) { /* page non lisible : on garde la hauteur par défaut */ }
    });

    fenetre.addEventListener('hidden.bs.modal', function () { cadre.removeAttribute('src'); });

    document.getElementById('ticketImprimer').addEventListener('click', function () {
        cadre.contentWindow.focus();
        cadre.contentWindow.print();
    });
})();