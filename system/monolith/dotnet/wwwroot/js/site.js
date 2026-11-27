// Please see documentation at https://docs.microsoft.com/aspnet/core/client-side/bundling-and-minification
// for details on configuring this project to bundle and minify static web assets.

// Write your JavaScript code.

// A product photo that fails to load falls back to the neutral placeholder (the img goes away, as in frontend-react's
// ProductImage): the placeholder is only a defensive fallback, never the normal look.
(function () {
    var PACKAGE_ICON = '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"/><path d="M12 22V12"/><polyline points="3.29 7 12 12 20.71 7"/><path d="m7.5 4.27 9 5.15"/></svg>';

    function showPlaceholder(img) {
        var box = img.closest('.product-image');
        if (!box || box.classList.contains('product-image-placeholder')) return;
        box.classList.add('product-image-placeholder');
        if (img.alt) {
            box.setAttribute('role', 'img');
            box.setAttribute('aria-label', img.alt);
        } else {
            box.setAttribute('aria-hidden', 'true');
        }
        box.innerHTML = PACKAGE_ICON;
    }

    // error does not bubble, so listen while it travels down.
    document.addEventListener('error', function (event) {
        if (event.target && event.target.tagName === 'IMG') showPlaceholder(event.target);
    }, true);

    // A photo that already failed before this script ran.
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.product-image img').forEach(function (img) {
            if (img.complete && img.naturalWidth === 0) showPlaceholder(img);
        });
    });
})();
