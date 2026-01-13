/* ========== QUIZZES PAGE LOGIC (OPTIMIZED & REVIEW ENABLED) ========== */

(function () {
  const STORAGE_KEY = "quizzes";
  const CLASSES_KEY = "classes";

  function useApi() {
    const t = localStorage.getItem("token");
    return !!t && t.length > 10;
  }

  // ===== API CALLS =====
  async function apiGetQuizzes() { return await apiRequest("/api/quizzes"); }
  async function apiCreateQuiz(payload) { return await apiRequest("/api/quizzes", { method: "POST", body: payload }); }
  async function apiUpdateQuiz(id, payload) { return await apiRequest(`/api/quizzes/${encodeURIComponent(id)}`, { method: "PUT", body: payload }); }
  async function apiDeleteQuiz(id) { return await apiRequest(`/api/quizzes/${encodeURIComponent(id)}`, { method: "DELETE" }); }
  async function apiGetClasses() { return await apiRequest("/api/classes"); }
  async function apiGetStudents() { return await apiRequest("/api/students"); }

  // Update Result (Link Student)
  async function apiUpdateResultStudent(resultId, studentId) {
      console.log(`🔗 Linking Student ID ${studentId} to Result ${resultId}`);
      // studentId este acum STRING (ex: "SG-1")
      const url = `/api/sheets/results/${resultId}/student/${encodeURIComponent(studentId)}`;
      return await apiRequest(url, { method: "PUT" });
  }

  // Delete Result
  async function apiDeleteResult(resultId) {
      return await apiRequest(`/api/sheets/results/${resultId}`, { method: "DELETE" });
  }

  // Get Results for Table
  async function apiGetQuizResults(quizId) {
      return await apiRequest(`/api/sheets/quiz/${encodeURIComponent(quizId)}/results`);
  }

  async function apiGeneratePapers(quizId, payload = {}) {
    return await apiRequest(`/api/quizzes/${encodeURIComponent(quizId)}/papers/generate`, {
      method: "POST",
      body: payload
    });
  }

  // Handler Generate Paper
  async function handleGeneratePaper() {
    const view = document.getElementById("viewQuizModal");
    const quizId = view?.dataset?.quizId;
    
    const btn = document.getElementById("quiz-generate-paper");
    const originalText = btn ? btn.innerText : "";
    if (btn) { btn.innerText = "⏳ Generare..."; btn.disabled = true; }

    try {
      if (!quizId) throw new Error("No quiz selected.");
      if (!useApi()) throw new Error("You must be logged in to generate papers.");

      const resp = await apiGeneratePapers(quizId, {});
      if (!resp || !resp.downloadUrl) {
        throw new Error("Backend did not return downloadUrl.");
      }

      if (typeof window.apiDownloadBlob === 'function') {
          await window.apiDownloadBlob(resp.downloadUrl, {
            filename: `${quizId}-paper.pdf`,
            auth: true
          });
      } else {
          alert("Eroare: Funcția apiDownloadBlob lipsește!");
      }

    } catch (err) {
      alert(err.message || String(err));
    } finally {
        if (btn) { btn.innerText = originalText; btn.disabled = false; }
    }
  }

  // ===== LOCAL STORAGE HANDLERS =====
  function getQuizzesLocal() { try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"); } catch { return []; } }
  function saveQuizzesLocal(list) { localStorage.setItem(STORAGE_KEY, JSON.stringify(list)); }
  function getClassesLocal() { try { return JSON.parse(localStorage.getItem(CLASSES_KEY) || "[]"); } catch { return []; } }

  // ===== UNIFIED DATA FETCHING =====
  async function getQuizzes() { return useApi() ? await apiGetQuizzes() : getQuizzesLocal(); }
  async function getClasses() { return useApi() ? await apiGetClasses() : getClassesLocal(); }

  async function saveQuiz(quiz, mode = "create") {
    if (useApi()) {
      return mode === "update" ? await apiUpdateQuiz(quiz.id, quiz) : await apiCreateQuiz(quiz);
    }
    const quizzes = getQuizzesLocal();
    const idx = quizzes.findIndex(q => q.id === quiz.id);
    if (idx === -1) quizzes.push(quiz); else quizzes[idx] = quiz;
    saveQuizzesLocal(quizzes);
    return quiz;
  }

  async function deleteQuizById(id) {
    if (useApi()) return await apiDeleteQuiz(id);
    const quizzes = getQuizzesLocal().filter(q => q.id !== id);
    saveQuizzesLocal(quizzes);
  }

  async function generateQuizId() {
    const quizzes = await getQuizzes();
    let max = 0;
    quizzes.forEach(q => {
      const m = (q.id || "").match(/\d+/);
      if (m) max = Math.max(max, parseInt(m[0], 10));
    });
    return "QZ-" + (max + 1);
  }

  // ===== CLASSES UI =====
  async function populateClassesCheckboxes(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = "Loading...";

    const classes = await getClasses();
    container.innerHTML = "";

    if (!classes.length) {
      container.innerHTML = '<div style="color:#999; padding:10px;"><em>No classes available</em></div>';
      return;
    }

    classes.forEach(c => {
      const div = document.createElement("div");
      div.className = "class-checkbox-item";
      const cb = document.createElement("input");
      cb.type = "checkbox";
      cb.value = c.id;
      cb.dataset.className = c.name;
      const label = document.createElement("label");
      label.textContent = c.name;
      div.appendChild(cb);
      div.appendChild(label);
      container.appendChild(div);
    });
  }

  // ===== QUESTIONS LOGIC =====
  let currentQuizQuestions = [];

  function normalizeQuestions(questions) {
    if (!questions || !Array.isArray(questions)) return [];
    return questions.map(q => ({
      text: q.text || "",
      answers: (q.answers || []).map(a => {
        if (typeof a === "string") return { text: a, correct: false };
        return { text: a.text || "", correct: !!a.correct };
      })
    }));
  }

  async function openQuestionsModal(quizId) {
    const modal = document.getElementById("questionsModal");
    if (!modal) return;
    modal.dataset.quizId = quizId;

    const quizzes = await getQuizzes();
    const quiz = quizzes.find(q => q.id === quizId);
    currentQuizQuestions = quiz && quiz.questions ? JSON.parse(JSON.stringify(quiz.questions)) : [];

    renderQuestionsBuilder();
    modal.classList.add("open");
  }

  function closeQuestionsModal() {
    document.getElementById("questionsModal")?.classList.remove("open");
  }

  function renderQuestionsBuilder() {
    const container = document.getElementById("questions-container");
    if (!container) return;
    container.innerHTML = "";
    
    currentQuizQuestions = normalizeQuestions(currentQuizQuestions);

    currentQuizQuestions.forEach((question, qIndex) => {
      const block = document.createElement("div");
      block.className = "question-block";

      const header = document.createElement("div");
      header.className = "question-header";
      
      const num = document.createElement("span");
      num.className = "question-number";
      num.textContent = "Q" + (qIndex + 1);

      const qInput = document.createElement("input");
      qInput.type = "text";
      qInput.className = "question-input";
      qInput.value = question.text || "";
      qInput.placeholder = "Enter question text";
      qInput.addEventListener("input", e => { currentQuizQuestions[qIndex].text = e.target.value; });

      const removeQ = document.createElement("button");
      removeQ.type = "button";
      removeQ.className = "remove-question-btn";
      removeQ.textContent = "✕";
      removeQ.addEventListener("click", () => {
        currentQuizQuestions.splice(qIndex, 1);
        renderQuestionsBuilder();
      });

      header.appendChild(num); header.appendChild(qInput); header.appendChild(removeQ);
      block.appendChild(header);

      const answersSection = document.createElement("div");
      answersSection.className = "answers-section";
      
      const answersContainer = document.createElement("div");
      answersContainer.className = "answers-container";

      (question.answers || []).forEach((answer, aIndex) => {
        const row = document.createElement("div");
        row.className = "answer-item";
        
        const label = document.createElement("span");
        label.className = "answer-label";
        label.textContent = String.fromCharCode(65 + aIndex); 

        const correctCb = document.createElement("input");
        correctCb.type = "checkbox";
        correctCb.checked = !!answer.correct;
        correctCb.addEventListener("change", () => { currentQuizQuestions[qIndex].answers[aIndex].correct = correctCb.checked; });

        const aInput = document.createElement("input");
        aInput.type = "text";
        aInput.className = "answer-input";
        aInput.value = answer.text || "";
        aInput.placeholder = `Answer ${String.fromCharCode(65 + aIndex)}`;
        aInput.addEventListener("input", e => { currentQuizQuestions[qIndex].answers[aIndex].text = e.target.value; });

        const removeA = document.createElement("button");
        removeA.type = "button";
        removeA.className = "remove-answer-btn";
        removeA.textContent = "✕";
        removeA.addEventListener("click", () => {
          currentQuizQuestions[qIndex].answers.splice(aIndex, 1);
          renderQuestionsBuilder();
        });

        row.appendChild(label); row.appendChild(correctCb); row.appendChild(aInput); row.appendChild(removeA);
        answersContainer.appendChild(row);
      });

      const addA = document.createElement("button");
      addA.type = "button";
      addA.className = "add-answer-btn";
      addA.textContent = "+ Add Answer";
      addA.addEventListener("click", () => {
        if (!currentQuizQuestions[qIndex].answers) currentQuizQuestions[qIndex].answers = [];
        currentQuizQuestions[qIndex].answers.push({ text: "", correct: false });
        renderQuestionsBuilder();
      });

      answersSection.appendChild(answersContainer); answersSection.appendChild(addA);
      block.appendChild(answersSection);
      container.appendChild(block);
    });
  }

  function addQuestion() {
    currentQuizQuestions.push({ text: "", answers: [] });
    renderQuestionsBuilder();
  }

  async function saveQuestionsAndAnswers() {
    const modal = document.getElementById("questionsModal");
    const quizId = modal ? modal.dataset.quizId : null;
    if (!quizId) return;

    try {
        const quizzes = await getQuizzes();
        const quiz = quizzes.find(q => q.id === quizId);
        if (!quiz) throw new Error("Quiz not found.");

        const updatedQuiz = { ...quiz, questions: normalizeQuestions(currentQuizQuestions) };
        await saveQuiz(updatedQuiz, "update");
        
        alert("Questions saved!");
        closeQuestionsModal();
        
        if(document.getElementById("viewQuizModal")?.classList.contains("open")) {
             openViewQuizModal(updatedQuiz);
        }
        await loadExistingQuizzes();

    } catch (err) { alert(err.message); }
  }

  // ===== MAIN MODALS =====
  async function openModal() {
    const modal = document.getElementById("quizModal");
    if (!modal) return;
    modal.querySelector("form").reset();
    document.getElementById("quiz-date").value = new Date().toISOString().split("T")[0];
    await populateClassesCheckboxes("quiz-classes");
    modal.classList.add("open");
  }

  function closeModal() { document.getElementById("quizModal")?.classList.remove("open"); }

  async function handleCreate(e) {
    e.preventDefault();
    const name = document.getElementById("quiz-name").value.trim();
    const date = document.getElementById("quiz-date").value;
    const selected = document.querySelectorAll("#quiz-classes input:checked");
    const classIds = Array.from(selected).map(cb => cb.value);

    if (!name || !date) return alert("Please fill name and date.");

    try {
      const quizId = await generateQuizId();
      await saveQuiz({ id: quizId, name, date, classIds, questions: [] }, "create");
      closeModal();
      await loadExistingQuizzes();
    } catch (err) { alert(err.message); }
  }

  // ===== VIEW QUIZ LOGIC =====
  async function openViewQuizModal(quiz) {
    const modal = document.getElementById("viewQuizModal");
    if (!modal) return;
    modal.dataset.quizId = quiz.id;

    document.getElementById("view-quiz-title").textContent = quiz.name;
    document.getElementById("view-quiz-name").textContent = quiz.name;
    document.getElementById("view-quiz-date").textContent = quiz.date || "-";

    const classes = await getClasses();
    const classNames = (quiz.classIds || []).map(id => {
      const c = classes.find(x => x.id === id || x.pk === id); 
      return c ? c.name : id;
    });
    document.getElementById("view-quiz-classes").textContent = classNames.length ? classNames.join(", ") : "-";
    document.getElementById("view-quiz-questions").textContent = (quiz.questions?.length || 0) + " question(s)";

    displaySavedQuestions(quiz.questions);
    modal.classList.add("open");
  }

  function closeViewQuizModal() { document.getElementById("viewQuizModal")?.classList.remove("open"); }

  // ===== RESULTS LIST MODAL (FULL SCREEN) =====
  let currentResults = [];
  let allStudentsMap = {};

  async function openResultsListModal() {
      const modal = document.getElementById("resultsListModal");
      const viewModal = document.getElementById("viewQuizModal");
      const quizId = viewModal?.dataset.quizId;

      if (!modal || !quizId) return;

      modal.dataset.quizId = quizId;
      modal.classList.add("open");
      
      document.getElementById("results-table-body").innerHTML = '<tr><td colspan="5">Loading results...</td></tr>';

      try {
          // 1. Fetch Students
          const students = await apiGetStudents();
          allStudentsMap = {};
          students.forEach(s => {
              // --- CORECCȚIE CRITICĂ: MAPARE DUPĂ ID TEXT ("SG-1") ---
              allStudentsMap[s.id] = s; 
          });

          // 2. Fetch Results
          currentResults = await apiGetQuizResults(quizId);
          renderResultsTable();
      } catch (e) {
          console.error(e);
          document.getElementById("results-table-body").innerHTML = '<tr><td colspan="5" style="color:red">Error loading results. Check console.</td></tr>';
      }
  }

  function closeResultsListModal() {
      document.getElementById("resultsListModal")?.classList.remove("open");
  }

  function renderResultsTable() {
      const tbody = document.getElementById("results-table-body");
      const search = (document.getElementById("search-results-input").value || "").toLowerCase();
      const filterMode = document.querySelector("#resultsListModal .filter-buttons .filter-btn.active")?.id || "filter-res-name";

      tbody.innerHTML = "";

      if (!currentResults || currentResults.length === 0) {
          tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:15px; color:#999;">No papers scanned yet.</td></tr>';
          return;
      }

      currentResults.forEach(res => {
          // res.studentId is now "SG-1" (String)
          // allStudentsMap keys are "SG-1" (String)
          const student = allStudentsMap[res.studentId]; 
          
          const name = student ? `${student.firstName} ${student.lastName}` : "Unknown/Unlinked";
          const id = student ? student.id : "-";
          const extId = student ? (student.externalId || student.extId || "-") : "-";
          
          // Fix NaN Score
          let score = "-";
          if (res.score !== null && res.score !== undefined) {
              score = parseFloat(res.score).toFixed(2);
          }

          // Filtering
          let match = false;
          if (filterMode === "filter-res-name") match = name.toLowerCase().includes(search);
          else if (filterMode === "filter-res-id") match = String(id).includes(search);
          else if (filterMode === "filter-res-extid") match = String(extId).includes(search);

          if (match) {
              const tr = document.createElement("tr");
              tr.innerHTML = `
                  <td>${name}</td>
                  <td>${id}</td>
                  <td>${extId}</td>
                  <td style="font-weight:bold; color:${score >= 5 ? 'green' : 'red'};">${score}</td>
                  <td>
                      <button class="btn-edit" style="padding:5px 10px; margin-right:5px; cursor:pointer;">Edit</button>
                      <button class="btn-delete" style="padding:5px 10px; background:#dc3545; color:white; border:none; cursor:pointer;">Delete</button>
                  </td>
              `;
              
              tr.querySelector(".btn-edit").addEventListener("click", () => {
                  openReviewModal({ backendResponse: res });
              });

              tr.querySelector(".btn-delete").addEventListener("click", async () => {
                  if(confirm("Delete this result?")) {
                      try {
                          await apiDeleteResult(res.resultId);
                          tr.remove();
                          currentResults = currentResults.filter(r => r.resultId !== res.resultId);
                      } catch(err) {
                          alert("Delete failed: " + err.message);
                      }
                  }
              });

              tbody.appendChild(tr);
          }
      });
  }

  document.getElementById("quiz-review-papers")?.addEventListener("click", (e) => {
      e.preventDefault();
      openResultsListModal();
  });

  document.getElementById("results-list-close")?.addEventListener("click", (e) => {
      e.preventDefault();
      closeResultsListModal();
  });

  document.getElementById("search-results-input")?.addEventListener("input", renderResultsTable);

  document.querySelectorAll("#resultsListModal .filter-btn").forEach(btn => {
      btn.addEventListener("click", (e) => {
          e.preventDefault();
          document.querySelectorAll("#resultsListModal .filter-btn").forEach(b => b.classList.remove("active"));
          btn.classList.add("active");
          renderResultsTable();
      });
  });


  // ===== EDIT QUIZ LOGIC =====
  async function openEditQuizModal(quiz) {
    const modal = document.getElementById("editQuizModal");
    if (!modal) return;
    modal.dataset.quizId = quiz.id;
    document.getElementById("edit-quiz-name").value = quiz.name || "";
    document.getElementById("edit-quiz-date").value = quiz.date || "";
    await populateClassesCheckboxes("edit-quiz-classes");
    
    document.querySelectorAll("#edit-quiz-classes input").forEach(cb => {
      if ((quiz.classIds || []).includes(cb.value)) cb.checked = true;
    });
    modal.classList.add("open");
  }

  function closeEditQuizModal() { document.getElementById("editQuizModal")?.classList.remove("open"); }

  async function handleEditQuizSave(e) {
    e.preventDefault();
    const modal = document.getElementById("editQuizModal");
    const quizId = modal.dataset.quizId;
    const name = document.getElementById("edit-quiz-name").value.trim();
    const date = document.getElementById("edit-quiz-date").value;
    const selected = document.querySelectorAll("#edit-quiz-classes input:checked");
    const classIds = Array.from(selected).map(cb => cb.value);

    try {
        const quizzes = await getQuizzes();
        const quiz = quizzes.find(q => q.id === quizId);
        if(!quiz) throw new Error("Quiz missing");

        const updated = { ...quiz, name, date, classIds };
        await saveQuiz(updated, "update");
        
        closeEditQuizModal();
        openViewQuizModal(updated);
        await loadExistingQuizzes();
    } catch(err) { alert(err.message); }
  }

  async function handleDeleteQuiz() {
    if (!confirm("Are you sure?")) return;
    const quizId = document.getElementById("editQuizModal").dataset.quizId;
    try {
      await deleteQuizById(quizId);
      closeEditQuizModal();
      closeViewQuizModal();
      await loadExistingQuizzes();
    } catch(err) { alert(err.message); }
  }

  // ===== SINGLE RESULT REVIEW MODAL =====
  async function openReviewModal(scanResult) {
      const modal = document.getElementById("reviewModal");
      const img = document.getElementById("review-crop-img");
      const placeholder = document.getElementById("review-crop-placeholder");
      const scoreSpan = document.getElementById("review-score-display");
      const studentSelect = document.getElementById("review-student-select");
      
      if (!modal) return;

      const data = scanResult.backendResponse || scanResult; 
      const detectedScore = data.score || 0;
      const detectedStudentId = data.studentId || data.extId; 
      const cropUrl = data.nameCropUrl || data.nameCropPath || data.cropPath;

      modal.dataset.resultId = data.resultId; 

      scoreSpan.textContent = parseFloat(detectedScore).toFixed(2);

      if (cropUrl) {
          const fullUrl = cropUrl.startsWith("http") ? cropUrl : `http://localhost:8080${cropUrl}`;
          img.src = fullUrl;
          img.style.display = "block";
          placeholder.style.display = "none";
      } else {
          img.style.display = "none";
          placeholder.style.display = "block";
          placeholder.textContent = "No name crop available";
      }

      studentSelect.innerHTML = '<option value="">-- Select Student --</option>';
      try {
          const students = await apiGetStudents();
          students.forEach(s => {
              const opt = document.createElement("option");
              
              // --- FIX: Value este "SG-1" (String) ---
              opt.value = s.id;  
              
              const extId = s.externalId || s.extId || "-";
              opt.textContent = `${s.firstName} ${s.lastName} (ID: ${s.id} | ExtID: ${extId})`;
              
              if (String(s.pk) == detectedStudentId || 
                  String(s.id) == detectedStudentId || 
                  String(extId) == detectedStudentId) {
                  opt.selected = true;
              }
              studentSelect.appendChild(opt);
          });
      } catch (e) {
          console.error("Could not load students for dropdown", e);
          studentSelect.innerHTML = '<option>Error loading students</option>';
      }

      modal.classList.add("open");
  }

  function closeReviewModal() {
      document.getElementById("reviewModal")?.classList.remove("open");
  }

  // --- Listeners for Single Review Modal ---
  document.getElementById("review-cancel")?.addEventListener("click", (e) => {
      e.preventDefault();
      closeReviewModal();
  });
  
  document.getElementById("review-confirm")?.addEventListener("click", async e => {
      e.preventDefault();
      const confirmBtn = document.getElementById("review-confirm");
      
      const modal = document.getElementById("reviewModal");
      const resultId = modal.dataset.resultId; 
      
      const studentSelect = document.getElementById("review-student-select");
      const studentId = studentSelect.value; 
      const studentName = studentSelect.options[studentSelect.selectedIndex]?.text;
      const scoreText = document.getElementById("review-score-display").textContent;

      if (!studentId || !resultId) {
           alert("⚠️ Please select a student first.");
           return;
      }

      try {
          confirmBtn.textContent = "Se salvează...";
          confirmBtn.disabled = true;

          // studentId is "SG-1" (string)
          await apiUpdateResultStudent(resultId, studentId);
          
          closeReviewModal();
          
          if(document.getElementById("resultsListModal")?.classList.contains("open")) {
              const quizId = document.getElementById("resultsListModal").dataset.quizId;
              if(quizId) {
                  currentResults = await apiGetQuizResults(quizId);
                  renderResultsTable();
              }
          }

      } catch (err) {
          console.error(err);
          alert("❌ Eroare la salvare: " + err.message);
      } finally {
          confirmBtn.textContent = "Confirm";
          confirmBtn.disabled = false;
      }
  });


  // ===== RENDER & OPTIMIZATION =====
  async function loadExistingQuizzes() {
    const grid = document.querySelector(".grid");
    if(grid) grid.innerHTML = ""; 

    try {
        const [quizzes, classes] = await Promise.all([
            getQuizzes(),
            getClasses()
        ]);

        for (const q of quizzes) {
            renderQuizCard(q, classes); 
        }

        attachSearchListener();
        attachFilterToggles();

    } catch (err) { console.error(err); }
  }

  function renderQuizCard(quiz, allClasses) {
    const grid = document.querySelector(".grid");
    if (!grid) return;
    grid.style.display = "";
    document.querySelector(".empty-placeholder")?.remove();

    const card = document.createElement("div");
    card.className = "card";
    
    const h3 = document.createElement("h3"); h3.textContent = quiz.name;
    const p1 = document.createElement("p"); p1.textContent = "ID: " + (quiz.id || "-");
    const p2 = document.createElement("p"); p2.textContent = "Questions: " + (quiz.questions?.length || 0);
    const p3 = document.createElement("p"); p3.textContent = "Date: " + (quiz.date || "-");
    
    const p4 = document.createElement("p");
    const classNames = (quiz.classIds || []).map(id => {
        const c = allClasses.find(x => x.id === id || x.pk === id);
        return c ? c.name : id;
    });
    p4.textContent = "Classes: " + (classNames.length ? classNames.join(", ") : "-");

    const btn = document.createElement("button");
    btn.className = "btn-primary";
    btn.textContent = "View Quiz";

    card.append(h3, p1, p2, p3, p4, btn);
    card.addEventListener("click", () => openViewQuizModal(quiz));
    grid.appendChild(card);
  }

  function displaySavedQuestions(questions) {
    const list = document.getElementById("saved-questions-list");
    const section = document.getElementById("saved-questions-section");
    if (!list || !section) return;

    let qArr = [];
    try { 
        qArr = typeof questions === "string" ? JSON.parse(questions) : (questions || []); 
    } catch { qArr = []; }
    qArr = normalizeQuestions(qArr);

    if (!qArr.length) { section.style.display = "none"; return; }
    
    section.style.display = "block";
    list.innerHTML = "";
    
    qArr.forEach((q, i) => {
        const div = document.createElement("div");
        div.className = "saved-question-item";
        div.innerHTML = `<div class="saved-question-text"><span class="saved-question-number">Q${i+1}</span> ${q.text}</div>`;
        
        if(q.answers?.length) {
            const ansDiv = document.createElement("div");
            ansDiv.className = "saved-answers";
            q.answers.forEach((a, ai) => {
                const row = document.createElement("div");
                row.className = "saved-answer-item";
                row.innerHTML = `<span class="saved-answer-label">${String.fromCharCode(97+ai)}.</span> ${a.text} ${a.correct ? '<strong>✔</strong>' : ''}`;
                ansDiv.appendChild(row);
            });
            div.appendChild(ansDiv);
        }
        list.appendChild(div);
    });
  }

  // ===== SEARCH & FILTER =====
  function filterQuizzes() {
    const search = (document.getElementById("search-quizzes").value || "").toLowerCase();
    const filter = document.querySelector(".filter-btn.active")?.getAttribute("data-filter") || "name";

    document.querySelectorAll(".card").forEach(card => {
        const name = card.querySelector("h3")?.textContent.toLowerCase() || "";
        const text = card.textContent.toLowerCase();
        
        let match = false;
        if(filter === "name") match = name.includes(search);
        else if(filter === "id" && text.includes("id: " + search)) match = true;
        else if(filter === "date" && text.includes("date: " + search)) match = true;
        
        card.style.display = match ? "" : "none";
    });
  }

  function attachSearchListener() {
    document.getElementById("search-quizzes")?.addEventListener("input", filterQuizzes);
    document.getElementById("refresh-quizzes-btn")?.addEventListener("click", (e) => {
        e.preventDefault();
        document.getElementById("search-quizzes").value = "";
        filterQuizzes();
    });
  }

  function attachFilterToggles() {
    document.querySelectorAll(".filter-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            filterQuizzes();
        });
    });
  }

  // ===== INIT & EVENT LISTENERS =====
  window.openNewQuizModal = openModal;

  document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("quizModal");
    if(modal) {
        document.getElementById("quiz-cancel")?.addEventListener("click", e => { e.preventDefault(); closeModal(); });
        document.getElementById("quiz-form")?.addEventListener("submit", handleCreate);
        document.getElementById("quiz-create")?.addEventListener("click", e => {
            e.preventDefault();
            document.getElementById("quiz-form").dispatchEvent(new Event("submit"));
        });
    }

    document.getElementById("view-quiz-back")?.addEventListener("click", e => { e.preventDefault(); closeViewQuizModal(); });
    document.getElementById("view-quiz-edit")?.addEventListener("click", async e => {
        e.preventDefault();
        const vid = document.getElementById("viewQuizModal")?.dataset.quizId;
        if(!vid) return;
        const quizzes = await getQuizzes();
        const quiz = quizzes.find(q => q.id === vid);
        if(quiz) { closeViewQuizModal(); openEditQuizModal(quiz); }
    });

    document.getElementById("quiz-questions-keys")?.addEventListener("click", async e => {
        e.preventDefault();
        const vid = document.getElementById("viewQuizModal")?.dataset.quizId;
        if(vid) openQuestionsModal(vid);
    });
    
    const generateBtn = document.getElementById("quiz-generate-paper");
    if (generateBtn) {
        generateBtn.addEventListener("click", async (e) => {
            e.preventDefault(); 
            e.stopPropagation();
            console.log("Generate button clicked!"); // Debug
            await handleGeneratePaper();
        });
    } else {
        console.error("Butonul quiz-generate-paper nu a fost gasit in HTML!");
    }

    // 6. SCAN LOGIC
    const scanBtn = document.getElementById("quiz-scan-papers");
    const scanInput = document.getElementById("hidden-scan-input");

    if (scanBtn && scanInput) {
        scanBtn.addEventListener("click", (e) => {
            e.preventDefault(); 
            e.stopPropagation();
            scanInput.click();
        });

        scanInput.addEventListener("change", async () => {
            const file = scanInput.files[0];
            if (!file) return;

            const originalText = scanBtn.innerHTML;
            scanBtn.innerHTML = "⏳ Scanning...";
            scanBtn.disabled = true;

            const formData = new FormData();
            formData.append("file", file);
            const token = localStorage.getItem("token");

            try {
                const response = await fetch("http://127.0.0.1:8001/scan", {
                    method: "POST",
                    headers: { "Authorization": "Bearer " + token },
                    body: formData
                });
                const result = await response.json();

                if (response.ok) {
                    console.log("Scan Result:", result);
                    await openReviewModal(result);
                } else {
                    alert("❌ Eroare: " + (result.detail || "Error processing image"));
                }
            } catch (err) {
                console.error(err);
                alert("❌ Nu pot conecta la scanner (verifică portul 8001).");
            } finally {
                scanBtn.innerHTML = originalText;
                scanBtn.disabled = false;
                scanInput.value = "";
            }
        });
    }

    loadExistingQuizzes();
  });

})();