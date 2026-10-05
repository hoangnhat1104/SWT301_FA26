<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký — EduSphere</title>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            min-height: 100vh;
            background: url('${pageContext.request.contextPath}/assets/images/banner.png') center center / cover no-repeat;
            display: flex;
            align-items: center;
            justify-content: flex-end;
            padding: 24px 80px 24px 24px;
        }

        .form-card {
            background: rgba(255, 255, 255, 0.97);
            border-radius: 16px;
            padding: 36px 36px;
            width: 100%;
            max-width: 420px;
            box-shadow: 0 8px 40px rgba(0,0,0,0.15);
        }

        .form-title { font-size: 1.7rem; font-weight: 800; color: #1a237e; margin-bottom: 4px; }
        .form-subtitle { font-size: 0.85rem; color: #78909c; line-height: 1.6; margin-bottom: 20px; }

        .alert { padding: 10px 14px; border-radius: 8px; margin-bottom: 14px; font-size: 0.85rem; }
        .alert-error { background: #fdecea; color: #c62828; border-left: 4px solid #e53935; }

        .input-group {
            display: flex; align-items: center;
            border: 1.5px solid #e3eaf5; border-radius: 10px;
            padding: 0 14px; margin-bottom: 4px; background: #f8fbff;
            transition: border-color 0.2s;
        }
        .input-group:focus-within { border-color: #1976d2; background: #fff; }
        .input-icon { font-size: 1rem; color: #90a4ae; margin-right: 10px; flex-shrink: 0; }
        .input-group input, .input-group .form-input {
            flex: 1; border: none; background: transparent;
            padding: 11px 0; font-size: 0.9rem; color: #37474f; outline: none; width: 100%;
        }
        .input-group input::placeholder,
        .input-group .form-input::placeholder { color: #b0bec5; }
        .toggle-eye { cursor: pointer; font-size: 1rem; color: #90a4ae; margin-left: 8px; user-select: none; }

        .field-error { display: block; color: #e53935; font-size: 0.78rem; margin: 2px 0 8px 4px; }
        .mb { margin-bottom: 12px; }

        .btn-submit {
            width: 100%; padding: 13px; background: #1976d2; color: #fff;
            border: none; border-radius: 10px; font-size: 1rem; font-weight: 700;
            cursor: pointer; transition: background 0.2s; margin-top: 8px;
        }
        .btn-submit:hover { background: #1565c0; }

        .divider {
            display: flex; align-items: center; gap: 10px;
            margin: 16px 0; color: #b0bec5; font-size: 0.8rem;
        }
        .divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: #e3eaf5; }

        .social-btns { display: flex; gap: 12px; margin-bottom: 16px; }
        .btn-social {
            flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px;
            padding: 10px 14px; border-radius: 10px; border: 1.5px solid #e3eaf5;
            background: #fff; font-size: 0.88rem; font-weight: 600; color: #37474f;
            text-decoration: none; transition: border-color 0.2s;
        }
        .btn-social:hover { border-color: #1976d2; }

        .form-footer { text-align: center; font-size: 0.86rem; color: #78909c; }
        .form-footer a { color: #1976d2; font-weight: 700; text-decoration: none; }

        @media (max-width: 768px) { body { justify-content: center; padding: 20px; } }
    </style>
</head>
<body>

<div class="form-card">
    <h2 class="form-title">Đăng ký</h2>
    <p class="form-subtitle">Tạo tài khoản miễn phí và bắt đầu học ngay hôm nay!</p>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">${errorMessage}</div>
    </c:if>

    <form:form method="post" action="${pageContext.request.contextPath}/register" modelAttribute="registerRequest">

        <div class="input-group mb">
            <span class="input-icon">👤</span>
            <form:input path="fullName" placeholder="Họ và tên" cssClass="form-input"/>
        </div>
        <form:errors path="fullName" cssClass="field-error"/>

        <div class="input-group mb">
            <span class="input-icon">✉</span>
            <form:input path="email" type="email" placeholder="Email" cssClass="form-input"/>
        </div>
        <form:errors path="email" cssClass="field-error"/>

        <div class="input-group mb">
            <span class="input-icon">🔒</span>
            <form:password path="password" id="pwd1" placeholder="Mật khẩu (tối thiểu 8 ký tự)" cssClass="form-input"/>
            <span class="toggle-eye" onclick="toggle('pwd1')">👁</span>
        </div>
        <form:errors path="password" cssClass="field-error"/>

        <div class="input-group mb">
            <span class="input-icon">🔒</span>
            <form:password path="confirmPassword" id="pwd2" placeholder="Xác nhận mật khẩu" cssClass="form-input"/>
            <span class="toggle-eye" onclick="toggle('pwd2')">👁</span>
        </div>
        <form:errors path="confirmPassword" cssClass="field-error"/>

        <div class="input-group mb">
            <span class="input-icon">📱</span>
            <form:input path="phone" placeholder="Số điện thoại (tuỳ chọn)" cssClass="form-input"/>
        </div>
        <form:errors path="phone" cssClass="field-error"/>

        <button type="submit" class="btn-submit">Đăng ký →</button>
    </form:form>

    <div class="divider"><span>Hoặc đăng ký bằng</span></div>

    <div class="social-btns">
        <a href="${pageContext.request.contextPath}/oauth2/authorization/google" class="btn-social">
            <svg width="18" height="18" viewBox="0 0 48 48">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
            </svg>
            Google
        </a>
        <a href="#" class="btn-social">
            <svg width="18" height="18" viewBox="0 0 21 21">
                <rect x="1" y="1" width="9" height="9" fill="#F25022"/>
                <rect x="11" y="1" width="9" height="9" fill="#7FBA00"/>
                <rect x="1" y="11" width="9" height="9" fill="#00A4EF"/>
                <rect x="11" y="11" width="9" height="9" fill="#FFB900"/>
            </svg>
            Microsoft
        </a>
    </div>

    <div class="form-footer">
        Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập ngay →</a>
    </div>
</div>

<script>
    function toggle(id) {
        const i = document.getElementById(id);
        i.type = i.type === 'password' ? 'text' : 'password';
    }
</script>
</body>
</html>
