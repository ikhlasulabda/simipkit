<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Manajemen User - SIMIPKIT</title>
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
                <li><a href="<c:url value='/user-management'/>" class="active">User Management</a></li>
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
                <h1>Manajemen Pengguna (User Management)</h1>
                <p>Kelola akun staff dan administrator aplikasi</p>
            </div>
            <div>
                <a href="<c:url value='/user-management/new'/>" class="btn btn-primary">Tambah User Baru</a>
            </div>
        </div>

        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th>ID User</th>
                        <th>Username</th>
                        <th>Role Access</th>
                        <th>Status Akun</th>
                        <th>Tanggal Dibuat</th>
                        <th>Aksi</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="u" items="${users}">
                        <tr>
                            <td><c:out value="${u.id}"/></td>
                            <td><strong><c:out value="${u.username}"/></strong></td>
                            <td>
                                <span class="role-badge" style="background-color: ${u.role == 'admin' ? '#0f172a' : '#475569'};">
                                    <c:out value="${u.role}"/>
                                </span>
                            </td>
                            <td>
                                <span style="font-weight: 600; color: ${u.active ? '#166534' : '#991b1b'};">
                                    ${u.active ? 'AKTIF' : 'NON-AKTIF'}
                                </span>
                            </td>
                            <td><c:out value="${u.createdAt}"/></td>
                            <td>
                                <div class="btn-group">
                                    <a href="<c:url value='/user-management/edit/${u.id}'/>" class="btn btn-sm">Edit</a>
                                    <c:if test="${u.id != sessionScope.userId}">
                                        <a href="<c:url value='/user-management/delete/${u.id}'/>" class="btn btn-sm btn-danger" onclick="return confirm('Apakah Anda yakin ingin menghapus user ini?');">Hapus</a>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty users}">
                        <tr>
                            <td colspan="6" class="text-center">Belum ada user terdaftar.</td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
