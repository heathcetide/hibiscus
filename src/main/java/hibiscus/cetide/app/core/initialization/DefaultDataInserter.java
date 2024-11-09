package hibiscus.cetide.app.core.initialization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import hibiscus.cetide.app.basic.log.core.Logger;
import hibiscus.cetide.app.basic.log.core.LogLevel;
import javax.sql.DataSource;

@Component
public class DefaultDataInserter {

    @Value("${default-user.account:admin}")
    private String defaultUserAccount;

    @Value("${default-user.password:admin}")
    private String defaultUserPassword;

    @Value("${default-user.role:admin}")
    private String defaultUserRole;

    private final JdbcTemplate jdbcTemplate;
    private final Logger logger;

    public DefaultDataInserter(DataSource dataSource, Logger logger) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.logger = logger;
    }

    // 检查表是否为空
    private boolean isTableEmpty(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == 0;
    }

    // 插入默认用户数据
    public void insertDefaultData() {
        try {
            if (isTableEmpty("user")) {
                String insertSql = "INSERT INTO user (userAccount, userPassword, userRole) VALUES (?, ?, ?)";
                jdbcTemplate.update(insertSql, defaultUserAccount, defaultUserPassword, defaultUserRole);
                logger.log(LogLevel.INFO, "Inserted default admin user.");
            } else {
                logger.log(LogLevel.INFO, "User table already contains data. Skipping default user insertion.");
            }
        } catch (Exception e) {
            logger.log(LogLevel.ERROR, "Failed to insert default data: {}", e.getMessage());
        }
    }
}
