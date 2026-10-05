<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập — EduSphere</title>
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

        /* ===== CARD ===== */
        .form-card {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border-radius: 20px;
            padding: 44px 40px;
            width: 100%;
            max-width: 410px;
            box-shadow:
                0 4px 6px rgba(0,0,0,0.04),
                0 20px 60px rgba(25, 118, 210, 0.15),
                0 0 0 1px rgba(255,255,255,0.6);
            /* Animation xuất hiện */
            animation: slideIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        /* Hover card: nhẹ nhàng nổi lên */
        .form-card:hover {
            transform: translateY(-4px);
            box-shadow:
                0 8px 12px rgba(0,0,0,0.06),
                0 28px 70px rgba(25, 118, 210, 0.22),
                0 0 0 1px rgba(255,255,255,0.7);
        }

        @keyframes slideIn {
            from { opacity: 0; transform: translateX(40px) scale(0.97); }
            to   { opacity: 1; transform: translateX(0)     scale(1); }
        }

        /* ===== HEADER ===== */
        .brand-row {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 20px;
        }
        .brand-icon {
            width: 36px; height: 36px;
            background: linear-gradient(135deg, #1976d2, #42a5f5);
            border-radius: 10px;
            display: flex; align-items: center; justify-content: center;
            font-size: 1.1rem;
        }
        .brand-name { font-size: 1.1rem; font-weight: 800; color: #1565c0; }

        .form-title { font-size: 1.65rem; font-weight: 800; color: #1a237e; margin-bottom: 6px; }
        .form-subtitle { font-size: 0.84rem; color: #78909c; line-height: 1.65; margin-bottom: 26px; }

        /* ===== ALERTS ===== */
        .alert {
            padding: 11px 15px; border-radius: 10px;
            margin-bottom: 16px; font-size: 0.85rem;
            display: flex; align-items: center; gap: 8px;
        }
        .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #43a047; }
        .alert-error   { background: #fdecea; color: #c62828; border-left: 4px solid #e53935; }

        /* ===== INPUT GROUPS ===== */
        .input-group {
            display: flex; align-items: center;
            border: 1.8px solid #e8edf5;
            border-radius: 12px;
            padding: 0 15px;
            margin-bottom: 14px;
            background: #f5f8ff;
            transition: border-color 0.25s, box-shadow 0.25s, background 0.25s;
            position: relative;
        }
        .input-group:hover {
            border-color: #90caf9;
            background: #f0f6ff;
        }
        .input-group:focus-within {
            border-color: #1976d2;
            box-shadow: 0 0 0 4px rgba(25,118,210,0.12);
            background: #fff;
        }
        .input-icon {
            font-size: 1rem; color: #90a4ae;
            margin-right: 11px; flex-shrink: 0;
            transition: color 0.2s;
        }
        .input-group:focus-within .input-icon { color: #1976d2; }

        .input-group input {
            flex: 1; border: none; background: transparent;
            padding: 13px 0; font-size: 0.92rem;
            color: #37474f; outline: none;
        }
        .input-group input::placeholder { color: #b0bec5; }

        .toggle-eye {
            cursor: pointer; font-size: 1rem; color: #b0bec5;
            margin-left: 8px; user-select: none;
            transition: color 0.2s;
        }
        .toggle-eye:hover { color: #1976d2; }

        /* ===== OPTIONS ROW ===== */
        .form-options {
            display: flex; align-items: center;
            justify-content: space-between;
            margin-bottom: 22px; font-size: 0.84rem;
        }
        .remember { display: flex; align-items: center; gap: 8px; color: #546e7a; cursor: pointer; }
        .remember input[type="checkbox"] { accent-color: #1976d2; width: 16px; height: 16px; }
        .forgot { color: #1976d2; text-decoration: none; font-weight: 600; font-size: 0.84rem; }
        .forgot:hover { text-decoration: underline; }

        /* ===== SUBMIT BUTTON ===== */
        .btn-submit {
            width: 100%; padding: 14px;
            background: linear-gradient(135deg, #1976d2 0%, #1e88e5 100%);
            color: #fff; border: none; border-radius: 12px;
            font-size: 1rem; font-weight: 700; cursor: pointer;
            letter-spacing: 0.4px;
            box-shadow: 0 4px 15px rgba(25,118,210,0.35);
            transition: transform 0.18s, box-shadow 0.18s, background 0.2s;
            position: relative; overflow: hidden;
        }
        .btn-submit::after {
            content: '';
            position: absolute; inset: 0;
            background: linear-gradient(rgba(255,255,255,0.15), transparent);
            opacity: 0; transition: opacity 0.2s;
        }
        .btn-submit:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 22px rgba(25,118,210,0.45);
        }
        .btn-submit:hover::after { opacity: 1; }
        .btn-submit:active { transform: translateY(0); box-shadow: 0 4px 15px rgba(25,118,210,0.35); }

        /* ===== DIVIDER ===== */
        .divider {
            display: flex; align-items: center; gap: 12px;
            margin: 20px 0; color: #b0bec5; font-size: 0.8rem;
        }
        .divider::before, .divider::after {
            content: ''; flex: 1; height: 1px; background: #e8edf5;
        }

        /* ===== SOCIAL BUTTONS ===== */
        .social-btns { display: flex; gap: 12px; margin-bottom: 20px; }
        .btn-social {
            flex: 1; display: flex; align-items: center;
            justify-content: center; gap: 8px;
            padding: 11px 14px; border-radius: 12px;
            border: 1.8px solid #e8edf5; background: #fff;
            font-size: 0.88rem; font-weight: 600; color: #37474f;
            text-decoration: none; cursor: pointer;
            transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s, background 0.2s;
        }
        .btn-social:hover {
            border-color: #1976d2;
            background: #f0f6ff;
            box-shadow: 0 4px 12px rgba(25,118,210,0.12);
            transform: translateY(-2px);
        }

        /* ===== FOOTER ===== */
        .form-footer { text-align: center; font-size: 0.86rem; color: #90a4ae; }
        .form-footer a { color: #1976d2; font-weight: 700; text-decoration: none; }
        .form-footer a:hover { text-decoration: underline; }

        /* ===== DIVIDER LINE ===== */
        .top-divider {
            height: 3px;
            background: linear-gradient(90deg, #1976d2, #42a5f5, #1976d2);
            border-radius: 3px 3px 0 0;
            margin: -44px -40px 36px;
            border-radius: 20px 20px 0 0;
        }

        @media (max-width: 768px) {
            body { justify-content: center; padding: 20px; }
        }
    </style>
</head>
<body>

<div class="form-card">
    <!-- Thanh màu gradient trên đầu card -->
    <div class="top-divider"></div>

    <!-- Brand mini -->
    <div class="brand-row">
        <div class="brand-icon">🎓</div>
        <span class="brand-name">EduSphere</span>
    </div>

    <h2 class="form-title">Đăng nhập</h2>
    <p class="form-subtitle">
        Chào mừng bạn quay trở lại!<br>
        Hãy đăng nhập để tiếp tục hành trình học tập.
    </p>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success">✓ ${successMessage}</div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error">✕ ${errorMessage}</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <div class="input-group">
            <span class="input-icon">✉</span>
            <input type="email" name="username"
                   placeholder="Email hoặc tên đăng nhập" required autofocus>
        </div>

        <div class="input-group">
            <span class="input-icon">🔒</span>
            <input type="password" id="pwd" name="password"
                   placeholder="Mật khẩu" required>
            <span class="toggle-eye" onclick="togglePwd()" title="Hiện/ẩn mật khẩu">👁</span>
        </div>

        <div class="form-options">
            <label class="remember">
                <input type="checkbox" name="remember-me"> Ghi nhớ đăng nhập
            </label>
            <a href="${pageContext.request.contextPath}/forgot-password" class="forgot">Quên mật khẩu?</a>
        </div>

        <button type="submit" class="btn-submit">Đăng nhập →</button>
    </form>

    <div class="divider"><span>Hoặc đăng nhập bằng</span></div>

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
        Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký ngay →</a>
    </div>
</div>

<script>
    function togglePwd() {
        const i = document.getElementById('pwd');
        i.type = i.type === 'password' ? 'text' : 'password';
    }
</script>
</body>
</html>
