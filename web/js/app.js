// DemoBank Client Script (vanilla JS, zero frameworks, textContent for XSS safety)

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    if (loginForm) initLogin();

    const dashboard = document.getElementById('dashboard-view');
    if (dashboard) initDashboard();

    const statusGrid = document.getElementById('status-grid');
    if (statusGrid) initStatusPolling();
});

function initLogin() {
    const form = document.getElementById('login-form');
    const msgBox = document.getElementById('message-box');
    const submitBtn = document.getElementById('submit-btn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        msgBox.className = 'message-box';
        msgBox.textContent = '';
        msgBox.style.display = 'none';

        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        const username = usernameInput.value.trim();
        const password = passwordInput.value;

        if (!username || !password) {
            showMessage(msgBox, 'Please enter username and password.', 'error');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = 'Signing in...';

        try {
            const resp = await fetch('/api/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await resp.json().catch(() => ({}));

            if (resp.status === 200) {
                showMessage(msgBox, 'Login successful! Redirecting...', 'success');
                setTimeout(() => { window.location.href = '/dashboard'; }, 400);
            } else if (resp.status === 403) {
                showMessage(msgBox, data.message || 'Your IP address is blocked.', 'warning');
            } else if (resp.status === 423) {
                showMessage(msgBox, data.message || 'This account is temporarily locked.', 'warning');
            } else if (resp.status === 401) {
                showMessage(msgBox, 'Invalid username or password.', 'error');
            } else {
                showMessage(msgBox, data.message || 'Authentication error', 'error');
            }
        } catch (err) {
            showMessage(msgBox, 'Server unreachable or network error', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Sign In';
        }
    });
}

function showMessage(el, text, type) {
    el.className = 'message-box message-' + type;
    el.textContent = text;
    el.style.display = 'block';
}

async function initDashboard() {
    try {
        const resp = await fetch('/api/account');
        if (resp.status === 401) {
            window.location.href = '/';
            return;
        }
        const data = await resp.json();

        document.getElementById('user-welcome').textContent = data.owner || 'Customer';
        document.getElementById('account-number').textContent = data.maskedNumber || '**** **** **** 0000';
        document.getElementById('account-balance').textContent = '$' + Number(data.balance || 0).toLocaleString('en-US', { minimumFractionDigits: 2 });

        const tbody = document.getElementById('transactions-body');
        tbody.innerHTML = '';
        if (data.transactions && Array.isArray(data.transactions)) {
            data.transactions.forEach(tx => {
                const tr = document.createElement('tr');

                const tdDate = document.createElement('td');
                tdDate.textContent = tx.date;
                tr.appendChild(tdDate);

                const tdDesc = document.createElement('td');
                tdDesc.textContent = tx.description;
                tr.appendChild(tdDesc);

                const tdAmt = document.createElement('td');
                tdAmt.textContent = (tx.amount >= 0 ? '+$' : '-$') + Math.abs(tx.amount).toFixed(2);
                tdAmt.className = tx.amount >= 0 ? 'amount-pos' : 'amount-neg';
                tr.appendChild(tdAmt);

                const tdBal = document.createElement('td');
                tdBal.textContent = '$' + Number(tx.balance).toFixed(2);
                tr.appendChild(tdBal);

                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            await fetch('/api/logout', { method: 'POST' }).catch(() => {});
            window.location.href = '/';
        });
    }
}

function initStatusPolling() {
    const updateStats = async () => {
        try {
            const resp = await fetch('/api/stats');
            if (resp.ok) {
                const data = await resp.json();
                document.getElementById('stat-attempts').textContent = data.attempts ?? 0;
                document.getElementById('stat-failures').textContent = data.failures ?? 0;
                document.getElementById('stat-blocked').textContent = data.blockedIps ?? 0;
                document.getElementById('stat-locked').textContent = data.lockedUsers ?? 0;
            }
        } catch (ignored) {}
    };

    updateStats();
    setInterval(updateStats, 1000);
}

function fillCreds(user, pass) {
    const userInput = document.getElementById('username');
    const passInput = document.getElementById('password');
    if (userInput && passInput) {
        userInput.value = user;
        passInput.value = pass;
        userInput.focus();
    }
}
