/* ========== AUTH (GUEST MODE + JWT) ========== */

(function () {
  const TOKEN_KEY = "token";

  function hasToken() {
    const t = localStorage.getItem(TOKEN_KEY);
    return !!t && t.length > 10;
  }

  // OPTIONAL: UI toggles (pui in HTML elemente cu data-auth="required" / "guest")
  function applyAuthUI() {
    const logged = hasToken();

    document.querySelectorAll("[data-auth='required']").forEach(el => {
      el.style.display = logged ? "" : "none";
    });

    document.querySelectorAll("[data-auth='guest']").forEach(el => {
      el.style.display = logged ? "none" : "";
    });
  }

  // logout global
  window.logout = function () {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    // ramai in site ca guest
    window.location.href = "Account.html"; // sau "Home.html" daca preferi
  };

  document.addEventListener("DOMContentLoaded", function () {
    applyAuthUI();
  });
})();


/* ========== SHARED PAGE LOGIC ========== */

/* Toggle sidebar menu */
function toggleMenu() {
  const sidebar = document.getElementById("sidebar-menu");
  const overlay = document.getElementById("sidebar-overlay");
  const mainContent = document.querySelector(".main-content");

  if (!sidebar || !overlay) return;

  sidebar.classList.toggle("open");
  overlay.classList.toggle("open");
  mainContent.classList.toggle("sidebar-open");
}

/* Close menu when clicking a link */
document.addEventListener("DOMContentLoaded", function () {
  const sidebarLinks = document.querySelectorAll(".sidebar-link");
  const closeBtn = document.getElementById("close-menu");

  sidebarLinks.forEach((link) => {
    link.addEventListener("click", () => {
      const sidebar = document.getElementById("sidebar-menu");
      const overlay = document.getElementById("sidebar-overlay");
      const mainContent = document.querySelector(".main-content");

      if (sidebar && overlay && mainContent) {
        sidebar.classList.remove("open");
        overlay.classList.remove("open");
        mainContent.classList.remove("sidebar-open");
      }
    });
  });

  if (closeBtn) {
    closeBtn.addEventListener("click", toggleMenu);
  }
});

/* Dispatch 'New' button to page-specific handlers */
function createNew() {
  const center = document.querySelector(".navbar-center");
  const page = center ? center.textContent.trim() : "";

  if (page === "Students" && typeof window.openNewStudentModal === "function") {
    window.openNewStudentModal();
    return;
  }

  if (page === "Classes" && typeof window.openNewClassModal === "function") {
    window.openNewClassModal();
    return;
  }

  if (page === "Quizzes" && typeof window.openNewQuizModal === "function") {
    window.openNewQuizModal();
    return;
  }
}

/* Show empty placeholder when no cards exist */
function showEmptyPlaceholderIfNeeded() {
  const grid = document.querySelector(".grid");
  if (!grid) return;

  const cards = grid.querySelectorAll(".card");
  if (cards.length > 0) return;

  const center = document.querySelector(".navbar-center");
  let name = center ? center.textContent.trim() : "Items";
  const singular =
    { Quizzes: "quiz", Students: "student", Classes: "class" }[name] ||
    name.toLowerCase();

  const placeholder = document.createElement("div");
  placeholder.className = "empty-placeholder";
  placeholder.innerHTML = `No ${name} created. Press "<strong>New</strong>" to create a new ${singular}.`;

  grid.style.display = "none";
  const container = document.querySelector(".container") || grid.parentElement;
  if (container) container.appendChild(placeholder);
}

document.addEventListener("DOMContentLoaded", showEmptyPlaceholderIfNeeded);
