<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Dashboard - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
</head>
<body>
    <header>
        <a href="<c:url value='/'/>" class="brand">SIMIPKIT</a>
        <ul class="nav-links">
            <li><a href="<c:url value='/'/>" class="active">Dashboard</a></li>
            <li><a href="<c:url value='/clients'/>">Klien</a></li>
            <li><a href="<c:url value='/documents/bulk-upload'/>">Bulk Upload KYC</a></li>
            <li><a href="<c:url value='/reports'/>">Laporan</a></li>
            <li><a href="<c:url value='/bank-sync-log'/>">Bank Sync Log</a></li>
            <c:if test="${sessionScope.role == 'admin'}">
                <li><a href="<c:url value='/user-management'/>">User Management</a></li>
                <li><a href="<c:url value='/report-template-upload'/>">Template Laporan</a></li>
            </c:if>
            <li><a href="<c:url value='/audit-log'/>">Audit Log</a></li>
        </ul>
        <div class="user-info">
            <span><c:out value="${sessionScope.user.username}"/></span>
            <span class="role-badge"><c:out value="${sessionScope.role}"/></span>
            <a href="<c:url value='/logout'/>" class="btn btn-sm btn-secondary">Logout</a>
        </div>
    </header>

    <div class="container">
        <div class="page-header">
            <div>
                <h1>Dashboard Utama</h1>
                <p>Ringkasan manajemen investasi dan aktivitas sistem</p>
            </div>
        </div>

        <%-- === 4 STAT CARDS (compacted, original content preserved) === --%>
        <div class="grid-4">
            <div class="stat-card stat-card-compact">
                <div class="label">Total Klien Terdaftar</div>
                <div class="value"><c:out value="${totalClients}"/></div>
            </div>
            <div class="stat-card stat-card-compact">
                <div class="label">Pengguna Sistem</div>
                <div class="value"><c:out value="${totalUsers}"/></div>
            </div>
            <div class="stat-card stat-card-compact">
                <div class="label">Status Server</div>
                <div class="value" style="font-size: 14px; color: var(--status-green);">AKTIF</div>
            </div>
            <div class="stat-card stat-card-compact">
                <div class="label">Log Aktivitas Terbaru</div>
                <div class="value"><c:out value="${recentAuditLogs.size()}"/></div>
            </div>
        </div>

        <%-- === ANALYTICS + AUDIT LOG (side by side) === --%>
        <div class="dashboard-analytics">

            <%-- LEFT: Analytics Charts --%>
            <div class="analytics-column">

                <%-- Hero: AUM Growth Trend --%>
                <div class="chart-card">
                    <div class="chart-header">
                        <div class="chart-title">Trend Pertumbuhan AUM</div>
                        <div class="chart-summary"><c:out value="${totalAum}"/> Total AUM</div>
                    </div>
                    <div class="chart-canvas-wrap chart-canvas-hero">
                        <canvas id="chartAumTrend"></canvas>
                    </div>
                </div>

                <%-- 2x2 Grid of smaller charts --%>
                <div class="chart-grid">

                    <%-- Donut: Instrument allocation --%>
                    <div class="chart-card">
                        <div class="chart-header">
                            <div class="chart-title">Alokasi AUM per Instrumen</div>
                            <div class="chart-summary-sm"><c:out value="${instrLabels.size()}"/> jenis instrumen</div>
                        </div>
                        <div class="chart-canvas-wrap chart-canvas-sm">
                            <canvas id="chartInstrumen"></canvas>
                        </div>
                    </div>

                    <%-- Horizontal Bar: Top 5 clients --%>
                    <div class="chart-card">
                        <div class="chart-header">
                            <div class="chart-title">Top 5 Klien AUM Terbesar</div>
                            <div class="chart-summary-sm"><c:out value="${topNames.size()}"/> klien</div>
                        </div>
                        <div class="chart-canvas-wrap chart-canvas-sm">
                            <canvas id="chartTopClients"></canvas>
                        </div>
                    </div>

                    <%-- Bar: KYC status --%>
                    <div class="chart-card">
                        <div class="chart-header">
                            <div class="chart-title">Distribusi Status KYC</div>
                            <div class="chart-summary-sm"><c:out value="${kycLabels.size()}"/> status</div>
                        </div>
                        <div class="chart-canvas-wrap chart-canvas-sm">
                            <canvas id="chartKycStatus"></canvas>
                        </div>
                    </div>

                    <%-- Donut: Document type --%>
                    <div class="chart-card">
                        <div class="chart-header">
                            <div class="chart-title">Distribusi Jenis Dokumen</div>
                            <div class="chart-summary-sm"><c:out value="${docLabels.size()}"/> jenis</div>
                        </div>
                        <div class="chart-canvas-wrap chart-canvas-sm">
                            <canvas id="chartDocType"></canvas>
                        </div>
                    </div>

                </div>
            </div>

            <%-- RIGHT: Audit Log Table --%>
            <div class="auditlog-column">
                <div class="card card-sticky-log">
                    <div class="card-title">Audit Log Aktivitas Terakhir</div>
                    <div class="table-scroll-container">
                        <table id="tbl-dashboard-log">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>User ID</th>
                                    <th>Aksi</th>
                                    <th>IP Address</th>
                                    <th>Detail</th>
                                    <th>Waktu</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="log" items="${recentAuditLogs}" varStatus="status">
                                    <c:if test="${status.index < 10}">
                                        <tr>
                                            <td><c:out value="${log.id}"/></td>
                                            <td><c:out value="${log.userId != null ? log.userId : '-'}"/></td>
                                            <td><c:out value="${log.action}"/></td>
                                            <td class="mono"><c:out value="${log.ipAddress}"/></td>
                                            <td><c:out value="${log.detail}"/></td>
                                            <td><c:out value="${log.timestamp}"/></td>
                                        </tr>
                                    </c:if>
                                </c:forEach>
                                <c:if test="${empty recentAuditLogs}">
                                    <tr>
                                        <td colspan="6" class="text-center">Belum ada aktivitas tercatat.</td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

        </div>
    </div>

