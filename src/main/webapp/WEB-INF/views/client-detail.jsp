<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Detail Klien - SIMIPKIT</title>
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
                <h1>Detail Profil Klien</h1>
                <p>Informasi identitas, dokumen KYC terlampir, dan ringkasan nilai portofolio</p>
            </div>
            <div>
                <a href="<c:url value='/clients'/>" class="btn btn-secondary">Kembali ke Daftar Klien</a>
            </div>
        </div>

        <div class="card mb-20">
            <div class="card-title">Informasi Pribadi</div>
            <div class="grid-4" style="grid-template-columns: repeat(2, 1fr); margin-bottom: 0;">
                <div>
                    <p><strong>ID Klien:</strong> <span class="mono"><c:out value="${client.id}"/></span></p>
                    <p><strong>Nama Lengkap:</strong> <c:out value="${client.nama}"/></p>
                    <p><strong>NIK:</strong> <span class="mono"><c:out value="${client.nik}"/></span></p>
                </div>
                <div>
                    <p><strong>Alamat:</strong> <c:out value="${client.alamat}"/></p>
                    <p><strong>Status KYC:</strong> 
                        <span style="font-weight: 600; color: ${client.statusKyc == 'VERIFIED' ? '#166534' : (client.statusKyc == 'REJECTED' ? '#991b1b' : '#b45309')};">
                            <c:out value="${client.statusKyc}"/>
                        </span>
                    </p>
                    <p><strong>Tanggal Pendaftaran:</strong> <c:out value="${client.createdAt}"/></p>
                </div>
            </div>
        </div>

        <div class="card mb-20">
            <div class="page-header" style="border-bottom: none; margin-bottom: 0; padding-bottom: 0;">
                <div class="card-title" style="border: none; margin: 0; padding: 0;">Dokumen KYC Terlampir</div>
                <div>
                    <a href="<c:url value='/documents/upload/${client.id}'/>" class="btn btn-sm btn-primary">Upload Dokumen KYC</a>
                </div>
            </div>
            <table class="mt-10">
                <thead>
                    <tr>
                        <th>ID Dokumen</th>
                        <th>Jenis Dokumen</th>
                        <th>Nama File Asli</th>
                        <th>Nama File Stored</th>
                        <th>Ukuran File (Bytes)</th>
                        <th>Waktu Upload</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="doc" items="${documents}">
                        <tr>
                            <td class="mono"><c:out value="${doc.id}"/></td>
                            <td><c:out value="${doc.jenisDokumen}"/></td>
                            <td><c:out value="${doc.namaFileAsli}"/></td>
                            <td class="mono"><c:out value="${doc.namaFileStored}"/></td>
                            <td><c:out value="${doc.fileSizeBytes}"/> B</td>
                            <td><c:out value="${doc.uploadedAt}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty documents}">
                        <tr>
                            <td colspan="6" class="text-center">Belum ada dokumen KYC terlampir untuk klien ini.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>

        <div class="card">
            <div class="page-header" style="border-bottom: none; margin-bottom: 0; padding-bottom: 0;">
                <div class="card-title" style="border: none; margin: 0; padding: 0;">Aset Portofolio Investasi</div>
                <div>
                    <a href="<c:url value='/portfolio/list/${client.id}'/>" class="btn btn-sm btn-secondary">Kelola Portofolio</a>
                </div>
            </div>
            <table class="mt-10">
                <thead>
                    <tr>
                        <th>Jenis Instrumen</th>
                        <th>Nama Instrumen</th>
                        <th>Jumlah Unit</th>
                        <th>Nilai Investasi (IDR)</th>
                        <th>Alokasi (%)</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="a" items="${assets}">
                        <tr>
                            <td><c:out value="${a.jenisInstrumen}"/></td>
                            <td><c:out value="${a.namaInstrumen}"/></td>
                            <td><c:out value="${a.jumlah}"/></td>
                            <td>Rp <c:out value="${a.nilai}"/></td>
                            <td><c:out value="${a.allocationPercent}"/>%</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty assets}">
                        <tr>
                            <td colspan="5" class="text-center">Belum ada aset portofolio terdaftar.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
