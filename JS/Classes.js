/* ========== CLASSES PAGE LOGIC ========== */
/* Manages class CRUD with modal, student selection, search, and filtering */
/* guest: localStorage | logged: backend API */

(function(){
    const STORAGE_KEY = 'classes';
    const STUDENTS_KEY = 'students';
    const QUIZZES_KEY = 'quizzes';

    // ========== AUTH / MODE ==========
    function isLoggedIn() {
        const keys = ['user', 'currentUser', 'smartgrade_user', 'smartgradeUser', 'authUser'];
        for (const k of keys) {
            const v = localStorage.getItem(k);
            if (!v) continue;
            try {
                const obj = JSON.parse(v);
                if (obj && (obj.email || obj.username || obj.loginTime || obj.registrationTime)) return true;
                return true;
            } catch(e) {
                return true;
            }
        }
        return false;
    }
    function useApi() { return isLoggedIn(); }

    // ========== API HELPERS ==========
    async function apiGetClasses() {
        return await apiRequest("/api/classes");
    }
    async function apiCreateClass(payload) {
        return await apiRequest("/api/classes", { method: "POST", body: payload });
    }
    async function apiUpdateClass(id, payload) {
        return await apiRequest(`/api/classes/${encodeURIComponent(id)}`, { method: "PUT", body: payload });
    }
    async function apiDeleteClass(id) {
        return await apiRequest(`/api/classes/${encodeURIComponent(id)}`, { method: "DELETE" });
    }

    async function apiGetStudents() {
        return await apiRequest("/api/students");
    }
    async function apiUpdateStudent(id, payload) {
        return await apiRequest(`/api/students/${encodeURIComponent(id)}`, { method: "PUT", body: payload });
    }

    async function apiGetQuizzes() {
        return await apiRequest("/api/quizzes");
    }

    // ========== LOCAL HELPERS ==========
    function getClassesLocal() {
        try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'); } catch(e) { return []; }
    }
    function saveClassesLocal(list) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    }
    function getStudentsLocal() {
        try { return JSON.parse(localStorage.getItem(STUDENTS_KEY) || '[]'); } catch(e) { return []; }
    }
    function saveStudentsLocal(list) {
        localStorage.setItem(STUDENTS_KEY, JSON.stringify(list));
    }
    function getQuizzesLocal() {
        try { return JSON.parse(localStorage.getItem(QUIZZES_KEY) || '[]'); } catch(e) { return []; }
    }

    // ========== UNIFIED ACCESS ==========
    async function getClasses() {
        if (useApi()) return await apiGetClasses();
        return getClassesLocal();
    }

    async function getStudents() {
        if (useApi()) return await apiGetStudents();
        return getStudentsLocal();
    }

    async function getQuizzes() {
        if (useApi()) return await apiGetQuizzes();
        return getQuizzesLocal();
    }

    // ========== UTILITY FUNCTIONS ==========
    async function syncClassStudentCounts() {
        // In guest: actualizam local storage classes.students + studentCount
        // In logged: nu salvam in backend, doar folosim counts la afisare
        if (useApi()) return;

        let classes = getClassesLocal();
        const students = getStudentsLocal();

        classes.forEach(cls => {
            const classStudents = students.filter(s => s.classIds && s.classIds.includes(cls.id));
            cls.students = classStudents.map(s => s.id);
            cls.studentCount = classStudents.length;
        });

        saveClassesLocal(classes);
    }

    async function generateClassId() {
        const classes = await getClasses();
        let max = 0;
        classes.forEach(c => {
            const match = (c.id || '').match(/\d+/);
            if (match) {
                const num = parseInt(match[0], 10);
                if (num > max) max = num;
            }
        });
        return 'CLS-' + (max + 1);
    }

    async function buildClassViewModelList() {
        // Returneaza clase cu studentCount calculat
        const classes = await getClasses();
        const students = await getStudents();

        return classes.map(cls => {
            const count = students.filter(s => s.classIds && s.classIds.includes(cls.id)).length;
            return {
                id: cls.id,
                name: cls.name,
                studentCount: count
            };
        });
    }

    async function buildClassDetailsById(classId) {
        // Pentru modalele view/edit: avem nevoie si de lista studentilor din clasa
        const classes = await getClasses();
        const students = await getStudents();
        const cls = classes.find(c => c.id === classId);
        if (!cls) return null;

        const studentIds = students
            .filter(s => s.classIds && s.classIds.includes(classId))
            .map(s => s.id);

        return {
            id: cls.id,
            name: cls.name,
            students: studentIds,
            studentCount: studentIds.length
        };
    }

    // ========== MODAL LIFECYCLE ==========
    async function openModal() {
        const modal = document.getElementById('classModal');
        if (!modal) return;
        const form = modal.querySelector('form');
        form.reset();

        document.getElementById('toggle-unassigned').checked = false;
        document.getElementById('search-class-students').value = '';

        document.querySelectorAll('[data-filter]').forEach(btn => btn.classList.remove('active'));
        document.querySelector('[data-filter="name"]').classList.add('active');

        await renderStudentList();
        modal.classList.add('open');
        setTimeout(() => document.getElementById('class-name').focus(), 100);
    }

    function closeModal() {
        const modal = document.getElementById('classModal');
        if (modal) modal.classList.remove('open');
    }

    // ========== STUDENT LIST RENDERING ==========
    async function renderStudentList() {
        const container = document.getElementById('student-items');
        if (!container) return;
        container.innerHTML = '';

        const students = await getStudents();
        const classes = await getClasses();
        const showUnassigned = document.getElementById('toggle-unassigned').checked;

        students.forEach(student => {
            const studentClasses = classes.filter(cls =>
                student.classIds && student.classIds.includes(cls.id)
            );

            if (showUnassigned && studentClasses.length > 0) return;

            const div = document.createElement('div');
            div.className = 'student-item';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'student-checkbox';
            checkbox.value = student.id;
            checkbox.dataset.studentName = (student.firstName + ' ' + student.lastName);
            checkbox.dataset.studentId = student.id;
            checkbox.dataset.studentExtId = student.externalId || '';

            const label = document.createElement('label');
            label.className = 'student-label';

            const mainContent = document.createElement('div');
            mainContent.className = 'student-main-content';

            const nameSpan = document.createElement('span');
            nameSpan.className = 'student-name';
            nameSpan.textContent = student.firstName + ' ' + student.lastName;

            const detailsSpan = document.createElement('span');
            detailsSpan.className = 'student-details';
            detailsSpan.textContent = '(' + student.id + ' • ' + (student.externalId || '-') + ')';

            mainContent.appendChild(nameSpan);
            mainContent.appendChild(detailsSpan);

            const classInfo = document.createElement('span');
            classInfo.className = 'student-class-info';
            if (studentClasses.length > 0) {
                classInfo.textContent = studentClasses.map(cls => cls.name).join(', ');
            } else {
                classInfo.textContent = 'Unassigned';
            }

            label.appendChild(checkbox);
            label.appendChild(mainContent);
            label.appendChild(classInfo);
            div.appendChild(label);
            container.appendChild(div);
        });

        attachStudentSearchListener();
        attachFilterToggles();
    }

    function attachStudentSearchListener() {
        const searchInput = document.getElementById('search-class-students');
        const toggleCheckbox = document.getElementById('toggle-unassigned');

        if (searchInput) searchInput.addEventListener('input', filterStudentList);
        if (toggleCheckbox) toggleCheckbox.addEventListener('change', () => renderStudentList());
    }

    function attachFilterToggles() {
        const toggles = document.querySelectorAll('[data-filter]');
        toggles.forEach(btn => {
            btn.addEventListener('click', function(e){
                e.preventDefault();
                document.querySelectorAll('[data-filter]').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                filterStudentList();
            });
        });
    }

    function filterStudentList() {
        const searchInput = (document.getElementById('search-class-students').value || '').toLowerCase();
        const activeFilter = document.querySelector('[data-filter].active');
        const filterType = activeFilter ? activeFilter.getAttribute('data-filter') : 'name';

        const items = document.querySelectorAll('.student-item');
        items.forEach(item => {
            const checkbox = item.querySelector('.student-checkbox');
            let match = true;

            if (searchInput) {
                if (filterType === 'name') {
                    match = (checkbox.dataset.studentName || '').toLowerCase().includes(searchInput);
                } else if (filterType === 'id') {
                    match = (checkbox.dataset.studentId || '').toLowerCase().includes(searchInput);
                } else if (filterType === 'extid') {
                    match = (checkbox.dataset.studentExtId || '').toLowerCase().includes(searchInput);
                }
            }

            item.style.display = match ? '' : 'none';
        });
    }

    // ========== CLASS CREATION & MANAGEMENT ==========
    async function handleCreate(e) {
        e.preventDefault();
        const name = document.getElementById('class-name').value.trim();

        if (!name) {
            alert('Please enter a class name.');
            return;
        }

        const selectedCheckboxes = document.querySelectorAll('.student-checkbox:checked');
        const studentIds = Array.from(selectedCheckboxes).map(cb => cb.value);

        const classId = await generateClassId();

        try {
            if (useApi()) {
                // 1) create class in backend (id + name)
                await apiCreateClass({ id: classId, name });

                // 2) update each selected student -> add classId to classIds
                const students = await apiGetStudents();
                const selected = new Set(studentIds);

                for (const s of students) {
                    const has = Array.isArray(s.classIds) && s.classIds.includes(classId);
                    const shouldHave = selected.has(s.id);

                    if (shouldHave && !has) {
                        const newIds = Array.isArray(s.classIds) ? [...s.classIds, classId] : [classId];
                        await apiUpdateStudent(s.id, { classIds: newIds });
                    }
                }

                closeModal();
                location.reload();
                return;
            }

            // guest localStorage
            const classes = getClassesLocal();
            const newClass = {
                id: classId,
                name,
                students: studentIds,
                studentCount: studentIds.length
            };

            classes.push(newClass);
            saveClassesLocal(classes);

            // update students local -> classIds
            let students = getStudentsLocal();
            students = students.map(s => {
                if (studentIds.includes(s.id)) {
                    const ids = Array.isArray(s.classIds) ? s.classIds : [];
                    if (!ids.includes(classId)) ids.push(classId);
                    return { ...s, classIds: ids };
                }
                return s;
            });
            saveStudentsLocal(students);

            renderClassCard(newClass);
            closeModal();
            await renderStudentList();
        } catch (err) {
            alert(err.message || String(err));
        }
    }

    // ========== VIEW CLASS MODAL ==========
    async function openViewClassModal(cls) {
        const modal = document.getElementById('viewClassModal');
        if (!modal) return;

        modal.dataset.classId = cls.id;
        document.getElementById('view-class-title').textContent = cls.name;

        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelector('[data-tab="students"]').classList.add('active');

        document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
        document.getElementById('students-tab').classList.add('active');

        document.getElementById('search-view-students').value = '';
        document.querySelectorAll('.view-filter').forEach(btn => btn.classList.remove('active'));
        document.querySelector('.view-filter[data-filter="name"]').classList.add('active');

        document.getElementById('search-view-quizzes').value = '';
        document.querySelectorAll('.view-quiz-filter').forEach(btn => btn.classList.remove('active'));
        document.querySelector('.view-quiz-filter[data-filter="name"]').classList.add('active');

        await renderViewStudentList(cls.id);
        await renderViewQuizList(cls.id);
        modal.classList.add('open');
    }

    function closeViewClassModal() {
        const modal = document.getElementById('viewClassModal');
        if (modal) modal.classList.remove('open');
    }

    async function renderViewStudentList(classId) {
        const container = document.getElementById('view-student-items');
        if (!container) return;
        container.innerHTML = '';

        const students = await getStudents();
        const classStudents = students.filter(s => s.classIds && s.classIds.includes(classId));

        if (classStudents.length === 0) {
            container.innerHTML = '<div style="color: #999; padding: 20px; text-align: center;"><em>No students in this class.</em></div>';
            return;
        }

        classStudents.forEach(student => {
            const div = document.createElement('div');
            div.className = 'student-item';

            const mainContent = document.createElement('div');
            mainContent.className = 'student-main-content';

            const nameSpan = document.createElement('span');
            nameSpan.className = 'student-name';
            nameSpan.textContent = student.firstName + ' ' + student.lastName;

            const detailsSpan = document.createElement('span');
            detailsSpan.className = 'student-details';
            detailsSpan.textContent = '(' + student.id + ' • ' + (student.externalId || '-') + ')';

            mainContent.appendChild(nameSpan);
            mainContent.appendChild(detailsSpan);
            div.appendChild(mainContent);
            container.appendChild(div);
        });

        attachViewStudentSearchListener();
    }

    function attachViewStudentSearchListener() {
        const searchInput = document.getElementById('search-view-students');
        if (searchInput) searchInput.addEventListener('input', filterViewStudentList);

        document.querySelectorAll('.view-filter').forEach(btn => {
            btn.addEventListener('click', function(e){
                e.preventDefault();
                document.querySelectorAll('.view-filter').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                filterViewStudentList();
            });
        });
    }

    function filterViewStudentList() {
        const searchInput = (document.getElementById('search-view-students').value || '').toLowerCase();
        const activeFilter = document.querySelector('.view-filter.active');
        const filterType = activeFilter ? activeFilter.getAttribute('data-filter') : 'name';

        const items = document.querySelectorAll('#view-student-items .student-item');
        items.forEach(item => {
            const nameText = (item.querySelector('.student-name').textContent || '').toLowerCase();
            const detailsText = (item.querySelector('.student-details').textContent || '').toLowerCase();

            let match = true;
            if (searchInput) {
                if (filterType === 'name') {
                    match = nameText.includes(searchInput);
                } else if (filterType === 'id') {
                    const idMatch = detailsText.match(/\((\w+-\d+)/);
                    match = idMatch ? idMatch[1].toLowerCase().includes(searchInput) : false;
                } else if (filterType === 'extid') {
                    const extIdMatch = detailsText.match(/• (.*)\)/);
                    match = extIdMatch ? extIdMatch[1].toLowerCase().includes(searchInput) : false;
                }
            }
            item.style.display = match ? '' : 'none';
        });
    }

    // ========== VIEW QUIZZES IN CLASS ==========
    async function renderViewQuizList(classId) {
        const container = document.getElementById('view-quiz-items');
        if (!container) return;
        container.innerHTML = '';

        let quizzes = [];
        try { quizzes = await getQuizzes(); } catch(e) { quizzes = []; }

        // suportam ambele structuri: q.classIds (local) sau q.classId (backend)
        const classQuizzes = quizzes.filter(q =>
            (Array.isArray(q.classIds) && q.classIds.includes(classId)) ||
            (q.classId && q.classId === classId)
        );

        if (classQuizzes.length === 0) {
            container.innerHTML = '<div style="color: #999; padding: 20px; text-align: center;"><em>No quizzes in this class.</em></div>';
            return;
        }

        classQuizzes.forEach(quiz => {
            const div = document.createElement('div');
            div.className = 'student-item';

            const mainContent = document.createElement('div');
            mainContent.className = 'student-main-content';

            const nameSpan = document.createElement('span');
            nameSpan.className = 'student-name';
            nameSpan.textContent = quiz.name || quiz.title || 'Quiz';

            const detailsSpan = document.createElement('span');
            detailsSpan.className = 'student-details';

            const qCount = Array.isArray(quiz.questions) ? quiz.questions.length : (quiz.questionsCount || quiz.questions || 0);
            const date = quiz.date || '-';
            detailsSpan.textContent = '(' + (quiz.id || '-') + ' • ' + qCount + ' Q • ' + date + ')';

            mainContent.appendChild(nameSpan);
            mainContent.appendChild(detailsSpan);
            div.appendChild(mainContent);
            container.appendChild(div);
        });

        attachViewQuizSearchListener();
    }

    function attachViewQuizSearchListener() {
        const searchInput = document.getElementById('search-view-quizzes');
        if (searchInput) searchInput.addEventListener('input', filterViewQuizList);

        document.querySelectorAll('.view-quiz-filter').forEach(btn => {
            btn.addEventListener('click', function(e){
                e.preventDefault();
                document.querySelectorAll('.view-quiz-filter').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                filterViewQuizList();
            });
        });
    }

    function filterViewQuizList() {
        const searchInput = (document.getElementById('search-view-quizzes').value || '').toLowerCase();
        const activeFilter = document.querySelector('.view-quiz-filter.active');
        const filterType = activeFilter ? activeFilter.getAttribute('data-filter') : 'name';

        const items = document.querySelectorAll('#view-quiz-items .student-item');
        items.forEach(item => {
            const nameText = (item.querySelector('.student-name').textContent || '').toLowerCase();
            const detailsText = (item.querySelector('.student-details').textContent || '').toLowerCase();

            let match = true;
            if (searchInput) {
                if (filterType === 'name') {
                    match = nameText.includes(searchInput);
                } else if (filterType === 'date') {
                    const dateMatch = detailsText.match(/• (\d{4}-\d{2}-\d{2})\)$/);
                    match = dateMatch ? dateMatch[1].includes(searchInput) : false;
                }
            }
            item.style.display = match ? '' : 'none';
        });
    }

    // ========== EDIT CLASS MODAL ==========
    async function openEditClassModal(cls) {
        const modal = document.getElementById('editClassModal');
        if (!modal) return;

        modal.dataset.classId = cls.id;
        document.getElementById('edit-class-name').value = cls.name || '';

        document.getElementById('toggle-edit-unassigned').checked = false;
        document.getElementById('search-edit-class-students').value = '';

        document.querySelectorAll('#toggle-edit-student-name, #toggle-edit-student-id, #toggle-edit-student-extid').forEach(btn => btn.classList.remove('active'));
        document.getElementById('toggle-edit-student-name').classList.add('active');

        await renderEditStudentList(cls.id);
        modal.classList.add('open');
        setTimeout(() => document.getElementById('edit-class-name').focus(), 100);
    }

    function closeEditClassModal() {
        const modal = document.getElementById('editClassModal');
        if (modal) modal.classList.remove('open');
    }

    async function renderEditStudentList(classId) {
        const container = document.getElementById('edit-student-items');
        if (!container) return;
        container.innerHTML = '';

        const students = await getStudents();
        const classes = await getClasses();
        const showUnassigned = document.getElementById('toggle-edit-unassigned').checked;

        const currentStudentIds = students
            .filter(s => s.classIds && s.classIds.includes(classId))
            .map(s => s.id);

        students.forEach(student => {
            const studentClasses = classes.filter(c =>
                c.id !== classId && student.classIds && student.classIds.includes(c.id)
            );

            if (showUnassigned && studentClasses.length > 0) return;

            const div = document.createElement('div');
            div.className = 'student-item';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'student-checkbox';
            checkbox.value = student.id;
            checkbox.checked = currentStudentIds.includes(student.id);
            checkbox.dataset.studentName = student.firstName + ' ' + student.lastName;
            checkbox.dataset.studentId = student.id;
            checkbox.dataset.studentExtId = student.externalId || '';

            const label = document.createElement('label');
            label.className = 'student-label';

            const mainContent = document.createElement('div');
            mainContent.className = 'student-main-content';

            const nameSpan = document.createElement('span');
            nameSpan.className = 'student-name';
            nameSpan.textContent = student.firstName + ' ' + student.lastName;

            const detailsSpan = document.createElement('span');
            detailsSpan.className = 'student-details';
            detailsSpan.textContent = '(' + student.id + ' • ' + (student.externalId || '-') + ')';

            mainContent.appendChild(nameSpan);
            mainContent.appendChild(detailsSpan);

            const classInfo = document.createElement('span');
            classInfo.className = 'student-class-info';
            if (studentClasses.length > 0) {
                classInfo.textContent = studentClasses.map(c => c.name).join(', ');
            } else {
                classInfo.textContent = 'Unassigned';
            }

            label.appendChild(checkbox);
            label.appendChild(mainContent);
            label.appendChild(classInfo);
            div.appendChild(label);
            container.appendChild(div);
        });

        attachEditStudentSearchListener();
    }

    function attachEditStudentSearchListener() {
        const searchInput = document.getElementById('search-edit-class-students');
        const toggleCheckbox = document.getElementById('toggle-edit-unassigned');

        if (searchInput) searchInput.addEventListener('input', filterEditStudentList);
        if (toggleCheckbox) toggleCheckbox.addEventListener('change', () => renderEditStudentList(document.getElementById('editClassModal').dataset.classId));

        document.querySelectorAll('#toggle-edit-student-name, #toggle-edit-student-id, #toggle-edit-student-extid').forEach(btn => {
            btn.addEventListener('click', function(e){
                e.preventDefault();
                document.querySelectorAll('#toggle-edit-student-name, #toggle-edit-student-id, #toggle-edit-student-extid').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                filterEditStudentList();
            });
        });
    }

    function filterEditStudentList() {
        const searchInput = (document.getElementById('search-edit-class-students').value || '').toLowerCase();
        const activeFilter = document.querySelector('#toggle-edit-student-name.active, #toggle-edit-student-id.active, #toggle-edit-student-extid.active');
        const filterType = activeFilter ? activeFilter.getAttribute('data-filter') : 'name';

        const items = document.querySelectorAll('#edit-student-items .student-item');
        items.forEach(item => {
            const checkbox = item.querySelector('.student-checkbox');
            let match = true;

            if (searchInput) {
                if (filterType === 'name') {
                    match = (checkbox.dataset.studentName || '').toLowerCase().includes(searchInput);
                } else if (filterType === 'id') {
                    match = (checkbox.dataset.studentId || '').toLowerCase().includes(searchInput);
                } else if (filterType === 'extid') {
                    match = (checkbox.dataset.studentExtId || '').toLowerCase().includes(searchInput);
                }
            }

            item.style.display = match ? '' : 'none';
        });
    }

    async function handleEditClassSave(e) {
        e.preventDefault();
        const modal = document.getElementById('editClassModal');
        const classId = modal.dataset.classId;
        const name = document.getElementById('edit-class-name').value.trim();

        if (!name) {
            alert('Please enter a class name.');
            return;
        }

        const selectedCheckboxes = document.querySelectorAll('#edit-student-items .student-checkbox:checked');
        const selectedStudentIds = Array.from(selectedCheckboxes).map(cb => cb.value);

        try {
            if (useApi()) {
                // update class name
                await apiUpdateClass(classId, { name });

                // we need to add/remove classId in students.classIds based on selection
                const students = await apiGetStudents();
                const selected = new Set(selectedStudentIds);

                for (const s of students) {
                    const ids = Array.isArray(s.classIds) ? [...s.classIds] : [];
                    const has = ids.includes(classId);
                    const shouldHave = selected.has(s.id);

                    if (shouldHave && !has) {
                        ids.push(classId);
                        await apiUpdateStudent(s.id, { classIds: ids });
                    } else if (!shouldHave && has) {
                        const newIds = ids.filter(x => x !== classId);
                        await apiUpdateStudent(s.id, { classIds: newIds });
                    }
                }

                closeEditClassModal();
                location.reload();
                return;
            }

            // guest
            let classes = getClassesLocal();
            const index = classes.findIndex(c => c.id === classId);
            if (index !== -1) {
                classes[index].name = name;
                classes[index].students = selectedStudentIds;
                classes[index].studentCount = selectedStudentIds.length;
                saveClassesLocal(classes);

                // update students local -> add/remove classId
                let students = getStudentsLocal();
                students = students.map(s => {
                    const ids = Array.isArray(s.classIds) ? [...s.classIds] : [];
                    const has = ids.includes(classId);
                    const shouldHave = selectedStudentIds.includes(s.id);

                    if (shouldHave && !has) ids.push(classId);
                    if (!shouldHave && has) return { ...s, classIds: ids.filter(x => x !== classId) };
                    return { ...s, classIds: ids };
                });
                saveStudentsLocal(students);

                closeEditClassModal();
                const updated = classes[index];
                openViewClassModal(updated);
            }
        } catch (err) {
            alert(err.message || String(err));
        }
    }

    async function handleDeleteClass() {
        const modal = document.getElementById('editClassModal');
        const classId = modal.dataset.classId;

        if (!confirm('Are you sure you want to delete this class?')) return;

        try {
            if (useApi()) {
                // remove classId from all students
                const students = await apiGetStudents();
                for (const s of students) {
                    const ids = Array.isArray(s.classIds) ? s.classIds : [];
                    if (ids.includes(classId)) {
                        await apiUpdateStudent(s.id, { classIds: ids.filter(x => x !== classId) });
                    }
                }
                await apiDeleteClass(classId);
                location.reload();
                return;
            }

            // guest
            let classes = getClassesLocal();
            classes = classes.filter(c => c.id !== classId);
            saveClassesLocal(classes);

            // remove classId from students local
            let students = getStudentsLocal();
            students = students.map(s => {
                const ids = Array.isArray(s.classIds) ? s.classIds : [];
                if (!ids.includes(classId)) return s;
                return { ...s, classIds: ids.filter(x => x !== classId) };
            });
            saveStudentsLocal(students);

            location.reload();
        } catch (err) {
            alert(err.message || String(err));
        }
    }

    // ========== CARD CLICK HANDLER ==========
    function renderClassCard(cls) {
        const grid = document.querySelector('.grid');
        if (!grid) return;
        grid.style.display = '';

        const placeholder = document.querySelector('.empty-placeholder');
        if (placeholder && placeholder.parentElement) placeholder.parentElement.removeChild(placeholder);

        const card = document.createElement('div');
        card.className = 'card';
        card.style.cursor = 'pointer';

        const h3 = document.createElement('h3');
        h3.textContent = cls.name;

        const p1 = document.createElement('p');
        p1.textContent = 'Class ID: ' + (cls.id || '-');

        const p2 = document.createElement('p');
        p2.textContent = 'Students: ' + (cls.studentCount || 0);

        const btn = document.createElement('button');
        btn.className = 'btn-primary';
        btn.textContent = 'View Class';

        card.appendChild(h3);
        card.appendChild(p1);
        card.appendChild(p2);
        card.appendChild(btn);
        card.addEventListener('click', () => openViewClassModal(cls));

        grid.appendChild(card);
    }

    async function loadExistingClasses() {
        await syncClassStudentCounts();
        const viewModels = await buildClassViewModelList();
        viewModels.forEach(cls => renderClassCard(cls));
        attachSearchListener();
        attachFilterToggles();
    }

    // ========== SEARCH & FILTERING ==========
    function filterClasses() {
        const searchInput = (document.getElementById('search-classes').value || '').toLowerCase();
        const activeFilter = document.querySelector('.filter-btn.active');
        const filterType = activeFilter ? activeFilter.getAttribute('data-filter') : 'name';

        document.querySelectorAll('.card').forEach(card => {
            const h3Text = (card.querySelector('h3').textContent || '').toLowerCase();
            const pTexts = Array.from(card.querySelectorAll('p')).map(p => p.textContent.toLowerCase());

            let match = true;
            if (searchInput) {
                if (filterType === 'name') {
                    match = h3Text.includes(searchInput);
                } else if (filterType === 'id') {
                    const idText = pTexts.find(t => t.includes('class id:'));
                    match = idText ? idText.split('class id:')[1].trim().includes(searchInput) : false;
                }
            }
            card.style.display = match ? '' : 'none';
        });
    }

    function attachSearchListener() {
        const searchElem = document.getElementById('search-classes');
        if (searchElem) searchElem.addEventListener('input', filterClasses);

        const refreshBtn = document.getElementById('refresh-classes-btn');
        if (refreshBtn) refreshBtn.addEventListener('click', function(e){
            e.preventDefault();
            document.getElementById('search-classes').value = '';
            filterClasses();
        });
    }

    function attachFilterToggles() {
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.addEventListener('click', function(e){
                e.preventDefault();
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                this.classList.add('active');
                filterClasses();
            });
        });
    }

    // ========== INITIALIZATION ==========
    window.openNewClassModal = () => { openModal(); };

    document.addEventListener('DOMContentLoaded', function(){
        const modal = document.getElementById('classModal');
        if (!modal) return;

        document.getElementById('class-cancel').addEventListener('click', (e) => { e.preventDefault(); closeModal(); });
        document.getElementById('class-form').addEventListener('submit', (e) => { handleCreate(e); });
        document.getElementById('class-create').addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('class-form').dispatchEvent(new Event('submit', {cancelable:true}));
        });

        // View Class Modal
        const viewModal = document.getElementById('viewClassModal');
        if (viewModal) {
            document.getElementById('view-back').addEventListener('click', (e) => { e.preventDefault(); closeViewClassModal(); });
            document.getElementById('view-edit').addEventListener('click', async (e) => {
                e.preventDefault();
                const classId = viewModal.dataset.classId;
                const cls = await buildClassDetailsById(classId);
                if (cls) {
                    closeViewClassModal();
                    openEditClassModal(cls);
                }
            });

            document.querySelectorAll('.tab-btn').forEach(btn => {
                btn.addEventListener('click', function(e) {
                    e.preventDefault();
                    const tabName = this.getAttribute('data-tab');
                    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
                    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
                    this.classList.add('active');
                    document.getElementById(tabName + '-tab').classList.add('active');
                });
            });
        }

        // Edit Class Modal
        const editModal = document.getElementById('editClassModal');
        if (editModal) {
            document.getElementById('edit-class-done').addEventListener('click', (e) => {
                e.preventDefault();
                document.getElementById('edit-class-form').dispatchEvent(new Event('submit', {cancelable:true}));
            });
            document.getElementById('edit-class-delete').addEventListener('click', (e) => { e.preventDefault(); handleDeleteClass(); });
            document.getElementById('edit-class-form').addEventListener('submit', (e) => { handleEditClassSave(e); });
        }

        (async () => {
            await loadExistingClasses();
        })();

        document.addEventListener('visibilitychange', function() {
            if (!document.hidden) {
                const grid = document.querySelector('.grid');
                const placeholder = document.querySelector('.empty-placeholder');
                if (grid) grid.innerHTML = '';
                if (placeholder && placeholder.parentElement) placeholder.parentElement.removeChild(placeholder);

                (async () => {
                    await loadExistingClasses();
                })();
            }
        });
    });

})();
