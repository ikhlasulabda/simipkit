<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Upload Dokumen KYC - SIMIPKIT</title>
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
                <h1>Upload Dokumen KYC Nasabah</h1>
                <p>Unggah file fisik pendukung verifikasi data klien</p>
            </div>
            <div>
                <a href="<c:url value='/clients/detail/${client.id}'/>" class="btn btn-secondary">Kembali ke Detail Klien</a>
            </div>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <div class="card" style="max-width: 600px;">
            <div class="card-title">Klien Target: <c:out value="${client.nama}"/> (<c:out value="${client.id}"/>)</div>

            <form action="<c:url value='/documents/upload'/>" method="post" enctype="multipart/form-data">
                <input type="hidden" name="clientId" value="<c:out value='${client.id}'/>">

                <div class="form-group">
                    <label for="jenisDokumen">Jenis Dokumen KYC</label>
                    <select id="jenisDokumen" name="jenisDokumen" class="form-control" required>
                        <option value="KTP">KTP (Kartu Tanda Penduduk)</option>
                        <option value="NPWP">NPWP (Nomor Pokok Wajib Pajak)</option>
                        <option value="PASPOR">Paspor</option>
                        <option value="KARTU_KELUARGA">Kartu Keluarga</option>
                        <option value="FORM_INVESTOR">Formulir Pendaftaran Investor</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="file">Pilih File Dokumen</label>
                    <input type="file" id="file" name="file" class="form-control" required>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Unggah Dokumen</button>
                    <a href="<c:url value='/clients/detail/${client.id}'/>" class="btn btn-secondary">Batal</a>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
