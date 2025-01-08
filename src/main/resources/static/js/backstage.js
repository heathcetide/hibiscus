// 添加在文件开头
// 请求拦截器
function setupAuthInterceptor() {
    const originalFetch = window.fetch;
    window.fetch = async function (url, options = {}) {
        // 获取 token
        const token = localStorage.getItem('token');
        if (token) {
            // 添加认证头
            options.headers = {
                ...options.headers,
                'Authorization': `Bearer ${token}`
            };
        }

        try {
            const response = await originalFetch(url, options);
            // 处理 401 未授权响应
            if (response.status === 401) {
                // 清除失效的 token
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                // 重定向到登录页面
                window.location.href = '/api/hibiscus/auth/login';
                return response;
            }
            return response;
        } catch (error) {
            console.error('请求失败:', error);
            throw error;
        }
    };
}

// 页面加载时设置拦截器
document.addEventListener('DOMContentLoaded', function () {
    setupAuthInterceptor();
    // 检查是否已登录
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/api/hibiscus/auth/login';
        return;
    }
});

let currentFile = '';
let hasUnsavedChanges = false;
let editor = null;

// 动态打开外部页面
function openExternalPage(path) {
    const baseURL = window.location.origin; // 动态获取当前的 IP 和端口
    // window.location.href = `${baseURL}${path}`;
    AuthUtil.redirectTo(`${baseURL}${path}`)
}

// 初始化Monaco Editor
require.config({paths: {'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs'}});
require(['vs/editor/editor.main'], function () {
    // 编辑器将在showGeneratedCode函数中初始化
});
checkInitialDatabaseConnection();
// 页面加载完成后检查数据库连接
document.addEventListener('DOMContentLoaded', function () {

    // 检查当前页面
    const currentPage = document.querySelector('.page.active');
    if (currentPage) {
        const pageId = currentPage.id;
        if (pageId === 'code-generate') {
            // 如果当前是代码生成页面，检查数据库连接
            checkInitialDatabaseConnection();
        }
    }
});

// 初始数据库连接检查
function checkInitialDatabaseConnection() {
    const token = AuthUtil.getToken();
    // 显示加载状态
    showLoading('databaseLoading');
    fetch('/api/hibiscus/database/check-connection', {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.connected) {
                // 如果已连接，自动填充表单并显示生成配置
                const form = document.getElementById('dbConfigForm');
                const url = data.url || '';

                // 解析URL中的信息
                let dbType = 'mysql';
                let host = 'localhost';
                let port = '3306';
                let database = '';

                if (url.includes('mysql')) {
                    dbType = 'mysql';
                } else if (url.includes('postgresql')) {
                    dbType = 'postgresql';
                    port = '5432';
                } else if (url.includes('oracle')) {
                    dbType = 'oracle';
                    port = '1521';
                } else if (url.includes('sqlserver')) {
                    dbType = 'sqlserver';
                    port = '1433';
                }

                try {
                    const urlParts = url.split('://')[1].split('/');
                    const hostPort = urlParts[0].split(':');
                    host = hostPort[0];
                    port = hostPort[1] || port;
                    database = urlParts[1].split('?')[0];
                } catch (e) {
                    console.error('Error parsing database URL:', e);
                }

                // 填充表单
                form.querySelector('[name="dbType"]').value = dbType;
                form.querySelector('[name="host"]').value = host;
                form.querySelector('[name="port"]').value = port;
                form.querySelector('[name="database"]').value = database;
                form.querySelector('[name="username"]').value = data.username || '';
                form.querySelector('[name="password"]').value = data.password || '';

                // 显示已连接状态
                showConnectionStatus(true);

                // 显示生成配置区域
                const generateConfig = document.getElementById('generateConfig');
                if (generateConfig) {
                    generateConfig.style.display = 'block';
                }

                // 显示表列表
                if (data.tables) {
                    showTables(data.tables);
                }

                // 预填充包名（如果有）
                const packageNameInput = document.querySelector('[name="packageName"]');
                if (packageNameInput && !packageNameInput.value) {
                    packageNameInput.value = `com.${database.toLowerCase()}`; // 默认包名
                }
            } else {
                showConnectionStatus(false);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showConnectionStatus(false);
        })
        .finally(() => {
            hideLoading('databaseLoading');
        });
}

// 显示表列表
function showTables(tables) {
    // 更新数据库配置表单下方的表列表
    const dbTableList = document.getElementById('dbTableList');
    if (dbTableList) {
        dbTableList.style.display = 'block';
        dbTableList.innerHTML = tables.map(table => `
            <div class="table-item">
                <label>
                    <input type="checkbox" name="selectedTables" value="${table.name}">
                    ${table.name}
                </label>
                <span class="table-comment">${table.comment || ''}</span>
            </div>
        `).join('');
    }

    // 更新生成配置区域中的表列表
    const generateTableList = document.getElementById('generateTableList');
    if (generateTableList) {
        generateTableList.innerHTML = tables.map(table => `
            <div class="table-item">
                <label>
                    <input type="checkbox" name="selectedTables" value="${table.name}">
                    ${table.name}
                </label>
                <span class="table-comment">${table.comment || ''}</span>
            </div>
        `).join('');
    }
}

// 生成代码
function generateCode() {
    const form = document.getElementById('generateForm');
    if (!form) return;

    const selectedTables = Array.from(form.querySelectorAll('[name="selectedTables"]:checked'))
        .map(checkbox => checkbox.value);

    if (selectedTables.length === 0) {
        showToast('请至少选择一个表', 'error');
        return;
    }

    const generateOptions = Array.from(form.querySelectorAll('[name="generateOptions"]:checked'))
        .map(checkbox => checkbox.value);

    if (generateOptions.length === 0) {
        showToast('请至少选择一个生成选项', 'error');
        return;
    }

    const data = {
        packageName: form.querySelector('[name="packageName"]').value,
        author: form.querySelector('[name="author"]').value,
        tables: selectedTables,
        options: generateOptions
    };

    // 显示加载状态
    showLoading('codeGenerate');
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/code/generate', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showGeneratedCode(data.files);
                showToast('代码生成成功', 'success');
            } else {
                showToast(data.message || '代码生成失败', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('代码生成失败', 'error');
        })
        .finally(() => {
            hideLoading('codeGenerate');
        });
}

// 显示连接状态
function showConnectionStatus(isConnected) {
    const statusDiv = document.createElement('div');
    statusDiv.className = `connection-status ${isConnected ? 'connected' : 'disconnected'}`;
    statusDiv.innerHTML = `
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="${isConnected ?
        'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' :
        'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'}"/>
                </svg>
                <span>${isConnected ? '数据库已连接' : '数据库未连接'}</span>
            `;

    // 移除旧的状态显示（如果存在）
    const oldStatus = document.querySelector('.connection-status');
    if (oldStatus) {
        oldStatus.remove();
    }

    // 添加新的状态显示
    const form = document.getElementById('dbConfigForm');
    form.insertBefore(statusDiv, form.firstChild);
}

// 添加连接状态样式
const styleSheet = document.createElement('style');
styleSheet.textContent = `
            .connection-status {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                padding: 0.75rem;
                border-radius: 4px;
                margin-bottom: 1rem;
            }
            
            .connection-status.connected {
                background-color: rgba(5, 150, 105, 0.1);
                color: var(--success-color);
            }
            
            .connection-status.disconnected {
                background-color: rgba(220, 38, 38, 0.1);
                color: var(--danger-color);
            }
            
            .connection-status svg {
                flex-shrink: 0;
            }
        `;
document.head.appendChild(styleSheet);

function getLanguageFromFilename(filename) {
    const ext = filename.split('.').pop().toLowerCase();
    const languageMap = {
        'java': 'java',
        'xml': 'xml',
        'sql': 'sql',
        'js': 'javascript',
        'ts': 'typescript',
        'html': 'html',
        'css': 'css',
        'json': 'json',
        'yml': 'yaml',
        'yaml': 'yaml',
        'md': 'markdown',
        'properties': 'properties'
    };
    return languageMap[ext] || 'plaintext';
}

function connectDatabase() {
    const form = document.getElementById('dbConfigForm');
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const token = AuthUtil.getToken();
    // 显示加载状态
    showLoading('databaseLoading');

    fetch('/api/hibiscus/code/connect-database', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showConnectionStatus(true);
                // 显示生成配置区域
                const generateConfig = document.getElementById('generateConfig');
                if (generateConfig) {
                    generateConfig.style.display = 'block';
                }
                // 显示表列表
                if (data.tables) {
                    showTables(data.tables);
                }
                showToast('数据库连接成功', 'success');
            } else {
                showConnectionStatus(false);
                showToast(data.message || '数据库连接失败', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showConnectionStatus(false);
            showToast('数据库连接失败', 'error');
        })
        .finally(() => {
            hideLoading('databaseLoading');
        });
}

// 修改显示/隐藏加载状态的辅助函数
function showLoading(id) {
    const mask = document.getElementById(id + 'Mask');
    if (mask) {
        mask.classList.add('show');
    }
}

function hideLoading(id) {
    const mask = document.getElementById(id + 'Mask');
    if (mask) {
        mask.classList.remove('show');
    }
}

// 修改代码生成相关函数
async function generateCode() {
    showLoading('codeGenerateLoading');
    try {
        const form = document.getElementById('generateForm');
        const selectedTables = Array.from(form.querySelectorAll('input[name="selectedTables"]:checked'))
            .map(checkbox => checkbox.value);
        const generateOptions = Array.from(form.querySelectorAll('input[name="generateOptions"]:checked'))
            .map(checkbox => checkbox.value);

        if (selectedTables.length === 0) {
            showToast('请选择要生成代码的表', 'warning');
            return;
        }

        const requestData = {
            packageName: form.packageName.value,
            author: form.author.value,
            tables: selectedTables,
            options: generateOptions
        };
        const token = AuthUtil.getToken();
        const response = await fetch('/api/hibiscus/code/generate', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });
        const data = await response.json();

        if (data.success) {
            window.generatedFiles = data.files;
            showGeneratedCode(data.files);
        } else {
            showAlert('生成失败：' + data.message, '错误');
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('生成代码失败', 'error');
    } finally {
        hideLoading('codeGenerateLoading');
    }
}

function showGeneratedCode(files) {
    const resultSection = document.getElementById('resultSection');
    const codeTabs = document.getElementById('codeTabs');

    // 创建文件标签页
    codeTabs.innerHTML = Object.keys(files).map((filename, index) => `
                <button class="code-tab ${index === 0 ? 'active' : ''}"
                        onclick="switchCodeTab(this, '${filename}')">${filename}</button>
            `).join('');

    // 显示第一个文件的代码
    const firstFile = Object.keys(files)[0];
    currentFile = firstFile;

    resultSection.style.display = 'block';
    window.generatedFiles = files;

    // 初始化或更新编辑器
    if (!editor) {
        editor = monaco.editor.create(document.getElementById('codeContent'), {
            value: files[firstFile],
            language: getLanguageFromFilename(firstFile),
            theme: 'vs-dark',
            automaticLayout: true,
            minimap: {
                enabled: true
            },
            scrollBeyondLastLine: false,
            fontSize: 14,
            tabSize: 4,
            insertSpaces: true,
            formatOnPaste: true,
            formatOnType: true,
            autoIndent: 'full',
            wordWrap: 'on'
        });

        // 监听编辑器内容变化
        editor.onDidChangeModelContent(() => {
            hasUnsavedChanges = true;
        });

        // 添加快捷键支持
        editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, function () {
            if (hasUnsavedChanges) {
                saveChanges();
            }
        });
    } else {
        const model = editor.getModel();
        monaco.editor.setModelLanguage(model, getLanguageFromFilename(firstFile));
        editor.setValue(files[firstFile]);
    }
}

function switchCodeTab(tab, filename) {
    if (hasUnsavedChanges) {
        saveChanges();
    }

    document.querySelectorAll('.code-tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');

    currentFile = filename;
    editor.setValue(window.generatedFiles[filename]);
    monaco.editor.setModelLanguage(editor.getModel(), getLanguageFromFilename(filename));
    hasUnsavedChanges = false;
}

function saveChanges() {
    window.generatedFiles[currentFile] = editor.getValue();
    hasUnsavedChanges = false;
    showToast('修改已保存');
}

