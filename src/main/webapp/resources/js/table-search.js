/**
 * table-search.js
 * Reusable client-side table row filter untuk SIMIPKIT.
 *
 * Penggunaan di JSP:
 *   <input oninput="tableSearch(this, 'tbl-id', colIndex)">
 *
 * @param {HTMLInputElement} input    - elemen input yang memicu filter
 * @param {string}           tableId  - id atribut pada elemen <table>
 * @param {number}           colIndex - indeks kolom yang di-filter (0-based)
 */
function tableSearch(input, tableId, colIndex) {
    var query = input.value.toLowerCase().trim();
    var table = document.getElementById(tableId);
    if (!table) return;
    var rows = table.tBodies[0].rows;
    for (var i = 0; i < rows.length; i++) {
        var row = rows[i];
        if (row.classList.contains('json-expand-row')) {
            var prevRow = row.previousElementSibling;
            if (prevRow && prevRow.style.display === 'none') {
                row.style.display = 'none';
            }
            continue;
        }
        var cell = row.cells[colIndex];
        if (cell) {
            var text = (cell.textContent || cell.innerText).toLowerCase();
            var matches = text.indexOf(query) > -1;
            row.style.display = matches ? '' : 'none';
            if (!matches) {
                var nextRow = row.nextElementSibling;
                if (nextRow && nextRow.classList.contains('json-expand-row')) {
                    nextRow.style.display = 'none';
                }
            }
        }
    }
}
