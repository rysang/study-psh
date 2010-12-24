package springbook.user.dao;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@Configuration
public class DaoFactory {
	@Bean
	public UserDaoJdbc userDao() {
		UserDaoJdbc userDao = new UserDaoJdbc();
		return userDao;
	}
}
