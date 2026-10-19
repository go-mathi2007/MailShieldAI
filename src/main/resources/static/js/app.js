/**
 * MailShield AI — Frontend Application Logic
 * Pure JavaScript, Zero Bloat, Enterprise Security & UX
 */

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initScannerTabs();
    initSampleButtons();
    initDropzone();
    initFormSubmissions();
    initClearHistoryModal();
    initDetailsToggle();

    // Load initial model info & dashboard statistics
    loadModelInfo();
    refreshDashboard();
});

/* ==========================================================================
   State & Constants
   ========================================================================== */
const SAMPLES = {
    spam: {
        sender: "account-verify@security-paypal-update.com",
        subject: "URGENT: Your Account Has Been Suspended!",
        body: "Dear Customer,\n\nWe detected an unauthorized login attempt from IP 192.168.1.1. Your account access has been suspended to prevent fraudulent wire transfers.\n\nPlease click http://secure-login-update.com/verify to verify your password, credit card number, and identity immediately.\n\nFailure to verify within 24 hours will result in permanent closure and forfeiture of your remaining $1,450.00 cash balance.\n\nThank you,\nAccount Security Department"
    },
    ham: {
        sender: "alex.kumar@company.com",
        subject: "Sprint Retrospective & Quarterly Planning Agenda",
        body: "Hi team,\n\nPlease find the meeting agenda for tomorrow's sprint retrospective attached. We will review the release notes for version 2.4.0 and discuss upcoming product architecture deliverables.\n\nLet me know if anyone has additional discussion topics to include before 4 PM.\n\nAlso, are we still meeting for lunch at the cafeteria around 12:30?\n\nBest regards,\nAlex"
    }
};

/* ==========================================================================
   Navigation
   ========================================================================== */
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');

    function navigate(viewId) {
        document.querySelectorAll('.section').forEach(sec => sec.classList.remove('active'));
        navItems.forEach(item => item.classList.remove('active'));

        const targetSection = document.getElementById(viewId) || document.getElementById('home');
        targetSection.classList.add('active');

        const activeNavItem = document.querySelector(`.nav-item[data-view="${viewId}"]`);
        if (activeNavItem) activeNavItem.classList.add('active');

        // Refresh views upon navigation
        if (viewId === 'dashboard') {
            refreshDashboard();
        } else if (viewId === 'evaluation') {
            loadModelInfo();
        }

        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const viewId = item.getAttribute('data-view');
            window.location.hash = viewId;
            navigate(viewId);
        });
    });

    document.querySelectorAll('[data-route]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const viewId = btn.getAttribute('data-route');
            window.location.hash = viewId;
            navigate(viewId);
        });
    });

    // Handle hash on initial load
    const currentHash = window.location.hash.replace('#', '') || 'home';
    navigate(currentHash);

    window.addEventListener('hashchange', () => {
        const hash = window.location.hash.replace('#', '') || 'home';
        navigate(hash);
    });
}

/* ==========================================================================
   Scanner Tabs (Mode A vs Mode B)
   ========================================================================== */
function initScannerTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const modePaste = document.getElementById('mode-paste-container');
    const modeUpload = document.getElementById('mode-upload-container');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            tabButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const mode = btn.getAttribute('data-tab');
            if (mode === 'paste') {
                modePaste.style.display = 'block';
                modeUpload.style.display = 'none';
            } else {
                modePaste.style.display = 'none';
                modeUpload.style.display = 'block';
            }
        });
    });
}

/* ==========================================================================
   Sample Buttons
   ========================================================================== */
function initSampleButtons() {
    const senderInput = document.getElementById('paste-sender');
    const subjectInput = document.getElementById('paste-subject');
    const bodyInput = document.getElementById('paste-body');

    document.getElementById('btn-sample-spam').addEventListener('click', () => {
        senderInput.value = SAMPLES.spam.sender;
        subjectInput.value = SAMPLES.spam.subject;
        bodyInput.value = SAMPLES.spam.body;
        bodyInput.focus();
    });

    document.getElementById('btn-sample-ham').addEventListener('click', () => {
        senderInput.value = SAMPLES.ham.sender;
        subjectInput.value = SAMPLES.ham.subject;
        bodyInput.value = SAMPLES.ham.body;
        bodyInput.focus();
    });

    document.getElementById('btn-clear-paste').addEventListener('click', () => {
        senderInput.value = '';
        subjectInput.value = '';
        bodyInput.value = '';
        resetResultPanel();
    });
}

