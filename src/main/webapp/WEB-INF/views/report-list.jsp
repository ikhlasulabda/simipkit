<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="id_ID"/>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Daftar Laporan Portofolio - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
</head>
<body>
    <header>
        <a href="<c:url value='/'/>" class="brand">SIMIPKIT</a>
        <ul class="nav-links">
            <li><a href="<c:url value='/'/>">Dashboard</a></li>
            <li><a href="<c:url value='/clients'/>">Klien</a></li>
            <li><a href="<c:url value='/documents/bulk-upload'/>">Bulk Upload KYC</a></li>
            <li><a href="<c:url value='/reports'/>" class="active">Laporan</a></li>
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
                <h1>Manajemen Laporan Portofolio</h1>
                <p>Histori penerbitan laporan investasi nasabah</p>
            </div>
        </div>

        <div class="card mb-20">
            <div class="card-title">Generate Laporan Baru per Klien</div>
            <form action="<c:url value='/reports/generate'/>" method="get" style="display: flex; gap: 12px; align-items: flex-end;">
                <div class="form-group" style="margin-bottom: 0; flex: 1;">
                    <label for="selectClient">Pilih Klien</label>
                    <select id="selectClient" class="form-control" onchange="if(this.value) window.location.href='<c:url value='/reports/generate/'/>' + this.value;">
                        <option value="">-- Pilih Klien Nasabah --</option>
                        <c:forEach var="c" items="${clients}">
                            <option value="<c:out value='${c.id}'/>"><c:out value="${c.nama}"/> (<c:out value="${c.id}"/>)</option>
                        </c:forEach>
                    </select>
                </div>
            </form>
        </div>

        <div class="card">
            <div class="card-title">Histori Summary Laporan Terbit</div>
            <div class="table-search-bar">
                <input type="text" id="search-report-list" class="table-search-input"
                       placeholder="Cari periode laporan..."
                       oninput="tableSearch(this, 'tbl-report-list', 2)">
            </div>
            <div class="table-scroll-container">
                <table id="tbl-report-list">
                    <thead>
                        <tr>
                            <th>ID Summary</th>
                            <th>ID Klien</th>
                            <th>Periode Laporan</th>
                            <th>Total Nilai Portofolio (IDR)</th>
                            <th>Tanggal Diterbitkan</th>
                            <th>Aksi</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="s" items="${summaries}">
                            <tr>
                                <td><c:out value="${s.id}"/></td>
                                <td class="mono"><c:out value="${s.clientId}"/></td>
                                <td><strong><c:out value="${s.periode}"/></strong></td>
                                <td><fmt:formatNumber value="${s.totalNilai}" type="currency" currencySymbol="Rp " maxFractionDigits="0"/></td>
                                <td><c:out value="${s.generatedAt}"/></td>
                                <td>
                                    <a href="<c:url value='/portfolio/report/${s.clientId}?periode=${s.periode}'/>" class="btn btn-sm">Lihat Detail</a>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty summaries}">
                            <tr>
                                <td colspan="6" class="text-center">Belum ada summary laporan diterbitkan.</td>
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
