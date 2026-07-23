<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login - SIMIPKIT</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/style.css'/>">
</head>
<body class="auth-wrapper">
    <div class="auth-box">
        <h2>SIMIPKIT</h2>
        <p>Sistem Informasi Manajemen Investasi dan Portofolio Klien</p>

        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <form action="<c:url value='/login'/>" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" class="form-control" maxlength="50" required autofocus autocomplete="username">
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" class="form-control" maxlength="100" required autocomplete="current-password">
            </div>

            <div class="form-group mt-20">
                <button type="submit" class="btn btn-primary" style="width: 100%;">Masuk</button>
            </div>
        </form>
    </div>
</body>
</html>
