/**
 * bank-sync-reconciliation.js
 * Modal JSON Viewer, Reconciliation Popup (Loading -> Positif/Negatif -> Sync),
 * dan Live Full-Text Search pada Raw JSON Payload untuk SIMIPKIT.
 */

var contextPath = '';

document.addEventListener('DOMContentLoaded', function () {
    // Detect context path from body or link tags
    var brandLink = document.querySelector('a.brand');
    if (brandLink) {
        var href = brandLink.getAttribute('href');
        if (href && href !== '/') {
            contextPath = href.replace(/\/$/, '');
        }
    }

    injectReconciliationModals();
});

function injectReconciliationModals() {
    if (!document.getElementById('simipkit-json-modal')) {
        var jsonModalHtml =
            '<div id="simipkit-json-modal" class="modal-backdrop" style="display: none;">' +
                '<div class="modal-box modal-box-terminal">' +
                    '<div class="json-expand-header">' +
                        '<span class="json-expand-title" id="json-modal-title">CONSOLE OUTPUT // PAYLOAD JSON RAW</span>' +
                        '<button type="button" class="btn-copy-json" id="btn-modal-copy-raw">Copy Raw</button>' +
                    '</div>' +
                    '<pre class="json-pre-block"><code class="json-formatted mono" id="json-modal-content"></code></pre>' +
                    '<div class="modal-actions mt-20">' +
                        '<button type="button" class="btn btn-secondary btn-sm" onclick="closeJsonModal()">Tutup</button>' +
                    '</div>' +
                '</div>' +
            '</div>';
        document.body.insertAdjacentHTML('beforeend', jsonModalHtml);
    }

    if (!document.getElementById('simipkit-recon-modal')) {
        var reconModalHtml =
            '<div id="simipkit-recon-modal" class="modal-backdrop" style="display: none;">' +
                '<div class="modal-box" id="recon-modal-box">' +
                    '<div id="recon-state-loading" style="display: none;">' +
                        '<div class="json-expand-header">' +
                            '<span class="json-expand-title">> RECONCILIATION ENGINE // PROCESSING</span>' +
                        '</div>' +
                        '<div class="terminal-loading-box">' +
                            '<div id="terminal-lines"></div>' +
                            '<span class="terminal-cursor">_</span>' +
                        '</div>' +
                    '</div>' +

                    '<div id="recon-state-positive" style="display: none;">' +
                        '<div class="recon-doc-header">' +
                            '<h3 class="recon-doc-title">HASIL PENCOCOKAN REKENING</h3>' +
                            '<span class="badge badge-event-sue">MATCHED</span>' +
                        '</div>' +
                        '<div class="recon-doc-body" id="recon-positive-content"></div>' +
                        '<div class="modal-actions mt-20">' +
                            '<button type="button" class="btn btn-secondary" onclick="closeReconModal()">Batal</button>' +
                            '<button type="button" class="btn btn-primary" id="btn-execute-sync">Sync ke Database</button>' +
                        '</div>' +
                    '</div>' +

                    '<div id="recon-state-negative" style="display: none;">' +
                        '<div class="recon-failed-header">' +
                            '<h3 class="recon-failed-title">REKENING TIDAK DITEMUKAN</h3>' +
                        '</div>' +
                        '<div class="recon-doc-body" id="recon-negative-content"></div>' +
                        '<div class="modal-actions mt-20">' +
                            '<button type="button" class="btn btn-secondary" onclick="closeReconModal()">Tutup</button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        document.body.insertAdjacentHTML('beforeend', reconModalHtml);
    }
}

// ===== 1. MODAL JSON VIEWER =====
var currentRawJsonText = '';

