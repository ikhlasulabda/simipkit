<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="id_ID"/>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Generate Laporan Portofolio - SIMIPKIT</title>
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
                <h1>Generate Ringkasan Laporan Portofolio</h1>
                <p>Klien: <strong><c:out value="${client.nama}"/></strong> (NIK: <c:out value="${client.nik}"/>)</p>
            </div>
            <div>
                <a href="<c:url value='/reports'/>" class="btn btn-secondary">Kembali ke Daftar Laporan</a>
            </div>
        </div>

        <c:if test="${not empty success}">
            <div class="alert alert-success"><c:out value="${success}"/></div>
            <div class="mb-20">
                <a href="<c:url value='/reports/summary/${summaryId}/pdf'/>" class="btn btn-pdf" id="btn-unduh-pdf">Unduh PDF</a>
            </div>
        </c:if>

        <div class="card mb-20" style="max-width: 600px;">
            <div class="card-title">Form Parameter Laporan</div>
            <form action="<c:url value='/reports/generate'/>" method="post">
                <input type="hidden" name="clientId" value="<c:out value='${client.id}'/>">

                <div class="form-group">
                    <label for="periode">Periode Laporan</label>
                    <input type="text" id="periode" name="periode" class="form-control" value="${not empty periode ? periode : '2026-Q3'}" placeholder="Contoh: 2026-Q3, Juli 2026" maxlength="50" required>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Generate Laporan & Simpan Summary</button>
                </div>
            </form>
        </div>

        <div class="card">
            <div class="card-title">Preview Ringkasan Portofolio (${periode})</div>

            <div style="background-color: #f1f5f9; padding: 14px; margin-bottom: 16px;">
                <strong>Total Nilai Portofolio:</strong> <fmt:formatNumber value="${totalValue}" type="currency" currencySymbol="Rp " maxFractionDigits="0"/>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>Jenis Instrumen</th>
                        <th>Nama Instrumen</th>
                        <th>Jumlah Unit</th>
                        <th>Nilai (IDR)</th>
                        <th>Alokasi (%)</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="a" items="${assets}">
                        <tr>
                            <td><c:out value="${a.jenisInstrumen}"/></td>
                            <td><c:out value="${a.namaInstrumen}"/></td>
                            <td><c:out value="${a.jumlah}"/></td>
                            <td><fmt:formatNumber value="${a.nilai}" type="currency" currencySymbol="Rp " maxFractionDigits="0"/></td>
                            <td><c:out value="${a.allocationPercent}"/>%</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty assets}">
                        <tr>
                            <td colspan="5" class="text-center">Belum ada aset terdaftar dalam portofolio klien ini.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
<script>
window.REPORT_TEMPLATES = [
    <c:forEach var="t" items="${templates}" varStatus="s">
        { id: ${t.id}, nama_template: '${com.happy.simipkit.util.JsStringUtil.escape(t.nama_template)}' }<c:if test="${!s.last}">,</c:if>
    </c:forEach>
];
</script>
<script src="<c:url value='/resources/js/template-modal.js'/>"></script>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</body>
</html>