function showToast(message, type = 'success', duration = 3000) {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');

    // 移除所有类型类名
    toast.classList.remove('success', 'error', 'warning', 'info');

    // 设置消息和类型
    toastMessage.textContent = message;
    toast.classList.add(type);

    // 更新图标
    const icon = toast.querySelector('svg');
    switch (type) {
        case 'success':
            icon.innerHTML = '<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>';
            break;
        case 'error':
            icon.innerHTML = '<path d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>';
            break;
        case 'warning':
            icon.innerHTML = '<path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>';
            break;
        case 'info':
            icon.innerHTML = '<path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>';
            break;
    }

    // 显示 toast
    toast.classList.add('show');

    // 设置定时器移除 toast
    setTimeout(() => {
        toast.classList.remove('show');
    }, duration);
}

function downloadAllCode() {
    if (hasUnsavedChanges) {
        saveChanges();
    }
    window.location.href = '/api/hibiscus/code/download';
}

// 自定义alert函数
function showAlert(message, title = '提示') {
    return new Promise((resolve) => {
        const dialog = document.getElementById('alertDialog');
        document.getElementById('alertTitle').textContent = title;
        document.getElementById('alertMessage').textContent = message;
        dialog.classList.add('show');

        window.closeAlertDialog = function () {
            dialog.classList.remove('show');
            resolve();
        };
    });
}

// 自定义prompt函数
function showPrompt(message, defaultValue = '', title = '请输入') {
    return new Promise((resolve) => {
        const dialog = document.getElementById('promptDialog');
        const input = document.getElementById('promptInput');
        document.getElementById('promptTitle').textContent = title;
        input.value = defaultValue;
        dialog.classList.add('show');
        input.focus();

        window.closePromptDialog = function (confirmed) {
            dialog.classList.remove('show');
            resolve(confirmed ? input.value : null);
        };

        input.onkeyup = function (e) {
            if (e.key === 'Enter') {
                closePromptDialog(true);
            } else if (e.key === 'Escape') {
                closePromptDialog(false);
            }
        };
    });
}

// 替换原有的alert调用
async function applyToProject() {
    if (!window.generatedFiles) {
        await showAlert('请先生成代码');
        return;
    }

    if (hasUnsavedChanges) {
        saveChanges();
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/code/apply-to-project', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(window.generatedFiles)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('代码已成功应用到项目中', 'success');
            } else {
                showAlert('应用失败：' + data.message, '错误');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('应用失败', '错误');
        });
}

// 信号管理相关的全局变量
let currentSignalMetrics = {};
let currentSignalGroups = new Set();

// 刷新信号指标
async function refreshSignalMetrics() {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch('/api/hibiscus/signals/metrics',{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        if (response.ok) {
            const data = await response.json();
            // 确保 data 是数组，如果不是则转换为数组
            const metrics = Array.isArray(data) ? data : Object.values(data);
            const tbody = document.querySelector('#signal-metrics-table tbody');
            tbody.innerHTML = '';

            if (metrics && metrics.length > 0) {
                metrics.forEach(metric => {
                    if (metric) {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${metric.name || ''}</td>
                            <td>${metric.priority || 'MEDIUM'}</td>
                            <td>${metric.handlerCount || 0}</td>
                            <td>${metric.successCount || 0}</td>
                            <td>${metric.failureCount || 0}</td>
                            <td>
                                <button class="btn btn-sm btn-primary" onclick="configureSignal('${metric.name}')">配置</button>
                                <button class="btn btn-sm btn-warning" onclick="toggleMetrics('${metric.name}')">切换指标</button>
                                <button class="btn btn-sm btn-danger" onclick="clearSignal('${metric.name}')">清除</button>
                            </td>
                        `;
                        tbody.appendChild(tr);
                    }
                });
            } else {
                // 如果没有数据，显示空状态
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center">暂无信号指标数据</td>
                    </tr>
                `;
            }
        } else {
            showToast('获取信号指标失败', 'error');
        }
    } catch (error) {
        console.error('Error refreshing signal metrics:', error);
        showToast('获取信号指标失败', 'error');
    }
}

// 更新信号指标表格
function updateSignalMetricsTable(metrics) {
    const tbody = document.querySelector('#signal-metrics-table tbody');
    tbody.innerHTML = '';

    Object.entries(metrics).forEach(([signalName, data]) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${signalName}</td>
            <td>${data.totalEmissions || 0}</td>
            <td>${data.totalHandlers || 0}</td>
            <td>${data.averageHandlingTimeMs?.toFixed(2) || '0.00'} ms</td>
            <td>${data.successRate?.toFixed(2) || '0.00'}%</td>
            <td>${data.recordingMetrics ? '启用' : '禁用'}</td>
            <td>${data.persistent ? '启用' : '禁用'}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn-icon" onclick="configureSignal('${signalName}')" title="配置">
                        <i class="fas fa-cog"></i>
                    </button>
                    <button class="btn-icon" onclick="toggleMetrics('${signalName}', ${data.recordingMetrics})" title="${data.recordingMetrics ? '停用' : '启用'}指标">
                        <i class="fas fa-chart-line"></i>
                    </button>
                    <button class="btn-icon" onclick="togglePersistence('${signalName}', ${data.persistent})" title="${data.persistent ? '停用' : '启用'}持久化">
                        <i class="fas fa-database"></i>
                    </button>
                    <button class="btn-icon" onclick="clearSignal('${signalName}')" title="清除">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// 配置信号
async function configureSignal(signalName) {
    const configForm = document.createElement('div');
    configForm.innerHTML = `
        <form id="signalConfigForm" class="config-form">
            <div class="form-group">
                <label>异步处理</label>
                <input type="checkbox" name="async">
            </div>
            <div class="form-group">
                <label>最大重试次数</label>
                <input type="number" name="maxRetries" value="3" min="0">
            </div>
            <div class="form-group">
                <label>重试延迟(ms)</label>
                <input type="number" name="retryDelayMs" value="1000" min="0">
            </div>
            <div class="form-group">
                <label>最大处理器数量</label>
                <input type="number" name="maxHandlers" value="10" min="1">
            </div>
            <div class="form-group">
                <label>超时时间(ms)</label>
                <input type="number" name="timeoutMs" value="5000" min="0">
            </div>
            <div class="form-group">
                <label>记录指标</label>
                <input type="checkbox" name="recordMetrics">
            </div>
            <div class="form-group">
                <label>优先级</label>
                <select name="priority">
                    <option value="LOW">低</option>
                    <option value="MEDIUM" selected>中</option>
                    <option value="HIGH">高</option>
                </select>
            </div>
            <div class="form-group">
                <label>持久化</label>
                <input type="checkbox" name="persistent">
            </div>
            <div class="form-group">
                <label>信号组</label>
                <input type="text" name="groupName">
            </div>
        </form>
    `;

    const result = await showDialog('配置信号', configForm, true);
    if (!result) return;

    const formData = new FormData(document.getElementById('signalConfigForm'));
    const config = {
        async: formData.get('async') === 'on',
        maxRetries: parseInt(formData.get('maxRetries')),
        retryDelayMs: parseInt(formData.get('retryDelayMs')),
        maxHandlers: parseInt(formData.get('maxHandlers')),
        timeoutMs: parseInt(formData.get('timeoutMs')),
        recordMetrics: formData.get('recordMetrics') === 'on',
        priority: formData.get('priority'),
        persistent: formData.get('persistent') === 'on',
        groupName: formData.get('groupName')
    };

    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/${signalName}/config`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });

        if (response.ok) {
            showToast('信号配置更新成功', 'success');
            refreshSignalMetrics();
        } else {
            showToast('信号配置更新失败', 'error');
        }
    } catch (error) {
        console.error('Error configuring signal:', error);
        showToast('信号配置更新失败', 'error');
    }
}

// 切换指标记录
async function toggleMetrics(signalName, currentState) {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/${signalName}/metrics/${currentState ? 'disable' : 'enable'}`, {
            method: 'POST',
            headers:{
                'Authorization': `Bearer ${token}`,
            }
        });

        if (response.ok) {
            showToast(`指标记录${currentState ? '停用' : '启用'}成功`, 'success');
            refreshSignalMetrics();
        } else {
            showToast(`指标记录${currentState ? '停用' : '启用'}失败`, 'error');
        }
    } catch (error) {
        console.error('Error toggling metrics:', error);
        showToast(`指标记录${currentState ? '停用' : '启用'}失败`, 'error');
    }
}

// 清除信号
async function clearSignal(signalName) {
    if (!confirm(`确定要清除信号 "${signalName}" 吗？`)) {
        return;
    }

    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/${signalName}`, {
            method: 'DELETE',
            headers:{
                'Authorization': `Bearer ${token}`,
            }
        });

        if (response.ok) {
            showToast('信号清除成功', 'success');
            refreshSignalMetrics();
        } else {
            showToast('信号清除失败', 'error');
        }
    } catch (error) {
        console.error('Error clearing signal:', error);
        showToast('信号清除失败', 'error');
    }
}

// 显示对话框的通用函数
async function showDialog(title, content, showConfirm = false) {
    return new Promise((resolve) => {
        const dialog = document.createElement('div');
        dialog.className = 'dialog';
        dialog.innerHTML = `
            <div class="dialog-content">
                <div class="dialog-header">
                    <h3>${title}</h3>
                    <button class="close-btn">&times;</button>
                </div>
                <div class="dialog-body"></div>
                <div class="dialog-footer">
                    ${showConfirm ? '<button class="confirm-btn">确认</button>' : ''}
                    <button class="cancel-btn">取消</button>
                </div>
            </div>
        `;

        dialog.querySelector('.dialog-body').appendChild(
            typeof content === 'string' ? document.createTextNode(content) : content
        );

        const closeDialog = (result) => {
            document.body.removeChild(dialog);
            resolve(result);
        };

        dialog.querySelector('.close-btn').onclick = () => closeDialog(false);
        dialog.querySelector('.cancel-btn').onclick = () => closeDialog(false);

        if (showConfirm) {
            dialog.querySelector('.confirm-btn').onclick = () => closeDialog(true);
        }

        document.body.appendChild(dialog);
    });
}

// 显示提示消息
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                document.body.removeChild(toast);
            }, 300);
        }, 3000);
    }, 100);
}

// 显示创建信号组对话框
async function showCreateGroupModal() {
    const dialog = document.createElement('div');
    dialog.className = 'modal';
    dialog.style.display = 'block';
    dialog.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>创建信号组</h3>
                <span class="close" onclick="this.closest('.modal').style.display='none'">&times;</span>
            </div>
            <div class="modal-body">
                <form id="signalGroupForm" class="config-form">
                    <div class="form-group">
                        <label>组名</label>
                        <input type="text" name="name" class="form-control" required>
                    </div>
                    <div class="form-group">
                        <label>默认配置</label>
                        <div class="nested-form">
                            <div class="form-group">
                                <label>异步处理</label>
                                <input type="checkbox" name="async">
                            </div>
                            <div class="form-group">
                                <label>最大重试次数</label>
                                <input type="number" name="maxRetries" class="form-control" value="3" min="0">
                            </div>
                            <div class="form-group">
                                <label>重试延迟(ms)</label>
                                <input type="number" name="retryDelayMs" class="form-control" value="1000" min="0">
                            </div>
                            <div class="form-group">
                                <label>最大处理器数量</label>
                                <input type="number" name="maxHandlers" class="form-control" value="10" min="1">
                            </div>
                            <div class="form-group">
                                <label>超时时间(ms)</label>
                                <input type="number" name="timeoutMs" class="form-control" value="5000" min="0">
                            </div>
                            <div class="form-group">
                                <label>记录指标</label>
                                <input type="checkbox" name="recordMetrics">
                            </div>
                            <div class="form-group">
                                <label>优先级</label>
                                <select name="priority" class="form-control">
                                    <option value="LOW">低</option>
                                    <option value="MEDIUM" selected>中</option>
                                    <option value="HIGH">高</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>持久化</label>
                                <input type="checkbox" name="persistent">
                            </div>
                        </div>
                    </div>
                    <div class="button-group">
                        <button type="button" class="btn btn-secondary" onclick="this.closest('.modal').style.display='none'">取消</button>
                        <button type="submit" class="btn btn-primary">创建</button>
                    </div>
                </form>
            </div>
        </div>
    `;

    document.body.appendChild(dialog);

    // 处理表单提交
    const form = dialog.querySelector('#signalGroupForm');
    form.onsubmit = async (e) => {
        e.preventDefault();
        const formData = new FormData(form);
        const name = formData.get('name');

        if (!name) {
            showToast('组名不能为空', 'error');
            return;
        }

        const groupData = {
            name: name,
            defaultConfig: {
                async: formData.get('async') === 'on',
                maxRetries: parseInt(formData.get('maxRetries')) || 3,
                retryDelayMs: parseInt(formData.get('retryDelayMs')) || 1000,
                maxHandlers: parseInt(formData.get('maxHandlers')) || 10,
                timeoutMs: parseInt(formData.get('timeoutMs')) || 5000,
                recordMetrics: formData.get('recordMetrics') === 'on',
                priority: formData.get('priority') || 'MEDIUM',
                persistent: formData.get('persistent') === 'on'
            }
        };

        try {
            const token = AuthUtil.getToken();
            const response = await fetch('/api/hibiscus/signals/groups', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(groupData)
            });

            if (response.ok) {
                showToast('信号组创建成功', 'success');
                dialog.style.display = 'none';
                document.body.removeChild(dialog);
                refreshSignalGroups();
            } else {
                const errorData = await response.json();
                showToast(errorData.message || '信号组创建失败', 'error');
            }
        } catch (error) {
            console.error('Error creating signal group:', error);
            showToast('信号组创建失败', 'error');
        }
    };

    // 点击模态框外部关闭
    dialog.onclick = (e) => {
        if (e.target === dialog) {
            dialog.style.display = 'none';
            document.body.removeChild(dialog);
        }
    };
}

// 刷新信号组列表
async function refreshSignalGroups() {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch('/api/hibiscus/signals/groups',{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (response.ok) {
            const groups = await response.json();
            const tbody = document.querySelector('#signal-groups-table tbody');
            tbody.innerHTML = '';

            groups.forEach(group => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${group.name}</td>
                    <td>${group.signalCount || 0}</td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="configureSignalGroup('${group.name}')">配置</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteSignalGroup('${group.name}')">删除</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } else {
            showToast('获取信号组列表失败', 'error');
        }
    } catch (error) {
        console.error('Error refreshing signal groups:', error);
        showToast('获取信号组列表失败', 'error');
    }
}

// 配置信号组
async function configureSignalGroup(groupName) {
    const dialog = document.createElement('div');
    dialog.className = 'modal';
    dialog.style.display = 'block';
    dialog.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>配置信号组 - ${groupName}</h3>
                <span class="close" onclick="this.closest('.modal').style.display='none'">&times;</span>
            </div>
            <div class="modal-body">
                <form id="signalGroupConfigForm" class="config-form">
                    <div class="form-group">
                        <label>默认配置</label>
                        <div class="nested-form">
                            <div class="form-group">
                                <label>异步处理</label>
                                <input type="checkbox" name="async">
                            </div>
                            <div class="form-group">
                                <label>最大重试次数</label>
                                <input type="number" name="maxRetries" class="form-control" value="3" min="0">
                            </div>
                            <div class="form-group">
                                <label>重试延迟(ms)</label>
                                <input type="number" name="retryDelayMs" class="form-control" value="1000" min="0">
                            </div>
                            <div class="form-group">
                                <label>最大处理器数量</label>
                                <input type="number" name="maxHandlers" class="form-control" value="10" min="1">
                            </div>
                            <div class="form-group">
                                <label>超时时间(ms)</label>
                                <input type="number" name="timeoutMs" class="form-control" value="5000" min="0">
                            </div>
                            <div class="form-group">
                                <label>记录指标</label>
                                <input type="checkbox" name="recordMetrics">
                            </div>
                            <div class="form-group">
                                <label>优先级</label>
                                <select name="priority" class="form-control">
                                    <option value="LOW">低</option>
                                    <option value="MEDIUM" selected>中</option>
                                    <option value="HIGH">高</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>持久化</label>
                                <input type="checkbox" name="persistent">
                            </div>
                        </div>
                    </div>
                    <div class="button-group">
                        <button type="button" class="btn btn-secondary" onclick="this.closest('.modal').style.display='none'">取消</button>
                        <button type="submit" class="btn btn-primary">保存</button>
                    </div>
                </form>
            </div>
        </div>
    `;

    document.body.appendChild(dialog);

    // 获取当前配置并填充表单
    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/groups/${groupName}/config`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        if (response.ok) {
            const config = await response.json();
            const form = dialog.querySelector('#signalGroupConfigForm');

            // 填充表单数据
            form.querySelector('[name="async"]').checked = config.async;
            form.querySelector('[name="maxRetries"]').value = config.maxRetries;
            form.querySelector('[name="retryDelayMs"]').value = config.retryDelayMs;
            form.querySelector('[name="maxHandlers"]').value = config.maxHandlers;
            form.querySelector('[name="timeoutMs"]').value = config.timeoutMs;
            form.querySelector('[name="recordMetrics"]').checked = config.recordMetrics;
            form.querySelector('[name="priority"]').value = config.priority;
            form.querySelector('[name="persistent"]').checked = config.persistent;
        }
    } catch (error) {
        console.error('Error fetching signal group config:', error);
        showToast('获取信号组配置失败', 'error');
    }

    // 处理表单提交
    const form = dialog.querySelector('#signalGroupConfigForm');
    form.onsubmit = async (e) => {
        e.preventDefault();
        const formData = new FormData(form);

        const config = {
            async: formData.get('async') === 'on',
            maxRetries: parseInt(formData.get('maxRetries')) || 3,
            retryDelayMs: parseInt(formData.get('retryDelayMs')) || 1000,
            maxHandlers: parseInt(formData.get('maxHandlers')) || 10,
            timeoutMs: parseInt(formData.get('timeoutMs')) || 5000,
            recordMetrics: formData.get('recordMetrics') === 'on',
            priority: formData.get('priority') || 'MEDIUM',
            persistent: formData.get('persistent') === 'on'
        };

        try {
            const token = AuthUtil.getToken();
            const response = await fetch(`/api/hibiscus/signals/groups/${groupName}/config`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(config)
            });

            if (response.ok) {
                showToast('信号组配置更新成功', 'success');
                dialog.style.display = 'none';
                document.body.removeChild(dialog);
                refreshSignalGroups();
            } else {
                const errorData = await response.json();
                showToast(errorData.message || '信号组配置更新失败', 'error');
            }
        } catch (error) {
            console.error('Error updating signal group config:', error);
            showToast('信号组配置更新失败', 'error');
        }
    };

    // 点击模态框外部关闭
    dialog.onclick = (e) => {
        if (e.target === dialog) {
            dialog.style.display = 'none';
            document.body.removeChild(dialog);
        }
    };
}

// 删除信号组
async function deleteSignalGroup(groupName) {
    if (!confirm(`确定要删除信号组 "${groupName}" 吗？`)) {
        return;
    }

    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/groups/${groupName}`, {
            method: 'DELETE',
            headers:{
                'Authorization': `Bearer ${token}`,
            }
        });

        if (response.ok) {
            showToast('信号组删除成功', 'success');
            refreshSignalGroups();
        } else {
            const errorData = await response.json();
            showToast(errorData.message || '信号组删除失败', 'error');
        }
    } catch (error) {
        console.error('Error deleting signal group:', error);
        showToast('信号组删除失败', 'error');
    }
}

// 刷新信号指标
async function refreshSignalMetrics() {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch('/api/hibiscus/signals/metrics',{
            method: 'GET',
            headers:{
                'Authorization': `Bearer ${token}`,
            }
        });
        if (response.ok) {
            const data = await response.json();
            // 确保 data 是数组，如果不是则转换为数组
            const metrics = Array.isArray(data) ? data : Object.values(data);
            const tbody = document.querySelector('#signal-metrics-table tbody');
            tbody.innerHTML = '';

            if (metrics && metrics.length > 0) {
                metrics.forEach(metric => {
                    if (metric) {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${metric.name || ''}</td>
                            <td>${metric.priority || 'MEDIUM'}</td>
                            <td>${metric.handlerCount || 0}</td>
                            <td>${metric.successCount || 0}</td>
                            <td>${metric.failureCount || 0}</td>
                            <td>
                                <button class="btn btn-sm btn-primary" onclick="configureSignal('${metric.name}')">配置</button>
                                <button class="btn btn-sm btn-warning" onclick="toggleMetrics('${metric.name}')">切换指标</button>
                                <button class="btn btn-sm btn-danger" onclick="clearSignal('${metric.name}')">清除</button>
                            </td>
                        `;
                        tbody.appendChild(tr);
                    }
                });
            } else {
                // 如果没有数据，显示空状态
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center">暂无信号指标数据</td>
                    </tr>
                `;
            }
        } else {
            showToast('获取信号指标失败', 'error');
        }
    } catch (error) {
        console.error('Error refreshing signal metrics:', error);
        showToast('获取信号指标失败', 'error');
    }
}

// 配置信号
async function configureSignal(signalName) {
    // TODO: 实现信号配置功能
    showToast('信号配置功能正在开发中', 'info');
}

// 切换信号指标记录
async function toggleMetrics(signalName) {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/${signalName}/metrics`, {
            method: 'POST',
            headers:{
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            showToast('信号指标状态已更新', 'success');
            refreshSignalMetrics();
        } else {
            const errorData = await response.json();
            showToast(errorData.message || '切换信号指标失败', 'error');
        }
    } catch (error) {
        console.error('Error toggling signal metrics:', error);
        showToast('切换信号指标失败', 'error');
    }
}

