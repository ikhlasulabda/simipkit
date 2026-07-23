/**
 * idle-timer.js
 * Automatically logs out user after 7 minutes of inactivity (no mouse, keypress, click, scroll, touch).
 */
(function () {
    var IDLE_TIMEOUT_MS = 7 * 60 * 1000; // 7 minutes
    var idleTimer = null;

    var currentScript = document.currentScript;
    var logoutUrl = currentScript ? currentScript.getAttribute('data-logout-url') : null;

    if (!logoutUrl) {
        var contextPath = window.location.pathname.split('/')[1];
        var basePath = window.location.origin + (contextPath ? '/' + contextPath : '');
        logoutUrl = basePath + '/logout?reason=timeout';
    }

    function resetTimer() {
        if (idleTimer) clearTimeout(idleTimer);
        idleTimer = setTimeout(function () {
            window.location.href = logoutUrl;
        }, IDLE_TIMEOUT_MS);
    }

    var activityEvents = ['mousemove', 'mousedown', 'keypress', 'keydown', 'scroll', 'touchstart', 'click'];
    activityEvents.forEach(function (evt) {
        window.addEventListener(evt, resetTimer, true);
    });

    resetTimer();
})();
