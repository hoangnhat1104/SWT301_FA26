<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu — EduSphere</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>

<div class="page-container">
    <h2>Đổi mật khẩu</h2>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <form:form method="post"
               action="${pageContext.request.contextPath}/change-password"
               modelAttribute="changePasswordRequest"
               class="form-card">

        <div class="form-group">
            <label for="currentPassword">Mật khẩu hiện tại</label>
            <form:password path="currentPassword" id="currentPassword"
                           placeholder="Nhập mật khẩu hiện tại" cssClass="form-control"/>
            <form:errors path="currentPassword" cssClass="field-error"/>
        </div>

        <div class="form-group">
            <label for="newPassword">Mật khẩu mới</label>
            <form:password path="newPassword" id="newPassword"
                           placeholder="Tối thiểu 8 ký tự" cssClass="form-control"/>
            <form:errors path="newPassword" cssClass="field-error"/>
        </div>

        <div class="form-group">
            <label for="confirmPassword">Xác nhận mật khẩu mới</label>
            <form:password path="confirmPassword" id="confirmPassword"
                           placeholder="Nhập lại mật khẩu mới" cssClass="form-control"/>
            <form:errors path="confirmPassword" cssClass="field-error"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Đổi mật khẩu</button>
            <a href="${pageContext.request.contextPath}/profile" class="btn btn-secondary">Huỷ</a>
        </div>
    </form:form>
</div>

</body>
</html>
