<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard — EduSphere</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
</head>
<body>

<nav class="navbar">
    <div class="nav-brand">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">EduSphere</a>
    </div>
    <div class="nav-links">
        <sec:authentication property="principal.fullName" var="fullName"/>
        <a href="${pageContext.request.contextPath}/profile">${fullName}</a>

        <%-- Logout dùng POST + CSRF --%>
        <form method="post" action="${pageContext.request.contextPath}/logout" style="display:inline;">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <button type="submit" class="btn-link">Đăng xuất</button>
        </form>
    </div>
</nav>

<div class="page-container">
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success">${successMessage}</div>
    </c:if>

    <h2>Chào mừng, <sec:authentication property="principal.fullName"/>!</h2>

    <div class="dashboard-grid">
        <a href="${pageContext.request.contextPath}/profile" class="dashboard-card">
            <span class="card-icon">👤</span>
            <span>Hồ sơ cá nhân</span>
        </a>
        <a href="${pageContext.request.contextPath}/change-password" class="dashboard-card">
            <span class="card-icon">🔒</span>
            <span>Đổi mật khẩu</span>
        </a>

        <%-- Chỉ ADMINISTRATOR thấy link Admin --%>
        <sec:authorize access="hasRole('ADMINISTRATOR')">
            <a href="${pageContext.request.contextPath}/admin" class="dashboard-card">
                <span class="card-icon">⚙️</span>
                <span>Quản trị</span>
            </a>
        </sec:authorize>
    </div>
</div>

</body>
</html>
