document.addEventListener('DOMContentLoaded', function () {
    const logs = [];
    const operationLogs = [];
    let autoRefreshInterval = null;

    const logContainer = document.getElementById('logContainer');
    const operationLogContainer = document.getElementById('operationLogContainer');
    const logLevel = document.getElementById('logLevel');
    const keyword = document.getElementById('keyword');
    const autoRefreshBtn = document.getElementById('autoRefreshBtn');

    document.getElementById('fetchLogsBtn').addEventListener('click', fetchLogs);
    document.getElementById('archiveLogsBtn').addEventListener('click', archiveLogs);
    document.getElementById('compressLogsBtn').addEventListener('click', compressLogs);
    document.getElementById('exportLogsBtn').addEventListener('click', handleExport);
    autoRefreshBtn.addEventListener('click', toggleAutoRefresh);

    function addOperationLog(message) {
        const timestamp = new Date().toISOString();
        operationLogs.unshift({ timestamp, message }); // 新日志添加到顶部
        if (operationLogs.length > 100) { // 限制操作日志数量
            operationLogs.pop();
        }
        renderOperationLogs();
    }

    function renderOperationLogs() {
        operationLogContainer.innerHTML = operationLogs.map(opLog => `
                <div class="p-2 border-b border-gray-200">
                    <strong>${new Date(opLog.timestamp).toLocaleString()}</strong> - 
                    <span class="font-semibold">${opLog.message}</span>
                </div>
            `).join('');
    }

    function renderLogs() {
        const selectedLevel = logLevel.value;
        const keywordFilter = keyword.value.toLowerCase();

        const filteredLogs = logs.filter(log => {
            const matchesLevel = !selectedLevel || log.level === selectedLevel;
            const matchesKeyword = !keywordFilter ||
                log.message.toLowerCase().includes(keywordFilter) ||
                log.logger.toLowerCase().includes(keywordFilter);
            return matchesLevel && matchesKeyword;
        });

        logContainer.innerHTML = filteredLogs.map(log => `
                <div class="log-entry ${log.level.toLowerCase()}-log">
                    <strong>${log.timestamp}</strong> [${log.thread}] ${log.level} ${log.logger} - ${log.message}
                </div>
            `).join('');

        // 自动滚动到底部
        logContainer.scrollTop = logContainer.scrollHeight;
    }

    function fetchLogs() {
        const level = logLevel.value;
        const searchKeyword = keyword.value;
        const start = document.getElementById('startDate').value;
        const end = document.getElementById('endDate').value;
        const token = AuthUtil.getToken();
        fetch(`/api/hibiscus/logs/api/logs?level=${encodeURIComponent(level)}&keyword=${encodeURIComponent(searchKeyword)}&startDate=${start}&endDate=${end}`,{
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.json())
            .then(data => {
                logs.length = 0;
                logs.push(...data);
                renderLogs();
                addOperationLog("Fetched logs successfully.");
            })
            .catch(error => {
                console.error('Error fetching logs:', error);
                addOperationLog("Failed to fetch logs: " + error.message);
            });
    }

    function archiveLogs() {
        const token = AuthUtil.getToken();
        fetch('/api/hibiscus/logs/api/logs/archive', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => {
                if (response.ok) {
                    addOperationLog("Logs have been archived successfully.");
                    fetchLogs(); // 刷新日志显示
                } else {
                    throw new Error('Failed to archive logs');
                }
            })
            .catch(error => {
                console.error('Error archiving logs:', error);
                addOperationLog("Failed to archive logs: " + error.message);
            });
    }

    function compressLogs() {
        const compressBtn = document.getElementById('compressLogsBtn');
        compressBtn.disabled = true;
        compressBtn.textContent = 'Compressing...';
        const token = AuthUtil.getToken();
        fetch('/api/hibiscus/logs/api/logs/compress', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(text || 'Failed to compress logs');
                    });
                }
                addOperationLog("Logs have been compressed successfully.");
                fetchLogs(); // 刷新日志显示
            })
            .catch(error => {
                console.error('Error compressing logs:', error);
                addOperationLog("Failed to compress logs: " + error.message);
                // 显示用户友好的错误消息
                const errorMessage = error.message.includes('另一个程序正在使用此文件')
                    ? "Log files are currently in use. Please try again later."
                    : error.message;
                logContainer.innerHTML += `
                        <div class="log-entry error-log">
                            <strong>${new Date().toISOString()}</strong> ERROR: ${errorMessage}
                        </div>
                    `;
            })
            .finally(() => {
                compressBtn.disabled = false;
                compressBtn.textContent = 'Compress';
            });
    }

    function handleExport() {
        redirectTo('/api/hibiscus/logs/api/logs/export')
        addOperationLog("Started logs export.");
    }

    function toggleAutoRefresh() {
        if (autoRefreshInterval) {
            clearInterval(autoRefreshInterval);
            autoRefreshInterval = null;
            autoRefreshBtn.textContent = 'Auto Refresh';
            autoRefreshBtn.classList.remove('bg-red-500', 'hover:bg-red-600');
            autoRefreshBtn.classList.add('bg-purple-500', 'hover:bg-purple-600');
            addOperationLog("Auto refresh disabled.");
        } else {
            autoRefreshInterval = setInterval(fetchLogs, 5000); // 每5秒刷新一次
            autoRefreshBtn.textContent = 'Stop Refresh';
            autoRefreshBtn.classList.remove('bg-purple-500', 'hover:bg-purple-600');
            autoRefreshBtn.classList.add('bg-red-500', 'hover:bg-red-600');
            addOperationLog("Auto refresh enabled (5s interval).");
        }
    }

    // 监听过滤器变化
    logLevel.addEventListener('change', renderLogs);
    keyword.addEventListener('input', renderLogs);

    // 初始加载
    fetchLogs();

    function initializeVisualizations() {
        // 时间轴视图
        const timeline = new Timeline({
            container: 'timelineContainer',
            data: logs,
            onClick: (event) => highlightRelatedLogs(event)
        });

        // 错误分布热图
        const heatmap = new ErrorHeatmap({
            container: 'heatmapContainer',
            data: logs.filter(log => log.level === 'ERROR')
        });

        // 实时监控面板
        const monitor = new LiveMonitor({
            container: 'monitorContainer',
            metrics: ['errors', 'warnings', 'api-calls']
        });
    }

    function highlightRelatedLogs(event) {
        const timeWindow = 5 * 60 * 1000; // 5分钟窗口
        const relatedLogs = findRelatedLogs(event, timeWindow);
        displayLogContext(relatedLogs);
    }

    function displayLogContext(relatedLogs) {
        const contextView = document.getElementById('contextView');
        contextView.innerHTML = `
                <div class="context-timeline">
                    ${generateTimelineHTML(relatedLogs)}
                </div>
                <div class="context-details">
                    ${generateContextDetails(relatedLogs)}
                </div>
            `;
    }

    // 初始化日期范围
    function initializeDateRange() {
        const token = AuthUtil.getToken();
        fetch('/api/hibiscus/logs/api/date-range',{
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.json())
            .then(range => {
                const startDate = document.getElementById('startDate');
                const endDate = document.getElementById('endDate');

                startDate.value = range.earliest;
                endDate.value = range.latest;

                // 设置最大和最小日期
                startDate.max = range.latest;
                endDate.max = range.latest;
                startDate.min = range.earliest;
                endDate.min = range.earliest;
            });
    }

    // 初始化
    initializeDateRange();
});