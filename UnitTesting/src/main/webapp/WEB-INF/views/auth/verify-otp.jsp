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
            display: flex;
            align-items: center;
            justify-content: flex-end;
            padding: 24px 80px 24px 24px;
        }

        /* Card */
        .otp-card {
            background: rgba(255,255,255,0.97);
            backdrop-filter: blur(12px);
            border-radius: 20px;
            width: 100%;
            max-width: 440px;
            margin: 20px;
            box-shadow: 0 20px 60px rgba(25,118,210,0.18), 0 0 0 1px rgba(255,255,255,0.6);
            overflow: hidden;
            animation: popIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
        }

        @keyframes popIn {
            from { opacity: 0; transform: scale(0.9) translateY(20px); }
            to   { opacity: 1; transform: scale(1)   translateY(0); }
        }

        /* Top bar */
        .top-bar {
            background: linear-gradient(135deg, #1976d2, #42a5f5);
            padding: 24px 32px;
            text-align: center;
        }
        .top-bar .icon { font-size: 2.5rem; margin-bottom: 8px; display: block; }
        .top-bar h2 { color: #fff; font-size: 1.4rem; font-weight: 800; }
        .top-bar p  { color: rgba(255,255,255,0.85); font-size: 0.85rem; margin-top: 4px; }

        /* Body */
        .card-body { padding: 36px 36px 28px; }

        .info-text {
            text-align: center;
            font-size: 0.9rem;
            color: #546e7a;
            margin-bottom: 28px;
            line-height: 1.6;
        }
        .info-text strong { color: #1565c0; }

        /* Alerts */
        .alert {
            padding: 11px 15px; border-radius: 10px;
            margin-bottom: 20px; font-size: 0.86rem;
            display: flex; align-items: center; gap: 8px;
        }
        .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #43a047; }
        .alert-error   { background: #fdecea; color: #c62828; border-left: 4px solid #e53935; }

        /* OTP input boxes */
        .otp-inputs {
            display: flex;
            justify-content: center;
            gap: 10px;
            margin-bottom: 28px;
        }
        .otp-box {
            width: 52px; height: 60px;
            border: 2px solid #e3eaf5;
            border-radius: 12px;
            font-size: 1.6rem; font-weight: 800;
            text-align: center;
            color: #1565c0;
            background: #f5f8ff;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s, background 0.2s, transform 0.15s;
            caret-color: transparent;
        }
        .otp-box:focus {
            border-color: #1976d2;
            box-shadow: 0 0 0 4px rgba(25,118,210,0.15);
            background: #fff;
            transform: scale(1.08);
        }
        .otp-box.filled {
            border-color: #1976d2;
            background: #e8f0fe;
        }

        /* Hidden real input */
        #realOtp { display: none; }

        /* Submit */
        .btn-submit {
            width: 100%; padding: 14px;
            background: linear-gradient(135deg, #1976d2, #1e88e5);
            color: #fff; border: none; border-radius: 12px;
            font-size: 1rem; font-weight: 700; cursor: pointer;
            box-shadow: 0 4px 15px rgba(25,118,210,0.35);
            transition: transform 0.18s, box-shadow 0.18s;
            margin-bottom: 16px;
        }
        .btn-submit:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 22px rgba(25,118,210,0.45);
        }
        .btn-submit:disabled {
            opacity: 0.6; cursor: not-allowed; transform: none;
        }

        /* Countdown + resend */
        .resend-section { text-align: center; font-size: 0.86rem; color: #90a4ae; }
        .countdown { color: #e53935; font-weight: 700; }
        .btn-resend {
            background: none; border: none; color: #1976d2;
            font-weight: 700; cursor: pointer; font-size: 0.86rem;
            text-decoration: underline; display: none;
        }
        .btn-resend:hover { color: #1565c0; }

        /* Back link */
        .back-link {
            display: block; text-align: center;
            margin-top: 16px; font-size: 0.84rem;
            color: #90a4ae; text-decoration: none;
        }
        .back-link:hover { color: #1976d2; }
    </style>
</head>
<body>

<div class="otp-card">
    <!-- Top bar -->
    <div class="top-bar">
        <span class="icon">📧</span>
        <h2>Xác thực OTP</h2>
        <p>Kiểm tra hộp thư email của bạn</p>
    </div>

    <!-- Body -->
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

        <form method="post" action="${pageContext.request.contextPath}/verify-otp" id="otpForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <!-- Input ẩn chứa giá trị OTP thật -->
            <input type="hidden" name="otp" id="realOtp"/>

            <!-- 6 ô nhập OTP -->
            <div class="otp-inputs">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b0">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b1">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b2">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b3">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b4">
                <input class="otp-box" type="tel" maxlength="1" inputmode="numeric" autocomplete="off" id="b5">
            </div>

            <button type="submit" class="btn-submit" id="submitBtn" disabled>
                Xác thực ngay →
            </button>
        </form>

        <!-- Đếm ngược + gửi lại -->
        <div class="resend-section">
            <span id="countdownText">Gửi lại mã sau <span class="countdown" id="timer">10:00</span></span>
            <form method="post" action="${pageContext.request.contextPath}/resend-otp" style="display:inline;" id="resendForm">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <button type="submit" class="btn-resend" id="resendBtn">Gửi lại mã OTP</button>
            </form>
        </div>

        <a href="${pageContext.request.contextPath}/register" class="back-link">← Quay lại đăng ký</a>
    </div>
</div>

<script>
    const boxes = document.querySelectorAll('.otp-box');
    const realOtp = document.getElementById('realOtp');
    const submitBtn = document.getElementById('submitBtn');
    boxes[0].focus();

    boxes.forEach((box, i) => {

        box.addEventListener('input', () => {
            // Chỉ lấy ký tự số đầu tiên
            const val = box.value.replace(/\D/g, '');
            box.value = val.slice(-1); // giữ số cuối cùng nếu có nhiều hơn 1

            if (box.value) {
                box.classList.add('filled');
                if (i < 5) boxes[i + 1].focus(); // nhảy sang ô tiếp theo
            } else {
                box.classList.remove('filled');
            }
            updateRealOtp();
        });

        box.addEventListener('keydown', (e) => {
            if (e.key === 'Backspace') {
                if (!box.value && i > 0) {
                    boxes[i - 1].value = '';
                    boxes[i - 1].classList.remove('filled');
                    boxes[i - 1].focus();
                    updateRealOtp();
                }
            }
            if (e.key === 'ArrowLeft'  && i > 0) boxes[i - 1].focus();
            if (e.key === 'ArrowRight' && i < 5) boxes[i + 1].focus();
        });

        box.addEventListener('paste', (e) => {
            e.preventDefault();
            const text = (e.clipboardData || window.clipboardData)
                .getData('text').replace(/\D/g, '').slice(0, 6);
            text.split('').forEach((char, j) => {
                if (boxes[j]) { boxes[j].value = char; boxes[j].classList.add('filled'); }
            });
            updateRealOtp();
            boxes[Math.min(text.length, 5)].focus();
        });

        // Khi click vào ô đã có số → xóa để nhập lại
        box.addEventListener('click', () => {
            box.select();
        });
    });

    function updateRealOtp() {
        const val = Array.from(boxes).map(b => b.value).join('');
        realOtp.value = val;
        submitBtn.disabled = val.length !== 6;
    }

    // Đếm ngược 10 phút
    let seconds = 600;
    const timerEl = document.getElementById('timer');
    const countdownText = document.getElementById('countdownText');
    const resendBtn = document.getElementById('resendBtn');
    const countdown = setInterval(() => {
        seconds--;
        if (seconds <= 0) {
            clearInterval(countdown);
            countdownText.style.display = 'none';
            resendBtn.style.display = 'inline';
        } else {
            timerEl.textContent = String(Math.floor(seconds/60)).padStart(2,'0') + ':' + String(seconds%60).padStart(2,'0');
            if (seconds < 60) timerEl.style.color = '#e53935';
        }
    }, 1000);
</script>

</body>
</html>
