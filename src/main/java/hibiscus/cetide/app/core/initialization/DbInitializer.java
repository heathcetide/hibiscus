package hibiscus.cetide.app.core.initialization;

import hibiscus.cetide.app.basic.log.core.LogLevel;
import hibiscus.cetide.app.basic.log.core.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Component
public class DbInitializer {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Logger logger;

    @Autowired
    private DefaultDataInserter defaultDataInserter;

    public void initializeDatabase() {
        if (dataSource == null) {
            System.out.println("Hibiscus:   DataSource is null. Cannot initialize database.");
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        try {
            // 创建审计表
            createAuditTableIfNotExists(jdbcTemplate);

            createIndexIfNotExists(jdbcTemplate);
            // 创建 user 表
            String createTableSql = "CREATE TABLE IF NOT EXISTS user (" +
                    "id BIGINT AUTO_INCREMENT COMMENT '用户ID' PRIMARY KEY," +
                    "userAccount VARCHAR(256) NOT NULL COMMENT '账号'," +
                    "userPassword VARCHAR(512) NOT NULL COMMENT '密码（哈希）'," +
                    "salt VARCHAR(64) NOT NULL DEFAULT '' COMMENT '密码盐'," +
                    "unionId VARCHAR(256) NULL COMMENT '微信开放平台ID'," +
                    "mpOpenId VARCHAR(256) NULL COMMENT '公众号OpenID'," +
                    "userName VARCHAR(256) NULL COMMENT '用户昵称'," +
                    "userAvatar VARCHAR(1024) NULL COMMENT '用户头像'," +
                    "userProfile VARCHAR(512) NULL COMMENT '用户简介'," +
                    "userRole VARCHAR(256) DEFAULT 'user' NOT NULL COMMENT '用户角色：user/admin/ban'," +
                    "createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间'," +
                    "updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "isDelete TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除'," +
                    "email VARCHAR(256) NULL COMMENT '用户邮箱'," +
                    "phone VARCHAR(20) NULL COMMENT '用户手机号'," +
                    "isVerified TINYINT DEFAULT 0 NOT NULL COMMENT '邮箱/手机号是否验证'," +
                    "lastLogin DATETIME NULL COMMENT '最后登录时间'," +
                    "failedLoginAttempts INT DEFAULT 0 COMMENT '连续失败登录次数'," +
                    "accountLocked TINYINT DEFAULT 0 COMMENT '账户是否被锁定'," +
                    "privacySettings JSON NULL COMMENT '用户隐私设置'," +
                    "preferences JSON NULL COMMENT '用户偏好设置'," +
                    "twoFactorEnabled TINYINT DEFAULT 0 COMMENT '是否启用双因素认证'," +
                    "lastPasswordChange DATETIME NULL COMMENT '最后密码更改时间'" +
                    ") COMMENT '用户表' ENGINE=InnoDB COLLATE=utf8mb4_unicode_ci";

            jdbcTemplate.execute(createTableSql);
            logSchemaChange("Created table 'user'.");
            // 检查索引是否存在
            String checkIndexExistsSql = "SELECT COUNT(*) AS count FROM information_schema.statistics WHERE table_name = ? AND index_name = ?";
            Integer indexCount = jdbcTemplate.queryForObject(checkIndexExistsSql, Integer.class, "user", "idx_unionId");

            if (indexCount == 0) {
                // 创建 user 表索引
                String createIndexSql = "CREATE INDEX idx_unionId ON user (unionId)";
                jdbcTemplate.execute(createIndexSql);
            }

            checkIndexExistsSql = "SELECT COUNT(*) AS count FROM information_schema.statistics WHERE table_name = ? AND index_name = ?";
            indexCount = jdbcTemplate.queryForObject(checkIndexExistsSql, Integer.class, "user", "idx_email");

            if (indexCount == 0) {
                String createIndexSql = "CREATE INDEX idx_email ON user (email)";
                jdbcTemplate.execute(createIndexSql);
            }

            checkIndexExistsSql = "SELECT COUNT(*) AS count FROM information_schema.statistics WHERE table_name = ? AND index_name = ?";
            indexCount = jdbcTemplate.queryForObject(checkIndexExistsSql, Integer.class, "user", "idx_phone");

            if (indexCount == 0) {
                String createIndexSql = "CREATE INDEX idx_phone ON user (phone)";
                jdbcTemplate.execute(createIndexSql);
            }

            // 创建 admin 表
            String createAdminTableSql = "CREATE TABLE IF NOT EXISTS admin (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID'," +
                    "adminAccount VARCHAR(256) NOT NULL UNIQUE COMMENT '管理员账号'," +
                    "adminPassword VARCHAR(512) NOT NULL COMMENT '管理员密码'," +
                    "salt VARCHAR(64) DEFAULT 'cetide' NOT NULL COMMENT '密码盐'," +
                    "adminName VARCHAR(256) NOT NULL COMMENT '管理员姓名'," +
                    "email VARCHAR(256) COMMENT '管理员邮箱'," +
                    "phone VARCHAR(20) COMMENT '管理员电话'," +
                    "userAvatar VARCHAR(1024) COMMENT '管理员头像'," +
                    "role VARCHAR(256) NOT NULL COMMENT '角色: super_admin/admin'," +
                    "status TINYINT DEFAULT 1 NOT NULL COMMENT '状态: 1-正常, 0-禁用'," +
                    "createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间'," +
                    "updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间'," +
                    "lastLoginTime DATETIME COMMENT '最后登录时间'," +
                    "lastLoginIP VARCHAR(45) COMMENT '最后登录IP'," +
                    "isDeleted TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除'" +
                    ") COMMENT '管理员用户表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            jdbcTemplate.execute(createAdminTableSql);

            // 检查索引是否存在
            checkIndexExistsSql = "SELECT COUNT(*) AS count FROM information_schema.statistics WHERE table_name = ? AND index_name = ?";
            indexCount = jdbcTemplate.queryForObject(checkIndexExistsSql, Integer.class, "admin", "idx_adminAccount");

            if (indexCount == 0) {
                String createIndexSql = "CREATE INDEX idx_adminAccount ON admin (adminAccount)";
                jdbcTemplate.execute(createIndexSql);
            }

            checkIndexExistsSql = "SELECT COUNT(*) AS count FROM information_schema.statistics WHERE table_name = ? AND index_name = ?";
            indexCount = jdbcTemplate.queryForObject(checkIndexExistsSql, Integer.class, "admin", "idx_email");

            if (indexCount == 0) {
                String createIndexSql = "CREATE INDEX idx_email ON admin (email)";
                jdbcTemplate.execute(createIndexSql);
            }
            // 插入默认数据
            defaultDataInserter.insertDefaultData();
            logger.log(LogLevel.INFO, "Database tables 'user' and 'admin' created successfully.");
        } catch (Exception e) {
            logger.log(LogLevel.ERROR, "Failed to initialize database tables: {}", e.getMessage());
        }
    }
    // 创建审计表（如果不存在）
    private void createAuditTableIfNotExists(JdbcTemplate jdbcTemplate) {
        String createAuditTableSql = "CREATE TABLE IF NOT EXISTS schema_audit (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审计记录ID', " +
                "change_description TEXT NOT NULL COMMENT '变更描述', " +
                "change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '变更时间'" +
                ") ENGINE=InnoDB COMMENT '表结构变更日志表'";
        jdbcTemplate.execute(createAuditTableSql);
        logger.log(LogLevel.INFO, "Audit table 'schema_audit' checked/created.");
    }

    // 记录表结构变更日志
    private void logSchemaChange(String changeDescription) {
        try {
            String sql = "INSERT INTO schema_audit (change_description, change_time) VALUES (?, CURRENT_TIMESTAMP)";
            new JdbcTemplate(dataSource).update(sql, changeDescription);
            logger.log(LogLevel.INFO, "Schema change logged: {}", changeDescription);
        } catch (Exception e) {
            logger.log(LogLevel.ERROR, "Failed to log schema change: {}", e.getMessage());
        }
    }
    public void createIndexIfNotExists(JdbcTemplate jdbcTemplate) {
        // 检查索引是否存在
        String checkIndexSql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'idx_user_account'";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(checkIndexSql);
        boolean indexExists = false;
        if (rowSet.next()) {
            indexExists = rowSet.getInt(1) > 0;
        }

        if (!indexExists) {
            String createUserIndexSql = "CREATE INDEX idx_user_account ON user (userAccount)";
            jdbcTemplate.execute(createUserIndexSql);
            logSchemaChange("Created index 'idx_user_account' on table 'user'.");
        } else {
            logSchemaChange("Index 'idx_user_account' already exists on table 'user'.");
        }
    }

}