function openJsonModal(btnOrEventId, rawJsonOrEventId) {
    var eventId = null;
    var rawJsonString = '';

    if (typeof btnOrEventId === 'object' && btnOrEventId !== null) {
        // Dipanggil via openJsonModal(this, eventId)
        eventId = rawJsonOrEventId;
        var row = btnOrEventId.closest('tr');
        var rawSourceElem = row ? row.querySelector('.json-raw-source') : null;
        rawJsonString = rawSourceElem ? (rawSourceElem.textContent || rawSourceElem.innerText) : '';
    } else {
        // Dipanggil via openJsonModal(eventId, rawJsonString)
        eventId = btnOrEventId;
        rawJsonString = rawJsonOrEventId || '';
    }

    currentRawJsonText = rawJsonString;
    var modal = document.getElementById('simipkit-json-modal');
    var titleEl = document.getElementById('json-modal-title');
    var contentEl = document.getElementById('json-modal-content');
    var copyBtn = document.getElementById('btn-modal-copy-raw');

    if (titleEl) titleEl.textContent = 'CONSOLE OUTPUT // PAYLOAD JSON RAW (EVENT ID: ' + eventId + ')';

    try {
        var parsed = JSON.parse(rawJsonString);
        if (contentEl) contentEl.textContent = JSON.stringify(parsed, null, 2);
    } catch (e) {
        if (contentEl) contentEl.textContent = rawJsonString;
    }

    if (copyBtn) {
        copyBtn.onclick = function () {
            copyTextToClipboard(currentRawJsonText, copyBtn);
        };
    }

    if (modal) modal.style.display = 'flex';
}

function closeJsonModal() {
    var modal = document.getElementById('simipkit-json-modal');
    if (modal) modal.style.display = 'none';
}

function copyTextToClipboard(text, btn) {
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(function () {
            showCopyFeedbackBtn(btn);
        }).catch(function () {
            fallbackCopy(text, btn);
        });
    } else {
        fallbackCopy(text, btn);
    }
}

function fallbackCopy(text, btn) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    try {
        document.execCommand('copy');
        showCopyFeedbackBtn(btn);
    } catch (err) {}
    document.body.removeChild(ta);
}

function showCopyFeedbackBtn(btn) {
    var orig = btn.textContent;
    btn.textContent = 'Tersalin!';
    btn.style.backgroundColor = 'var(--terminal-accent)';
    btn.style.color = '#000000';
    setTimeout(function () {
        btn.textContent = orig;
        btn.style.backgroundColor = '';
        btn.style.color = '';
    }, 1500);
}

// ===== 2. ALUR RECONCILIATION (MATCH & SYNC) =====
var currentReconEventId = null;

function openMatchModal(eventId) {
    currentReconEventId = eventId;
    var modal = document.getElementById('simipkit-recon-modal');
    var loadingState = document.getElementById('recon-state-loading');
    var posState = document.getElementById('recon-state-positive');
    var negState = document.getElementById('recon-state-negative');
    var linesContainer = document.getElementById('terminal-lines');

    if (!modal || !loadingState || !posState || !negState) return;

    // Reset states
    posState.style.display = 'none';
    negState.style.display = 'none';
    loadingState.style.display = 'block';
    if (linesContainer) linesContainer.innerHTML = '';
    modal.style.display = 'flex';

    var stepLines = [
        "> Membaca nomor rekening dari payload...",
        "> Menghubungi database klien...",
        "> Mencocokkan nomor rekening ke rekening klien terdaftar..."
    ];

    var stepIdx = 0;
    var interval = setInterval(function () {
        if (stepIdx < stepLines.length) {
            var div = document.createElement('div');
            div.className = 'terminal-line';
            div.textContent = stepLines[stepIdx];
            linesContainer.appendChild(div);
            stepIdx++;
        } else {
            clearInterval(interval);
            // Call match endpoint
            fetchMatchResult(eventId);
        }
    }, 700);
}

function fetchMatchResult(eventId) {
    var url = contextPath + '/bank-sync-log/' + eventId + '/match';
    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
    .then(function (resp) {
        return resp.json();
    })
    .then(function (data) {
        var loadingState = document.getElementById('recon-state-loading');
        if (loadingState) loadingState.style.display = 'none';

        if (data.reconciliationStatus === 'MATCHED') {
            renderPositiveMatch(data);
        } else {
            renderNegativeMatch(data);
        }
    })
    .catch(function (err) {
        var loadingState = document.getElementById('recon-state-loading');
        if (loadingState) loadingState.style.display = 'none';

        renderNegativeMatch({
            errorMessage: "Gagal terhubung ke server atau terjadi kesalahan: " + err.message
        });
    });
}