/* ==========================================================================
   File Dropzone (Mode B: .eml Upload)
   ========================================================================== */
let selectedFile = null;

function initDropzone() {
    const dropzone = document.getElementById('eml-dropzone');
    const fileInput = document.getElementById('eml-file-input');
    const preview = document.getElementById('file-preview-card');
    const fileNameSpan = document.getElementById('preview-file-name');
    const fileSizeSpan = document.getElementById('preview-file-size');
    const removeBtn = document.getElementById('btn-remove-file');

    dropzone.addEventListener('click', () => fileInput.click());

    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.remove('dragover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        const files = e.dataTransfer.files;
        if (files && files.length > 0) {
            handleSelectedFile(files[0]);
        }
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files && fileInput.files.length > 0) {
            handleSelectedFile(fileInput.files[0]);
        }
    });

    removeBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        selectedFile = null;
        fileInput.value = '';
        preview.style.display = 'none';
        dropzone.style.display = 'block';
    });

    function handleSelectedFile(file) {
        if (!file.name.toLowerCase().endsWith('.eml')) {
            showToast("Invalid file type. Please upload a standard RFC-822 (.eml) email file.", "error");
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            showToast("File exceeds the maximum allowed size limit of 5MB.", "error");
            return;
        }

        selectedFile = file;
        fileNameSpan.textContent = file.name;
        fileSizeSpan.textContent = formatBytes(file.size);
        dropzone.style.display = 'none';
        preview.style.display = 'flex';
    }
}

/* ==========================================================================
   Form Submissions (Analyze API)
   ========================================================================== */
function initFormSubmissions() {
    // Mode A Submit
    document.getElementById('form-paste-analysis').addEventListener('submit', async (e) => {
        e.preventDefault();
        const sender = document.getElementById('paste-sender').value.trim();
        const subject = document.getElementById('paste-subject').value.trim();
        const body = document.getElementById('paste-body').value.trim();

        if (!body) {
            showToast("Please enter or paste the email body content to analyze.", "error");
            return;
        }

        const submitBtn = document.getElementById('btn-analyze-paste');
        setButtonLoading(submitBtn, true, "Analyzing Email...");

        try {
            const response = await fetch('/api/analyze', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sender, subject, body })
            });

            if (!response.ok) {
                const errData = await response.json();
                throw new Error(errData.message || "Classification failed");
            }

            const result = await response.json();
            renderAnalysisResult(result);
            showToast("Classification completed successfully!", "success");
        } catch (err) {
            showToast(err.message || "An error occurred during email analysis.", "error");
        } finally {
            setButtonLoading(submitBtn, false, "Analyze Email");
        }
    });

    // Mode B Submit
    document.getElementById('form-upload-analysis').addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!selectedFile) {
            showToast("Please select or drag & drop a .eml file first.", "error");
            return;
        }

        const submitBtn = document.getElementById('btn-analyze-upload');
        setButtonLoading(submitBtn, true, "Parsing & Classifying...");

        const formData = new FormData();
        formData.append('file', selectedFile);

        try {
            const response = await fetch('/api/analyze/upload', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errData = await response.json();
                throw new Error(errData.message || "File analysis failed");
            }

            const result = await response.json();
            renderAnalysisResult(result);
            showToast("EML message analyzed successfully!", "success");
        } catch (err) {
            showToast(err.message || "Failed to parse and analyze EML file.", "error");
        } finally {
            setButtonLoading(submitBtn, false, "Analyze Uploaded .eml");
        }
    });
}

/* ==========================================================================
   Render Analysis Results
   ========================================================================== */
