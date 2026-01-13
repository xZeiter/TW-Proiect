const API_BASE_URL = "http://localhost:8080";

const TOKEN_KEY = "token";
const USER_KEY = "user";

const GUEST_PREFIX = "guest_";
const GUEST_STUDENTS_KEY = GUEST_PREFIX + "students";
const GUEST_CLASSES_KEY  = GUEST_PREFIX + "classes";
const GUEST_QUIZZES_KEY  = GUEST_PREFIX + "quizzes";

function hasToken() {
  const t = localStorage.getItem(TOKEN_KEY);
  return !!t && t.length > 10;
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

/* ---------------- API request ---------------- */
async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };

  if (auth) {
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(API_BASE_URL + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!res.ok) {
    if (res.status === 401) clearAuth();
    const msg = (data && data.error) ? data.error : `Request failed: ${res.status}`;
    throw new Error(msg);
  }

  return data;
}

/* ---------------- Download (blob) ---------------- */
async function apiDownloadBlob(pathOrUrl, { filename = "papers.pdf", auth = true } = {}) {
  const headers = {};

  if (auth) {
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  const url = (pathOrUrl || "").startsWith("http")
    ? pathOrUrl
    : (API_BASE_URL + pathOrUrl);

  const res = await fetch(url, { method: "GET", headers });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    if (res.status === 401) clearAuth();
    throw new Error(text || `Download failed: ${res.status}`);
  }

  const blob = await res.blob();
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(a.href);
}

/* ---------------- Guest local DB ---------------- */
function _load(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}
function _save(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}
function _uuid() {
  return "g_" + Math.random().toString(16).slice(2) + Date.now().toString(16);
}

const guestStore = {
  students: {
    list() { return _load(GUEST_STUDENTS_KEY, []); },
    create(student) {
      const all = _load(GUEST_STUDENTS_KEY, []);
      const item = { id: _uuid(), ...student };
      all.push(item);
      _save(GUEST_STUDENTS_KEY, all);
      return item;
    },
    update(id, patch) {
      const all = _load(GUEST_STUDENTS_KEY, []);
      const idx = all.findIndex(s => s.id === id);
      if (idx === -1) throw new Error("Student not found (guest)");
      all[idx] = { ...all[idx], ...patch };
      _save(GUEST_STUDENTS_KEY, all);
      return all[idx];
    },
    remove(id) {
      const all = _load(GUEST_STUDENTS_KEY, []);
      _save(GUEST_STUDENTS_KEY, all.filter(s => s.id !== id));
      return true;
    }
  },

  classes: {
    list() { return _load(GUEST_CLASSES_KEY, []); },
    create(cls) {
      const all = _load(GUEST_CLASSES_KEY, []);
      const item = { id: _uuid(), ...cls };
      all.push(item);
      _save(GUEST_CLASSES_KEY, all);
      return item;
    },
    update(id, patch) {
      const all = _load(GUEST_CLASSES_KEY, []);
      const idx = all.findIndex(c => c.id === id);
      if (idx === -1) throw new Error("Class not found (guest)");
      all[idx] = { ...all[idx], ...patch };
      _save(GUEST_CLASSES_KEY, all);
      return all[idx];
    },
    remove(id) {
      const all = _load(GUEST_CLASSES_KEY, []);
      _save(GUEST_CLASSES_KEY, all.filter(c => c.id !== id));
      return true;
    }
  },

  quizzes: {
    list() { return _load(GUEST_QUIZZES_KEY, []); },
    create(q) {
      const all = _load(GUEST_QUIZZES_KEY, []);
      const item = { id: _uuid(), ...q };
      all.push(item);
      _save(GUEST_QUIZZES_KEY, all);
      return item;
    },
    update(id, patch) {
      const all = _load(GUEST_QUIZZES_KEY, []);
      const idx = all.findIndex(q => q.id === id);
      if (idx === -1) throw new Error("Quiz not found (guest)");
      all[idx] = { ...all[idx], ...patch };
      _save(GUEST_QUIZZES_KEY, all);
      return all[idx];
    },
    remove(id) {
      const all = _load(GUEST_QUIZZES_KEY, []);
      _save(GUEST_QUIZZES_KEY, all.filter(q => q.id !== id));
      return true;
    }
  }
};

/* ---------------- Unified DATA API ---------------- */
const data = {
  isLoggedIn: () => hasToken(),

  students: {
    async list() {
      if (hasToken()) return apiRequest("/api/students");
      return guestStore.students.list();
    },
    async create(student) {
      if (hasToken()) return apiRequest("/api/students", { method: "POST", body: student });
      return guestStore.students.create(student);
    },
    async update(id, patch) {
      if (hasToken()) return apiRequest(`/api/students/${id}`, { method: "PUT", body: patch });
      return guestStore.students.update(id, patch);
    },
    async remove(id) {
      if (hasToken()) return apiRequest(`/api/students/${id}`, { method: "DELETE" });
      return guestStore.students.remove(id);
    }
  },

  classes: {
    async list() {
      if (hasToken()) return apiRequest("/api/classes");
      return guestStore.classes.list();
    },
    async create(cls) {
      if (hasToken()) return apiRequest("/api/classes", { method: "POST", body: cls });
      return guestStore.classes.create(cls);
    },
    async update(id, patch) {
      if (hasToken()) return apiRequest(`/api/classes/${id}`, { method: "PUT", body: patch });
      return guestStore.classes.update(id, patch);
    },
    async remove(id) {
      if (hasToken()) return apiRequest(`/api/classes/${id}`, { method: "DELETE" });
      return guestStore.classes.remove(id);
    }
  },

  quizzes: {
    async list() {
      if (hasToken()) return apiRequest("/api/quizzes");
      return guestStore.quizzes.list();
    },
    async create(q) {
      if (hasToken()) return apiRequest("/api/quizzes", { method: "POST", body: q });
      return guestStore.quizzes.create(q);
    },
    async update(id, patch) {
      if (hasToken()) return apiRequest(`/api/quizzes/${id}`, { method: "PUT", body: patch });
      return guestStore.quizzes.update(id, patch);
    },
    async remove(id) {
      if (hasToken()) return apiRequest(`/api/quizzes/${id}`, { method: "DELETE" });
      return guestStore.quizzes.remove(id);
    }
  }
};

window.apiRequest = apiRequest;
window.apiDownloadBlob = apiDownloadBlob;
window.data = data;
window.auth = { hasToken, getToken, clearAuth };
