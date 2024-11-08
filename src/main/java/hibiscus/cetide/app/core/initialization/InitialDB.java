//package hibiscus.cetide.app.core.initialization;
//
//import hibiscus.cetide.app.basic.log.core.LogLevel;
//import hibiscus.cetide.app.basic.log.core.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.jdbc.core.JdbcTemplate;
//import javax.sql.DataSource;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class InitialDB implements ApplicationRunner {
//
//    @Autowired
//    private Logger logger;
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private DbInitializer dbInitializer;
//
//    @Value("${hibiscus.db-initial.enabled:false}")
//    private boolean isEnabled;
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        if (isEnabled){
//            logger.log(LogLevel.INFO, "Custom Application Runner is running...");
//
//            try {
//                dbInitializer.initializeDatabase();
//                logger.log(LogLevel.INFO, "Database initialization completed successfully.");
//
//                // 查看当前数据库中的所有表
//                List<Map<String, Object>> tables = getTables(dataSource);
//                for (Map<String, Object> table : tables) {
//                    String tableName = (String) table.get("TABLE_NAME");
//                    logger.log(LogLevel.INFO, "Table: {}", tableName);
//                }
//            } catch (Exception e) {
//                logger.log(LogLevel.ERROR, "Failed to initialize database: {}", e.getMessage());
//            }
//        }
//    }
//
//    private List<Map<String, Object>> getTables(DataSource dataSource) {
//        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()";
//        return jdbcTemplate.queryForList(sql);
//    }
//
////    private List<Map<String, Object>> getColumns(DataSource dataSource, String tableName) {
////        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
////        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
////        return jdbcTemplate.queryForList(sql, tableName);
////    }
//}
