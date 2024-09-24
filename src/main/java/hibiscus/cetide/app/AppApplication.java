package hibiscus.cetide.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages={"hibiscus.cetide.app"})
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}
//	private static final String[] INIT_DATA = new String[]{
//			"INSERT INTO user(id, login_name, mobile, password) VALUES ('1', 'admin', '13800000000', '123456');"
//	};
}
