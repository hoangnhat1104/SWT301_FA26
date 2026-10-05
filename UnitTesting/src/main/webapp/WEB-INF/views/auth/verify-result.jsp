<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Xác minh Email — EduSphere</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body class="auth-page">

<div class="auth-container">
    <div class="auth-card" style="text-align:center;">
        <h1 class="brand">EduSphere</h1>

        <c:choose>
            <c:when test="${not empty successMessage}">
                <div class="verify-icon success">✓</div>
                <h2>Xác minh thành công!</h2>
                <p>${successMessage}</p>
                <a href="${pageContext.request.contextPath}/login"
                   class="btn btn-primary">Đăng nhập ngay</a>
            </c:when>
            <c:otherwise>
                <div class="verify-icon error">✗</div>
                <h2>Xác minh thất bại</h2>
                <p class="text-error">${errorMessage}</p>
                <a href="${pageContext.request.contextPath}/register"
                   class="btn btn-secondary">Đăng ký lại</a>
            </c:otherwise>
        </c:choose>
    </div>
</div>

</body>
</html>
