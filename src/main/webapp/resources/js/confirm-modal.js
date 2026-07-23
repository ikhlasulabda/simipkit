/**
 * confirm-modal.js
 * Custom reusable confirmation modal for SIMIPKIT (Old Money Theme).
 * Intercepts elements with class .btn-confirm-action and submits POST forms securely.
 */
document.addEventListener('DOMContentLoaded', function () {
    if (!document.getElementById('simipkit-confirm-modal')) {
        var modalHtml = 
            '<div id="simipkit-confirm-modal" class="modal-backdrop" style="display: none;">' +
                '<div class="modal-box">' +
                    '<div class="modal-header">' +
                        '<h3 id="modal-confirm-title" class="modal-title">KONFIRMASI HAPUS</h3>' +
                    '</div>' +
                    '<div class="modal-body">' +
                        '<p id="modal-confirm-message">Apakah Anda yakin ingin menghapus data ini?</p>' +
                    '</div>' +
                    '<div class="modal-actions">' +
                        '<button id="modal-btn-cancel" type="button" class="btn btn-secondary">Batal</button>' +
                        '<button id="modal-btn-proceed" type="button" class="btn btn-danger">Ya, Hapus Data</button>' +
                    '</div>' +
                '</div>' +
            '</div>';
        document.body.insertAdjacentHTML('beforeend', modalHtml);
    }

    var modal = document.getElementById('simipkit-confirm-modal');
    var titleEl = document.getElementById('modal-confirm-title');
    var messageEl = document.getElementById('modal-confirm-message');
    var cancelBtn = document.getElementById('modal-btn-cancel');
    var proceedBtn = document.getElementById('modal-btn-proceed');

    var currentTargetUrl = '';

    function closeModal() {
        if (modal) modal.style.display = 'none';
        currentTargetUrl = '';
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', closeModal);
    }

    if (modal) {
        modal.addEventListener('click', function (e) {
            if (e.target === modal) closeModal();
        });
    }

    if (proceedBtn) {
        proceedBtn.addEventListener('click', function () {
            if (currentTargetUrl) {
                var form = document.createElement('form');
                form.method = 'POST';
                form.action = currentTargetUrl;
                document.body.appendChild(form);
                form.submit();
            }
        });
    }

    document.addEventListener('click', function (e) {
        var target = e.target.closest('.btn-confirm-action');
        if (target) {
            e.preventDefault();
            var title = target.getAttribute('data-title') || 'KONFIRMASI HAPUS';
            var message = target.getAttribute('data-message') || 'Apakah Anda yakin ingin menghapus data ini?';
            var url = target.getAttribute('data-url') || target.getAttribute('href');

            if (titleEl) titleEl.textContent = title;
            if (messageEl) messageEl.textContent = message;
            currentTargetUrl = url;

            if (modal) modal.style.display = 'flex';
        }
    });
});