function renderPositiveMatch(data) {
    var posState = document.getElementById('recon-state-positive');
    var contentEl = document.getElementById('recon-positive-content');
    var syncBtn = document.getElementById('btn-execute-sync');

    if (!posState || !contentEl) return;

    var html = '';
    if (data.clientPrimary) {
        html += buildClientCardHtml('Klien Terdaftar (Primary)', data.clientPrimary);
    }
    if (data.clientSecondary) {
        html += buildClientCardHtml('Klien Terdaftar (Tujuan / Secondary)', data.clientSecondary);
    }
    if (data.unmatchedRekeningPrimary && !data.clientPrimary) {
        html += '<p class="recon-note">Rekening Pengirim [' + escapeHtml(data.unmatchedRekeningPrimary) + ']: Di luar sistem SIMIPKIT (tidak terpengaruh)</p>';
    }
    if (data.unmatchedRekeningSecondary && !data.clientSecondary) {
        html += '<p class="recon-note">Rekening Tujuan [' + escapeHtml(data.unmatchedRekeningSecondary) + ']: Di luar sistem SIMIPKIT (tidak terpengaruh)</p>';
    }

    if (data.totalBiayaSettlement) {
        html += '<div class="recon-impact-box">';
        html += '<strong>Dampak Sinkronisasi Settlement:</strong>';
        html += '<ul style="margin-top: 6px; margin-left: 18px; line-height: 1.6;">';
        html += '<li>Akan menambahkan/meng-update <strong>' + formatNumber(data.jumlahUnit) + ' unit ' + escapeHtml(data.kodeInstrumen) + '</strong> ke portofolio</li>';
        html += '<li>Akan mengurangi saldo RDN sebesar: <strong>Rp ' + formatNumber(data.totalBiayaSettlement) + '</strong> (' + formatNumber(data.jumlahUnit) + ' unit × Rp ' + formatNumber(data.hargaSettlement) + ')</li>';
        if (typeof data.saldoRdnSaatIni !== 'undefined') {
            html += '<li>Saldo RDN saat ini: <strong>Rp ' + formatNumber(data.saldoRdnSaatIni) + '</strong></li>';
        }
        html += '</ul>';

        if (data.isSaldoCukup === false) {
            html += '<div class="recon-warning-box mt-10">';
            html += '<strong>⚠️ Peringatan Saldo RDN Tidak Mencukupi:</strong> Saldo RDN saat ini (Rp ' + formatNumber(data.saldoRdnSaatIni) + ') lebih kecil dari total biaya settlement (Rp ' + formatNumber(data.totalBiayaSettlement) + '). Eksekusi sync akan ditolak oleh sistem.';
            html += '</div>';
        }
        html += '</div>';
    } else if (data.previewMessage) {
        html += '<div class="recon-impact-box"><strong>Dampak Sinkronisasi:</strong> ' + escapeHtml(data.previewMessage) + '</div>';
    }

    contentEl.innerHTML = html;
    posState.style.display = 'block';

    if (syncBtn) {
        syncBtn.onclick = function () {
            executeSync(data.eventId);
        };
    }
}

function buildClientCardHtml(title, client) {
    return '<div class="recon-client-card">' +
                '<h4>' + escapeHtml(title) + '</h4>' +
                '<p><strong>Nama:</strong> ' + escapeHtml(client.nama) + '</p>' +
                '<p><strong>ID Klien:</strong> <span class="mono">' + escapeHtml(client.id) + '</span></p>' +
                '<p><strong>Status KYC:</strong> <span class="status-tag">' + escapeHtml(client.statusKyc) + '</span></p>' +
                '<p><strong>No. Rekening RDN:</strong> <span class="mono">' + escapeHtml(client.nomorRekening || '-') + '</span></p>' +
                '<p><strong>Saldo RDN saat ini:</strong> Rp ' + formatNumber(client.saldoRdn) + '</p>' +
            '</div>';
}

