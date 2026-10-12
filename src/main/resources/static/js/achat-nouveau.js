(function () {
    var formulaire = document.getElementById('formAchat');
    var corps = document.getElementById('lignes');
    var modele = document.getElementById('modeleLigne');
    var boite = document.getElementById('suggestions');
    var btnValider = document.getElementById('btnValider');
    var devise = corps.dataset.devise || '';

    var catalogue = Array.prototype.map.call(
        document.querySelectorAll('#catalogue span'),
        function (s) {
            return {
                id: s.dataset.id,
                nom: s.dataset.nom,
                prix: parseFloat(s.dataset.prix),
                stock: parseInt(s.dataset.stock, 10),
                image: s.dataset.image
            };
        });
    var parId = {};
    catalogue.forEach(function (p) { parId[p.id] = p; });

    var champActif = null;   // champ de saisie qui a le focus
    var resultats = [];      // suggestions sélectionnables affichées
    var indexActif = -1;     // suggestion surlignée au clavier

    function formater(nombre) {
        return nombre.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    function montant(nombre) { return formater(nombre) + (devise ? ' ' + devise : ''); }

    // Montant d'une cellule du tableau : le chiffre, et la devise en petit dessous (valeur null : un tiret)
    function ecrireMontant(element, valeur) {
        element.textContent = '';
        if (valeur === null) { element.textContent = '-'; return; }
        element.appendChild(document.createTextNode(formater(valeur)));
        if (devise) {
            var petit = document.createElement('small');
            petit.textContent = devise;
            element.appendChild(petit);
        }
    }

    // ---------- Calculs et affichage (aide à la saisie : le serveur fait foi) ----------
    function recalculer() {
        var total = 0, articles = 0, lignes = 0;
        corps.querySelectorAll('tr.ligne').forEach(function (tr) {
            var p = parId[tr.querySelector('.produit-id').value];
            var quantite = parseInt(tr.querySelector('.quantite').value, 10) || 0;
            var sousTotal = p ? p.prix * quantite : 0;

            ecrireMontant(tr.querySelector('.prix'), p ? p.prix : null);
            ecrireMontant(tr.querySelector('.sous-total'), sousTotal);
            majMiniature(tr, p);

            var avert = tr.querySelector('.avert-stock');
            var depasse = !!p && quantite > p.stock;
            avert.classList.toggle('d-none', !depasse);
            if (depasse) { avert.querySelector('span').textContent = 'Stock dispo : ' + p.stock; }

            if (p) { lignes++; articles += quantite; total += sousTotal; }
        });
        document.getElementById('compteLignes').textContent = lignes + (lignes > 1 ? ' lignes' : ' ligne');
        document.getElementById('nbArticles').textContent = articles;
        document.getElementById('total').textContent = montant(total);

        var enCours = lignes > 0;
        btnValider.disabled = !enCours;
        var statut = document.getElementById('statutAchat');
        statut.textContent = enCours ? 'En cours' : 'En attente';
        statut.className = 'pastille ' + (enCours ? 'pastille--bleu' : 'pastille--gris');
    }

    function majMiniature(tr, p) {
        var miniature = tr.querySelector('.miniature');
        if (p && p.image) {
            if (miniature.getAttribute('src') !== p.image) { miniature.src = p.image; }
            miniature.classList.remove('d-none');
        } else {
            miniature.classList.add('d-none');
        }
    }

    // Renumérote lignes[0], lignes[1]... sans trou après un retrait
    function renumeroter() {
        corps.querySelectorAll('tr.ligne').forEach(function (tr, i) {
            tr.querySelectorAll('input').forEach(function (champ) {
                if (champ.name) { champ.name = champ.name.replace(/lignes\[\d+\]/, 'lignes[' + i + ']'); }
            });
        });
    }

    // ---------- Recherche de produit ----------
    function chercher(texte) {
        var t = texte.trim().toLowerCase();
        if (t === '') { return catalogue.slice(); }
        var debut = [], contient = [];
        catalogue.forEach(function (p) {
            var pos = p.nom.toLowerCase().indexOf(t);
            if (pos === 0) { debut.push(p); } else if (pos > 0) { contient.push(p); }
        });
        return debut.concat(contient);
    }

    function positionner() {
        if (!champActif) { return; }
        var r = champActif.getBoundingClientRect();
        var largeur = Math.max(r.width, 360);
        var gauche = Math.min(r.left, window.innerWidth - largeur - 8);
        boite.style.left = Math.max(8, gauche) + 'px';
        boite.style.top = (r.bottom + 4) + 'px';
        boite.style.width = largeur + 'px';
    }

    // Une suggestion : miniature, nom, prix et stock (index < 0 : produit en rupture, non sélectionnable)
    function creerItem(p, index) {
        var rupture = index < 0;
        var item = document.createElement(rupture ? 'div' : 'button');
        item.className = 'sugg__item' + (rupture ? ' sugg__item--rupture' : '');
        if (!rupture) { item.type = 'button'; item.dataset.index = index; }

        var img = document.createElement('img');
        img.className = 'sugg__img';
        img.alt = '';
        img.loading = 'lazy';
        img.src = p.image;

        var nom = document.createElement('span');
        nom.className = 'sugg__nom';
        nom.textContent = p.nom;

        var info;
        if (rupture) {
            info = document.createElement('span');
            info.className = 'pastille pastille--rouge';
            info.textContent = 'Rupture';
        } else {
            info = document.createElement('span');
            info.className = 'sugg__info';
            var prix = document.createElement('strong');
            prix.textContent = montant(p.prix);
            var stock = document.createElement('small');
            stock.textContent = 'Stock : ' + p.stock;
            info.appendChild(prix);
            info.appendChild(stock);
        }
        item.appendChild(img);
        item.appendChild(nom);
        item.appendChild(info);
        return item;
    }

    function afficher(champ) {
        champActif = champ;
        var trouves = chercher(champ.value);
        resultats = trouves.filter(function (p) { return p.stock > 0; });
        var ruptures = trouves.filter(function (p) { return p.stock <= 0; });
        boite.innerHTML = '';

        if (trouves.length === 0) {
            var vide = document.createElement('div');
            vide.className = 'sugg__vide';
            vide.textContent = 'Aucun produit trouvé';
            boite.appendChild(vide);
        }
        resultats.forEach(function (p, i) { boite.appendChild(creerItem(p, i)); });
        ruptures.forEach(function (p) { boite.appendChild(creerItem(p, -1)); });

        indexActif = -1;
        if (resultats.length > 0) { surligner(0); }
        positionner();
        boite.style.display = 'block';
    }

    function fermer() {
        boite.style.display = 'none';
        champActif = null;
        indexActif = -1;
    }

    function surligner(nouvelIndex) {
        var items = boite.querySelectorAll('button[data-index]');
        if (items.length === 0) { return; }
        indexActif = (nouvelIndex + items.length) % items.length;
        items.forEach(function (it, i) { it.classList.toggle('sugg__item--actif', i === indexActif); });
        items[indexActif].scrollIntoView({ block: 'nearest' });
    }

    function choisir(p) {
        var tr = champActif.closest('tr.ligne');
        champActif.value = p.nom;
        tr.querySelector('.produit-id').value = p.id;
        champActif.classList.remove('is-invalid');
        fermer();
        recalculer();
        var quantite = tr.querySelector('.quantite');
        quantite.focus();
        quantite.select();
    }

    // Met l'id du produit dans le champ caché si le texte correspond exactement au nom d'un produit disponible
    function synchroniser(champ) {
        var t = champ.value.trim().toLowerCase();
        var trouve = null;
        catalogue.forEach(function (p) { if (p.stock > 0 && p.nom.toLowerCase() === t) { trouve = p; } });
        champ.closest('tr.ligne').querySelector('.produit-id').value = trouve ? trouve.id : '';
        return trouve;
    }

    function estSaisie(el) { return el.classList && el.classList.contains('produit-saisie'); }

    // ---------- Événements ----------
    corps.addEventListener('focusin', function (e) {
        if (estSaisie(e.target)) { afficher(e.target); }
    });

    corps.addEventListener('input', function (e) {
        if (estSaisie(e.target)) {
            synchroniser(e.target);
            afficher(e.target);
        }
        recalculer();
    });

    corps.addEventListener('change', recalculer);

    corps.addEventListener('focusout', function (e) {
        if (!estSaisie(e.target)) { return; }
        var trouve = synchroniser(e.target);
        if (trouve) {
            e.target.classList.remove('is-invalid');
        } else if (e.target.value.trim() !== '') {
            e.target.classList.add('is-invalid');
        }
        fermer();
        recalculer();
    });

    corps.addEventListener('keydown', function (e) {
        if (!estSaisie(e.target) || boite.style.display === 'none') { return; }
        if (e.key === 'ArrowDown') { e.preventDefault(); surligner(indexActif + 1); }
        else if (e.key === 'ArrowUp') { e.preventDefault(); surligner(indexActif - 1); }
        else if (e.key === 'Escape') { fermer(); }
        else if (e.key === 'Enter') {
            e.preventDefault();   // évite l'envoi du formulaire
            var p = resultats[indexActif >= 0 ? indexActif : 0];
            if (p) { choisir(p); }
        }
    });

    // Clic sur une suggestion : mousedown (avant la perte de focus du champ)
    boite.addEventListener('mousedown', function (e) {
        e.preventDefault();
        var item = e.target.closest('button[data-index]');
        if (item && champActif) { choisir(resultats[item.dataset.index]); }
    });

    // Boutons − et + de la quantité, et bouton de retrait d'une ligne
    corps.addEventListener('click', function (e) {
        var pas = e.target.closest('.stepper__btn');
        if (pas) {
            var champ = pas.parentElement.querySelector('.quantite');
            var valeur = (parseInt(champ.value, 10) || 0) + parseInt(pas.dataset.delta, 10);
            champ.value = valeur < 1 ? 1 : valeur;
            recalculer();
            return;
        }
        var retirer = e.target.closest('.btn-retirer');
        if (!retirer) { return; }
        var tr = retirer.closest('tr.ligne');
        if (corps.querySelectorAll('tr.ligne').length === 1) {
            // Dernière ligne : on la vide au lieu de la supprimer
            tr.querySelector('.produit-id').value = '';
            tr.querySelector('.produit-saisie').value = '';
            tr.querySelector('.produit-saisie').classList.remove('is-invalid');
            tr.querySelector('.quantite').value = 1;
        } else {
            tr.remove();
        }
        fermer();
        renumeroter();
        recalculer();
    });

    // La liste suit le champ si la page défile ou change de taille
    window.addEventListener('scroll', function (e) {
        if (e.target !== boite) { positionner(); }
    }, true);
    window.addEventListener('resize', positionner);

    // ---------- Lignes ----------
    document.getElementById('ajouterLigne').addEventListener('click', function () {
        var index = corps.querySelectorAll('tr.ligne').length;
        corps.insertAdjacentHTML('beforeend', modele.innerHTML.replace(/__INDEX__/g, index));
        corps.lastElementChild.querySelector('.produit-saisie').focus();
        recalculer();
    });

    // Une validation à la fois : évite d'enregistrer deux fois le même achat par un double clic
    formulaire.addEventListener('submit', function () { btnValider.disabled = true; });
    window.addEventListener('pageshow', recalculer);

    // Après un refus du serveur : réaffiche le nom des produits déjà choisis
    corps.querySelectorAll('tr.ligne').forEach(function (tr) {
        var p = parId[tr.querySelector('.produit-id').value];
        if (p) { tr.querySelector('.produit-saisie').value = p.nom; }
    });

    recalculer();
})();