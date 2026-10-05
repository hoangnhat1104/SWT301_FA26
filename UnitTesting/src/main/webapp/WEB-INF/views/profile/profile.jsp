<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ cá nhân — EduSphere</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
</head>
<body>

<div class="page-container">
    <h2>Hồ sơ cá nhân</h2>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success">${successMessage}</div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <div class="profile-card">

        <%-- Avatar --%>
        <div class="avatar-section">
            <c:choose>
                <c:when test="${not empty user.avatarUrl}">
                    <img src="${pageContext.request.contextPath}${user.avatarUrl}"
                         alt="Avatar" class="avatar-img">
                </c:when>
                <c:otherwise>
                    <div class="avatar-placeholder">
                        ${fn:substring(user.fullName, 0, 1)}
                    </div>
                </c:otherwise>
            </c:choose>

            <%-- Form upload avatar --%>
            <form method="post"
                  action="${pageContext.request.contextPath}/profile/avatar"
                  enctype="multipart/form-data"
                  class="avatar-upload-form">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <label class="btn btn-small btn-secondary">
                    Đổi ảnh
                    <input type="file" name="avatarFile" accept="image/*"
                           style="display:none;" onchange="this.form.submit()">
                </label>
            </form>
        </div>

        <%-- Thông tin --%>
        <div class="profile-info">
            <table class="info-table">
                <tr>
                    <th>Họ tên:</th>
                    <td>${user.fullName}</td>
                </tr>
                <tr>
                    <th>Email:</th>
                    <td>${user.email}</td>
                </tr>
                <tr>
                    <th>Số điện thoại:</th>
                    <td>
                        <c:choose>
                            <c:when test="${not empty user.phone}">${user.phone}</c:when>
                            <c:otherwise><span class="text-muted">Chưa cập nhật</span></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <th>Trạng thái:</th>
                    <td>
                        <span class="badge badge-${user.status.toLowerCase()}">${user.status}</span>
                    </td>
                </tr>
                <tr>
                    <th>Ngày tạo:</th>
                    <td><fmt:formatDate value="${user.createdAt}" pattern="dd/MM/yyyy HH:mm"/></td>
                </tr>
                <tr>
                    <th>Vai trò:</th>
                    <td>
                        <c:forEach var="ur" items="${user.userRolesCollection}">
                            <span class="badge badge-role">${ur.roles.roleName}</span>
                        </c:forEach>
                    </td>
                </tr>
            </table>
        </div>

        <div class="profile-actions">
            <a href="${pageContext.request.contextPath}/profile/edit" class="btn btn-primary">Chỉnh sửa hồ sơ</a>
            <a href="${pageContext.request.contextPath}/change-password" class="btn btn-secondary">Đổi mật khẩu</a>
        </div>
    </div>
</div>

</body>
</html>
