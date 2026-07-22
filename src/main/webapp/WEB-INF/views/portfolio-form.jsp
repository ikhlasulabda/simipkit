<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${isNew ? 'Tambah Aset Portofolio' : 'Edit Aset Portofolio'} - SIMIPKIT</title>
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
                <h1>${isNew ? 'Form Tambah Aset Portofolio' : 'Form Edit Aset Portofolio'}</h1>
                <p>Klien: <strong><c:out value="${client.nama}"/></strong></p>
            </div>
            <div>
                <a href="<c:url value='/portfolio/list/${client.id}'/>" class="btn btn-secondary">Batal</a>
            </div>
        </div>

        <div class="card" style="max-width: 600px;">
            <form action="<c:url value='/portfolio/save'/>" method="post">
                <input type="hidden" name="id" value="<c:out value='${asset.id}'/>">
                <input type="hidden" name="clientId" value="<c:out value='${client.id}'/>">

                <div class="form-group">
                    <label for="jenisInstrumen">Jenis Instrumen Investasi</label>
                    <select id="jenisInstrumen" name="jenisInstrumen" class="form-control" required>
                        <option value="SAHAM" ${asset.jenisInstrumen == 'SAHAM' ? 'selected' : ''}>Saham</option>
                        <option value="REKSADANA" ${asset.jenisInstrumen == 'REKSADANA' ? 'selected' : ''}>Reksadana</option>
                        <option value="OBLIGASI" ${asset.jenisInstrumen == 'OBLIGASI' ? 'selected' : ''}>Obligasi / Surat Berharga</option>
                        <option value="DEPOSITO" ${asset.jenisInstrumen == 'DEPOSITO' ? 'selected' : ''}>Deposito</option>
                        <option value="PASAR_UANG" ${asset.jenisInstrumen == 'PASAR_UANG' ? 'selected' : ''}>Pasar Uang</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="namaInstrumen">Nama Instrumen / Efek</label>
                    <input type="text" id="namaInstrumen" name="namaInstrumen" class="form-control" value="<c:out value='${asset.namaInstrumen}'/>" placeholder="Contoh: BBCA, Reksa Dana Saham Mandiri" required>
                </div>

                <div class="form-group">
                    <label for="jumlah">Jumlah Unit / Lembar</label>
                    <input type="number" step="any" id="jumlah" name="jumlah" class="form-control" value="<c:out value='${asset.jumlah}'/>" required>
                </div>

                <div class="form-group">
                    <label for="nilai">Nilai Total Portofolio (IDR)</label>
                    <input type="number" step="any" id="nilai" name="nilai" class="form-control" value="<c:out value='${asset.nilai}'/>" required>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Simpan Aset Portofolio</button>
                    <a href="<c:url value='/portfolio/list/${client.id}'/>" class="btn btn-secondary">Batal</a>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
