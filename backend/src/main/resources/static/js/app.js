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
