
(function() {
    const SETTINGS_KEY = 'app-settings';

    const defaultSettings = {
        theme: 'light',
        fontSize: 'medium',
        colorScheme: 'blue',
        notificationNew: true,
        notificationEdit: true,
        notificationDelete: false,
        autosave: true,
        autosaveInterval: 30,
        analytics: false,
        cookies: true
    };

    function getSettings() {
        try {
            return JSON.parse(localStorage.getItem(SETTINGS_KEY) || JSON.stringify(defaultSettings));
        } catch (e) {
            return defaultSettings;
        }
    }

    function saveSettings() {
        const settings = {
            theme: document.getElementById('theme-toggle').checked ? 'dark' : 'light',
            fontSize: document.getElementById('font-size').value,
            colorScheme: document.getElementById('color-scheme').value,
            notificationNew: document.getElementById('notification-new').checked,
            notificationEdit: document.getElementById('notification-edit').checked,
            notificationDelete: document.getElementById('notification-delete').checked,
            autosave: document.getElementById('autosave').checked,
            autosaveInterval: parseInt(document.getElementById('autosave-interval').value),
            analytics: document.getElementById('analytics').checked,
            cookies: document.getElementById('cookies').checked
        };

        try {
            localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
            showMessage('Settings saved successfully!', 'success');
            applySettings(settings);
        } catch (e) {
            showMessage('Error saving settings.', 'error');
        }
    }

    function applySettings(settings) {
        if (settings.theme === 'dark') {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }

        const fontSizeMap = {
            'small': '12px',
            'medium': '14px',
            'large': '16px',
            'extra-large': '18px'
        };
        document.documentElement.style.fontSize = fontSizeMap[settings.fontSize] || fontSizeMap['medium'];

        applyColorScheme(settings.colorScheme);
    }

    function applyColorScheme(scheme) {
        const colorMap = {
            'blue': '#667eea',
            'green': '#4caf50',
            'purple': '#9c27b0',
            'orange': '#ff9800'
        };
        const color = colorMap[scheme] || colorMap['blue'];
        document.documentElement.style.setProperty('--primary-color', color);
    }

    function loadSettings() {
        const settings = getSettings();

        document.getElementById('theme-toggle').checked = settings.theme === 'dark';
        document.getElementById('font-size').value = settings.fontSize;
        document.getElementById('color-scheme').value = settings.colorScheme;
        document.getElementById('notification-new').checked = settings.notificationNew;
        document.getElementById('notification-edit').checked = settings.notificationEdit;
        document.getElementById('notification-delete').checked = settings.notificationDelete;
        document.getElementById('autosave').checked = settings.autosave;
        document.getElementById('autosave-interval').value = settings.autosaveInterval;
        document.getElementById('analytics').checked = settings.analytics;
        document.getElementById('cookies').checked = settings.cookies;

        applySettings(settings);
    }

    function exportData() {
        try {
            const data = {
                students: JSON.parse(localStorage.getItem('students') || '[]'),
                classes: JSON.parse(localStorage.getItem('classes') || '[]'),
                quizzes: JSON.parse(localStorage.getItem('quizzes') || '[]'),
                user: JSON.parse(localStorage.getItem('user') || 'null'),
                settings: getSettings(),
                exportDate: new Date().toISOString()
            };

            const dataStr = JSON.stringify(data, null, 2);
            const dataBlob = new Blob([dataStr], { type: 'application/json' });
            const url = URL.createObjectURL(dataBlob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `education-app-backup-${Date.now()}.json`;
            link.click();
            URL.revokeObjectURL(url);

            showMessage('Data exported successfully!', 'success');
        } catch (e) {
            showMessage('Error exporting data.', 'error');
        }
    }

    function confirmClearData() {
        if (confirm('⚠️ Are you sure you want to delete ALL data? This action cannot be undone!')) {
            clearData();
        }
    }

    function clearData() {
        try {
            localStorage.removeItem('students');
            localStorage.removeItem('classes');
            localStorage.removeItem('quizzes');
            localStorage.removeItem('user');
            showMessage('All data has been cleared!', 'success');
            setTimeout(() => {
                location.reload();
            }, 1500);
        } catch (e) {
            showMessage('Error clearing data.', 'error');
        }
    }

    function changePassword() {
        const newPassword = prompt('Enter new password:');
        if (!newPassword) return;

        if (newPassword.length < 8) {
            showMessage('Password must be at least 8 characters.', 'error');
            return;
        }

        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            if (!user || !user.email) {
                showMessage('You must be logged in to change password.', 'error');
                return;
            }

            user.password = newPassword;
            localStorage.setItem('user', JSON.stringify(user));
            showMessage('Password changed successfully!', 'success');
        } catch (e) {
            showMessage('Error changing password.', 'error');
        }
    }

    function showMessage(message, type) {
        const settingsContainer = document.querySelector('.settings-container');
        const messageDiv = document.createElement('div');
        messageDiv.className = `settings-message ${type}`;
        messageDiv.textContent = message;
        settingsContainer.insertBefore(messageDiv, settingsContainer.firstChild);

        setTimeout(() => {
            messageDiv.remove();
        }, 4000);
    }

    document.addEventListener('DOMContentLoaded', function() {
        loadSettings();

        const themeToggle = document.getElementById('theme-toggle');
        const fontSizeSelect = document.getElementById('font-size');
        const colorSchemeSelect = document.getElementById('color-scheme');

        if (themeToggle) {
            themeToggle.addEventListener('change', () => {
                const settings = getSettings();
                applySettings({...settings, theme: themeToggle.checked ? 'dark' : 'light'});
            });
        }

        if (fontSizeSelect) {
            fontSizeSelect.addEventListener('change', () => {
                const settings = getSettings();
                applySettings({...settings, fontSize: fontSizeSelect.value});
            });
        }

        if (colorSchemeSelect) {
            colorSchemeSelect.addEventListener('change', () => {
                const settings = getSettings();
                applySettings({...settings, colorScheme: colorSchemeSelect.value});
            });
        }
    });

    window.saveSettings = saveSettings;
    window.exportData = exportData;
    window.confirmClearData = confirmClearData;
    window.changePassword = changePassword;

})();