function renderAnalysisResult(res) {
    const resultsContainer = document.getElementById('results-container');
    const placeholder = document.getElementById('results-placeholder');
    const verdictBox = document.getElementById('result-verdict-box');
    const verdictTag = document.getElementById('verdict-tag');
    const verdictTitle = document.getElementById('verdict-title');
    const verdictConfidence = document.getElementById('verdict-confidence');

    const spamPercent = (res.spamProbability * 100).toFixed(1);
    const hamPercent = (res.hamProbability * 100).toFixed(1);

    // Confidence and Verdict styling
    const isSpam = res.predictedLabel === 'SPAM';
    verdictBox.className = `result-verdict-box ${isSpam ? 'spam' : 'ham'}`;

    if (isSpam) {
        verdictTag.textContent = "THREAT DETECTED";
        verdictTitle.textContent = "SPAM / PHISHING";
        verdictConfidence.textContent = `Model Confidence: ${spamPercent}%`;
    } else {
        verdictTag.textContent = "LEGITIMATE MESSAGE";
        verdictTitle.textContent = "HAM (SAFE EMAIL)";
        verdictConfidence.textContent = `Model Confidence: ${hamPercent}%`;
    }

    // Probability Bars
    document.getElementById('bar-spam-val').textContent = `${spamPercent}%`;
    document.getElementById('bar-ham-val').textContent = `${hamPercent}%`;
    document.getElementById('progress-bar-spam').style.width = `${spamPercent}%`;
    document.getElementById('progress-bar-ham').style.width = `${hamPercent}%`;

    // Tokens Cloud
    renderTokenChips('top-spam-indicators', res.topSpamIndicators, 'spam');
    renderTokenChips('top-ham-indicators', res.topHamIndicators, 'ham');

    // Natural Language Explanation
    document.getElementById('result-explanation-text').textContent = res.explanation;

    // Disclaimer
    document.getElementById('result-disclaimer-text').textContent = res.disclaimer;

    // Mathematical Details Accordion
    document.getElementById('stat-total-tokens').textContent = res.totalTokens;
    document.getElementById('stat-recognized-tokens').textContent = res.recognizedTokens;
    document.getElementById('stat-unknown-tokens').textContent = res.unknownTokens;
    document.getElementById('stat-log-prior-spam').textContent = res.logPriorSpam.toFixed(4);
    document.getElementById('stat-log-prior-ham').textContent = res.logPriorHam.toFixed(4);
    document.getElementById('stat-log-likelihood-spam').textContent = res.logLikelihoodSpam.toFixed(4);
    document.getElementById('stat-log-likelihood-ham').textContent = res.logLikelihoodHam.toFixed(4);
    document.getElementById('stat-log-score-spam').textContent = res.totalLogScoreSpam.toFixed(4);
    document.getElementById('stat-log-score-ham').textContent = res.totalLogScoreHam.toFixed(4);
    document.getElementById('stat-content-hash').textContent = res.contentHash || 'N/A';

    // Metadata details
    const metaSubject = res.subject ? escapeHtml(res.subject) : '<em style="color:var(--text-light)">No subject provided</em>';
    const metaSender = res.sender ? escapeHtml(res.sender) : '<em style="color:var(--text-light)">No sender provided</em>';
    document.getElementById('meta-display-subject').innerHTML = metaSubject;
    document.getElementById('meta-display-sender').innerHTML = metaSender;

    const attachmentBadge = document.getElementById('meta-display-attachments');
    if (res.attachmentCount > 0) {
        attachmentBadge.style.display = 'inline-block';
        attachmentBadge.textContent = `📎 ${res.attachmentCount} Attachment(s) detected (safely isolated)`;
    } else {
        attachmentBadge.style.display = 'none';
    }

    placeholder.style.display = 'none';
    resultsContainer.style.display = 'block';

    // Smooth scroll to results on small screens
    if (window.innerWidth < 960) {
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    }
}

function renderTokenChips(containerId, tokens, category) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';

    if (!tokens || tokens.length === 0) {
        container.innerHTML = `<span style="font-size:0.78rem; color:var(--text-light)">None detected</span>`;
        return;
    }

    tokens.forEach(item => {
        const chip = document.createElement('span');
        chip.className = `token-chip ${category}`;
        chip.innerHTML = `${escapeHtml(item.token)} <span class="token-count">×${item.countInDoc} (LLR: ${item.logLikelihoodRatio > 0 ? '+' : ''}${item.logLikelihoodRatio.toFixed(2)})</span>`;
        chip.title = `P(w|Spam)=${item.spamLikelihood}, P(w|Ham)=${item.hamLikelihood}`;
        container.appendChild(chip);
    });
}

function resetResultPanel() {
    document.getElementById('results-container').style.display = 'none';
    document.getElementById('results-placeholder').style.display = 'block';
}

