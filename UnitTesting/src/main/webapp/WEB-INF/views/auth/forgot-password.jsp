<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu — EduSphere</title>
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
            background: rgba(255,255,255,0.97);
            backdrop-filter: blur(12px);
            border-radius: 20px;
            width: 100%;
            max-width: 410px;
            box-shadow: 0 20px 60px rgba(25,118,210,0.18), 0 0 0 1px rgba(255,255,255,0.6);
            overflow: hidden;
            animation: slideIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        .form-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 28px 70px rgba(25,118,210,0.22), 0 0 0 1px rgba(255,255,255,0.7);
        }
        @keyframes slideIn {
            from { opacity: 0; transform: translateX(40px) scale(0.97); }
            to   { opacity: 1; transform: translateX(0) scale(1); }
        }

        /* Top gradient bar */
        .top-bar {
            background: linear-gradient(135deg, #1976d2, #42a5f5);
            padding: 26px 32px;
            text-align: center;
        }
        .top-bar .icon { font-size: 2.2rem; display: block; margin-bottom: 8px; }
        .top-bar h2   { color: #fff; font-size: 1.4rem; font-weight: 800; }
        .top-bar p    { color: rgba(255,255,255,0.85); font-size: 0.84rem; margin-top: 4px; }

        .card-body { padding: 32px 36px 28px; }

        .form-subtitle {
            font-size: 0.88rem; color: #78909c;
            line-height: 1.65; margin-bottom: 24px; text-align: center;
        }

        /* Alerts */
        .alert {
            padding: 11px 15px; border-radius: 10px;
            margin-bottom: 18px; font-size: 0.86rem;
            display: flex; align-items: center; gap: 8px;
        }
        .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #43a047; }
        .alert-error   { background: #fdecea; color: #c62828; border-left: 4px solid #e53935; }

        /* Input */
        .input-group {
            display: flex; align-items: center;
            border: 1.8px solid #e8edf5; border-radius: 12px;
            padding: 0 15px; margin-bottom: 20px; background: #f5f8ff;
            transition: border-color 0.25s, box-shadow 0.25s, background 0.25s;
        }
        .input-group:hover  { border-color: #90caf9; background: #f0f6ff; }
        .input-group:focus-within {
            border-color: #1976d2;
            box-shadow: 0 0 0 4px rgba(25,118,210,0.12);
            background: #fff;
        }
        .input-icon { font-size: 1rem; color: #90a4ae; margin-right: 11px; flex-shrink: 0; transition: color 0.2s; }
        .input-group:focus-within .input-icon { color: #1976d2; }
        .input-group input {
            flex: 1; border: none; background: transparent;
            padding: 13px 0; font-size: 0.92rem; color: #37474f; outline: none;
        }
        .input-group input::placeholder { color: #b0bec5; }

        /* Field error */
        .field-error { display: block; color: #e53935; font-size: 0.8rem; margin: -14px 0 14px 4px; }

        /* Submit */
        .btn-submit {
            width: 100%; padding: 14px;
            background: linear-gradient(135deg, #1976d2, #1e88e5);
            color: #fff; border: none; border-radius: 12px;
            font-size: 1rem; font-weight: 700; cursor: pointer;
            box-shadow: 0 4px 15px rgba(25,118,210,0.35);
            transition: transform 0.18s, box-shadow 0.18s;
            margin-bottom: 20px;
        }
        .btn-submit:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(25,118,210,0.45); }
        .btn-submit:active { transform: translateY(0); }

        /* Back link */
        .back-link {
            display: block; text-align: center;
            font-size: 0.86rem; color: #90a4ae; text-decoration: none;
        }
        .back-link:hover { color: #1976d2; }

        @media (max-width: 768px) { body { justify-content: center; padding: 20px; } }
    </style>
</head>
<body>

<div class="form-card">
    <div class="top-bar">
        <span class="icon">🔑</span>
        <h2>Quên mật khẩu</h2>
        <p>Đặt lại mật khẩu của bạn</p>
    </div>

    <div class="card-body">
        <p class="form-subtitle">
            Nhập email đăng ký của bạn.<br>
            Chúng tôi sẽ gửi link đặt lại mật khẩu đến email đó.
        </p>

        <c:if test="${not empty successMessage}">
            <div class="alert alert-success">✓ ${successMessage}</div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-error">✕ ${errorMessage}</div>
        </c:if>

        <form:form method="post"
                   action="${pageContext.request.contextPath}/forgot-password"
                   modelAttribute="forgotPasswordRequest">

            <div class="input-group">
                <span class="input-icon">✉</span>
                <form:input path="email" type="email"
                            placeholder="Nhập email của bạn" cssClass="form-input"
                            style="flex:1;border:none;background:transparent;padding:13px 0;font-size:0.92rem;color:#37474f;outline:none;"/>
            </div>
            <form:errors path="email" cssClass="field-error"/>

            <button type="submit" class="btn-submit">Gửi hướng dẫn →</button>
        </form:form>

        <a href="${pageContext.request.contextPath}/login" class="back-link">← Quay lại đăng nhập</a>
    </div>
</div>

</body>
</html>
