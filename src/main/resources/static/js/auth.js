// 认证工具类
const AuthUtil = {
    // 存储token
    setToken(token) {
        localStorage.setItem('token', token);
        // 更新axios默认请求头
        this.setupAxiosDefaults();
    },

    // 获取token
    getToken() {
        return localStorage.getItem('token');
    },

    // 移除token
    removeToken() {
        localStorage.removeItem('token');
        // 清除axios默认请求头
        delete axios.defaults.headers.common['Authorization'];
    },

    // 设置axios默认配置
    setupAxiosDefaults() {
        const token = this.getToken();
        if (token) {
            axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        }
    },
    // 跳转到指定页面
    redirectTo(url) {
        const token = this.getToken();
        if (token) {
            // 如果URL已经包含参数，使用&，否则使用?
            const separator = url.includes('?') ? '&' : '?';
            url = `${url}${separator}token=${token}`;
        }
        window.location.href = url;
    }
};
// 初始化时设置axios默认配置
AuthUtil.setupAxiosDefaults();