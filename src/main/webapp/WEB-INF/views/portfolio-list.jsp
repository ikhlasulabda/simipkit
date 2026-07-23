<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Portofolio Klien - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
</head>
<body>
    <header>
        <a href="<c:url value='/'/>" class="brand">SIMIPKIT</a>
        <ul class="nav-links">
            <li><a href="<c:url value='/'/>">Dashboard</a></li>
            <li><a href="<c:url value='/clients'/>" class="active">Klien</a></li>
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
                <h1>Aset Portofolio Klien</h1>
                <p>Klien: <strong><c:out value="${client.nama}"/></strong> (ID: <span class="mono"><c:out value="${client.id}"/></span>)</p>
            </div>
            <div>
                <a href="<c:url value='/portfolio/new/${client.id}'/>" class="btn btn-primary">Tambah Aset Portofolio</a>
                <a href="<c:url value='/clients/detail/${client.id}'/>" class="btn btn-secondary">Detail Klien</a>
            </div>
        </div>

        <div class="card mb-20" style="background-color: var(--table-row-even);">
            <div style="font-size: 13px; color: var(--text-muted);">Total Nilai Portofolio Investasi:</div>
            <div style="font-size: 24px; font-weight: 700; color: var(--text-primary);">Rp <c:out value="${totalValue}"/></div>
        </div>

        <div class="card">
            <div class="card-title">Daftar Aset Portofolio</div>
            <div class="table-search-bar">
                <input type="text" id="search-portfolio-list" class="table-search-input"
                       placeholder="Cari nama instrumen..."
                       oninput="tableSearch(this, 'tbl-portfolio-list', 2)">
            </div>
            <div class="table-scroll-container">
                <table id="tbl-portfolio-list">
                    <thead>
                        <tr>
                            <th>ID Aset</th>
                            <th>Jenis Instrumen</th>
                            <th>Nama Instrumen</th>
                            <th>Jumlah Unit</th>
                            <th>Nilai (IDR)</th>
                            <th>Alokasi (%)</th>
                            <th>Terakhir Diperbarui</th>
                            <th>Aksi</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="a" items="${assets}">
                            <tr>
                                <td><c:out value="${a.id}"/></td>
                                <td><c:out value="${a.jenisInstrumen}"/></td>
                                <td><strong><c:out value="${a.namaInstrumen}"/></strong></td>
                                <td><c:out value="${a.jumlah}"/></td>
                                <td>Rp <c:out value="${a.nilai}"/></td>
                                <td><c:out value="${a.allocationPercent}"/>%</td>
                                <td><c:out value="${a.updatedAt}"/></td>
                                <td>
                                    <div class="btn-group">
                                        <a href="<c:url value='/portfolio/edit/${a.id}'/>" class="btn btn-sm">Edit</a>
                                        <a href="<c:url value='/portfolio/delete/${a.id}'/>" class="btn btn-sm btn-danger" onclick="return confirm('Hapus aset ini dari portofolio?');">Hapus</a>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty assets}">
                            <tr>
                                <td colspan="8" class="text-center">Belum ada aset terdaftar pada portofolio ini.</td>
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