function initDetailsToggle() {
    const toggleBtn = document.getElementById('btn-details-toggle');
    const content = document.getElementById('details-content');

    toggleBtn.addEventListener('click', () => {
        const isOpen = content.classList.contains('open');
        if (isOpen) {
            content.classList.remove('open');
            toggleBtn.innerHTML = `<span>View Mathematical Log-Likelihood Details</span> <span>▼</span>`;
        } else {
            content.classList.add('open');
            toggleBtn.innerHTML = `<span>Hide Mathematical Log-Likelihood Details</span> <span>▲</span>`;
        }
    });
}

/* ==========================================================================
   Dashboard & Scan History
   ========================================================================== */
async function refreshDashboard() {
    try {
        const [statsRes, historyRes] = await Promise.all([
            fetch('/api/statistics'),
            fetch('/api/history')
        ]);

        if (statsRes.ok) {
            const stats = await statsRes.json();
            renderDashboardStats(stats);
        }

        if (historyRes.ok) {
            const history = await historyRes.json();
            renderHistoryTable(history);
        }
    } catch (err) {
        console.error("Failed to load dashboard data:", err);
    }
}

function renderDashboardStats(stats) {
    document.getElementById('dash-total-scans').textContent = stats.totalScans;
    document.getElementById('dash-spam-count').textContent = stats.spamCount;
    document.getElementById('dash-ham-count').textContent = stats.hamCount;
    document.getElementById('dash-spam-ratio').textContent = `${stats.spamPercentage}%`;

    // Render Donut Chart
    const total = stats.totalScans;
    const spamPct = total === 0 ? 0 : (stats.spamCount / total) * 100;
    const hamPct = total === 0 ? 0 : (stats.hamCount / total) * 100;

    const circumference = 2 * Math.PI * 40; // r=40
    const spamDash = (circumference * spamPct) / 100;
    const hamDash = (circumference * hamPct) / 100;

    const circleSpam = document.getElementById('donut-circle-spam');
    const circleHam = document.getElementById('donut-circle-ham');

    if (total === 0) {
        circleSpam.setAttribute('stroke-dasharray', `0 ${circumference}`);
        circleHam.setAttribute('stroke-dasharray', `0 ${circumference}`);
        document.getElementById('donut-center-total').textContent = "0";
    } else {
        circleSpam.setAttribute('stroke-dasharray', `${spamDash} ${circumference}`);
        circleHam.setAttribute('stroke-dasharray', `${hamDash} ${circumference}`);
        circleHam.setAttribute('stroke-dashoffset', `${-spamDash}`);
        document.getElementById('donut-center-total').textContent = total;
    }
}

