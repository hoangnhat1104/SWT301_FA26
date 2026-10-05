<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chỉnh sửa hồ sơ — EduSphere</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
</head>
<body>

<div class="page-container">
    <h2>Chỉnh sửa hồ sơ</h2>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <form:form method="post"
               action="${pageContext.request.contextPath}/profile/edit"
               modelAttribute="updateProfileRequest"
               class="form-card">

        <div class="form-group">
            <label for="fullName">Họ và tên</label>
            <form:input path="fullName" id="fullName" cssClass="form-control"/>
            <form:errors path="fullName" cssClass="field-error"/>
        </div>

        <div class="form-group">
            <label>Email <span class="text-muted">(không thể thay đổi)</span></label>
            <input type="text" value="${user.email}" class="form-control" disabled>
        </div>

        <div class="form-group">
            <label for="phone">Số điện thoại</label>
            <form:input path="phone" id="phone" placeholder="0912345678" cssClass="form-control"/>
            <form:errors path="phone" cssClass="field-error"/>
        </div>

        <div class="form-actions">
            <button type="submit" class="btn btn-primary">Lưu thay đổi</button>
            <a href="${pageContext.request.contextPath}/profile" class="btn btn-secondary">Huỷ</a>
        </div>
    </form:form>
</div>

</body>
</html>
