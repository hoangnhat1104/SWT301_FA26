<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu — EduSphere</title>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            min-height: 100vh;
            background: url('${pageContext.request.contextPath}/assets/images/banner.png') center center / cover no-repeat;
            display: flex; align-items: center; justify-content: flex-end;
            padding: 24px 80px 24px 24px;
        }
        .form-card {
            background: rgba(255,255,255,0.97);
            backdrop-filter: blur(12px);
            border-radius: 20px; width: 100%; max-width: 410px;
            box-shadow: 0 20px 60px rgba(25,118,210,0.18), 0 0 0 1px rgba(255,255,255,0.6);
            overflow: hidden;
            animation: slideIn 0.45s cubic-bezier(0.34,1.56,0.64,1) both;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        .form-card:hover { transform: translateY(-4px); box-shadow: 0 28px 70px rgba(25,118,210,0.22); }
        @keyframes slideIn {
            from { opacity:0; transform:translateX(40px) scale(0.97); }
            to   { opacity:1; transform:translateX(0) scale(1); }
        }
        .top-bar {
            background: linear-gradient(135deg, #1976d2, #42a5f5);
            padding: 26px 32px; text-align: center;
        }
        .top-bar .icon { font-size: 2.2rem; display: block; margin-bottom: 8px; }
        .top-bar h2 { color: #fff; font-size: 1.4rem; font-weight: 800; }
        .top-bar p  { color: rgba(255,255,255,0.85); font-size: 0.84rem; margin-top: 4px; }
        .card-body { padding: 32px 36px 28px; }
        .form-subtitle { font-size: 0.88rem; color: #78909c; line-height: 1.65; margin-bottom: 24px; text-align: center; }

        .alert { padding: 11px 15px; border-radius: 10px; margin-bottom: 18px; font-size: 0.86rem; display: flex; align-items: center; gap: 8px; }
        .alert-error { background: #fdecea; color: #c62828; border-left: 4px solid #e53935; }

        .input-group {
            display: flex; align-items: center;
            border: 1.8px solid #e8edf5; border-radius: 12px;
            padding: 0 15px; margin-bottom: 14px; background: #f5f8ff;
            transition: border-color 0.25s, box-shadow 0.25s, background 0.25s;
        }
        .input-group:hover { border-color: #90caf9; background: #f0f6ff; }
        .input-group:focus-within { border-color: #1976d2; box-shadow: 0 0 0 4px rgba(25,118,210,0.12); background: #fff; }
        .input-icon { font-size: 1rem; color: #90a4ae; margin-right: 11px; transition: color 0.2s; }
        .input-group:focus-within .input-icon { color: #1976d2; }
        .input-group input { flex: 1; border: none; background: transparent; padding: 13px 0; font-size: 0.92rem; color: #37474f; outline: none; }
        .input-group input::placeholder { color: #b0bec5; }
        .toggle-eye { cursor: pointer; font-size: 1rem; color: #b0bec5; margin-left: 8px; user-select: none; transition: color 0.2s; }
        .toggle-eye:hover { color: #1976d2; }

        /* Password strength */
        .strength-bar { height: 4px; border-radius: 4px; margin: -8px 0 14px; background: #e8edf5; overflow: hidden; }
        .strength-fill { height: 100%; border-radius: 4px; width: 0; transition: width 0.3s, background 0.3s; }

        .btn-submit {
            width: 100%; padding: 14px;
            background: linear-gradient(135deg, #1976d2, #1e88e5);
            color: #fff; border: none; border-radius: 12px;
            font-size: 1rem; font-weight: 700; cursor: pointer;
            box-shadow: 0 4px 15px rgba(25,118,210,0.35);
            transition: transform 0.18s, box-shadow 0.18s; margin-top: 6px; margin-bottom: 20px;
        }
        .btn-submit:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(25,118,210,0.45); }

        .back-link { display: block; text-align: center; font-size: 0.86rem; color: #90a4ae; text-decoration: none; }
        .back-link:hover { color: #1976d2; }

        @media (max-width: 768px) { body { justify-content: center; padding: 20px; } }
    </style>
</head>
<body>
<div class="form-card">
    <div class="top-bar">
        <span class="icon">🔒</span>
        <h2>Đặt lại mật khẩu</h2>
        <p>Tạo mật khẩu mới cho tài khoản</p>
    </div>
    <div class="card-body">
        <p class="form-subtitle">Mật khẩu mới phải có ít nhất <strong>8 ký tự</strong>.</p>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-error">✕ ${errorMessage}</div>
        </c:if>

        <%-- Form đơn giản, không dùng form:form vì không cần token --%>
        <form method="post" action="${pageContext.request.contextPath}/reset-password">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <div class="input-group">
                <span class="input-icon">🔒</span>
                <input type="password" id="pwd1" name="newPassword"
                       placeholder="Mật khẩu mới" required minlength="8"
                       oninput="checkStrength(this.value)">
                <span class="toggle-eye" onclick="toggle('pwd1')">👁</span>
            </div>
            <div class="strength-bar"><div class="strength-fill" id="strengthFill"></div></div>

            <div class="input-group">
                <span class="input-icon">🔒</span>
                <input type="password" id="pwd2" name="confirmPassword"
                       placeholder="Xác nhận mật khẩu mới" required minlength="8">
                <span class="toggle-eye" onclick="toggle('pwd2')">👁</span>
            </div>

            <button type="submit" class="btn-submit">Đặt lại mật khẩu →</button>
        </form>

        <a href="${pageContext.request.contextPath}/login" class="back-link">← Quay lại đăng nhập</a>
    </div>
</div>

<script>
    function toggle(id) {
        const i = document.getElementById(id);
        i.type = i.type === 'password' ? 'text' : 'password';
    }

    function checkStrength(val) {
        const fill = document.getElementById('strengthFill');
        let strength = 0;
        if (val.length >= 8)  strength++;
        if (/[A-Z]/.test(val)) strength++;
        if (/[0-9]/.test(val)) strength++;
        if (/[^A-Za-z0-9]/.test(val)) strength++;
        const colors = ['#e53935','#ff9800','#fdd835','#43a047'];
        const widths = ['25%','50%','75%','100%'];
        fill.style.width     = strength > 0 ? widths[strength-1] : '0';
        fill.style.background = strength > 0 ? colors[strength-1] : '';
    }
</script>
</body>
</html>
