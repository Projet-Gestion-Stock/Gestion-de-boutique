(function () {
    var formulaire = document.getElementById('formLivraison');
    var corps = document.getElementById('lignes');
    var modele = document.getElementById('modeleLigne');
    var boite = document.getElementById('suggestions');
    var btnValider = document.getElementById('btnValider');
    var selectGrossiste = document.getElementById('grossisteId');
    var recap = document.getElementById('recap');
    var devise = corps.dataset.devise || '';
    var erreursServeur = parseInt(recap.dataset.erreurs, 10) || 0;

    var catalogue = Array.prototype.map.call(
        document.querySelectorAll('#catalogue span'),
        function (s) {
            return { id: s.dataset.id, nom: s.dataset.nom, stock: parseInt(s.dataset.stock, 10), image: s.dataset.image };
        });
    var parId = {};
    catalogue.forEach(function (p) { parId[p.id] = p; });

    var champActif = null;   // champ de recherche qui a le focus
    var resultats = [];      // suggestions affichées
    var indexActif = -1;     // suggestion surlignée au clavier

    function formater(nombre) {
        return nombre.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
    function montant(nombre) { return formater(nombre) + (devise ? ' ' + devise : ''); }

    function ecrireMontant(element, valeur) {
        element.textContent = '';
        element.appendChild(document.createTextNode(formater(valeur)));
        if (devise) {
            var petit = document.createElement('small');
            petit.textContent = devise;
            element.appendChild(petit);
        }
    }

    // ---------- Mode de la ligne : produit existant / nouveau produit ----------
    function modeNouveau(tr) { return tr.querySelector('.mode-valeur').value === 'true'; }

    // Les champs du mode inactif sont désactivés : ils ne sont pas envoyés au serveur
    function appliquerMode(tr) {
        var nouveau = modeNouveau(tr);
        var existant = tr.querySelector('.bloc-existant');
        var neuf = tr.querySelector('.bloc-nouveau');
        existant.classList.toggle('d-none', nouveau);
        neuf.classList.toggle('d-none', !nouveau);
        existant.querySelectorAll('input').forEach(function (c) { c.disabled = nouveau; });
        neuf.querySelectorAll('input, select').forEach(function (c) { c.disabled = !nouveau; });
        tr.querySelectorAll('.mode-ligne__btn').forEach(function (b) {
            b.classList.toggle('mode-ligne__btn--actif', (b.dataset.mode === 'true') === nouveau);
        });
    }

    // ---------- Calculs et affichage (aide à la saisie : le serveur fait foi) ----------
    function recalculer() {
        var total = 0, unites = 0, lignes = 0, nouveaux = 0;
        corps.querySelectorAll('tr.ligne').forEach(function (tr) {
            var neuf = modeNouveau(tr);
            var p = parId[tr.querySelector('.produit-id').value];
            var renseignee = neuf ? tr.querySelector('.nouveau-nom').value.trim() !== '' : !!p;
            var quantite = parseInt(tr.querySelector('.quantite').value, 10) || 0;
            var prix = parseFloat(tr.querySelector('.prix-achat').value) || 0;
            var sousTotal = quantite * prix;

            ecrireMontant(tr.querySelector('.sous-total'), sousTotal);
            majMiniature(tr, neuf ? null : p);

            if (renseignee) {
                lignes++; unites += quantite; total += sousTotal;
                if (neuf) { nouveaux++; }
            }
        });

        document.getElementById('compteLignes').textContent = lignes + (lignes > 1 ? ' lignes' : ' ligne');
        document.getElementById('nbLignes').textContent = lignes;
        document.getElementById('nbUnites').textContent = unites;
        document.getElementById('nbNouveaux').textContent = nouveaux;
        document.getElementById('total').textContent = montant(total);
        document.getElementById('recapGrossiste').textContent =
            selectGrossiste.value ? selectGrossiste.options[selectGrossiste.selectedIndex].text : '-';

        btnValider.disabled = lignes === 0;
        var statut = document.getElementById('statutLivraison');
        if (erreursServeur > 0) {
            statut.textContent = erreursServeur + (erreursServeur > 1 ? ' erreurs' : ' erreur');
            statut.className = 'pastille pastille--rouge';
        } else {
            statut.textContent = lignes > 0 ? 'En cours' : 'En attente';
            statut.className = 'pastille ' + (lignes > 0 ? 'pastille--bleu' : 'pastille--gris');
        }
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
            tr.querySelectorAll('select, input').forEach(function (champ) {
                if (champ.name) { champ.name = champ.name.replace(/lignes\[\d+\]/, 'lignes[' + i + ']'); }
            });
        });
    }

    // ---------- Recherche de produit existant ----------
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

    // Une suggestion : miniature, nom et stock actuel
    function creerItem(p, index) {
        var item = document.createElement('button');
        item.type = 'button';
        item.className = 'sugg__item';
        item.dataset.index = index;

        var img = document.createElement('img');
        img.className = 'sugg__img';
        img.alt = '';
        img.loading = 'lazy';
        img.src = p.image;

        var nom = document.createElement('span');
        nom.className = 'sugg__nom';
        nom.textContent = p.nom;

        var info = document.createElement('span');
        info.className = 'sugg__info';
        var stock = document.createElement('small');
        stock.textContent = p.stock === 0 ? 'Rupture' : 'Stock : ' + p.stock;
        info.appendChild(stock);

        item.appendChild(img);
        item.appendChild(nom);
        item.appendChild(info);
        return item;
    }

    function afficher(champ) {
        champActif = champ;
        resultats = chercher(champ.value);
        boite.innerHTML = '';
        if (resultats.length === 0) {
            var vide = document.createElement('div');
            vide.className = 'sugg__vide';
            vide.textContent = 'Aucun produit trouvé';
            boite.appendChild(vide);
        }
        resultats.forEach(function (p, i) { boite.appendChild(creerItem(p, i)); });
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

    // Met l'id du produit dans le champ caché si le texte correspond exactement à un nom
    function synchroniser(champ) {
        var t = champ.value.trim().toLowerCase();
        var trouve = null;
        catalogue.forEach(function (p) { if (p.nom.toLowerCase() === t) { trouve = p; } });
        champ.closest('tr.ligne').querySelector('.produit-id').value = trouve ? trouve.id : '';
        return trouve;
    }

    function estSaisie(el) { return el.classList && el.classList.contains('produit-saisie'); }

    // ---------- Événements ----------
    // Dès que l'utilisateur modifie quelque chose, le compteur d'erreurs du serveur est périmé
    function oublierErreurs() { erreursServeur = 0; }
    formulaire.addEventListener('input', oublierErreurs, true);
    formulaire.addEventListener('change', oublierErreurs, true);

    selectGrossiste.addEventListener('change', recalculer);

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

    // Clics : mode de la ligne, boutons − et +, retrait d'une ligne
    corps.addEventListener('click', function (e) {
        var bascule = e.target.closest('.mode-ligne__btn');
        if (bascule) {
            var ligne = bascule.closest('tr.ligne');
            ligne.querySelector('.mode-valeur').value = bascule.dataset.mode;
            appliquerMode(ligne);
            var cible = ligne.querySelector(bascule.dataset.mode === 'true' ? '.nouveau-nom' : '.produit-saisie');
            if (cible) { cible.focus(); }
            recalculer();
            return;
        }

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
            // Dernière ligne : on la remet à zéro au lieu de la supprimer
            tr.querySelector('.mode-valeur').value = 'false';
            tr.querySelectorAll('input[type=text], input[type=date], input[type=hidden].produit-id, .prix-achat, .bloc-nouveau input[type=number]')
                .forEach(function (c) { c.value = ''; c.classList.remove('is-invalid'); });
            tr.querySelector('.bloc-nouveau select').value = '';
            tr.querySelector('.bloc-nouveau input[name$="seuilAlerte"]').value = 5;
            tr.querySelector('.quantite').value = 1;
            appliquerMode(tr);
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
        var tr = corps.lastElementChild;
        appliquerMode(tr);
        tr.querySelector('.produit-saisie').focus();
        recalculer();
    });

    // Une validation à la fois : évite d'enregistrer deux fois la même livraison par un double clic
    formulaire.addEventListener('submit', function () { btnValider.disabled = true; });
    window.addEventListener('pageshow', recalculer);

    // Après un refus du serveur : réaffiche le mode et le nom des produits déjà choisis
    corps.querySelectorAll('tr.ligne').forEach(function (tr) {
        var p = parId[tr.querySelector('.produit-id').value];
        if (p) { tr.querySelector('.produit-saisie').value = p.nom; }
        appliquerMode(tr);
    });

    recalculer();
})();