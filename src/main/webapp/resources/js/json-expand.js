/**
 * SIMIPKIT - JSON Expand / Collapse Utility
 * Provides safe HTML-escaped pretty-printing for JSON payloads in log tables.
 */

function toggleJsonRow(btn) {
    var mainRow = btn.closest('tr');
    if (!mainRow) return;

    var expandRow = mainRow.nextElementSibling;
    if (!expandRow || !expandRow.classList.contains('json-expand-row')) return;

    var isHidden = (expandRow.style.display === 'none' || !expandRow.style.display);
    var textSpan = btn.querySelector('.toggle-text');

    if (isHidden) {
        // Format JSON if not already formatted
        var rawSourceElem = expandRow.querySelector('.json-raw-source');
        var formattedTargetElem = expandRow.querySelector('.json-formatted');

        if (rawSourceElem && formattedTargetElem && !formattedTargetElem.dataset.formatted) {
            var rawText = rawSourceElem.textContent || '';
            try {
                var parsedObj = JSON.parse(rawText);
                formattedTargetElem.textContent = JSON.stringify(parsedObj, null, 2);
            } catch (err) {
                // If not valid JSON, display raw text safely via textContent
                formattedTargetElem.textContent = rawText;
            }
            formattedTargetElem.dataset.formatted = 'true';
        }

        expandRow.style.display = 'table-row';
        btn.classList.add('expanded');
        if (textSpan) textSpan.textContent = 'Tutup';
    } else {
        expandRow.style.display = 'none';
        btn.classList.remove('expanded');
        if (textSpan) textSpan.textContent = 'Lihat Lengkap';
    }
}

function copyJsonPayload(btn) {
    var container = btn.closest('.json-expand-container');
    if (!container) return;

    var codeElem = container.querySelector('.json-formatted') || container.querySelector('.json-raw-source');
    if (!codeElem) return;

    var textToCopy = codeElem.textContent;

    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(textToCopy).then(function() {
            showCopyFeedback(btn);
        }).catch(function() {
            fallbackCopyText(textToCopy, btn);
        });
    } else {
        fallbackCopyText(textToCopy, btn);
    }
}

function fallbackCopyText(text, btn) {
    var textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-9999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
        document.execCommand('copy');
        showCopyFeedback(btn);
    } catch (err) {
        console.error('Fallback copy failed', err);
    }
    document.body.removeChild(textArea);
}

function showCopyFeedback(btn) {
    var originalText = btn.textContent;
    btn.textContent = 'Tersalin!';
    btn.style.backgroundColor = 'var(--terminal-accent)';
    btn.style.color = '#000000';
    btn.style.borderColor = 'var(--terminal-accent)';
    setTimeout(function() {
        btn.textContent = originalText;
        btn.style.backgroundColor = '';
        btn.style.color = '';
        btn.style.borderColor = '';
    }, 1800);
}