// 清除信号
async function clearSignal(signalName) {
    if (!confirm(`确定要清除信号 "${signalName}" 吗？`)) {
        return;
    }

    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/signals/${signalName}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });

        if (response.ok) {
            showToast('信号清除成功', 'success');
            refreshSignalMetrics();
        } else {
            const errorData = await response.json();
            showToast(errorData.message || '信号清除失败', 'error');
        }
    } catch (error) {
        console.error('Error clearing signal:', error);
        showToast('信号清除失败', 'error');
    }
}

// 初始化信号管理页面
document.addEventListener('DOMContentLoaded', () => {
    // 初始加载信号组和指标数据
    refreshSignalGroups();
    refreshSignalMetrics();
});

// 在页面切换功能
function switchPage(pageId) {
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
        page.style.display = 'none';
    });
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });

    const targetPage = document.getElementById(pageId);
    if (targetPage) {
        targetPage.style.display = 'block';
        targetPage.classList.add('active');
    }

    document.querySelector(`.nav-item[onclick="switchPage('${pageId}')"]`)?.classList.add('active');

    // 停止之前的连接检查
    stopRedisConnectionCheck();

    if (pageId === 'redis-admin') {
        checkRedisConnection();
        startRedisConnectionCheck();
    } else if (pageId === 'database-admin') {
        checkDatabaseConnection();
    } else if (pageId === 'config-manager') {
        loadConfigFiles();
    } else if (pageId === 'cache-manager') {
        // refreshCacheOverview();
        refreshCacheInstances();
    } else if (pageId === 'performance-monitor') {
        updateAllMetrics();
        if (monitorTimer) {
            clearInterval(monitorTimer);
        }
        monitorTimer = setInterval(updateAllMetrics, 12000);
    } else if (pageId === 'code-generate') {
        // 在code-generate页面也检查数据库连接
        checkInitialDatabaseConnection();
    } else if (pageId === 'signal-manager') {
        refreshSignalMetrics();
        refreshSignalGroups();
    } else {
        if (monitorTimer) {
            clearInterval(monitorTimer);
            monitorTimer = null;
        }
    }
}

// 在页面卸载时清理
window.addEventListener('unload', () => {
    stopSignalRefresh();
});

// 数据库管理相关函数
async function checkDatabaseConnection() {
    showLoading('databaseLoading');
    try {
        const token = AuthUtil.getToken();
        const response = await fetch('/api/hibiscus/database/check-connection',{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
            }
        });
        const data = await response.json();
        if (data.connected) {
            showDatabaseManager(data.tables);
        } else {
            showEmptyState();
        }
    } catch (error) {
        console.error('Error:', error);
        showEmptyState();
    } finally {
        hideLoading('databaseLoading');
    }
}

function showEmptyState() {
    const content = document.getElementById('database-content');
    content.innerHTML = `
                <div class="empty-state">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4"/>
                    </svg>
                    <h3>未检测到数据库连接</h3>
                    <p>请在配置文件中配置数据库连接信息</p>
                </div>
            `;
}

