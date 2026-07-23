/**
 * template-modal.js
 * Handles XML template example modal, template selection popup, fetch+blob download,
 * and fallback toast notifications for SIMIPKIT report generation.
 */
(function () {
    'use strict';

    // ====================================================================
    // Toast Notification Banner (Old Money Palette)
    // ====================================================================
    function showToastNotification(message) {
        var existingToast = document.querySelector('.toast-notification');
        if (existingToast) existingToast.remove();

        var toast = document.createElement('div');
        toast.className = 'toast-notification';
        toast.innerHTML = '<span>⚠️ ' + escapeHtml(message) + '</span>';

        document.body.appendChild(toast);

        setTimeout(function () {
            toast.classList.add('toast-fade-out');
            setTimeout(function () {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 400);
        }, 4000);
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;")
                   .replace(/</g, "&lt;")
                   .replace(/>/g, "&gt;")
                   .replace(/"/g, "&quot;")
                   .replace(/'/g, "&#039;");
    }

    // ====================================================================
    // Fetch + Blob PDF Download Handler
    // ====================================================================
    function downloadPdfWithFetch(pdfBaseUrl, templateId, templateName) {
        var targetUrl = pdfBaseUrl;
        if (templateId) {
            targetUrl += (targetUrl.indexOf('?') >= 0 ? '&' : '?') + 'templateId=' + encodeURIComponent(templateId);
        }

        // Show loading state
        var btnUnduh = document.getElementById('btn-unduh-pdf');
        var originalBtnText = btnUnduh ? btnUnduh.innerHTML : 'Unduh PDF';
        if (btnUnduh) {
            btnUnduh.disabled = true;
            btnUnduh.innerHTML = 'Memproses PDF...';
        }

        fetch(targetUrl)
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Gagal mengunduh PDF (Status ' + response.status + ')');
                }

                var templateApplied = response.headers.get('X-Template-Applied');
                var fallbackReason = response.headers.get('X-Template-Fallback-Reason');

                if (templateApplied === 'false' && fallbackReason) {
                    var displayMsg = 'Template ' + (templateName ? '"' + templateName + '"' : '') + ' error, fallback diterapkan ke default.';
                    if (fallbackReason.indexOf('tidak ditemukan') >= 0) {
                        displayMsg = fallbackReason + ', fallback diterapkan ke default.';
                    }
                    showToastNotification(displayMsg);
                }

                return response.blob();
            })
            .then(function (blob) {
                var url = window.URL.createObjectURL(blob);
                var a = document.createElement('a');
                a.style.display = 'none';
                a.href = url;
                a.download = 'laporan-portofolio.pdf';
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            })
            .catch(function (err) {
                alert('Error: ' + err.message);
            })
            .finally(function () {
                if (btnUnduh) {
                    btnUnduh.disabled = false;
                    btnUnduh.innerHTML = originalBtnText;
                }
            });
    }

    // ====================================================================
    // 1. Template Selection Popup (report-generate.jsp)
    // ====================================================================
    var btnUnduhPdf = document.getElementById('btn-unduh-pdf');
    if (btnUnduhPdf) {
        btnUnduhPdf.addEventListener('click', function (e) {
            e.preventDefault();
            var pdfBaseUrl = btnUnduhPdf.getAttribute('href');
            var templates = window.REPORT_TEMPLATES || [];

            // If no templates stored in DB, download directly with default layout
            if (!templates || templates.length === 0) {
                downloadPdfWithFetch(pdfBaseUrl, '', '');
                return;
            }

            // Open Template Selection Modal
            openTemplateSelectionModal(pdfBaseUrl, templates);
        });
    }

    function openTemplateSelectionModal(pdfBaseUrl, templates) {
        var existingModal = document.getElementById('modal-select-template');
        if (existingModal) existingModal.remove();

        var backdrop = document.createElement('div');
        backdrop.id = 'modal-select-template';
        backdrop.className = 'modal-backdrop';

        var modalBox = document.createElement('div');
        modalBox.className = 'modal-box';
        modalBox.style.width = '500px';

        var radioHtml = '<label class="template-radio-item active">' +
            '<input type="radio" name="modalSelectedTemplate" value="" data-name="Bawaan (Default)" checked> ' +
            '<div>' +
                '<strong>Template Bawaan (Default)</strong>' +
                '<div style="font-size: 11.5px; color: var(--text-muted);">Layout standar SIMIPKIT</div>' +
            '</div>' +
        '</label>';

        for (var i = 0; i < templates.length; i++) {
            var t = templates[i];
            radioHtml += '<label class="template-radio-item">' +
                '<input type="radio" name="modalSelectedTemplate" value="' + escapeHtml(String(t.id)) + '" data-name="' + escapeHtml(t.nama_template) + '"> ' +
                '<div>' +
                    '<strong>' + escapeHtml(t.nama_template) + '</strong>' +
                    '<div style="font-size: 11.5px; color: var(--text-muted);">ID Template: ' + escapeHtml(String(t.id)) + '</div>' +
                '</div>' +
            '</label>';
        }

        modalBox.innerHTML =
            '<div class="modal-header">' +
                '<div class="modal-title">Pilih Template Laporan PDF</div>' +
            '</div>' +
            '<div class="modal-body">' +
                '<p style="margin-bottom: 12px;">Pilih layout template yang akan diterapkan pada dokumen PDF:</p>' +
                '<div class="template-radio-group">' + radioHtml + '</div>' +
            '</div>' +
            '<div class="modal-actions">' +
                '<button type="button" class="btn btn-secondary" id="btn-cancel-select-template">Batal</button>' +
                '<button type="button" class="btn btn-pdf" id="btn-confirm-select-template">Unduh PDF</button>' +
            '</div>';

        backdrop.appendChild(modalBox);
        document.body.appendChild(backdrop);

        // Highlight active radio selection
        var radioItems = modalBox.querySelectorAll('.template-radio-item input');
        radioItems.forEach(function (radio) {
            radio.addEventListener('change', function () {
                modalBox.querySelectorAll('.template-radio-item').forEach(function (item) {
                    item.classList.remove('active');
                });
                if (radio.checked) {
                    radio.closest('.template-radio-item').classList.add('active');
                }
            });
        });

        document.getElementById('btn-cancel-select-template').addEventListener('click', function () {
            backdrop.remove();
        });

        document.getElementById('btn-confirm-select-template').addEventListener('click', function () {
            var selectedRadio = modalBox.querySelector('input[name="modalSelectedTemplate"]:checked');
            var templateId = selectedRadio ? selectedRadio.value : '';
            var templateName = selectedRadio ? selectedRadio.getAttribute('data-name') : '';

            backdrop.remove();
            downloadPdfWithFetch(pdfBaseUrl, templateId, templateName);
        });
    }

    // ====================================================================
    // 2. "Lihat Contoh Template" Modal (report-template-upload.jsp)
    // ====================================================================
    var btnShowExample = document.getElementById('btn-show-example-modal');
    if (btnShowExample) {
        btnShowExample.addEventListener('click', function (e) {
            e.preventDefault();
            openExampleTemplateModal();
        });
    }

    function openExampleTemplateModal() {
        var existingModal = document.getElementById('modal-example-template');
        if (existingModal) existingModal.remove();

        var backdrop = document.createElement('div');
        backdrop.id = 'modal-example-template';
        backdrop.className = 'modal-backdrop';

        var modalBox = document.createElement('div');
        modalBox.className = 'modal-box';
        modalBox.style.width = '580px';
        modalBox.style.maxWidth = '95%';

        var exampleXml = '<reportTemplate>\n' +
            '    <title>Laporan Portofolio Klien</title>\n' +
            '    <subtitle>SIMIPKIT - Sistem Informasi Manajemen Investasi dan Portofolio Klien</subtitle>\n' +
            '    <showClientInfo>true</showClientInfo>\n' +
            '    <showGeneratedDate>true</showGeneratedDate>\n' +
            '    <table>\n' +
            '        <columns>\n' +
            '            <column>jenisInstrumen</column>\n' +
            '            <column>namaInstrumen</column>\n' +
            '            <column>jumlah</column>\n' +
            '            <column>nilai</column>\n' +
            '            <column>allocationPercent</column>\n' +
            '        </columns>\n' +
            '    </table>\n' +
            '    <footerNote>Dokumen ini digenerate secara otomatis oleh sistem SIMIPKIT.</footerNote>\n' +
            '</reportTemplate>';

        modalBox.innerHTML =
            '<div class="modal-header">' +
                '<div class="modal-title">Contoh Skema XML Template Laporan</div>' +
            '</div>' +
            '<div class="modal-body" style="max-height: 420px; overflow-y: auto;">' +
                '<p style="margin-bottom: 8px;">Gunakan struktur XML berikut untuk mengkustomisasi layout PDF:</p>' +
                '<pre class="mono xml-example-code">' + escapeHtml(exampleXml) + '</pre>' +
                '<div style="font-size: 12px; margin-top: 12px; line-height: 1.6; color: var(--text-muted);">' +
                    '<strong style="color: var(--text-primary);">Keterangan Field:</strong>' +
                    '<ul style="padding-left: 18px; margin-top: 4px;">' +
                        '<li><code>title</code>: Judul utama di bagian paling atas dokumen PDF.</li>' +
                        '<li><code>subtitle</code>: Subjudul di bawah judul utama.</li>' +
                        '<li><code>showClientInfo</code>: Set <code>true</code> / <code>false</code> untuk menampilkan tabel data klien.</li>' +
                        '<li><code>showGeneratedDate</code>: Set <code>true</code> / <code>false</code> untuk menampilkan tanggal buat.</li>' +
                        '<li><code>table/columns/column</code>: Urutan kolom tabel rincian instrumen.<br>' +
                            'Key valid: <code>jenisInstrumen</code>, <code>namaInstrumen</code>, <code>jumlah</code>, <code>nilai</code>, <code>allocationPercent</code>.</li>' +
                        '<li><code>footerNote</code>: Catatan kaki di bagian paling bawah PDF.</li>' +
                    '</ul>' +
                '</div>' +
            '</div>' +
            '<div class="modal-actions">' +
                '<button type="button" class="btn btn-secondary" id="btn-close-example">Tutup</button>' +
                '<button type="button" class="btn btn-primary" id="btn-copy-example">Salin ke Form</button>' +
            '</div>';

        backdrop.appendChild(modalBox);
        document.body.appendChild(backdrop);

        document.getElementById('btn-close-example').addEventListener('click', function () {
            backdrop.remove();
        });

        document.getElementById('btn-copy-example').addEventListener('click', function () {
            var textarea = document.getElementById('xmlContent');
            if (textarea) {
                textarea.value = exampleXml;
                textarea.focus();
            }
            backdrop.remove();
        });
    }

})();
