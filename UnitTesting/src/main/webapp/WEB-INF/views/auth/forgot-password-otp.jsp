<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực OTP — EduSphere</title>
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
            border-radius: 20px; width: 100%; max-width: 440px;
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
            background: linear-gradient(135deg, #e53935, #ef5350);
            padding: 26px 32px; text-align: center;
        }
        .top-bar .icon { font-size: 2.2rem; display: block; margin-bottom: 8px; }
        .top-bar h2 { color: #fff; font-size: 1.4rem; font-weight: 800; }
        .top-bar p  { color: rgba(255,255,255,0.85); font-size: 0.84rem; margin-top: 4px; }

        .card-body { padding: 32px 36px 28px; }
        .info-text { text-align: center; font-size: 0.9rem; color: #546e7a; margin-bottom: 28px; line-height: 1.65; }
        .info-text strong { color: #c62828; }

        .alert { padding: 11px 15px; border-radius: 10px; margin-bottom: 20px; font-size: 0.86rem; display: flex; align-items: center; gap: 8px; }
        .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #43a047; }
        .alert-error   { background: #fdecea; color: #c62828; border-left: 4px solid #e53935; }

        /* OTP boxes */
        .otp-inputs { display: flex; justify-content: center; gap: 10px; margin-bottom: 28px; }
        .otp-box {
            width: 52px; height: 60px;
            border: 2px solid #e3eaf5; border-radius: 12px;
            font-size: 1.6rem; font-weight: 800; text-align: center;
            color: #c62828; background: #fff5f5; outline: none;
            transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
            caret-color: transparent;
        }
        .otp-box:focus { border-color: #e53935; box-shadow: 0 0 0 4px rgba(229,57,53,0.15); background: #fff; transform: scale(1.08); }
        .otp-box.filled { border-color: #e53935; background: #fdecea; }

        .btn-submit {
            width: 100%; padding: 14px;
            background: linear-gradient(135deg, #e53935, #ef5350);
            color: #fff; border: none; border-radius: 12px;
            font-size: 1rem; font-weight: 700; cursor: pointer;
            box-shadow: 0 4px 15px rgba(229,57,53,0.35);
            transition: transform 0.18s, box-shadow 0.18s; margin-bottom: 16px;
        }
        .btn-submit:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(229,57,53,0.45); }
        .btn-submit:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

        .resend-section { text-align: center; font-size: 0.86rem; color: #90a4ae; margin-bottom: 12px; }
        .countdown { color: #e53935; font-weight: 700; }
        .btn-resend { background: none; border: none; color: #e53935; font-weight: 700; cursor: pointer; font-size: 0.86rem; text-decoration: underline; display: none; }

        .back-link { display: block; text-align: center; font-size: 0.84rem; color: #90a4ae; text-decoration: none; }
        .back-link:hover { color: #1976d2; }

        @media (max-width: 768px) { body { justify-content: center; padding: 20px; } }
    </style>
</head>
<body>
<div class="form-card">
    <div class="top-bar">
        <span class="icon">🔑</span>
        <h2>Xác thực OTP</h2>
        <p>Xác minh để đặt lại mật khẩu</p>
    </div>
    <div class="card-body">
        <p class="info-text">
            Chúng tôi đã gửi mã OTP 6 số đến<br>
            <strong>${maskedEmail}</strong><br>
            Mã có hiệu lực trong <strong>10 phút</strong>.
        </p>

        <c:if test="${not empty successMessage}">
            <div class="alert alert-success">✓ ${successMessage}</div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-error">✕ ${errorMessage}</div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/forgot-password/verify-otp" id="otpForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <input type="hidden" name="otp" id="realOtp"/>

            <div class="otp-inputs">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b0">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b1">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b2">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b3">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b4">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b5">
            </div>

            <button type="submit" class="btn-submit" id="submitBtn" disabled>Xác thực ngay →</button>
        </form>

        <div class="resend-section">
            <span id="countdownText">Gửi lại mã sau <span class="countdown" id="timer">10:00</span></span>
            <form method="post" action="${pageContext.request.contextPath}/forgot-password/resend-otp" style="display:inline;">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <button type="submit" class="btn-resend" id="resendBtn">Gửi lại mã OTP</button>
            </form>
        </div>

        <a href="${pageContext.request.contextPath}/forgot-password" class="back-link">← Quay lại</a>
    </div>
</div>

<script>
    const boxes = document.querySelectorAll('.otp-box');
    const realOtp = document.getElementById('realOtp');
    const submitBtn = document.getElementById('submitBtn');
    boxes[0].focus();

    boxes.forEach((box, i) => {
        box.addEventListener('input', () => {
            const val = box.value.replace(/\D/g, '');
            box.value = val.slice(-1);
            if (box.value) {
                box.classList.add('filled');
                if (i < 5) boxes[i + 1].focus();
            } else {
                box.classList.remove('filled');
            }
            updateOtp();
        });
        box.addEventListener('keydown', (e) => {
            if (e.key === 'Backspace' && !box.value && i > 0) {
                boxes[i-1].value = ''; boxes[i-1].classList.remove('filled'); boxes[i-1].focus();
                updateOtp();
            }
            if (e.key === 'ArrowLeft'  && i > 0) boxes[i-1].focus();
            if (e.key === 'ArrowRight' && i < 5) boxes[i+1].focus();
        });
        box.addEventListener('paste', (e) => {
            e.preventDefault();
            const t = (e.clipboardData||window.clipboardData).getData('text').replace(/\D/g,'').slice(0,6);
            t.split('').forEach((c,j)=>{if(boxes[j]){boxes[j].value=c;boxes[j].classList.add('filled');}});
            updateOtp(); boxes[Math.min(t.length,5)].focus();
        });
        box.addEventListener('click', () => box.select());
    });

    function updateOtp() {
        const val = Array.from(boxes).map(b=>b.value).join('');
        realOtp.value = val;
        submitBtn.disabled = val.length !== 6;
    }

    let sec = 600;
    const timerEl = document.getElementById('timer');
    const countdownText = document.getElementById('countdownText');
    const resendBtn = document.getElementById('resendBtn');
    const iv = setInterval(() => {
        sec--;
        if (sec <= 0) { clearInterval(iv); countdownText.style.display='none'; resendBtn.style.display='inline'; return; }
        timerEl.textContent = String(Math.floor(sec/60)).padStart(2,'0')+':'+String(sec%60).padStart(2,'0');
        if (sec < 60) timerEl.style.color = '#e53935';
    }, 1000);
</script>
</body>
</html>
