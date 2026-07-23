<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Daftar Klien - SIMIPKIT</title>
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
                <h1>Manajemen Data Klien</h1>
                <p>Kelola data nasabah, dokumen KYC, dan portofolio investasi</p>
            </div>
            <div>
                <a href="<c:url value='/clients/new'/>" class="btn btn-primary">Tambah Klien Baru</a>
            </div>
        </div>

        <div class="card">
            <div class="card-title">Daftar Klien</div>
            <div class="table-search-bar">
                <input type="text" id="search-client-list" class="table-search-input"
                       placeholder="Cari nama klien..."
                       oninput="tableSearch(this, 'tbl-client-list', 1)">
            </div>
            <div class="table-scroll-container">
                <table id="tbl-client-list">
                    <thead>
                        <tr>
                            <th>ID Klien</th>
                            <th>Nama Lengkap</th>
                            <th>NIK</th>
                            <th>Alamat</th>
                            <th>Status KYC</th>
                            <th>Tanggal Daftar</th>
                            <th>Aksi</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="c" items="${clients}">
                            <tr>
                                <td class="mono"><c:out value="${c.id}"/></td>
                                <td><strong><c:out value="${c.nama}"/></strong></td>
                                <td class="mono"><c:out value="${c.nik}"/></td>
                                <td><c:out value="${c.alamat}"/></td>
                                <td>
                                    <span style="font-weight: 600; color: ${c.statusKyc == 'VERIFIED' ? 'var(--status-green)' : (c.statusKyc == 'REJECTED' ? 'var(--status-red)' : 'var(--status-amber)')};">
                                        <c:out value="${c.statusKyc}"/>
                                    </span>
                                </td>
                                <td><c:out value="${c.createdAt}"/></td>
                                <td>
                                    <div class="btn-group">
                                        <a href="<c:url value='/clients/detail/${c.id}'/>" class="btn btn-sm">Detail</a>
                                        <a href="<c:url value='/portfolio/list/${c.id}'/>" class="btn btn-sm btn-secondary">Portofolio</a>
                                        <a href="<c:url value='/clients/edit/${c.id}'/>" class="btn btn-sm">Edit</a>
                                        <a href="<c:url value='/clients/delete/${c.id}'/>" class="btn btn-sm btn-danger btn-confirm-action" data-title="Hapus Data Klien" data-message="Hapus data klien ini beserta seluruh dokumen dan portofolionya? Tindakan ini tidak dapat dibatalkan.">Hapus</a>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty clients}">
                            <tr>
                                <td colspan="7" class="text-center">Belum ada data klien terdaftar.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
<script src="<c:url value='/resources/js/table-search.js'/>"></script>
<script src="<c:url value='/resources/js/confirm-modal.js'/>"></script>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</html>
