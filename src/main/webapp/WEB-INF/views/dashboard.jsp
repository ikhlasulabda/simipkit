<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Dashboard - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
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

        <div class="grid-4">
            <div class="stat-card">
                <div class="label">Total Klien Terdaftar</div>
                <div class="value"><c:out value="${totalClients}"/></div>
            </div>
            <div class="stat-card">
                <div class="label">Pengguna Sistem</div>
                <div class="value"><c:out value="${totalUsers}"/></div>
            </div>
            <div class="stat-card">
                <div class="label">Status Server</div>
                <div class="value" style="font-size: 16px; color: var(--status-green);">AKTIF</div>
            </div>
            <div class="stat-card">
                <div class="label">Log Aktivitas Terbaru</div>
                <div class="value"><c:out value="${recentAuditLogs.size()}"/></div>
            </div>
        </div>

        <div class="card">
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
</body>
</html>
