<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Upload Template Laporan - SIMIPKIT</title>
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
            <li><a href="<c:url value='/bank-sync-log'/>">Bank Sync Log</a></li>
            <c:if test="${sessionScope.role == 'admin'}">
                <li><a href="<c:url value='/user-management'/>">User Management</a></li>
                <li><a href="<c:url value='/report-template-upload'/>" class="active">Template Laporan</a></li>
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
                <h1>Upload Template XML Laporan (Khusus Admin)</h1>
                <p>Kustomisasi struktur layout laporan PDF menggunakan template XML</p>
            </div>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success"><c:out value="${success}"/></div>
        </c:if>

        <div class="card mb-20" style="max-width: 700px;">
            <div class="card-title" style="display: flex; justify-content: space-between; align-items: center;">
                <span>Unggah & Test Parse Template XML</span>
                <button type="button" class="btn btn-sm btn-secondary" id="btn-show-example-modal">Lihat Contoh Template</button>
            </div>

            <form action="<c:url value='/report-template-upload'/>" method="post" enctype="multipart/form-data">
                <div class="form-group">
                    <label for="namaTemplate">Nama Template</label>
                    <input type="text" id="namaTemplate" name="namaTemplate" class="form-control" placeholder="Contoh: Template Triwulan Standard" maxlength="100">
                </div>

                <div class="form-group">
                    <label for="xmlFile">File Template (.XML)</label>
                    <input type="file" id="xmlFile" name="xmlFile" class="form-control" accept=".xml">
                </div>

                <div class="form-group">
                    <label for="xmlContent">Atau Input Langsung Konten XML</label>
                    <textarea id="xmlContent" name="xmlContent" class="form-control mono" rows="8" placeholder="<reportConfig><title>Laporan Portofolio</title></reportConfig>" maxlength="100000"></textarea>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Unggah & Preview Template</button>
                </div>
            </form>
        </div>

        <c:if test="${not empty previewResult}">
            <div class="card mb-20">
                <div class="card-title">Hasil Parse Object XStream (Preview)</div>
                <pre class="mono" style="background-color: var(--table-row-even); padding: 12px; font-size: 12px; overflow-x: auto;"><c:out value="${previewResult}"/></pre>
            </div>
        </c:if>

        <div class="card">
            <div class="card-title">Daftar Template Tersimpan</div>
            <div class="table-search-bar">
                <input type="text" id="search-templates" class="table-search-input"
                       placeholder="Cari nama template..."
                       oninput="tableSearch(this, 'tbl-templates', 1)">
            </div>
            <div class="table-scroll-container">
                <table id="tbl-templates">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Nama Template</th>
                            <th>Tanggal Upload</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="t" items="${templates}">
                            <tr>
                                <td><c:out value="${t.id}"/></td>
                                <td><strong><c:out value="${t.nama_template}"/></strong></td>
                                <td><c:out value="${t.uploaded_at}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty templates}">
                            <tr>
                                <td colspan="3" class="text-center">Belum ada template XML tersimpan.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
<script src="<c:url value='/resources/js/table-search.js'/>"></script>
<script src="<c:url value='/resources/js/template-modal.js'/>"></script>
<script src="<c:url value='/resources/js/idle-timer.js'/>" data-logout-url="<c:url value='/logout?reason=timeout'/>"></script>
</html>
