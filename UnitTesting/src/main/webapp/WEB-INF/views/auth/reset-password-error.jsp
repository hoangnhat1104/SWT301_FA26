<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Link không hợp lệ — EduSphere</title>
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
            box-shadow: 0 20px 60px rgba(25,118,210,0.18);
            overflow: hidden;
            animation: slideIn 0.45s cubic-bezier(0.34,1.56,0.64,1) both;
        }
        @keyframes slideIn {
            from { opacity:0; transform:translateX(40px) scale(0.97); }
            to   { opacity:1; transform:translateX(0) scale(1); }
        }
        .top-bar {
            background: linear-gradient(135deg, #e53935, #ef5350);
            padding: 28px 32px; text-align: center;
        }
        .top-bar .icon { font-size: 2.4rem; display: block; margin-bottom: 8px; }
        .top-bar h2 { color: #fff; font-size: 1.4rem; font-weight: 800; }
        .card-body { padding: 32px 36px; text-align: center; }
        .error-msg { color: #c62828; font-size: 0.92rem; margin-bottom: 24px; line-height: 1.6; }
        .btn-primary {
            display: inline-block; padding: 13px 28px;
            background: linear-gradient(135deg, #1976d2, #1e88e5);
            color: #fff; border-radius: 12px; text-decoration: none;
            font-weight: 700; font-size: 0.95rem;
            box-shadow: 0 4px 15px rgba(25,118,210,0.35);
            transition: transform 0.18s, box-shadow 0.18s;
        }
        .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(25,118,210,0.45); }
        @media (max-width: 768px) { body { justify-content: center; padding: 20px; } }
    </style>
</head>
<body>
<div class="form-card">
    <div class="top-bar">
        <span class="icon">⚠️</span>
        <h2>Link không hợp lệ</h2>
    </div>
    <div class="card-body">
        <p class="error-msg">${errorMessage}</p>
        <a href="${pageContext.request.contextPath}/forgot-password" class="btn-primary">
            Yêu cầu link mới →
        </a>
    </div>
</div>
</body>
</html>
