package springbook.user.service;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.*;
import static springbook.user.service.UserServiceImpl.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import springbook.user.dao.UserDao;
import springbook.user.domain.Level;
import springbook.user.domain.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/test-applicationContext.xml")
public class UserServiceTest {
	@Autowired
	UserDao userDao;
	@Autowired
	PlatformTransactionManager transactionManager;
	@Autowired
	MailSender mailSender;
	@Autowired
	UserService userService;
	@Autowired
	UserService testUserService;
	@Autowired
	ApplicationContext context;

	List<User> users;
	
	@Before
	public void setUp() {
		System.out.println("userService:"+userService);
		System.out.println("testUserService:"+testUserService);
		users= Arrays.asList(
			new User("want813", "psh", "1111", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER-1, 0),
			new User("shiny", "shiny", "1111", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
			new User("ppss", "ppss", "1111", Level.SILVER, MIN_RECCOMEND_FOR_GOLD-1, 29),
			new User("111", "444", "1111", Level.SILVER, MIN_RECCOMEND_FOR_GOLD, 30),
			new User("222", "555", "1111", Level.GOLD, 100, Integer.MAX_VALUE)
		);
	}
	
	@Test
	public void upgradeLevels() throws Exception {
		UserServiceImpl userServiceImpl = new UserServiceImpl();
		
		MockUserDao mockUserDao = new MockUserDao(this.users);
		userServiceImpl.setUserDao(mockUserDao);		
		
		MokMailSender mailSender = new MokMailSender();
		userServiceImpl.setMailSender(mailSender);
		userServiceImpl.upgradeLevels();
		
		List<User> updated = mockUserDao.getUpdated();
		assertEquals(updated.size(), 2);
		checkUserAndLevel(updated.get(0), "shiny", Level.SILVER);
		checkUserAndLevel(updated.get(1), "111", Level.GOLD);
		
		
	}
	
	
	private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
		assertEquals(updated.getId(), expectedId);
		assertEquals(updated.getLevel(), expectedLevel);
	}

	@Test
	@DirtiesContext
	public void upgradeAllOrNothing() throws Exception {
		
		userDao.deleteAll();
		for(User user : users) userDao.add(user);
		
		try {
			testUserService.upgradeLevels();
			fail("TestUserServiceException expected");
		} catch(TestUserServiceException e) {
			System.out.println("fail");
			
		}
		checkLevelUpgraded(users.get(1), false);
	}
	
	@Test
	public void add() throws Exception {
		userDao.deleteAll();
		
		User userWithLevel = users.get(4);
		User userWithoutLevel = users.get(0);
		userWithoutLevel.setLevel(null);
		
		userService.add(userWithLevel);
		userService.add(userWithoutLevel);
		
		User userWithLevelRead = userDao.get(userWithLevel.getId());
		User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());
		
		assertEquals(userWithLevel.getLevel(), userWithLevelRead.getLevel());
		assertEquals(userWithoutLevel.getLevel(), userWithoutLevelRead.getLevel());
	}

	private void checkLevelUpgraded(User user, boolean upgraded) {
		User userUpdate = userDao.get(user.getId());
		if(upgraded) {
			assertEquals(userUpdate.getLevel(), user.getLevel().nextLevel());	
		} else {
			assertEquals(userUpdate.getLevel(), user.getLevel());
		}
		
	}
	
	static class MockUserDao implements UserDao {
		private List<User> users;
		private List<User> updated = new ArrayList();

		private MockUserDao(List<User> users) {
			this.users = users;
		}
		public List<User> getUpdated() {
			return this.updated;
		}

		public List<User> getAll() {
			return this.users;
		}
		public void update(User user) {
			System.out.println("update:"+user.getId());
			this.updated.add(user);
		}
		
		public void add(User user) {
			throw new UnsupportedOperationException();
		}
		public User get(String id) {
			throw new UnsupportedOperationException();
		}
		public void deleteAll() {
			throw new UnsupportedOperationException();
		}
		public int getCount() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	@Test
	public void advisorAutoProxyCreator() throws Exception {
		//assertEquals(userService.getClass(), java.lang.reflect.Proxy.class);
		//assertThat(testUserService.getClass(), is(java.lang.reflect.Proxy.class));
	}
	
	static class TestUserServiceImpl extends UserServiceImpl {
		private String id = "111";
		protected void upgradeLevel(User user) {
			System.out.println("upgradeLevel : "+user.getId());
			if(user.getId().equals(this.id)) { System.out.println("here"); throw new TestUserServiceException(); }
			super.upgradeLevel(user);
		}
	}
	
	static class TestUserServiceException extends RuntimeException {
		
	}
}
