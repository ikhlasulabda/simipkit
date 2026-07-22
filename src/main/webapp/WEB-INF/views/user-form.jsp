<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${isNew ? 'Tambah User Baru' : 'Edit Data User'} - SIMIPKIT</title>
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
                <h1>${isNew ? 'Form Tambah User Baru' : 'Form Edit Data User'}</h1>
                <p>Pengaturan akun dan hak akses pengguna aplikasi</p>
            </div>
            <div>
                <a href="<c:url value='/user-management'/>" class="btn btn-secondary">Kembali ke Daftar User</a>
            </div>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>

        <div class="card" style="max-width: 600px;">
            <form action="<c:url value='/user-management/save'/>" method="post">
                <input type="hidden" name="id" value="<c:out value='${user.id}'/>">

                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" class="form-control" value="<c:out value='${user.username}'/>" required>
                </div>

                <div class="form-group">
                    <label for="password">Password ${isNew ? '' : '(Kosongkan jika tidak ingin mengubah)'}</label>
                    <input type="password" id="password" name="password" class="form-control" ${isNew ? 'required' : ''}>
                </div>

                <div class="form-group">
                    <label for="role">Role Akses</label>
                    <select id="role" name="role" class="form-control" required>
                        <option value="staff" ${user.role == 'staff' ? 'selected' : ''}>Staff</option>
                        <option value="admin" ${user.role == 'admin' ? 'selected' : ''}>Admin</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="active">Status Akun</label>
                    <select id="active" name="active" class="form-control">
                        <option value="true" ${user.active ? 'selected' : ''}>Aktif</option>
                        <option value="false" ${!user.active ? 'selected' : ''}>Non-Aktif</option>
                    </select>
                </div>

                <div class="form-group mt-20">
                    <button type="submit" class="btn btn-primary">Simpan User</button>
                    <a href="<c:url value='/user-management'/>" class="btn btn-secondary">Batal</a>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
