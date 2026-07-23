<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Audit Log - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
</head>
<body>
    <header>
        <a href="<c:url value='/'/>" class="brand">SIMIPKIT</a>
        <ul class="nav-links">
            <li><a href="<c:url value='/'/>">Dashboard</a></li>
            <li><a href="<c:url value='/clients'/>">Klien</a></li>
            <li><a href="<c:url value='/documents/bulk-upload'/>">Bulk Upload KYC</a></li>
            <li><a href="<c:url value='/reports'/>">Laporan</a></li>
            <li><a href="<c:url value='/bank-sync-log'/>">Bank Sync Log</a></li>
            <c:if test="${sessionScope.role == 'admin'}">
                <li><a href="<c:url value='/user-management'/>">User Management</a></li>
                <li><a href="<c:url value='/report-template-upload'/>">Template Laporan</a></li>
            </c:if>
            <li><a href="<c:url value='/audit-log'/>" class="active">Audit Log</a></li>
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
                <h1>Audit Log Viewer</h1>
                <p>Jejak audit sistem seluruh aktivitas pengguna dan event aplikasi</p>
            </div>
        </div>

        <div class="card">
            <div class="card-title">Riwayat Aktivitas Sistem</div>
            <div class="table-search-bar">
                <input type="text" id="search-audit-log" class="table-search-input"
                       placeholder="Cari aksi atau event..."
                       oninput="tableSearch(this, 'tbl-audit-log', 2)">
            </div>
            <div class="table-scroll-container">
                <table id="tbl-audit-log">
                    <thead>
                        <tr>
                            <th>ID Log</th>
                            <th>User ID</th>
                            <th>Aksi / Event</th>
                            <th>IP Address</th>
                            <th>Detail Keterangan</th>
                            <th>Waktu Kejadian</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="log" items="${logs}">
                            <tr>
                                <td><c:out value="${log.id}"/></td>
                                <td><c:out value="${log.userId != null ? log.userId : '-'}"/></td>
                                <td><strong><c:out value="${log.action}"/></strong></td>
                                <td class="mono"><c:out value="${log.ipAddress}"/></td>
                                <td><c:out value="${log.detail}"/></td>
                                <td><c:out value="${log.timestamp}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty logs}">
                            <tr>
                                <td colspan="6" class="text-center">Belum ada entri audit log.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
<script src="<c:url value='/resources/js/table-search.js'/>"></script>
</html>