function renderNegativeMatch(data) {
    var negState = document.getElementById('recon-state-negative');
    var contentEl = document.getElementById('recon-negative-content');
    if (!negState || !contentEl) return;

    var msg = 'Nomor rekening pada payload event ini tidak ditemukan di database klien SIMIPKIT.';
    if (data.unmatchedRekeningPrimary) {
        msg = 'Nomor rekening [' + escapeHtml(data.unmatchedRekeningPrimary) + '] tidak ditemukan di database klien SIMIPKIT.';
    }
    if (data.errorMessage) {
        msg = data.errorMessage;
    }

    contentEl.innerHTML = '<p class="recon-failed-msg">' + msg + '</p>' +
                          '<p class="recon-failed-sub">Status rekonsiliasi ditandai sebagai FAILED. Anda dapat menambahkan nomor rekening ini ke data klien melalui menu Manajemen Klien lalu mencoba lagi.</p>';
    negState.style.display = 'block';
}

function executeSync(eventId) {
    var syncBtn = document.getElementById('btn-execute-sync');
    if (syncBtn) {
        syncBtn.disabled = true;
        syncBtn.textContent = 'Proses Sync...';
    }

    // Clear previous inline error box if any
    var oldErrBox = document.getElementById('recon-sync-error-box');
    if (oldErrBox) oldErrBox.remove();

    var url = contextPath + '/bank-sync-log/' + eventId + '/sync';
    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
    .then(function (resp) {
        if (!resp.ok) {
            return resp.json().then(function (errData) {
                throw new Error(errData.error || ('HTTP Error ' + resp.status));
            });
        }
        return resp.json();
    })
    .then(function (data) {
        closeReconModal();
        // Update table row badge
        updateTableRowSynced(eventId, data.detail);
    })
    .catch(function (err) {
        if (syncBtn) {
            syncBtn.disabled = false;
            syncBtn.textContent = 'Sync ke Database';
        }

        var contentEl = document.getElementById('recon-positive-content');
        if (contentEl) {
            var errDiv = document.createElement('div');
            errDiv.id = 'recon-sync-error-box';
            errDiv.className = 'recon-error-box mt-10';
            errDiv.innerHTML = '<strong>Gagal Sinkronisasi:</strong> ' + escapeHtml(err.message);
            contentEl.appendChild(errDiv);
        }
    });
}

function updateTableRowSynced(eventId, detail) {
    var row = document.querySelector('tr[data-event-id="' + eventId + '"]');
    if (!row) {
        // Fallback reload if row selector not found
        window.location.reload();
        return;
    }

    var reconCell = row.querySelector('.col-recon-cell');
    if (reconCell) {
        var now = new Date();
        var timeStr = now.getHours() + ':' + (now.getMinutes() < 10 ? '0' : '') + now.getMinutes();
        reconCell.innerHTML = '<span class="badge badge-recon-synced" title="' + escapeHtml(detail || '') + '">Tersinkronisasi (' + timeStr + ')</span>';
    }
}

function closeReconModal() {
    var modal = document.getElementById('simipkit-recon-modal');
    if (modal) modal.style.display = 'none';
}

// ===== 3. LIVE FULL-TEXT SEARCH DI RAW JSON PAYLOAD =====
var searchDebounceTimer = null;

function searchBankSyncPayload(inputEl) {
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(function () {
        var query = inputEl.value.toLowerCase().trim();
        var table = document.getElementById('tbl-bank-sync-log');
        if (!table) return;

        var rows = table.tBodies[0].querySelectorAll('tr.log-row');
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            var payloadCell = row.querySelector('.col-payload-raw');
            var rawText = payloadCell ? (payloadCell.textContent || payloadCell.innerText).toLowerCase() : '';
            var matches = (rawText.indexOf(query) > -1);
            row.style.display = matches ? '' : 'none';
        }
    }, 300);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatNumber(num) {
    if (!num) return '0';
    return Number(num).toLocaleString('id-ID');
}