function showDatabaseManager(tables) {
    const content = document.getElementById('database-content');
    content.innerHTML = `
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>表名</th>
                                <th>记录数</th>
                                <th>备注</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tables.map(table => `
                                <tr>
                                    <td>${table.name}</td>
                                    <td>${table.recordCount >= 0 ? table.recordCount : '未知'}</td>
                                    <td>${table.comment || ''}</td>
                                    <td class="action-buttons">
                                        <button class="btn-icon" onclick="viewTable('${table.name}')" title="查看数据">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                                <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                                            </svg>
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            `;
}

function viewTable(tableName) {
    const modal = document.getElementById('dataModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalContent = document.getElementById('modalContent');

    modalTitle.textContent = `${tableName} 表数据`;
    modalContent.innerHTML = '<div class="loading">加载中...</div>';
    modal.classList.add('show');
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/database/table/${tableName}`,{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('获取数据失败');
            }
            return response.json();
        })
        .then(data => {
            if (Array.isArray(data) && data.length > 0) {
                currentTableData = data;
                currentTableName = tableName;
                currentPrimaryKey = Object.keys(data[0])[0];
                currentPage = 1;
                searchTerm = '';
                document.getElementById('searchInput').value = '';
                renderTableData();
            } else {
                modalContent.innerHTML = '<div class="empty-state">暂无数据</div>';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            modalContent.innerHTML = `<div class="error">获取数据失败：${error.message}</div>`;
        });
}

let currentTableData = [];
let currentPage = 1;
const pageSize = 10;
let searchTerm = '';
let currentTableName = '';
let currentPrimaryKey = '';
let deleteRowData = null;

function renderTableData() {
    const modalContent = document.getElementById('modalContent');
    const filteredData = currentTableData.filter(row =>
        Object.values(row).some(value =>
            String(value).toLowerCase().includes(searchTerm)
        )
    );

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const pageData = filteredData.slice(startIndex, endIndex);

    if (pageData.length === 0) {
        modalContent.innerHTML = '<div class="empty-state"><p>没有找到匹配的数据</p></div>';
        return;
    }

    const columns = Object.keys(currentTableData[0]);

    modalContent.innerHTML = `
                <div class="table-wrapper">
                    <table class="data-table">
                        <thead>
                            <tr>
                                ${columns.map(col => `<th>${col}</th>`).join('')}
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${pageData.map(row => `
                                <tr>
                                    ${columns.map(col => `<td title="${row[col]}">${row[col]}</td>`).join('')}
                                    <td class="action-buttons">
                                        <button class="btn-icon" onclick="editRow('${JSON.stringify(row).replace(/"/g, '&quot;')}')" title="编辑">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                                            </svg>
                                        </button>
                                        <button class="btn-icon" onclick="deleteRow('${JSON.stringify(row).replace(/"/g, '&quot;')}')" title="删除">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                                            </svg>
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            `;

    renderPagination(filteredData.length);
}

function renderPagination(totalItems) {
    const totalPages = Math.ceil(totalItems / pageSize);
    const pagination = document.getElementById('pagination');

    let paginationHtml = `
                <button onclick="changePage(1)" ${currentPage === 1 ? 'disabled' : ''}>
                    首页
                </button>
                <button onclick="changePage(${currentPage - 1})" ${currentPage === 1 ? 'disabled' : ''}>
                    上一页
                </button>
            `;

    for (let i = Math.max(1, currentPage - 2); i <= Math.min(totalPages, currentPage + 2); i++) {
        paginationHtml += `
                    <button onclick="changePage(${i})" class="${i === currentPage ? 'active' : ''}">
                        ${i}
                    </button>
                `;
    }

    paginationHtml += `
                <button onclick="changePage(${currentPage + 1})" ${currentPage === totalPages ? 'disabled' : ''}>
                    下一页
                </button>
                <button onclick="changePage(${totalPages})" ${currentPage === totalPages ? 'disabled' : ''}>
                    末页
                </button>
            `;

    pagination.innerHTML = paginationHtml;
}

function changePage(page) {
    currentPage = page;
    renderTableData();
}

function closeModal() {
    const modal = document.getElementById('dataModal');
    modal.classList.remove('show');
}

function editRow(rowData) {
    const row = JSON.parse(rowData);
    const editModal = document.getElementById('editModal');
    const editForm = document.getElementById('editForm');

    // 生成编辑表单
    let formHtml = '';
    for (const [key, value] of Object.entries(row)) {
        formHtml += `
                    <div class="form-group">
                        <label for="${key}">${key}</label>
                        <input type="text" id="${key}" name="${key}" value="${value || ''}"
                               ${key === currentPrimaryKey ? 'readonly' : ''}>
                    </div>
                `;
    }

    formHtml += `
                <div class="button-group">
                    <button type="button" class="btn btn-primary" onclick="closeEditModal()">取消</button>
                    <button type="submit" class="btn btn-success">保存</button>
                </div>
            `;

    editForm.innerHTML = formHtml;

    // 添加表单提交处理
    editForm.onsubmit = function (e) {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = {};
        formData.forEach((value, key) => {
            data[key] = value;
        });

        updateRow(data);
    };

    editModal.classList.add('show');
}

function updateRow(data) {
    const primaryKeyValue = data[currentPrimaryKey];
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/database/table/${currentTableName}?primaryKey=${currentPrimaryKey}&primaryKeyValue=${primaryKeyValue}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('更新失败');
            }
            showToast('更新成功', 'success');
            closeEditModal();
            // 刷新表格数据
            viewTable(currentTableName);
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('更新失败：' + error.message, 'error');
        });
}

function deleteRow(rowData) {
    try {
        deleteRowData = typeof rowData === 'string' ? JSON.parse(rowData) : rowData;
        if (!currentPrimaryKey || !deleteRowData[currentPrimaryKey]) {
            throw new Error('未找到主键或主键值');
        }
        const confirmDialog = document.getElementById('confirmDialog');
        confirmDialog.classList.add('show');
    } catch (error) {
        console.error('Error:', error);
        showToast('删除失败：' + error.message, 'error');
    }
}

function confirmDelete() {
    if (!deleteRowData || !currentPrimaryKey || !deleteRowData[currentPrimaryKey]) {
        showToast('无法获取主键信息', 'error');
        return;
    }

    const primaryKeyValue = deleteRowData[currentPrimaryKey];
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/database/table/${currentTableName}?primaryKey=${currentPrimaryKey}&primaryKeyValue=${primaryKeyValue}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.message === '删除成功') {
                showToast('删除成功', 'success');
                closeConfirmDialog();
                viewTable(currentTableName);
            } else {
                throw new Error(data.message || '删除失败');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('删除失败：' + error.message, 'error');
        });
}

function closeEditModal() {
    const editModal = document.getElementById('editModal');
    editModal.classList.remove('show');
}

function closeConfirmDialog() {
    const confirmDialog = document.getElementById('confirmDialog');
    confirmDialog.classList.remove('show');
    deleteRowData = null;
}

// 点击模态框外部关闭
window.onclick = function (event) {
    const dataModal = document.getElementById('dataModal');
    const editModal = document.getElementById('editModal');
    const confirmDialog = document.getElementById('confirmDialog');

    if (event.target === dataModal) {
        closeModal();
    } else if (event.target === editModal) {
        closeEditModal();
    } else if (event.target === confirmDialog) {
        closeConfirmDialog();
    }
}

// Redis管理相关函数
async function connectRedis() {
    showLoading('redisLoading');
    try {
        const form = document.getElementById('redisConfigForm');
        const formData = new FormData(form);
        const config = {};
        formData.forEach((value, key) => config[key] = value);
        const token = AuthUtil.getToken();
        const response = await fetch('/api/hibiscus/redis/connect', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });
        const data = await response.json();

        if (data.success) {
            document.getElementById('redisManager').classList.remove('hidden');
            showToast('Redis连接成功', 'success');
            // 立即检查连接状态并加载数据
            checkRedisConnection();
            searchRedisKeys();
        } else {
            showToast('连接失败：' + data.message, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('连接失败', 'error');
    } finally {
        hideLoading('redisLoading');
    }
}

// Redis分页相关变量
let redisCurrentPage = 1;
const redisPageSize = 10;
let redisAllKeys = [];

function searchRedisKeys() {
    const pattern = document.getElementById('redisSearchPattern').value || '*';
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/redis/keys?pattern=${encodeURIComponent(pattern)}`,{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                redisAllKeys = data.keys;
                renderRedisKeys();
            } else {
                showToast('获取键列表失败：' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('获取键列表失败', 'error');
        });
}

function renderRedisKeys() {
    const tbody = document.getElementById('redisKeysList');
    tbody.innerHTML = '';

    const startIndex = (redisCurrentPage - 1) * redisPageSize;
    const endIndex = startIndex + redisPageSize;
    const pageKeys = redisAllKeys.slice(startIndex, endIndex);

    pageKeys.forEach(key => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
                    <td>${key.name}</td>
                    <td>${key.type}</td>
                    <td>${formatRedisValue(key.value, key.type)}</td>
                    <td>${key.ttl === -1 ? '永久' : key.ttl + '秒'}</td>
                    <td class="action-buttons">
                        <button class="btn-icon" onclick="editRedisKey('${key.name}')" title="编辑">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                            </svg>
                        </button>
                        <button class="btn-icon" onclick="deleteRedisKey('${key.name}')" title="删除">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                            </svg>
                        </button>
                        <button class="btn-icon" onclick="setTTL('${key.name}')" title="设置过期时间">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                            </svg>
                        </button>
                    </td>
                `;
        tbody.appendChild(tr);
    });

    renderRedisPagination();
}

function renderRedisPagination() {
    const totalPages = Math.ceil(redisAllKeys.length / redisPageSize);
    const pagination = document.getElementById('redisPagination');

    let paginationHtml = `
                <button onclick="changeRedisPage(1)" ${redisCurrentPage === 1 ? 'disabled' : ''}>
                    首页
                </button>
                <button onclick="changeRedisPage(${redisCurrentPage - 1})" ${redisCurrentPage === 1 ? 'disabled' : ''}>
                    上一页
                </button>
            `;

    for (let i = Math.max(1, redisCurrentPage - 2); i <= Math.min(totalPages, redisCurrentPage + 2); i++) {
        paginationHtml += `
                    <button onclick="changeRedisPage(${i})" class="${i === redisCurrentPage ? 'active' : ''}">
                        ${i}
                    </button>
                `;
    }

    paginationHtml += `
                <button onclick="changeRedisPage(${redisCurrentPage + 1})" ${redisCurrentPage === totalPages ? 'disabled' : ''}>
                    下一页
                </button>
                <button onclick="changeRedisPage(${totalPages})" ${redisCurrentPage === totalPages ? 'disabled' : ''}>
                    末页
                </button>
                <span class="pagination-info">共 ${redisAllKeys.length} 条</span>
            `;

    pagination.innerHTML = paginationHtml;
}

function changeRedisPage(page) {
    redisCurrentPage = page;
    renderRedisKeys();
}

// 修改刷新方法
function refreshKeys() {
    redisCurrentPage = 1; // 重置到第一页
    searchRedisKeys();
}

function formatRedisValue(value, type) {
    switch (type) {
        case 'string':
            return value;
        case 'list':
        case 'set':
        case 'zset':
            return Array.isArray(value) ? value.join(', ') : value;
        case 'hash':
            return JSON.stringify(value, null, 2);
        default:
            return value;
    }
}

function addRedisKey() {
    const key = document.getElementById('newKey').value;
    const type = document.getElementById('valueType').value;
    const value = document.getElementById('newValue').value;

    if (!key || !value) {
        showToast('键和值不能为空', 'warning');
        return;
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/redis/key', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            key,
            type,
            value: parseRedisValue(value, type)
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('添加成功', 'success');
                document.getElementById('newKey').value = '';
                document.getElementById('newValue').value = '';
                searchRedisKeys();
            } else {
                showToast('添加失败：' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('添加失败', 'error');
        });
}

function parseRedisValue(value, type) {
    switch (type) {
        case 'string':
            return value;
        case 'list':
        case 'set':
            return value.split(',').map(v => v.trim());
        case 'hash':
            try {
                return JSON.parse(value);
            } catch (e) {
                throw new Error('Hash值必须是有效的JSON格式');
            }
        case 'zset':
            return value.split(',').map(v => {
                const [member, score] = v.trim().split(':');
                return {member, score: parseFloat(score)};
            });
        default:
            return value;
    }
}

function editRedisKey(key) {
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/redis/key/${encodeURIComponent(key)}`,{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showEditModal(key, data.type, data.value);
            } else {
                showToast('获取键值失败：' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('获取键值失败', 'error');
        });
}

function showEditModal(key, type, value) {
    const editModal = document.getElementById('editModal');
    const editForm = document.getElementById('editForm');

    editForm.innerHTML = `
                <div class="form-group">
                    <label>键名</label>
                    <input type="text" value="${key}" readonly>
                </div>
                <div class="form-group">
                    <label>类型</label>
                    <input type="text" value="${type}" readonly>
                </div>
                <div class="form-group">
                    <label>值</label>
                    <textarea class="form-control">${formatRedisValueForEdit(value, type)}</textarea>
                </div>
                <div class="button-group">
                    <button type="button" class="btn btn-primary" onclick="closeEditModal()">取消</button>
                    <button type="submit" class="btn btn-success">保存</button>
                </div>
            `;

    editForm.onsubmit = function (e) {
        e.preventDefault();
        const newValue = e.target.querySelector('textarea').value;

        updateRedisKey(key, type, newValue);
    };

    editModal.classList.add('show');
}

function formatRedisValueForEdit(value, type) {
    switch (type) {
        case 'string':
            return value;
        case 'list':
        case 'set':
            return Array.isArray(value) ? value.join(',') : value;
        case 'hash':
            return JSON.stringify(value, null, 2);
        case 'zset':
            return Array.isArray(value) ? value.map(v => `${v.member}:${v.score}`).join(',') : value;
        default:
            return value;
    }
}

function updateRedisKey(key, type, newValue) {
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/redis/key/${encodeURIComponent(key)}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            type,
            value: parseRedisValue(newValue, type)
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('更新成功', 'success');
                closeEditModal();
                searchRedisKeys();
            } else {
                showToast('更新失败：' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('更新失败', 'error');
        });
}

