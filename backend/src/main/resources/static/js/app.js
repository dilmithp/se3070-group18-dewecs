/*
 * Optional progressive enhancement: a confirm dialog before a destructive action.
 * Mark a button or link with data-confirm="Are you sure?". Without JavaScript the form simply posts as before.
 */
document.addEventListener('click', function (event) {
    var control = event.target.closest('[data-confirm]');
    if (control && !window.confirm(control.getAttribute('data-confirm'))) {
        event.preventDefault();
    }
});

/*
 * "By District" / "By River Basin" on the issue page: only the chosen area's list is enabled.
 * Without JavaScript both lists stay enabled and the radio button decides which one the server uses.
 */
(function () {
    function syncArea() {
        var chosen = document.querySelector('input[name="areaMode"]:checked');
        if (!chosen) {
            return;
        }
        document.querySelectorAll('select[data-area]').forEach(function (select) {
            select.disabled = select.getAttribute('data-area') !== chosen.value;
        });
    }
    document.addEventListener('change', function (event) {
        if (event.target && event.target.name === 'areaMode') {
            syncArea();
        }
    });
    syncArea();
})();

/*
 * Relief command center: remember the stock of the chosen supply in a hidden field, so the server can tell that
 * another officer dispatched from it in the meantime. Without JavaScript that check is simply skipped.
 */
(function () {
    var select = document.querySelector('select[name="resourceId"]');
    var hidden = document.querySelector('input[name="expectedStock"]');
    if (!select || !hidden) {
        return;
    }
    function sync() {
        var option = select.options[select.selectedIndex];
        hidden.value = option && option.getAttribute('data-stock') ? option.getAttribute('data-stock') : '';
    }
    select.addEventListener('change', sync);
    sync();
})();

/*
 * District coordination: an occupancy update made while the device is offline is kept in this browser (localStorage)
 * with the time it was made, and replayed in time order when the connection returns. The server flags an update that is
 * older than the latest one. Without JavaScript the form is an ordinary post.
 */
(function () {
    var KEY = 'dewecs.queuedOccupancy';
    var banner = document.getElementById('pending-sync');
    var forms = document.querySelectorAll('form.occupancy-form');
    if (!forms.length && !banner) {
        return;
    }

    function load() {
        try { return JSON.parse(window.localStorage.getItem(KEY) || '[]'); } catch (e) { return []; }
    }
    function save(list) {
        try { window.localStorage.setItem(KEY, JSON.stringify(list)); } catch (e) { /* storage blocked */ }
        if (banner) { banner.hidden = list.length === 0; }
    }
    function stamp() {
        var d = new Date();
        d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
        return d.toISOString().slice(0, 19);
    }
    function flush() {
        var list = load();
        if (!list.length || !navigator.onLine) { return; }
        var body = new URLSearchParams();
        body.set('actions', list.map(function (a) { return a.shelterId + ',' + a.occupancy + ',' + a.time; }).join('\n'));
        fetch('/coordination/sync', { method: 'POST', body: body, headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.ok ? r.json() : Promise.reject(); })
            .then(function (result) {
                save([]);
                if (banner) {
                    banner.textContent = 'Synchronised: ' + result.applied + ' applied, ' + result.conflicts +
                        ' flagged for review, ' + result.rejected + ' rejected. Reload to see the latest picture.';
                    banner.hidden = false;
                }
            })
            .catch(function () { /* still offline: keep the queue */ });
    }

    forms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (navigator.onLine) { return; }
            event.preventDefault();
            var action = form.getAttribute('action').match(/shelters\/(\d+)\//);
            var value = parseInt(form.elements['occupancy'].value, 10);
            if (!action || isNaN(value) || value < 0) { return; }
            var list = load();
            list.push({ shelterId: action[1], occupancy: value, time: stamp() });
            save(list);
        });
    });
    window.addEventListener('online', flush);
    save(load());
    flush();
})();
