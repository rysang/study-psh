package springbook.user.dao;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import springbook.user.domain.Level;
import springbook.user.domain.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/applicationContext-test.xml")
public class UserDaoTest {
	@Autowired UserDao dao;
	@Autowired DataSource dataSource;
	private User user1;
	private User user2;
	private User user3;
	
	@Autowired
	private ApplicationContext context;
	
	@Before
	public void setUp() {
		//dao = this.context.getBean("userDao", UserDaoJdbc.class);
		this.user1 = new User("want813", "psh", "1111", Level.BASIC, 1, 0);
		this.user2 = new User("shiny", "shiny", "1111", Level.SILVER, 55, 10);
		this.user3 = new User("ppss", "ppss", "1111", Level.GOLD, 100, 40);
	}

	@Test
	public void addAndGet() throws SQLException, ClassNotFoundException {
		
		dao.deleteAll();
		assertEquals(dao.getCount(), 0);
		
		List<User> users = dao.getAll();
		assertEquals(users.size(), 0);
		
		dao.add(user1);		
		assertEquals(dao.getCount(), 1);
		dao.add(user2);		
		assertEquals(dao.getCount(), 2);
		dao.add(user3);		
		assertEquals(dao.getCount(), 3);
		
		User userget2 = dao.get("shiny");
		checkSameUser(user2, userget2);
		User userget3 = dao.get("ppss");
		checkSameUser(user3, userget3);
		
	}
	
	@Test
	public void update() {
		dao.deleteAll();
		
		dao.add(user1);
		dao.add(user2);
		
		user1.setName("¤·¤µÈþ");
		user1.setPassword("222222");
		user1.setLevel(Level.GOLD);
		
		dao.update(user1);
		
		User user1update = dao.get(user1.getId());
		checkSameUser(user1update, user1);
		
		User user2update = dao.get(user2.getId());
		checkSameUser(user2update, user2);
	}
	
	@Test(expected = EmptyResultDataAccessException.class)
	public void getUserFailure() throws SQLException, ClassNotFoundException {
		User userget4 = dao.get("aaaa");
	}
	
	private void checkSameUser(User user1, User user2) {
		assertEquals(user1.getName(), user2.getName());
		assertEquals(user1.getId(), user2.getId());
		assertEquals(user1.getPassword(), user2.getPassword());
		assertEquals(user1.getLevel(), user2.getLevel());
		assertEquals(user1.getLogin(), user2.getLogin());
		assertEquals(user1.getRecommend(), user2.getRecommend());
	}
	
	
	
	
}
