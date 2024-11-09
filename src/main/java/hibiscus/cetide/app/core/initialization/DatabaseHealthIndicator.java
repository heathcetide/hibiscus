//package hibiscus.cetide.app.core.initialization;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.actuate.health.Health;
//import org.springframework.boot.actuate.health.HealthIndicator;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//public class DatabaseHealthIndicator implements HealthIndicator {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    @Autowired
//    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    @Override
//    public Health health() {
//        try {
//            // 尝试执行一个简单的 SQL 查询，以确认数据库是否可用
//            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
//            return Health.up().withDetail("database", "Database is reachable").build();
//        } catch (Exception e) {
//            return Health.down().withDetail("database", "Database is not reachable: " + e.getMessage()).build();
//        }
//    }
//}