function deleteRedisKey(key) {
    const confirmDialog = document.getElementById('confirmDialog');
    document.querySelector('#confirmDialog h3').textContent = '确认删除';
    document.querySelector('#confirmDialog p').textContent = `确定要删除键 "${key}" 吗？此操作不可恢复。`;

    // 更新确认按钮的点击事件
    window.confirmDelete = function () {
        const token = AuthUtil.getToken();
        fetch(`/api/hibiscus/redis/key/${encodeURIComponent(key)}`, {
            method: 'DELETE',
            headers:{
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('删除成功', 'success');
                    searchRedisKeys();
                } else {
                    showToast('删除失败：' + data.message, 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showToast('删除失败', 'error');
            });
        closeConfirmDialog();
    };

    confirmDialog.classList.add('show');
}

async function setTTL(key) {
    const ttl = await showPrompt('请输入过期时间（秒），-1表示永久：', '-1', '设置过期时间');
    if (ttl === null) return; // 用户取消了输入

    if (!/^-?\d+$/.test(ttl)) {
        showAlert('请输入有效的数字', '错误');
        return;
    }
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/redis/key/${encodeURIComponent(key)}/ttl`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ttl: parseInt(ttl)})
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('过期时间设置成功', 'success');
                refreshKeys();
            } else {
                showAlert('设置失败：' + data.message, '错误');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('设置失败', '错误');
        });
}

function checkRedisConnection() {
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/redis/connection-info',{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 显示连接状态
                const form = document.getElementById('redisConfigForm');
                const statusDiv = document.createElement('div');
                statusDiv.className = `connection-status ${data.connected ? 'connected' : 'disconnected'}`;
                statusDiv.innerHTML = `
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="${data.connected ?
                    'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' :
                    'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'}"/>
                            </svg>
                            <span>Redis ${data.connected ? '已连接' : '未连接'}</span>
                        `;

                // 移除旧的状态显示（如果存在）
                const oldStatus = form.querySelector('.connection-status');
                if (oldStatus) {
                    oldStatus.remove();
                }
                form.insertBefore(statusDiv, form.firstChild);

                if (data.connected) {
                    // 如果已连接，自动填充表单并显示管理界面
                    form.querySelector('[name="host"]').value = data.host || 'localhost';
                    form.querySelector('[name="port"]').value = data.port || 6379;
                    form.querySelector('[name="database"]').value = data.database || 0;

                    // 显示Redis管理界面
                    document.getElementById('redisManager').classList.remove('hidden');

                    // 加载Redis数据
                    searchRedisKeys();
                } else {
                    // 如果未连接，隐藏管理界面
                    document.getElementById('redisManager').classList.add('hidden');
                }
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('获取Redis连接状态失败', 'error');
        });
}

// 添加定期检查Redis连接状态的功能
let redisConnectionChecker;

function startRedisConnectionCheck() {
    // 清除可能存在的旧定时器
    if (redisConnectionChecker) {
        clearInterval(redisConnectionChecker);
    }
    // 每30秒检查一次连接状态
    redisConnectionChecker = setInterval(checkRedisConnection, 30000);
}

function stopRedisConnectionCheck() {
    if (redisConnectionChecker) {
        clearInterval(redisConnectionChecker);
        redisConnectionChecker = null;
    }
}

// 添加搜索功能
const searchInput = document.getElementById('searchInput');
if (searchInput) {
    searchInput.oninput = function () {
        searchTerm = this.value.toLowerCase();
        currentPage = 1;
        renderTableData();
    };
}

// 配置管理相关函数
let configEditor = null;
let compareEditor1 = null;
let compareEditor2 = null;
let currentConfigFile = null;

// 初始化编辑器
function initConfigEditors() {
    if (!configEditor) {
        configEditor = monaco.editor.create(document.getElementById('configEditor'), {
            language: 'yaml',
            theme: 'vs-dark',
            automaticLayout: true,
            minimap: {enabled: false}
        });
    }

    if (!compareEditor1) {
        compareEditor1 = monaco.editor.create(document.getElementById('compareEditor1'), {
            language: 'yaml',
            theme: 'vs-dark',
            readOnly: true,
            automaticLayout: true,
            minimap: {enabled: false}
        });
    }

    if (!compareEditor2) {
        compareEditor2 = monaco.editor.create(document.getElementById('compareEditor2'), {
            language: 'yaml',
            theme: 'vs-dark',
            readOnly: true,
            automaticLayout: true,
            minimap: {enabled: false}
        });
    }
}

// 加载配置文件列表
async function loadConfigFiles() {
    showLoading('configLoading');
    try {
        // 从后端获取配置文件列表
        const files = [
            {
                name: 'application.properties',
                path: 'src/main/resources/application.properties',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application.yml',
                path: 'src/main/resources/application.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application-dev.yml',
                path: 'src/main/resources/application-dev.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application-prod.yml',
                path: 'src/main/resources/application-prod.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application-test.yml',
                path: 'src/main/resources/application-test.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application-prop.yml',
                path: 'src/main/resources/application-prop.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application-staging.yml',
                path: 'src/main/resources/application-staging.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'application-local.yml',
                path: 'src/main/resources/application-local.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'logback-spring.xml',
                path: 'src/main/resources/logback-spring.xml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'bootstrap.yml',
                path: 'src/main/resources/bootstrap.yml',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'banner.txt',
                path: 'src/main/resources/banner.txt',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'messages.properties',
                path: 'src/main/resources/messages/messages.properties',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'messages_en_US.properties',
                path: 'src/main/resources/messages/messages_en_US.properties',
                lastModified: new Date().toLocaleString()
            },
            {
                name: 'messages_zh_CN.properties',
                path: 'src/main/resources/messages/messages_zh_CN.properties',
                lastModified: new Date().toLocaleString()
            },
        ];

        renderConfigFiles(files);
        updateHistoryFileSelect(files);

        // 如果有当前选中的文件，显示其历史记录
        if (currentConfigFile) {
            await viewHistory(currentConfigFile);
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('加载配置文件失败', 'error');
    } finally {
        hideLoading('configLoading');
    }
}

// 更新历史记录文件选择下拉框
function updateHistoryFileSelect(files) {
    const select = document.getElementById('historyFileSelect');
    select.innerHTML = '<option value="">选择文件查看历史</option>' +
        files.map(file => `<option value="${file.path}">${file.name}</option>`).join('');

    // 如果有当前选中的文件，设置为选中状态
    if (currentConfigFile) {
        select.value = currentConfigFile;
    }
}

// 渲染配置文件列表（折叠结构）
function renderConfigFiles(files) {
    // 按类别分类
    const categorizedFiles = {
        "配置文件类": files.filter(file =>
            file.name.startsWith("application") || file.name === "bootstrap.yml"
        ),
        "语言包类": files.filter(file =>
            file.name.startsWith("messages")
        ),
        "日志配置类": files.filter(file =>
            file.name === "logback-spring.xml" || file.name === "banner.txt"
        ),
        "其他": files.filter(file =>
            !file.name.startsWith("application") &&
            !file.name.startsWith("messages") &&
            file.name !== "logback-spring.xml" &&
            file.name !== "banner.txt"
        )
    };

    // 渲染 HTML
    const tbody = document.getElementById("configFilesList");
    tbody.innerHTML = Object.keys(categorizedFiles)
        .map(category => `
            <tr class="category-header" onclick="toggleCategory('${category}')">
                <td colspan="4">
                    <strong>${category}</strong>
                    <span class="toggle-icon">+</span>
                </td>
            </tr>
            <tr class="category-content hidden" id="category-${category}">
                <td colspan="4">
                    <table class="nested-table">
                        <tbody>
                            ${categorizedFiles[category].map(file => `
                                <tr>
                                    <td>${file.name}</td>
                                    <td>${file.path}</td>
                                    <td>${file.lastModified}</td>
                                    <td class="action-buttons">
                                        <button class="btn-icon" onclick="editConfig('${file.path}')" title="编辑">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                                            </svg>
                                        </button>
                                        <button class="btn-icon" onclick="viewHistory('${file.path}')" title="历史记录">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                                            </svg>
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </td>
            </tr>
        `).join('');
}

// 折叠/展开类别
function toggleCategory(category) {
    // 获取折叠内容行
    const contentRow = document.getElementById(`category-${category}`);
    // 获取标题行的折叠图标
    const headerRow = Array.from(document.querySelectorAll(".category-header"))
        .find(header => header.textContent.includes(category))
        ?.querySelector(".toggle-icon");

    if (!contentRow || !headerRow) return;

    // 切换折叠状态
    if (contentRow.classList.contains("hidden")) {
        contentRow.classList.remove("hidden"); // 展开
        headerRow.textContent = "-";          // 更新图标
    } else {
        contentRow.classList.add("hidden");   // 折叠
        headerRow.textContent = "+";          // 更新图标
    }
}


// 编辑配置文件
async function editConfig(filePath) {
    showLoading('configLoading');
    try {
        currentConfigFile = filePath;
        const modal = document.getElementById('configEditModal');
        modal.classList.add('show');
        initConfigEditors();
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/config/content?filePath=${encodeURIComponent(filePath)}`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const data = await response.json();

        if (data.success) {
            configEditor.setValue(data.content);
            document.getElementById('configDescription').value = '';
        } else {
            showToast(data.error, 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('获取配置内容失败', 'error');
    } finally {
        hideLoading('configLoading');
    }
}

// 验证配置
function validateConfig() {
    const content = configEditor.getValue();
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/config/validate', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({content})
    })
        .then(response => response.json())
        .then(data => {
            if (data.valid) {
                showToast('配置验证通过', 'success');
            } else {
                showToast('配置验证失败', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('验证失败', 'error');
        });
}

// 保存配置
function saveConfig() {
    const content = configEditor.getValue();
    const description = document.getElementById('configDescription').value;

    if (!description) {
        showToast('请输入修改说明', 'warning');
        return;
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/config/backup', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            filePath: currentConfigFile,
            operator: 'ADMIN', // 这里应该是当前登录用户
            description,
            content
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                showToast('保存成功', 'success');
                closeConfigEditModal();
                loadConfigFiles();
                // 如果历史记录已打开，刷新历史记录
                if (!document.querySelector('.config-history').classList.contains('hidden')) {
                    viewHistory(currentConfigFile);
                }
            } else {
                showToast(data.error, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('保存失败', 'error');
        });
}

// 查看历史记录
function viewHistory(filePath) {
    if (!filePath) {
        document.getElementById('configHistoryList').innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center">请选择文件查看历史记录</td>
                    </tr>
                `;
        return;
    }

    currentConfigFile = filePath;

    // 更新文件选择下拉框
    const select = document.getElementById('historyFileSelect');
    select.value = filePath;

    // 更新标题显示当前文件
    const historyTitle = document.querySelector('.config-history .section-header h3');
    historyTitle.innerHTML = `修改历史 - ${filePath}`;
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/config/history?filePath=${encodeURIComponent(filePath)}`,{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('configHistoryList');
            if (Array.isArray(data) && data.length > 0) {
                tbody.innerHTML = data.map(history => `
                            <tr>
                                <td>${history.id}</td>
                                <td>${formatDateTime(history.createTime)}</td>
                                <td>${history.operator}</td>
                                <td>${history.description}</td>
                                <td>${history.active ? '<span class="badge success">当前版本</span>' : ''}</td>
                                <td class="action-buttons">
                                    <button class="btn-icon" onclick="compareConfig(${history.id})" title="对比">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M16 3h5v5M4 21l16-16M21 3v5M3 16h5"/>
                                        </svg>
                                    </button>
                                    ${!history.active ? `
                                        <button class="btn-icon" onclick="confirmRollback(${history.id})" title="回滚到此版本">
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                                <path d="M3 12a9 9 0 019-9 9.75 9.75 0 017 3l3 3m0-6v6h-6"/>
                                            </svg>
                                        </button>
                                    ` : ''}
                                </td>
                            </tr>
                        `).join('');
            } else {
                tbody.innerHTML = `
                            <tr>
                                <td colspan="6" class="text-center">暂无修改历史</td>
                            </tr>
                        `;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('获取历史记录失败', 'error');
        });
}

// 确认回滚
async function confirmRollback(historyId) {
    const result = await showConfirm(
        '确认回滚',
        '确定要回滚到此版本吗？此操作不可撤销，系统会自动备份当前版本。'
    );

    if (result) {
        rollbackConfig(historyId);
    }
}

// 执行回滚
async function rollbackConfig(historyId) {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/config/rollback/${historyId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const data = await response.json();

        if (data.message === '回滚成功') {
            showToast('回滚成功', 'success');
            // 重新加载历史记录和配置文件列表
            viewHistory(currentConfigFile);
            loadConfigFiles();
        } else {
            showToast(data.error || '回滚失败', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('回滚失败', 'error');
    }
}

// 获取当前版本ID
async function getCurrentVersionId() {
    try {
        const token = AuthUtil.getToken();
        const response = await fetch(`/api/hibiscus/config/current-version?filePath=${encodeURIComponent(currentConfigFile)}`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const data = await response.json();
        if (data.success) {
            return data.versionId;
        }
        showToast(data.error || '获取当前版本失败', 'error');
        return 1;
    } catch (error) {
        console.error('Error:', error);
        showToast('获取当前版本失败', 'error');
        return 1;
    }
}

// 对比配置
async function compareConfig(historyId) {
    const modal = document.getElementById('configCompareModal');
    modal.classList.add('show');
    initConfigEditors();

    try {
        const token = AuthUtil.getToken();
        const currentVersionId = await getCurrentVersionId();
        const response = await fetch(`/api/hibiscus/config/compare?historyId1=${historyId}&historyId2=${currentVersionId}`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const data = await response.json();

        // 直接使用返回的内容，不检查success字段
        if (data.content1 !== undefined && data.content2 !== undefined) {
            compareEditor1.setValue(data.content1);
            compareEditor2.setValue(data.content2);

            // 更新对比标题
            const version1Title = document.querySelector('#configCompareModal .compare-item:first-child h4');
            const version2Title = document.querySelector('#configCompareModal .compare-item:last-child h4');

            if (data.history1 && data.history2) {
                version1Title.textContent = `版本 ${data.history1.id} (${formatDateTime(data.history1.createTime)})`;
                version2Title.textContent = `版本 ${data.history2.id} (${formatDateTime(data.history2.createTime)})`;
            }
        } else {
            showToast(data.error || '获取对比内容失败', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('获取对比内容失败', 'error');
    }
}

// 关闭编辑模态框
function closeConfigEditModal() {
    document.getElementById('configEditModal').classList.remove('show');
}

// 关闭对比模态框
function closeConfigCompareModal() {
    document.getElementById('configCompareModal').classList.remove('show');
}


// 格式化日期时间
function formatDateTime(dateTime) {
    return new Date(dateTime).toLocaleString();
}

// 显示确认对话框
function showConfirm(title, message) {
    return new Promise((resolve) => {
        const dialog = document.createElement('div');
        dialog.className = 'modal';
        dialog.innerHTML = `
                    <div class="modal-content confirm-dialog">
                        <h3>${title}</h3>
                        <p>${message}</p>
                        <div class="button-group">
                            <button class="btn btn-secondary" onclick="closeConfirmDialog(false)">取消</button>
                            <button class="btn btn-primary" onclick="closeConfirmDialog(true)">确定</button>
                        </div>
                    </div>
                `;
        document.body.appendChild(dialog);
        dialog.classList.add('show');

        window.closeConfirmDialog = function (result) {
            dialog.classList.remove('show');
            setTimeout(() => {
                document.body.removeChild(dialog);
            }, 300);
            resolve(result);
        };
    });
}

// 性能监控相关变量和函数
let monitorTimer = null;
let threadChart = null;
let dbPoolChart = null;
let apiResponseChart = null;
let apiRequestChart = null;


// 更新线程监控数据
function updateThreadMetrics() {
    const button = document.querySelector('.monitor-section:nth-child(2) .btn-refresh');
    if (button) {
        button.classList.add('loading');
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/monitor/threads',{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            try {
                // 更新基本指标
                const activeThreads = document.getElementById('activeThreadCount');
                const daemonThreads = document.getElementById('daemonThreadCount');
                const peakThreads = document.getElementById('peakThreadCount');

                if (activeThreads) activeThreads.textContent = data?.activeThreads || '0';
                if (daemonThreads) daemonThreads.textContent = data?.daemonThreads || '0';
                if (peakThreads) peakThreads.textContent = data?.peakThreads || '0';

                // 更新图表
                if (threadChart && data?.threadHistory) {
                    const labels = data.threadHistory.map(item => {
                        const date = new Date(item.timestamp);
                        return date.toLocaleTimeString();
                    });
                    const values = data.threadHistory.map(item => item.threadCount);

                    threadChart.data.labels = labels;
                    threadChart.data.datasets[0].data = values;
                    threadChart.update();
                }
            } catch (error) {
                console.error('处理线程监控数据时出错:', error);
            }
        })
        .finally(() => {
            if (button) {
                button.classList.remove('loading');
            }
        });
}

// 更新数据库连接池监控数据
function updateDbPoolMetrics() {
    const button = document.querySelector('.monitor-section:nth-child(3) .btn-refresh');
    if (button) {
        button.classList.add('loading');
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/monitor/dbpool',{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            try {
                // 更新基本指标
                const activeConns = document.getElementById('activeConnections');
                const idleConns = document.getElementById('idleConnections');
                const totalConns = document.getElementById('totalConnections');
                const maxConns = document.getElementById('maxConnections');

                if (activeConns) activeConns.textContent = data?.activeConnections || '0';
                if (idleConns) idleConns.textContent = data?.idleConnections || '0';
                if (totalConns) totalConns.textContent = data?.totalConnections || '0';
                if (maxConns) maxConns.textContent = data?.maxConnections || '0';

                // 更新图表
                if (dbPoolChart && data?.connectionHistory) {
                    const labels = data.connectionHistory.map(item => {
                        const date = new Date(item.timestamp);
                        return date.toLocaleTimeString();
                    });
                    const activeValues = data.connectionHistory.map(item => item.activeCount || 0);
                    const totalValues = data.connectionHistory.map(item => (item.activeCount || 0) + (item.idleCount || 0));

                    dbPoolChart.data.labels = labels;
                    dbPoolChart.data.datasets[0].data = activeValues;
                    dbPoolChart.data.datasets[1].data = totalValues;
                    dbPoolChart.update();
                }
            } catch (error) {
                console.error('处理数据库连接池监控数据时出错:', error);
            }
        })
        .finally(() => {
            if (button) {
                button.classList.remove('loading');
            }
        });
}

// 更新API性能监控数据
function updateApiMetrics() {
    const button = document.querySelector('.monitor-section .btn-refresh');
    if (button) {
        button.classList.add('loading');
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/monitor/api-stats',{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            try {
                // 更新基本指标
                document.getElementById('totalRequests').textContent = data.totalRequests.toLocaleString();
                document.getElementById('avgResponseTime').textContent = data.avgResponseTime.toFixed(2) + 'ms';
                document.getElementById('errorRate').textContent = data.errorRate.toFixed(2) + '%';

                // 更新API调用历史表格
                const historyTableBody = document.getElementById('apiHistoryTable');
                historyTableBody.innerHTML = '';

                if (data.apiHistory && data.apiHistory.length > 0) {
                    data.apiHistory.forEach(record => {
                        const row = historyTableBody.insertRow();
                        const time = new Date(record.timestamp).toLocaleTimeString();
                        row.innerHTML = `
                            <td>${time}</td>
                            <td class="api-path">${record.path}</td>
                            <td class="response-time">${record.responseTime}ms</td>
                            <td class="${record.statusCode >= 400 ? 'text-danger' : 'text-success'}">${record.statusCode}</td>
                        `;
                    });
                } else {
                    const row = historyTableBody.insertRow();
                    row.innerHTML = '<td colspan="4" class="text-center">暂无数据</td>';
                }

                // 更新API路径统计表格
                const pathStatsTableBody = document.getElementById('apiPathStatsTable');
                pathStatsTableBody.innerHTML = '';

                if (data.pathStats && data.pathStats.length > 0) {
                    data.pathStats.forEach(stats => {
                        const row = pathStatsTableBody.insertRow();
                        row.innerHTML = `
                            <td class="api-path">${stats.path}</td>
                            <td>${stats.totalRequests.toLocaleString()}</td>
                            <td class="response-time">${stats.avgResponseTime.toFixed(2)}ms</td>
                            <td class="${stats.errorRate > 0 ? 'text-danger' : 'text-success'}">${stats.errorRate.toFixed(2)}%</td>
                        `;
                    });
                } else {
                    const row = pathStatsTableBody.insertRow();
                    row.innerHTML = '<td colspan="4" class="text-center">暂无数据</td>';
                }
            } catch (error) {
                console.error('处理API监控数据时出错:', error);
            }
        })
        .catch(error => {
            console.error('获取API监控数据失败:', error);
        })
        .finally(() => {
            if (button) {
                button.classList.remove('loading');
            }
        });
}

// 更新所有监控数据
function updateAllMetrics() {
    updateJvmMetrics();
    updateThreadMetrics();
    updateDbPoolMetrics();
    updateApiMetrics();
}

// 添加JVM监控相关函数
function updateJvmMetrics() {
    const button = document.querySelector('.monitor-section:first-child .btn-refresh');
    if (button) {
        button.classList.add('loading');
    }
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/monitor/jvm',{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            // 更新堆内存使用情况
            const heapUsed = (data.heapUsed / (1024 * 1024)).toFixed(2);
            const heapMax = (data.heapMax / (1024 * 1024)).toFixed(2);
            const heapUsage = data.heapUsage;

            const heapProgress = document.getElementById('heapMemoryProgress');
            const heapText = document.getElementById('heapMemoryText');

            if (heapProgress && heapText) {
                heapProgress.style.width = `${heapUsage}%`;
                heapProgress.className = `progress ${getMemoryUsageClass(heapUsage)}`;
                heapText.textContent = `${heapUsed}MB / ${heapMax}MB (${heapUsage}%)`;
            }

            // 更新非堆内存使用情况
            const nonHeapProgress = document.getElementById('nonHeapMemoryProgress');
            const nonHeapText = document.getElementById('nonHeapMemoryText');

            if (nonHeapProgress && nonHeapText) {
                nonHeapProgress.style.width = `${data.nonHeapUsage}%`;
                nonHeapProgress.className = `progress ${getMemoryUsageClass(data.nonHeapUsage)}`;
                nonHeapText.textContent = `${data.nonHeapUsage}%`;
            }

            // 更新GC次数
            const youngGcCount = document.getElementById('youngGcCount');
            const fullGcCount = document.getElementById('fullGcCount');

            if (youngGcCount) {
                youngGcCount.textContent = data.youngGcCount;
                youngGcCount.className = `metric-value ${getGCCountClass(data.youngGcCount)}`;
            }

            if (fullGcCount) {
                fullGcCount.textContent = data.fullGcCount;
                fullGcCount.className = `metric-value ${getGCCountClass(data.fullGcCount, true)}`;
            }
        })
        .catch(error => {
            console.error('获取JVM监控数据失败:', error);
        })
        .finally(() => {
            if (button) {
                button.classList.remove('loading');
            }
        });
}

// 根据内存使用率返回对应的样式类
function getMemoryUsageClass(usagePercent) {
    if (usagePercent >= 90) {
        return 'danger';
    } else if (usagePercent >= 70) {
        return 'warning';
    }
    return '';
}

// 根据GC次数返回对应的样式类
function getGCCountClass(count, isFullGC = false) {
    if (isFullGC) {
        return count > 0 ? 'danger' : '';
    }
    if (count >= 100) {
        return 'danger';
    } else if (count >= 50) {
        return 'warning';
    }
    return '';
}

// 修改缓存实例列表刷新函数
function refreshCacheInstances() {
    const token = AuthUtil.getToken();
    fetch('/api/hibiscus/cache/instances',{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {

            // 更新基本指标
            document.getElementById('totalCaches').textContent = data.totalInstances;

            // 计算总条目数
            let totalEntries = 0;
            Object.values(data.instances).forEach(cache => {
                totalEntries += cache.size || 0;
            });

            // 更新显示
            document.getElementById('totalEntries').textContent = totalEntries;
            document.getElementById('memoryUsage').textContent = formatBytes(data.totalMemoryUsage || 0);
            const tbody = document.getElementById('cacheInstancesList');
            tbody.innerHTML = '';

            Object.entries(data.instances).forEach(([name, cache]) => {
                const tr = document.createElement('tr');
                const config = cache.config;

                tr.innerHTML = `
                    <td>${name}</td>
                    <td>${cache.size || 0}</td>
                    <td>
                        <div class="stats-group">
                            <span class="stat-item">最大容量: ${config.maxSize}</span>
                            <span class="stat-item">驱逐策略: ${config.evictionPolicy}</span>
                            <span class="stat-item">默认TTL: ${formatTTL(config.defaultTTL)}</span>
                        </div>
                    </td>
                    <td>${config.compressionEnabled ? '是' : '否'}</td>
                    <td>${config.metricsEnabled ? '是' : '否'}</td>
                    <td class="action-buttons">
                        <button class="btn-icon" onclick="viewCacheDetails('${name}')" title="查看详情">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                            </svg>
                        </button>
                        <button class="btn-icon" onclick="clearCache('${name}')" title="清空缓存">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                            </svg>
                        </button>
                        <button class="btn-icon" onclick="configureCache('${name}')" title="配置缓存">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
                            </svg>
                        </button>
                        <button class="btn-icon" onclick="deleteCache('${name}')" title="删除缓存">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M18 6L6 18M6 6l12 12"/>
                            </svg>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => {
            console.error('Error fetching cache instances:', error);
            showToast('获取缓存实例列表失败', 'error');
        });
}

// 添加TTL格式化函数
function formatTTL(milliseconds) {
    if (!milliseconds || milliseconds <= 0) return '永不过期';
    const seconds = milliseconds / 1000;
    if (seconds < 60) return `${seconds}秒`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}小时`;
    return `${Math.floor(seconds / 86400)}天`;
}

// 添加缓存配置函数
function configureCache(cacheName) {
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/cache/instances/${cacheName}/config`,{
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(config => {
            showConfigureModal(cacheName, config);
        })
        .catch(error => {
            console.error('Error fetching cache config:', error);
            showToast('获取缓存配置失败', 'error');
        });
}

// 修改显示缓存配置模态框函数
function showConfigureModal(cacheName, config) {
    const modal = document.getElementById('updateConfigModal'); // 改用正确的模态框ID
    if (!modal) return;

    const form = document.getElementById('updateConfigForm');
    if (!form) return;

    // 更新缓存名称
    document.getElementById('updateCacheName').textContent = cacheName;

    // 填充表单
    document.getElementById('updateMaxSize').value = config.maxSize || '';
    document.getElementById('updateTTL').value = config.defaultTTL || '';

    // 显示模态框
    modal.classList.add('show');
}

// 更新缓存配置
function updateCacheConfig(cacheName, config) {
    const params = new URLSearchParams({
        maxSize: config.maxSize,
        ttlSeconds: config.expireAfterWrite
    });
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/cache/instances/${cacheName}/config?${params.toString()}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.text())
        .then(result => {
            if (result.includes('Error')) {
                showToast(result, 'error');
            } else {
                showToast('缓存配置更新成功', 'success');
                document.getElementById('configureCacheModal').classList.remove('show');
                refreshCacheInstances();
            }
        })
        .catch(error => {
            console.error('Error updating cache config:', error);
            showToast('更新缓存配置失败', 'error');
        });
}