function renderHistoryTable(records) {
    const tbody = document.getElementById('history-tbody');
    const emptyState = document.getElementById('history-empty-state');
    const tableElement = document.getElementById('history-table');

    tbody.innerHTML = '';

    if (!records || records.length === 0) {
        tableElement.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    tableElement.style.display = 'table';
    emptyState.style.display = 'none';

    records.forEach(rec => {
        const tr = document.createElement('tr');
        const isSpam = rec.predictedLabel === 'SPAM';
        const formattedDate = formatTimestamp(rec.timestamp);

        tr.innerHTML = `
            <td><span style="font-size:0.8rem; color:var(--text-muted)">${formattedDate}</span></td>
            <td>
                <div style="font-weight:600; max-width:240px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
                    ${rec.subject ? escapeHtml(rec.subject) : '<em style="color:var(--text-light)">[No Subject]</em>'}
                </div>
                <div style="font-size:0.75rem; color:var(--text-muted); font-family:monospace">
                    SHA-256: ${escapeHtml(rec.contentHash ? rec.contentHash.substring(0, 16) + '...' : 'N/A')}
                </div>
            </td>
            <td>
                <span class="label-badge ${isSpam ? 'spam' : 'ham'}">
                    ${isSpam ? '🚨 SPAM' : '🛡️ HAM'}
                </span>
            </td>
            <td>
                <div style="font-weight:700; color:${isSpam ? 'var(--spam-solid)' : 'var(--safe-solid)'}">
                    ${isSpam ? (rec.spamProbability * 100).toFixed(1) + '%' : (rec.hamProbability * 100).toFixed(1) + '%'}
                </div>
            </td>
            <td>
                <span style="font-size:0.75rem; background:#F3F4F6; padding:2px 8px; border-radius:4px; font-weight:600">
                    ${escapeHtml(rec.inputSource)}
                </span>
            </td>
            <td style="max-width:200px; font-size:0.78rem; color:var(--text-muted); overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
                ${escapeHtml(rec.snippet || 'No preview available')}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function initClearHistoryModal() {
    const modal = document.getElementById('clear-history-modal');
    const openBtn = document.getElementById('btn-open-clear-modal');
    const cancelBtn = document.getElementById('btn-cancel-clear');
    const confirmBtn = document.getElementById('btn-confirm-clear');

    openBtn.addEventListener('click', () => modal.classList.add('active'));
    cancelBtn.addEventListener('click', () => modal.classList.remove('active'));

    confirmBtn.addEventListener('click', async () => {
        try {
            const res = await fetch('/api/history', { method: 'DELETE' });
            if (res.ok) {
                showToast("Classification history cleared successfully.", "success");
                modal.classList.remove('active');
                refreshDashboard();
            } else {
                throw new Error("Failed to clear history.");
            }
        } catch (err) {
            showToast(err.message || "Failed to clear history.", "error");
        }
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.classList.remove('active');
    });
}

/* ==========================================================================
   Model Evaluation & Info View
   ========================================================================== */
async function loadModelInfo() {
    try {
        const res = await fetch('/api/model/info');
        if (!res.ok) return;

        const info = await res.json();
        document.getElementById('info-status').textContent = info.isTrained ? "Trained & Active" : "Untrained";
        document.getElementById('info-algorithm').textContent = info.algorithm;
        document.getElementById('info-vocab-size').textContent = info.vocabularySize;
        document.getElementById('info-total-samples').textContent = info.totalTrainingDocuments;
        document.getElementById('info-spam-samples').textContent = info.spamTrainingDocuments;
        document.getElementById('info-ham-samples').textContent = info.hamTrainingDocuments;
        document.getElementById('info-prior-spam').textContent = (info.priorSpam * 100).toFixed(1) + '%';
        document.getElementById('info-prior-ham').textContent = (info.priorHam * 100).toFixed(1) + '%';

        if (info.evaluation) {
            const ev = info.evaluation;
            document.getElementById('eval-accuracy').textContent = (ev.accuracy * 100).toFixed(1) + '%';
            document.getElementById('eval-precision').textContent = (ev.precision * 100).toFixed(1) + '%';
            document.getElementById('eval-recall').textContent = (ev.recall * 100).toFixed(1) + '%';
            document.getElementById('eval-f1').textContent = (ev.f1Score * 100).toFixed(1) + '%';

            // Confusion Matrix
            document.getElementById('cm-tp').textContent = ev.confusionMatrix.truePositives;
            document.getElementById('cm-fp').textContent = ev.confusionMatrix.falsePositives;
            document.getElementById('cm-fn').textContent = ev.confusionMatrix.falseNegatives;
            document.getElementById('cm-tn').textContent = ev.confusionMatrix.trueNegatives;

            document.getElementById('eval-note-text').textContent = ev.evaluationNote;

            const warningBox = document.getElementById('eval-warning-box');
            if (ev.isSmallDataset) {
                warningBox.style.display = 'flex';
            } else {
                warningBox.style.display = 'none';
            }
        }
    } catch (err) {
        console.error("Failed to load model info:", err);
    }
}

/* ==========================================================================
   Utility Functions
   ========================================================================== */
function setButtonLoading(btn, isLoading, text) {
    if (isLoading) {
        btn.disabled = true;
        btn.innerHTML = `<span class="spinner"></span> <span>${text}</span>`;
    } else {
        btn.disabled = false;
        btn.innerHTML = text;
    }
}

function showToast(message, type = "info") {
    const existing = document.getElementById('app-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.id = 'app-toast';
    toast.style.cssText = `
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 300;
        padding: 14px 20px;
        background: ${type === 'error' ? 'var(--spam-solid)' : (type === 'success' ? 'var(--safe-solid)' : 'var(--text-main)')};
        color: #FFFFFF;
        border-radius: var(--radius-sm);
        box-shadow: var(--shadow-lg);
        font-size: 0.9rem;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 10px;
        animation: toastIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    `;

    toast.innerHTML = `<span>${type === 'error' ? '⚠️' : (type === 'success' ? '✅' : 'ℹ️')}</span> <span>${escapeHtml(message)}</span>`;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatTimestamp(isoString) {
    if (!isoString) return '';
    try {
        const d = new Date(isoString);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return isoString;
    }
}
