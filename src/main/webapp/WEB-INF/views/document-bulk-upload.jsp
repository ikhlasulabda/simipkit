<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Bulk Upload KYC - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
</head>
<body>
    <header>
        <a href="<c:url value='/'/>" class="brand">SIMIPKIT</a>
        <ul class="nav-links">
            <li><a href="<c:url value='/'/>">Dashboard</a></li>
            <li><a href="<c:url value='/clients'/>">Klien</a></li>
            <li><a href="<c:url value='/documents/bulk-upload'/>" class="active">Bulk Upload KYC</a></li>
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
                <h1>Bulk Upload Dokumen KYC (Arsip ZIP)</h1>
                <p>Unggah banyak dokumen sekali jalan untuk proses verifikasi batch nasabah</p>
            </div>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success"><c:out value="${success}"/></div>
        </c:if>

        <div class="card" style="max-width: 650px;">
            <div class="card-title">Form Unggah Arsip ZIP Dokumen</div>

            <form action="<c:url value='/documents/bulk-upload'/>" method="post" enctype="multipart/form-data">
                <div class="form-group">
                    <label for="clientId">Pilih Klien Tujuan</label>
                    <select id="clientId" name="clientId" class="form-control" required>
                        <c:forEach var="c" items="${clients}">
                            <option value="<c:out value='${c.id}'/>"><c:out value="${c.nama}"/> (<c:out value="${c.nik}"/>)</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label for="zipFile">File Arsip (.ZIP)</label>
                    <input type="file" id="zipFile" name="zipFile" class="form-control" accept=".zip" required>
                    <p style="font-size: 12px; color: #64748b; margin-top: 4px;">Pilih file zip yang berisi kumpulan dokumen fisik pendukung.</p>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Proses & Ekstrak ZIP</button>
                </div>
            </form>
        </div>
    </div>
</body>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</html>
