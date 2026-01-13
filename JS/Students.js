/* Students page JS - Manages student CRUD with modals and localStorage / backend persistence */

(function () {
  const STORAGE_KEY = "students"; // guest local only
  const CLASSES_KEY = "classes";  // guest local only
  const TOKEN_KEY = "token";      // JWT

  // ========== AUTH / MODE ==========
  function isLoggedIn() {
    // Prefera helper-ul din api.js daca exista
    if (window.auth && typeof window.auth.hasToken === "function") {
      return window.auth.hasToken();
    }
    const t = localStorage.getItem(TOKEN_KEY);
    return !!t && t.length > 10;
  }

  function useApi() {
    return isLoggedIn();
  }

  // ========== API HELPERS ==========
  async function apiGetStudents() {
    return await apiRequest("/api/students"); // api.js adauga Authorization automat
  }
  async function apiCreateStudent(payload) {
    return await apiRequest("/api/students", { method: "POST", body: payload });
  }
  async function apiUpdateStudent(id, payload) {
    return await apiRequest(`/api/students/${encodeURIComponent(id)}`, { method: "PUT", body: payload });
  }
  async function apiDeleteStudent(id) {
    return await apiRequest(`/api/students/${encodeURIComponent(id)}`, { method: "DELETE" });
  }
  async function apiGetClasses() {
    return await apiRequest("/api/classes");
  }

  // ========== LOCAL (guest) HELPERS ==========
  function getStudentsLocal() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"); } catch { return []; }
  }
  function saveStudentsLocal(list) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
  }
  function getClassesLocal() {
    try { return JSON.parse(localStorage.getItem(CLASSES_KEY) || "[]"); } catch { return []; }
  }
  function saveClassesLocal(list) {
    localStorage.setItem(CLASSES_KEY, JSON.stringify(list));
  }

  // ========== UNIFIED DATA ACCESS ==========
  async function getStudents() {
    if (useApi()) return await apiGetStudents();
    return getStudentsLocal();
  }

  async function getClasses() {
    if (useApi()) return await apiGetClasses();
    return getClassesLocal();
  }

  async function createStudent(payload) {
    if (useApi()) return await apiCreateStudent(payload);

    const students = getStudentsLocal();
    students.push(payload);
    saveStudentsLocal(students);
    return payload;
  }

  async function updateStudent(id, payload) {
    if (useApi()) return await apiUpdateStudent(id, payload);

    let students = getStudentsLocal();
    const index = students.findIndex((s) => s.id === id);
    if (index === -1) throw new Error("Student not found");
    students[index] = { ...students[index], ...payload, id };
    saveStudentsLocal(students);
    return students[index];
  }

  async function deleteStudent(id) {
    if (useApi()) return await apiDeleteStudent(id);

    let students = getStudentsLocal();
    students = students.filter((s) => s.id !== id);
    saveStudentsLocal(students);
    return true;
  }

  // ========== UTILITY FUNCTIONS ==========
  async function generateStudentId() {
    const students = await getStudents();
    let max = 0;
    students.forEach((s) => {
      const match = (s.id || "").match(/\d+/);
      if (match) {
        const num = parseInt(match[0], 10);
        if (num > max) max = num;
      }
    });
    return "SG-" + (max + 1);
  }

  async function externalIdExists(extId, ignoreStudentId) {
    if (!extId) return false;
    const students = await getStudents();
    return students.some(
      (s) =>
        s.externalId &&
        s.externalId === extId &&
        (!ignoreStudentId || s.id !== ignoreStudentId)
    );
  }

  // guest-only (pt ca Classes nu sunt legate inca in UI)
  async function syncClassStudentCountsGuestOnly() {
    if (useApi()) return;

    let classes = getClassesLocal();
    const students = getStudentsLocal();

    classes.forEach((cls) => {
      const classStudents = students.filter(
        (s) => s.classIds && s.classIds.includes(cls.id)
      );
      cls.students = classStudents.map((s) => s.id);
      cls.studentCount = classStudents.length;
    });

    saveClassesLocal(classes);
  }

  async function populateClassesSelect(selectId) {
    const sel = document.getElementById(selectId);
    if (!sel) return;
    sel.innerHTML = "";

    let classes = [];
    try {
      classes = await getClasses();
    } catch {
      classes = [];
    }

    if (!classes.length) {
      sel.innerHTML =
        '<div style="color: #999; padding: 10px; text-align: center;"><em>No classes available</em></div>';
      return;
    }

    classes.forEach((c) => {
      const div = document.createElement("div");
      div.className = "class-checkbox-item";

      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.value = c.id;
      checkbox.className = "class-checkbox";
      checkbox.dataset.className = c.name;

      const label = document.createElement("label");
      label.textContent = c.name;

      div.appendChild(checkbox);
      div.appendChild(label);
      sel.appendChild(div);
    });
  }

  // ========== NEW STUDENT MODAL ==========
  async function openModal() {
    const modal = document.getElementById("studentModal");
    if (!modal) return;
    const form = modal.querySelector("form");
    form.reset();

    document.getElementById("student-smartgrade").value = await generateStudentId();
    await populateClassesSelect("student-classes");

    modal.classList.add("open");
    setTimeout(() => document.getElementById("student-firstname")?.focus(), 100);
  }

  function closeModal() {
    const modal = document.getElementById("studentModal");
    if (modal) modal.classList.remove("open");
  }

  async function handleCreate(e) {
    e.preventDefault();
    const firstName = document.getElementById("student-firstname").value.trim();
    const lastName = document.getElementById("student-lastname").value.trim();
    const id = document.getElementById("student-smartgrade").value.trim();
    const externalId = document.getElementById("student-external").value.trim();

    const selectedCheckboxes = document.querySelectorAll(
      "#student-classes .class-checkbox:checked"
    );
    const classIds = Array.from(selectedCheckboxes).map((cb) => cb.value);

    if (!firstName || !lastName) {
      alert("Please enter first and last name.");
      return;
    }

    try {
      if (externalId && (await externalIdExists(externalId))) {
        alert("External ID already exists. Choose a different External ID.");
        return;
      }

      // ✅ payload EXACT ca StudentDto
      const payload = {
        id,
        firstName,
        lastName,
        externalId: externalId || null,
        classIds,
      };

      const created = await createStudent(payload);
      await syncClassStudentCountsGuestOnly();
      await renderStudentCard(created);

      closeModal();
    } catch (err) {
      alert(err.message || String(err));
    }
  }

  // ========== EDIT STUDENT MODAL ==========
  async function openEditModal(student) {
    const modal = document.getElementById("editStudentModal");
    if (!modal) return;

    modal.dataset.studentId = student.id;
    modal.dataset.originalExtId = student.externalId || "";

    document.getElementById("edit-student-firstname").value = student.firstName || "";
    document.getElementById("edit-student-lastname").value = student.lastName || "";
    document.getElementById("edit-student-external").value = student.externalId || "";
    document.getElementById("edit-student-smartgrade").value = student.id || "";

    await populateClassesSelect("edit-student-classes");

    const classIds = student.classIds || [];
    document
      .querySelectorAll("#edit-student-classes .class-checkbox")
      .forEach((cb) => {
        cb.checked = classIds.includes(cb.value);
      });

    modal.classList.add("open");
  }

  function closeEditModal() {
    const modal = document.getElementById("editStudentModal");
    if (modal) modal.classList.remove("open");
  }

  async function handleEditSave(e) {
    e.preventDefault();
    const modal = document.getElementById("editStudentModal");
    const studentId = modal.dataset.studentId;
    const originalExtId = modal.dataset.originalExtId;

    const firstName = document.getElementById("edit-student-firstname").value.trim();
    const lastName = document.getElementById("edit-student-lastname").value.trim();
    const externalId = document.getElementById("edit-student-external").value.trim();

    const selectedCheckboxes = document.querySelectorAll(
      "#edit-student-classes .class-checkbox:checked"
    );
    const classIds = Array.from(selectedCheckboxes).map((cb) => cb.value);

    if (!firstName || !lastName) {
      alert("Please enter first and last name.");
      return;
    }

    try {
      if (
        externalId &&
        externalId !== originalExtId &&
        (await externalIdExists(externalId, studentId))
      ) {
        alert("External ID already exists. Choose a different External ID.");
        return;
      }

      await updateStudent(studentId, {
        firstName,
        lastName,
        externalId: externalId || null,
        classIds,
      });

      await syncClassStudentCountsGuestOnly();
      closeEditModal();
      location.reload();
    } catch (err) {
      alert(err.message || String(err));
    }
  }

  async function handleDeleteStudent() {
    if (!confirm("Are you sure you want to delete this student?")) return;

    const modal = document.getElementById("editStudentModal");
    const studentId = modal.dataset.studentId;

    try {
      await deleteStudent(studentId);
      await syncClassStudentCountsGuestOnly();
      closeEditModal();
      location.reload();
    } catch (err) {
      alert(err.message || String(err));
    }
  }

  // ========== RENDERING & DISPLAY ==========
  async function renderStudentCard(student) {
    const grid = document.querySelector(".grid");
    if (!grid) return;
    grid.style.display = "";

    const placeholder = document.querySelector(".empty-placeholder");
    if (placeholder && placeholder.parentElement)
      placeholder.parentElement.removeChild(placeholder);

    const card = document.createElement("div");
    card.className = "card";
    card.style.cursor = "pointer";

    const h3 = document.createElement("h3");
    h3.textContent = (student.firstName || "") + " " + (student.lastName || "");

    const p1 = document.createElement("p");
    p1.textContent = "SmartGrade Id: " + (student.id || "-");

    const p2 = document.createElement("p");
    p2.textContent = "External ID: " + (student.externalId || "-");

    const p3 = document.createElement("p");

    const classIds = student.classIds || [];
    let classes = [];
    try {
      classes = await getClasses();
    } catch {
      classes = [];
    }

    const classNames = classIds.map((id) => {
      const cls = classes.find((c) => c.id === id);
      return cls ? cls.name : id;
    });
    p3.textContent =
      "Classes: " + (classNames.length > 0 ? classNames.join(", ") : "-");

    const btn = document.createElement("button");
    btn.className = "btn-primary";
    btn.textContent = "View Profile";

    card.appendChild(h3);
    card.appendChild(p1);
    card.appendChild(p2);
    card.appendChild(p3);
    card.appendChild(btn);

    card.addEventListener("click", () => openEditModal(student));
    grid.appendChild(card);
  }

  async function loadExistingStudents() {
    try {
      const students = await getStudents();
      for (const s of students) {
        await renderStudentCard(s);
      }
      attachSearchListener();
      attachFilterToggles();
    } catch (err) {
      alert(err.message || String(err));
    }
  }

  // ========== SEARCH & FILTERING ==========
  function filterStudents() {
    const searchInput = (document.getElementById("search-students").value || "").toLowerCase();
    const activeFilter = document.querySelector(".filter-btn.active");
    const filterType = activeFilter ? activeFilter.getAttribute("data-filter") : "name";

    document.querySelectorAll(".card").forEach((card) => {
      const h3Text = (card.querySelector("h3").textContent || "").toLowerCase();
      const pTexts = Array.from(card.querySelectorAll("p")).map((p) => p.textContent.toLowerCase());

      let match = true;
      if (searchInput) {
        if (filterType === "name") {
          match = h3Text.includes(searchInput);
        } else if (filterType === "id") {
          const idText = pTexts.find((t) => t.includes("smartgrade id:"));
          match = idText ? idText.split("smartgrade id:")[1].trim().includes(searchInput) : false;
        } else if (filterType === "extid") {
          const extIdText = pTexts.find((t) => t.includes("external id:"));
          match = extIdText ? extIdText.split("external id:")[1].trim().includes(searchInput) : false;
        }
      }
      card.style.display = match ? "" : "none";
    });
  }

  function attachSearchListener() {
    const searchElem = document.getElementById("search-students");
    if (searchElem) searchElem.addEventListener("input", filterStudents);

    const refreshBtn = document.getElementById("refresh-btn");
    if (refreshBtn)
      refreshBtn.addEventListener("click", function (e) {
        e.preventDefault();
        document.getElementById("search-students").value = "";
        filterStudents();
      });
  }

  function attachFilterToggles() {
    document.querySelectorAll(".filter-btn").forEach((btn) => {
      btn.addEventListener("click", function (e) {
        e.preventDefault();
        document.querySelectorAll(".filter-btn").forEach((b) => b.classList.remove("active"));
        this.classList.add("active");
        filterStudents();
      });
    });
  }

  // ========== INITIALIZATION ==========
  window.openNewStudentModal = () => {
    openModal();
  };

  document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("studentModal");
    if (!modal) return;

    document.getElementById("student-cancel").addEventListener("click", (e) => {
      e.preventDefault();
      closeModal();
    });
    document.getElementById("student-form").addEventListener("submit", (e) => {
      handleCreate(e);
    });
    document.getElementById("student-create").addEventListener("click", (e) => {
      e.preventDefault();
      document.getElementById("student-form").dispatchEvent(new Event("submit", { cancelable: true }));
    });

    const editModal = document.getElementById("editStudentModal");
    if (editModal) {
      document.getElementById("edit-cancel").addEventListener("click", (e) => {
        e.preventDefault();
        closeEditModal();
      });
      document.getElementById("edit-student-form").addEventListener("submit", (e) => {
        handleEditSave(e);
      });
      document.getElementById("edit-save").addEventListener("click", (e) => {
        e.preventDefault();
        document.getElementById("edit-student-form").dispatchEvent(new Event("submit", { cancelable: true }));
      });
      document.getElementById("delete-student").addEventListener("click", (e) => {
        e.preventDefault();
        handleDeleteStudent();
      });
    }

    (async () => {
      await loadExistingStudents();
    })();

    document.addEventListener("visibilitychange", function () {
      if (!document.hidden) {
        const grid = document.querySelector(".grid");
        const placeholder = document.querySelector(".empty-placeholder");
        if (grid) grid.innerHTML = "";
        if (placeholder && placeholder.parentElement) placeholder.parentElement.removeChild(placeholder);

        (async () => {
          await loadExistingStudents();
        })();
      }
    });
  });
})();
