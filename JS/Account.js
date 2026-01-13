(function () {
  const USER_KEY = "user";   // info user (id/email)
  const TOKEN_KEY = "token"; // jwt

  function openLoginModal() {
    const modal = document.getElementById("loginModal");
    if (modal) {
      modal.classList.add("open");
      setTimeout(() => document.getElementById("login-email")?.focus(), 100);
    }
  }

  function closeLoginModal() {
    const modal = document.getElementById("loginModal");
    if (modal) {
      modal.classList.remove("open");
      document.getElementById("login-form")?.reset();
    }
  }

  function openRegisterModal() {
    const modal = document.getElementById("registerModal");
    if (modal) {
      modal.classList.add("open");
      setTimeout(() => document.getElementById("register-email")?.focus(), 100);
    }
  }

  function closeRegisterModal() {
    const modal = document.getElementById("registerModal");
    if (modal) {
      modal.classList.remove("open");
      document.getElementById("register-form")?.reset();
    }
  }

  function updateUIAfterLogin(user) {
    const profileSection = document.getElementById("profile-section");
    const authSection = document.getElementById("auth-section");
    const statusMsg = document.getElementById("status-message");
    const profileUsername = document.getElementById("profile-username");
    const profileEmail = document.getElementById("profile-email");

    if (profileUsername) profileUsername.textContent = user.email.split("@")[0];
    if (profileEmail) profileEmail.textContent = user.email;

    if (profileSection) profileSection.classList.add("active");
    if (authSection) {
      const buttons = authSection.querySelector(".auth-buttons");
      if (buttons) buttons.style.display = "none";
    }
    if (statusMsg) statusMsg.style.display = "none";
  }

  function logout() {
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
    window.location.reload();
  }

  function checkLoginStatus() {
    try {
      const token = localStorage.getItem(TOKEN_KEY);
      const user = JSON.parse(localStorage.getItem(USER_KEY) || "null");
      if (token && user && user.email) {
        updateUIAfterLogin(user);
      }
    } catch {
      // ignore
    }
  }

  async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById("login-email").value.trim();
    const password = document.getElementById("login-password").value.trim();
    const errorMsg = document.getElementById("login-error");

    if (!email || !password) {
      if (errorMsg) errorMsg.textContent = "Please fill in all fields.";
      return;
    }

    try {
      const data = await apiRequest("/api/auth/login", {
        method: "POST",
        body: { email, password },
        auth: false
      });
      // data: { id, email, token }
      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(USER_KEY, JSON.stringify({ id: data.id, email: data.email }));

      closeLoginModal();
      updateUIAfterLogin({ id: data.id, email: data.email });
    } catch (e) {
      if (errorMsg) errorMsg.textContent = e.message || "Login failed.";
    }
  }

  async function handleRegister(event) {
    event.preventDefault();

    const email = document.getElementById("register-email").value.trim();
    const password = document.getElementById("register-password").value.trim();
    const confirm = document.getElementById("register-confirm").value.trim();
    const terms = document.getElementById("register-terms").checked;
    const errorMsg = document.getElementById("register-error");

    if (!email || !password || !confirm) {
      if (errorMsg) errorMsg.textContent = "Please fill in all fields.";
      return;
    }
    if (password !== confirm) {
      if (errorMsg) errorMsg.textContent = "Passwords do not match.";
      return;
    }
    if (password.length < 8) {
      if (errorMsg) errorMsg.textContent = "Password must be at least 8 characters.";
      return;
    }
    if (!terms) {
      if (errorMsg) errorMsg.textContent = "You must accept the Terms of Service and Privacy Policy.";
      return;
    }

    try {
      const data = await apiRequest("/api/auth/register", {
        method: "POST",
        body: { email, password },
        auth: false
      });
      // data: { id, email, token }
      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(USER_KEY, JSON.stringify({ id: data.id, email: data.email }));

      closeRegisterModal();
      updateUIAfterLogin({ id: data.id, email: data.email });
    } catch (e) {
      if (errorMsg) errorMsg.textContent = e.message || "Register failed.";
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    checkLoginStatus();

    const loginCancel = document.getElementById("login-cancel");
    const registerCancel = document.getElementById("register-cancel");
    const loginSubmit = document.getElementById("login-submit");
    const registerSubmit = document.getElementById("register-submit");

    if (loginCancel) loginCancel.addEventListener("click", closeLoginModal);
    if (registerCancel) registerCancel.addEventListener("click", closeRegisterModal);
    if (loginSubmit) loginSubmit.addEventListener("click", handleLogin);
    if (registerSubmit) registerSubmit.addEventListener("click", handleRegister);

    const loginForm = document.getElementById("login-form");
    const registerForm = document.getElementById("register-form");

    if (loginForm) {
      loginForm.addEventListener("submit", handleLogin);
    }
    if (registerForm) {
      registerForm.addEventListener("submit", handleRegister);
    }
  });

  window.openLoginModal = openLoginModal;
  window.closeLoginModal = closeLoginModal;
  window.openRegisterModal = openRegisterModal;
  window.closeRegisterModal = closeRegisterModal;
  window.handleLogin = handleLogin;
  window.handleRegister = handleRegister;
  window.logout = logout;
})();