// 增强缓存详情查看函数
function viewCacheDetails(cacheName) {
    const token = AuthUtil.getToken();
    Promise.all([
        fetch(`/api/hibiscus/cache/instances/${cacheName}/config`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        }).then(r => r.json()),
        fetch(`/api/hibiscus/cache/instances/${cacheName}/keys`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        }).then(r => r.json())
    ])
        .then(([config, keysData]) => {
            const modal = document.getElementById('cacheDetailsModal');

            // 更新基本信息
            document.getElementById('modalCacheName').textContent = cacheName;
            document.getElementById('modalMaxSize').textContent = config.maxSize;
            document.getElementById('modalEvictionPolicy').textContent = config.evictionPolicy;
            document.getElementById('modalTotalRequests').textContent = keysData.size;

            // 添加操作按钮
            const actionButtons = document.getElementById('cacheDetailActions');
            if (actionButtons) {
                actionButtons.innerHTML = `
                <button class="btn btn-primary" onclick="addCacheEntry('${cacheName}')">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 4v16m8-8H4"/>
                    </svg>
                    添加条目
                </button>
            `;
            }

            // 更新缓存条目列表
            const tbody = document.getElementById('cacheContentList');
            tbody.innerHTML = '';

            Object.entries(keysData.entries).forEach(([key, value]) => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td>${key}</td>
                <td>${formatCacheValue(value)}</td>
                <td>${config.defaultTTL ? `${config.defaultTTL}秒` : '永不过期'}</td>
                <td>
                    <button class="btn-icon" onclick="removeEntry('${cacheName}', '${key}')" title="删除条目">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M18 6L6 18M6 6l12 12"/>
                        </svg>
                    </button>
                </td>
            `;
                tbody.appendChild(tr);
            });

            modal.classList.add('show');
        })
        .catch(error => {
            console.error('Error fetching cache details:', error);
            showToast('获取缓存详情失败', 'error');
        });
}

// 格式化缓存值显示
function formatCacheValue(value) {
    if (typeof value === 'object') {
        return JSON.stringify(value);
    }
    return String(value);
}

// 格式化过期时间显示
function formatExpiration(expireTime) {
    if (!expireTime) return '永不过期';
    const remaining = new Date(expireTime) - new Date();
    if (remaining <= 0) return '已过期';
    return `${Math.ceil(remaining / 1000)}秒后过期`;
}

// 删除缓存条目
function removeEntry(cacheName, key) {
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/cache/instances/${cacheName}/entries/${encodeURIComponent(key)}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            console.log(data)
            if (data.success) {
                showToast('缓存条目删除成功', 'success');
                viewCacheDetails(cacheName); // 刷新详情视图
            } else {
                showToast(data.message || '删除失败', 'error');
            }
        })
        .catch(error => {
            console.error('Error removing cache entry:', error);
            showToast('删除缓存条目失败', 'error');
        });
}

// 添加清空缓存函数
function clearCache(cacheName) {
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/cache/instances/${cacheName}/clear`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.text())
        .then(result => {
            showToast(result, result.includes('Error') ? 'error' : 'success');
            refreshCacheInstances();
        })
        .catch(error => {
            console.error('Error clearing cache:', error);
            showToast('清空缓存失败', 'error');
        });
}

// 添加删除缓存实例函数
function deleteCache(cacheName) {
    const token = AuthUtil.getToken();
    fetch(`/api/hibiscus/cache/instances/${cacheName}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.text())
        .then(result => {
            showToast(result, result.includes('Error') ? 'error' : 'success');
            refreshCacheInstances();
            // refreshCacheOverview();
        })
        .catch(error => {
            console.error('Error deleting cache:', error);
            showToast('删除缓存失败', 'error');
        });
}

// 添加 formatBytes 函数
function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

// 修改模态框关闭相关的代码
function closeModal(modalId) {
    const modal = document.getElementById(modalId || 'dataModal');
    if (modal) {
        modal.classList.remove('show');
    }
}

// 修改事件监听器初始化
document.addEventListener('DOMContentLoaded', function () {
    // 初始化模态框关闭事件
    initModalCloseHandlers();

    // 初始化缓存搜索功能
    initCacheSearch();

    // 初始加载缓存数据
    // refreshCacheOverview();
    refreshCacheInstances();

    // 设置定时刷新（每30秒刷新一次）
    setInterval(() => {
        // refreshCacheOverview();
        refreshCacheInstances();
    }, 30000);
});

// 修改缓存搜索初始化函数
function initCacheSearch() {
    const searchInput = document.getElementById('instanceSearch');
    if (!searchInput) return;

    // 添加回车键搜索支持
    searchInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            searchCaches();
        }
    });
}

// 添加搜索执行函数
function searchCaches() {
    const searchInput = document.getElementById('instanceSearch');
    const searchText = searchInput.value.toLowerCase().trim();
    const tbody = document.getElementById('cacheInstancesList');
    const rows = tbody.getElementsByTagName('tr');

    if (!searchText) {
        // 如果搜索框为空，显示所有行
        Array.from(rows).forEach(row => {
            row.style.display = '';
        });
        document.getElementById('totalCaches').textContent = rows.length;
        return;
    }

    // 执行搜索
    let visibleCount = 0;
    Array.from(rows).forEach(row => {
        const name = row.cells[0].textContent.toLowerCase();
        const configInfo = row.cells[2].textContent.toLowerCase();
        const shouldShow = name.includes(searchText) || configInfo.includes(searchText);
        row.style.display = shouldShow ? '' : 'none';
        if (shouldShow) visibleCount++;
    });

    // 更新显示的总数
    document.getElementById('totalCaches').textContent = visibleCount;

    // 显示搜索结果提示
    showToast(`找到 ${visibleCount} 个匹配的缓存实例`, 'info');
}

// 添加模态框关闭事件处理初始化函数
function initModalCloseHandlers() {
    // 为所有模态框的关闭按钮添加事件监听
    document.querySelectorAll('.modal .close, .modal .modal-close').forEach(closeBtn => {
        closeBtn.onclick = function (e) {
            e.preventDefault();
            e.stopPropagation();
            const modal = this.closest('.modal');
            if (modal) {
                modal.classList.remove('show');
            }
        };
    });

    // 点击模态框外部关闭
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', function (event) {
            if (event.target === this) {
                this.classList.remove('show');
            }
        });
    });

    // 为所有取消按钮添加事件监听
    document.querySelectorAll('.modal .btn-secondary[onclick*="closeModal"]').forEach(btn => {
        btn.onclick = function (e) {
            e.preventDefault();
            const modal = this.closest('.modal');
            if (modal) {
                modal.classList.remove('show');
            }
        };
    });
}


// 修改缓存内容搜索初始化函数
function initCacheContentSearch() {
    const searchInput = document.getElementById('instanceSearch');
    if (searchInput) {
        let searchTimeout;

        searchInput.addEventListener('input', function (e) {
            // 清除之前的定时器
            if (searchTimeout) {
                clearTimeout(searchTimeout);
            }

            // 设置新的定时器，延迟300ms执行搜索
            searchTimeout = setTimeout(() => {
                const keyword = e.target.value.trim();

                // 如果搜索框为空，刷新所有实例
                if (!keyword) {
                    refreshCacheInstances();
                    return;
                }
                const token = AuthUtil.getToken();
                // 调用搜索API
                fetch(`/api/hibiscus/cache/instances/search?keyword=${encodeURIComponent(keyword)}`,{
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                })
                    .then(response => response.json())
                    .then(data => {
                        // 更新缓存实例列表
                        const tbody = document.getElementById('cacheInstancesList');
                        tbody.innerHTML = '';

                        Object.entries(data.instances).forEach(([name, cache]) => {
                            const tr = document.createElement('tr');
                            const config = cache.config;

                            tr.innerHTML = `
                                <td>${name}</td>
                                <td>${cache.size || 0}</td>
                                <td>
                                    <div class="stats-group">
                                        <span class="stat-item">最大容量: ${config.maxSize}</span>
                                        <span class="stat-item">驱逐策略: ${config.evictionPolicy}</span>
                                        <span class="stat-item">默认TTL: ${formatTTL(config.defaultTTL)}</span>
                                    </div>
                                </td>
                                <td>${config.compressionEnabled ? '是' : '否'}</td>
                                <td>${config.metricsEnabled ? '是' : '否'}</td>
                                <td class="action-buttons">
                                    <button class="btn-icon" onclick="viewCacheDetails('${name}')" title="查看详情">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                            <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                                        </svg>
                                    </button>
                                    <button class="btn-icon" onclick="clearCache('${name}')" title="清空缓存">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                                        </svg>
                                    </button>
                                    <button class="btn-icon" onclick="configureCache('${name}')" title="配置缓存">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
                                        </svg>
                                    </button>
                                    <button class="btn-icon" onclick="deleteCache('${name}')" title="删除缓存">
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M18 6L6 18M6 6l12 12"/>
                                        </svg>
                                    </button>
                                </td>
                            `;
                            tbody.appendChild(tr);
                        });

                        // 更新概览数据
                        document.getElementById('totalCaches').textContent = data.totalInstances;
                        document.getElementById('memoryUsage').textContent = formatBytes(data.totalMemoryUsage || 0);
                    })
                    .catch(error => {
                        console.error('Error searching caches:', error);
                        showToast('搜索缓存实例失败', 'error');
                    });
            }, 300); // 300ms的防抖延迟
        });
    }
}

// // 添加缓存内容搜索初始化函数
// function initCacheContentSearch() {
//     const searchInput = document.getElementById('cacheContentSearch');
//     if (searchInput) {
//         searchInput.addEventListener('input', function(e) {
//             const searchText = e.target.value.toLowerCase();
//             const rows = document.getElementById('cacheContentList').getElementsByTagName('tr');
//
//             Array.from(rows).forEach(row => {
//                 const key = row.cells[0].textContent.toLowerCase();
//                 const value = row.cells[1].textContent.toLowerCase();
//                 row.style.display = key.includes(searchText) || value.includes(searchText) ? '' : 'none';
//             });
//         });
//     }
// }

// 修改通用的模态框关闭函数
function closeModalById(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
        // 如果模态框中有表单，重置表单
        const form = modal.querySelector('form');
        if (form) {
            form.reset();
        }
    }
}

function closePromptDialog(result) {
    const dialog = document.getElementById('promptDialog');
    dialog.classList.remove('show');
    if (typeof dialog.resolve === 'function') {
        dialog.resolve(result ? dialog.querySelector('#promptInput').value : null);
    }
}

function closeAlertDialog() {
    closeModalById('alertDialog');
    if (typeof window.alertResolve === 'function') {
        window.alertResolve();
    }
}

// 创建新缓存函数
function createNewCache() {
    const modal = document.getElementById('createCacheModal');
    if (!modal) return;

    // 重置表单
    const form = document.getElementById('createCacheForm');
    if (form) {
        form.reset();
    }

    // 添加表单提交处理
    form.onsubmit = function (e) {
        e.preventDefault();
        const name = document.getElementById('newCacheName').value;
        const maxSize = document.getElementById('newCacheMaxSize').value;
        const ttlSeconds = document.getElementById('newCacheExpiration').value;

        const params = new URLSearchParams({
            name: name,
            maxSize: maxSize || 1000,
            ttlSeconds: ttlSeconds || 3600
        });
        const token = AuthUtil.getToken();
        fetch(`/api/hibiscus/cache/instances?${params.toString()}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.text())
            .then(result => {
                if (result.includes('Error')) {
                    showToast(result, 'error');
                } else {
                    showToast('缓存创建成功', 'success');
                    modal.classList.remove('show');
                    refreshCacheInstances();
                    // refreshCacheOverview();
                }
            })
            .catch(error => {
                console.error('Error creating cache:', error);
                showToast('创建缓存失败', 'error');
            });
    };

    modal.classList.add('show');
}

