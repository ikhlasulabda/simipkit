/**
 * dashboard-charts.js
 * Initializes all Chart.js charts on the SIMIPKIT dashboard.
 * Data is read from window.DASHBOARD_DATA (embedded server-side by JSP).
 */
(function () {
    'use strict';

    var DATA = window.DASHBOARD_DATA;
    if (!DATA) return;

    // Project palette (from style.css :root variables)
    var PALETTE = [
        '#1c3a2a', // --primary
        '#8a7862', // --accent-muted
        '#2d6a4f', // --status-green
        '#0369a1', // --pdf
        '#8a6420', // --status-amber
        '#7c2020', // --status-red
        '#4a5568'  // --secondary
    ];

    var KYC_COLORS = {
        'VERIFIED': '#2d6a4f',
        'PENDING':  '#8a6420',
        'REJECTED': '#7c2020'
    };
    var KYC_FALLBACK = '#4a5568';

    // Font matching style.css body
    var FONT_FAMILY = "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";

    // Set Chart.js global defaults
    Chart.defaults.font.family = FONT_FAMILY;
    Chart.defaults.font.size = 12;
    Chart.defaults.color = '#6b6b55'; // --text-muted

    /**
     * Format number as Indonesian Rupiah string (no scientific notation).
     * Example: 10500000 -> "Rp 10.500.000"
     */
    function formatRupiah(val) {
        if (val == null || isNaN(val)) return 'Rp 0';
        var num = Math.round(val);
        var str = Math.abs(num).toString();
        var result = '';
        var count = 0;
        for (var i = str.length - 1; i >= 0; i--) {
            result = str[i] + result;
            count++;
            if (count % 3 === 0 && i > 0) {
                result = '.' + result;
            }
        }
        return 'Rp ' + (num < 0 ? '-' : '') + result;
    }

    /**
     * Format number with dot thousands separator (no Rp prefix).
     */
    function formatNumber(val) {
        if (val == null || isNaN(val)) return '0';
        var num = Math.round(val);
        var str = Math.abs(num).toString();
        var result = '';
        var count = 0;
        for (var i = str.length - 1; i >= 0; i--) {
            result = str[i] + result;
            count++;
            if (count % 3 === 0 && i > 0) {
                result = '.' + result;
            }
        }
        return (num < 0 ? '-' : '') + result;
    }

    // ====================================================================
    // 1. AUM Growth Trend (Hero Line/Area Chart)
    // ====================================================================
    var trendCtx = document.getElementById('chartAumTrend');
    if (trendCtx && DATA.trendDates && DATA.trendDates.length > 0) {
        var ctx = trendCtx.getContext('2d');
        var gradient = ctx.createLinearGradient(0, 0, 0, 280);
        gradient.addColorStop(0, 'rgba(28, 58, 42, 0.25)');
        gradient.addColorStop(1, 'rgba(28, 58, 42, 0.02)');

        new Chart(trendCtx, {
            type: 'line',
            data: {
                labels: DATA.trendDates,
                datasets: [{
                    label: 'Total AUM Kumulatif',
                    data: DATA.trendValues,
                    borderColor: '#1c3a2a',
                    backgroundColor: gradient,
                    borderWidth: 2.5,
                    pointBackgroundColor: '#264d38',
                    pointBorderColor: '#264d38',
                    pointRadius: 3,
                    pointHoverRadius: 5,
                    fill: true,
                    tension: 0.3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return formatRupiah(context.parsed.y);
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { maxTicksLimit: 8, font: { size: 11 } }
                    },
                    y: {
                        grid: { color: 'rgba(217, 207, 192, 0.5)' },
                        ticks: {
                            callback: function (value) {
                                if (value >= 1000000000) return 'Rp ' + (value / 1000000000).toFixed(1) + ' M';
                                if (value >= 1000000) return 'Rp ' + (value / 1000000).toFixed(1) + ' Jt';
                                if (value >= 1000) return 'Rp ' + (value / 1000).toFixed(0) + ' Rb';
                                return 'Rp ' + value;
                            },
                            font: { size: 11 }
                        }
                    }
                }
            }
        });
    }

    // ====================================================================
    // 2. AUM Allocation by Instrument Type (Donut Chart)
    // ====================================================================
    var instrCtx = document.getElementById('chartInstrumen');
    if (instrCtx && DATA.instrLabels && DATA.instrLabels.length > 0) {
        new Chart(instrCtx, {
            type: 'doughnut',
            data: {
                labels: DATA.instrLabels,
                datasets: [{
                    data: DATA.instrValues,
                    backgroundColor: PALETTE.slice(0, DATA.instrLabels.length),
                    borderColor: '#fffef9',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '60%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { padding: 12, boxWidth: 12, font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                var label = context.label || '';
                                var value = formatRupiah(context.parsed);
                                var total = context.dataset.data.reduce(function (a, b) { return a + b; }, 0);
                                var pct = total > 0 ? ((context.parsed / total) * 100).toFixed(1) : '0.0';
                                return label + ': ' + value + ' (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    // ====================================================================
    // 3. Top 5 Clients by AUM (Horizontal Bar Chart)
    // ====================================================================
    var topCtx = document.getElementById('chartTopClients');
    if (topCtx && DATA.topNames && DATA.topNames.length > 0) {
        new Chart(topCtx, {
            type: 'bar',
            data: {
                labels: DATA.topNames,
                datasets: [{
                    label: 'Total AUM',
                    data: DATA.topValues,
                    backgroundColor: PALETTE.slice(0, DATA.topNames.length),
                    borderColor: 'transparent',
                    borderWidth: 0,
                    borderRadius: 2,
                    barPercentage: 0.7
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return formatRupiah(context.parsed.x);
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { color: 'rgba(217, 207, 192, 0.5)' },
                        ticks: {
                            callback: function (value) {
                                if (value >= 1000000000) return 'Rp ' + (value / 1000000000).toFixed(1) + ' M';
                                if (value >= 1000000) return 'Rp ' + (value / 1000000).toFixed(1) + ' Jt';
                                if (value >= 1000) return 'Rp ' + (value / 1000).toFixed(0) + ' Rb';
                                return 'Rp ' + value;
                            },
                            font: { size: 11 }
                        }
                    },
                    y: {
                        grid: { display: false },
                        ticks: { font: { size: 11 } }
                    }
                }
            }
        });
    }

    // ====================================================================
    // 4. KYC Status Distribution (Bar Chart)
    // ====================================================================
    var kycCtx = document.getElementById('chartKycStatus');
    if (kycCtx && DATA.kycLabels && DATA.kycLabels.length > 0) {
        var kycColors = DATA.kycLabels.map(function (label) {
            return KYC_COLORS[label.toUpperCase()] || KYC_FALLBACK;
        });

        new Chart(kycCtx, {
            type: 'bar',
            data: {
                labels: DATA.kycLabels,
                datasets: [{
                    label: 'Jumlah Klien',
                    data: DATA.kycCounts,
                    backgroundColor: kycColors,
                    borderColor: 'transparent',
                    borderWidth: 0,
                    borderRadius: 2,
                    barPercentage: 0.6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return context.label + ': ' + formatNumber(context.parsed.y) + ' klien';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 11 } }
                    },
                    y: {
                        grid: { color: 'rgba(217, 207, 192, 0.5)' },
                        ticks: {
                            stepSize: 1,
                            font: { size: 11 }
                        }
                    }
                }
            }
        });
    }

    // ====================================================================
    // 5. Document Type Distribution (Donut Chart)
    // ====================================================================
    var docCtx = document.getElementById('chartDocType');
    if (docCtx && DATA.docLabels && DATA.docLabels.length > 0) {
        new Chart(docCtx, {
            type: 'doughnut',
            data: {
                labels: DATA.docLabels,
                datasets: [{
                    data: DATA.docCounts,
                    backgroundColor: PALETTE.slice(0, DATA.docLabels.length),
                    borderColor: '#fffef9',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '60%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { padding: 12, boxWidth: 12, font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                var label = context.label || '';
                                var value = context.parsed;
                                var total = context.dataset.data.reduce(function (a, b) { return a + b; }, 0);
                                var pct = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
                                return label + ': ' + formatNumber(value) + ' dokumen (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

})();
