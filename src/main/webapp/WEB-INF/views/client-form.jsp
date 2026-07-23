<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${isNew ? 'Tambah Klien Baru' : 'Edit Data Klien'} - SIMIPKIT</title>
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
                <h1>${isNew ? 'Form Tambah Klien' : 'Form Edit Klien'}</h1>
                <p>Isi informasi identitas nasabah dan status verifikasi KYC</p>
            </div>
            <div>
                <a href="<c:url value='/clients'/>" class="btn btn-secondary">Kembali ke Daftar Klien</a>
            </div>
        </div>

        <div class="card" style="max-width: 600px;">
            <form action="<c:url value='/clients/save'/>" method="post">
                <input type="hidden" name="id" value="<c:out value='${client.id}'/>">

                <div class="form-group">
                    <label for="nama">Nama Lengkap</label>
                    <input type="text" id="nama" name="nama" class="form-control" value="<c:out value='${client.nama}'/>" maxlength="100" required>
                </div>

                <div class="form-group">
                    <label for="nik">NIK (Nomor Induk Kependudukan)</label>
                    <input type="text" id="nik" name="nik" class="form-control" value="<c:out value='${client.nik}'/>" maxlength="20" required>
                </div>

                <div class="form-group">
                    <label for="alamat">Alamat Lengkap</label>
                    <textarea id="alamat" name="alamat" class="form-control" rows="3" maxlength="250"><c:out value="${client.alamat}"/></textarea>
                </div>

                <div class="form-group">
                    <label for="statusKyc">Status Verifikasi KYC</label>
                    <select id="statusKyc" name="statusKyc" class="form-control">
                        <option value="PENDING" ${client.statusKyc == 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="VERIFIED" ${client.statusKyc == 'VERIFIED' ? 'selected' : ''}>VERIFIED</option>
                        <option value="REJECTED" ${client.statusKyc == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                    </select>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Simpan Data Klien</button>
                    <a href="<c:url value='/clients'/>" class="btn btn-secondary">Batal</a>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
