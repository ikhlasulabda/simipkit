<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Bank Sync Log - SIMIPKIT</title>
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
            <li><a href="<c:url value='/bank-sync-log'/>" class="active">Bank Sync Log</a></li>
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
                <h1>Histori Integrasi Bank Feed</h1>
                <p>Log sinkronisasi transaksi otomatis dari sistem bank mitra</p>
            </div>
        </div>

        <div class="card">
            <div class="card-title">Event Transaksi Diterima</div>
            <div class="table-search-bar">
                <input type="text" id="search-bank-sync" class="table-search-input"
                       placeholder="Cari tipe event..."
                       oninput="tableSearch(this, 'tbl-bank-sync-log', 1)">
            </div>
            <div class="table-scroll-container">
                <table id="tbl-bank-sync-log">
                    <thead>
                        <tr>
                            <th>ID Event</th>
                            <th>Tipe Event</th>
                            <th>Status</th>
                            <th>Waktu Diproses</th>
                            <th>Payload JSON Raw</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="e" items="${events}">
                            <tr>
                                <td><c:out value="${e.id}"/></td>
                                <td><strong><c:out value="${e.event_type}"/></strong></td>
                                <td>
                                    <span style="font-weight: 600; color: ${e.status == 'PROCESSED' ? 'var(--status-green)' : 'var(--status-amber)'};"
                                    ><c:out value="${e.status}"/></span>
                                </td>
                                <td><c:out value="${e.processed_at}"/></td>
                                <td class="mono" style="max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                    <c:out value="${e.payload_raw}"/>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty events}">
                            <tr>
                                <td colspan="5" class="text-center">Belum ada log sinkronisasi bank terikat.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
<script src="<c:url value='/resources/js/table-search.js'/>"></script>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</html>