<%-- Embed chart data from server-side model (all strings pre-escaped via JsStringUtil) --%>
<script>
window.DASHBOARD_DATA = {
    totalAum: ${totalAumRaw},
    totalAumFormatted: '${totalAum}',
    instrLabels: [<c:forEach var="l" items="${instrLabels}" varStatus="s">'${l}'<c:if test="${!s.last}">,</c:if></c:forEach>],
    instrValues: [<c:forEach var="v" items="${instrValues}" varStatus="s">${v}<c:if test="${!s.last}">,</c:if></c:forEach>],
    instrPercents: [<c:forEach var="p" items="${instrPercents}" varStatus="s">${p}<c:if test="${!s.last}">,</c:if></c:forEach>],
    trendDates: [<c:forEach var="d" items="${trendDates}" varStatus="s">'${d}'<c:if test="${!s.last}">,</c:if></c:forEach>],
    trendValues: [<c:forEach var="v" items="${trendValues}" varStatus="s">${v}<c:if test="${!s.last}">,</c:if></c:forEach>],
    topNames: [<c:forEach var="n" items="${topNames}" varStatus="s">'${n}'<c:if test="${!s.last}">,</c:if></c:forEach>],
    topValues: [<c:forEach var="v" items="${topValues}" varStatus="s">${v}<c:if test="${!s.last}">,</c:if></c:forEach>],
    kycLabels: [<c:forEach var="l" items="${kycLabels}" varStatus="s">'${l}'<c:if test="${!s.last}">,</c:if></c:forEach>],
    kycCounts: [<c:forEach var="c" items="${kycCounts}" varStatus="s">${c}<c:if test="${!s.last}">,</c:if></c:forEach>],
    docLabels: [<c:forEach var="l" items="${docLabels}" varStatus="s">'${l}'<c:if test="${!s.last}">,</c:if></c:forEach>],
    docCounts: [<c:forEach var="c" items="${docCounts}" varStatus="s">${c}<c:if test="${!s.last}">,</c:if></c:forEach>]
};
</script>
<script src="<c:url value='/resources/js/dashboard-charts.js'/>"></script>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</body>
</html>