// 修改添加缓存条目函数
function addCacheEntry(cacheName) {
    const modal = document.getElementById('addEntryModal');
    const form = document.getElementById('addEntryForm');

    form.onsubmit = function (e) {
        e.preventDefault();
        const key = document.getElementById('entryKey').value;
        const value = document.getElementById('entryValue').value;

        const params = new URLSearchParams({
            key: key,
            value: value
        });
        const token = AuthUtil.getToken();
        fetch(`/api/hibiscus/cache/instances/${cacheName}/put?${params.toString()}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.text())
            .then(result => {
                if (result.includes('Error')) {
                    showToast(result, 'error');
                } else {
                    showToast('添加成功', 'success');
                    closeModalById('addEntryModal');
                    viewCacheDetails(cacheName); // 刷新详情视图
                }
            })
            .catch(error => {
                console.error('Error adding cache entry:', error);
                showToast('添加失败', 'error');
            });
    };

    // 重置表单并显示模态框
    form.reset();
    modal.classList.add('show');
}

// SSH终端相关代码
let sshTerminal = null;
let sshWebSocket = null;
let commandBuffer = '';

function toggleSSHTerminal() {
    const modal = document.getElementById('sshTerminalModal');
    console.log(modal.classList)
    if (modal.classList.contains('show')) {
        modal.classList.remove('show');
    } else {
        modal.classList.add('show');
        initSSHTerminal();
    }
}

function initSSHTerminal() {
    if (!sshTerminal) {
        sshTerminal = new Terminal({
            cursorBlink: true,
            theme: {
                background: '#F8FFFE',
                foreground: '#2A4B4A',
                cursor: '#00B8A9',
                cursorAccent: '#1e1e1e',
                selection: 'rgba(255, 255, 255, 0.3)',
                black: '#2A4B4A',
                red: '#FF6B6B',
                green: '#4ECDC4',
                yellow: '#FFE66D',
                blue: '#45B7D1',
                magenta: '#96CEB4',
                cyan: '#00B8A9',
                white: '#d0d0d0',
                brightBlack: '#666666',
                brightRed: '#FF8787',
                brightGreen: '#65E5D7',
                brightYellow: '#FFF3A3',
                brightBlue: '#7CD5E6',
                brightMagenta: '#B5E9E4',
                brightCyan: '#00D1BF',
                brightWhite: '#ffffff'
            },
            fontSize: 14,
            fontFamily: 'Consolas, monospace',
            scrollback: 1000,
            smoothScrollDuration: 300,
            rendererType: 'canvas',
            allowProposedApi: true,
            windowsMode: true,
            convertEol: true,
            termName: 'xterm-256color',
            rows: 24,
            cols: 80
        });

        // 创建fit插件实例
        const fitAddon = new FitAddon.FitAddon();

        // 在打开终端之前加载插件
        sshTerminal.loadAddon(fitAddon);

        // 打开终端
        sshTerminal.open(document.getElementById('terminal'));

        // 确保终端已经完全加载
        sshTerminal.onRender(() => {
            // 在第一次渲染后执行fit
            if (document.getElementById('terminal').offsetHeight > 0) {
                try {
                    fitAddon.fit();
                } catch (e) {
                    console.warn('Initial fit failed, will retry');
                }
            }
        });

        // 添加窗口大小改变事件监听
        let resizeTimeout;
        const handleResize = () => {
            if (resizeTimeout) {
                clearTimeout(resizeTimeout);
            }
            resizeTimeout = setTimeout(() => {
                const terminalElement = document.getElementById('terminal');
                if (terminalElement && terminalElement.offsetHeight > 0) {
                    try {
                        fitAddon.fit();
                        if (sshWebSocket && sshWebSocket.readyState === WebSocket.OPEN) {
                            const dimensions = fitAddon.proposeDimensions();
                            if (dimensions && dimensions.cols > 0 && dimensions.rows > 0) {
                                sshWebSocket.send(JSON.stringify({
                                    type: 'resize',
                                    cols: dimensions.cols,
                                    rows: dimensions.rows
                                }));
                            }
                        }
                    } catch (e) {
                        console.warn('Resize fit failed:', e);
                    }
                }
            }, 100);
        };

        window.addEventListener('resize', handleResize);

        let commandHistory = [];
        let historyIndex = -1;
        let currentPosition = 0;
        let commandBuffer = '';

        sshTerminal.onData(data => {
            if (sshWebSocket && sshWebSocket.readyState === WebSocket.OPEN) {
                // 处理上下键
                if (data === '\x1b[A') { // 上键
                    if (historyIndex < commandHistory.length - 1) {
                        // 清除当前行
                        while (currentPosition > 0) {
                            sshTerminal.write('\b \b');
                            currentPosition--;
                        }
                        // 显示历史命令
                        historyIndex++;
                        commandBuffer = commandHistory[commandHistory.length - 1 - historyIndex];
                        currentPosition = commandBuffer.length;
                        sshTerminal.write(commandBuffer);
                    }
                    return;
                }
                if (data === '\x1b[B') { // 下键
                    // 清除当前行
                    while (currentPosition > 0) {
                        sshTerminal.write('\b \b');
                        currentPosition--;
                    }
                    if (historyIndex > 0) {
                        // 显示较新的历史命令
                        historyIndex--;
                        commandBuffer = commandHistory[commandHistory.length - 1 - historyIndex];
                        currentPosition = commandBuffer.length;
                        sshTerminal.write(commandBuffer);
                    } else if (historyIndex === 0) {
                        // 回到空命令行
                        historyIndex = -1;
                        commandBuffer = '';
                        currentPosition = 0;
                    }
                    return;
                }

                // 处理退格键
                if (data === '\x7f') {
                    if (currentPosition > 0) {
                        commandBuffer = commandBuffer.slice(0, -1);
                        currentPosition--;
                        sshTerminal.write('\b \b');
                    }
                    return;
                }

                // 处理回车键
                if (data === '\r' || data === '\n') {
                    sshTerminal.write('\r\n');
                    if (commandBuffer.trim() !== '') {
                        // 添加到历史记录
                        commandHistory.unshift(commandBuffer);
                        // 限制历史记录数量
                        if (commandHistory.length > 100) {
                            commandHistory.pop();
                        }
                        historyIndex = -1;
                    }
                    sshWebSocket.send(JSON.stringify({
                        type: 'command',
                        command: commandBuffer + '\n'
                    }));
                    commandBuffer = '';
                    currentPosition = 0;
                    return;
                }

                // 其他字符添加到缓冲区
                commandBuffer = commandBuffer.slice(0, currentPosition) + data + commandBuffer.slice(currentPosition);
                currentPosition += data.length;
                sshTerminal.write(data);
            }
        });
    }
}

function connectSSH() {
    if (sshWebSocket) {
        sshWebSocket.close();
    }

    const host = document.getElementById('sshHost').value;
    const port = parseInt(document.getElementById('sshPort').value);
    const username = document.getElementById('sshUsername').value;
    const password = document.getElementById('sshPassword').value;

    if (!host || !port || !username || !password) {
        showToast('请填写完整的连接信息', 'error');
        return;
    }

    sshWebSocket = new WebSocket(`ws://${window.location.host}/webssh`);
    //
    // sshWebSocket.onopen = () => {
    //     sshWebSocket.send(JSON.stringify({
    //         type: 'connect',
    //         host,
    //         port,
    //         username,
    //         password
    //     }));
    //     commandBuffer = '';  // 重置命令缓冲区
    // };
    sshWebSocket.onopen = () => {
        sshWebSocket.send(JSON.stringify({
            type: 'connect',
            host,
            port,
            username,
            password
        }));
        commandBuffer = '';  // 重置命令缓冲区
        commandHistory = []; // 重置历史记录
        historyIndex = -1;   // 重置历史索引
    };


    sshWebSocket.onmessage = (event) => {
        sshTerminal.write(event.data);  // 只依赖服务器的回显
    };

    sshWebSocket.onclose = () => {
        sshTerminal.write('\r\n连接已关闭\r\n');
        showToast('SSH连接已断开', 'warning');
        commandBuffer = '';  // 清空命令缓冲区
        commandHistory = []; // 清空历史记录
        historyIndex = -1;   // 重置历史索引
    };

    sshWebSocket.onerror = () => {
        showToast('SSH连接失败', 'error');
        commandBuffer = '';  // 清空命令缓冲区
        commandHistory = []; // 清空历史记录
        historyIndex = -1;   // 重置历史索引
    };
    // sshWebSocket.onclose = () => {
    //     sshTerminal.write('\r\n连接已关闭\r\n');
    //     showToast('SSH连接已断开', 'warning');
    //     commandBuffer = '';  // 清空命令缓冲区
    // };
    //
    // sshWebSocket.onerror = () => {
    //     showToast('SSH连接失败', 'error');
    //     commandBuffer = '';  // 清空命令缓冲区
    // };
}

// 在页面关闭时清理连接
window.addEventListener('beforeunload', () => {
    if (sshWebSocket) {
        sshWebSocket.close();
    }
});