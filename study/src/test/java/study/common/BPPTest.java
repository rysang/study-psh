package study.common;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import study.dto.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/study/common/applicationContext-test.xml")
public class BPPTest {

	@Autowired
	User user;
	
	@Test
	public void BBP() throws Exception {
		assertNotNull(user);
		assertEquals(user.getId(), "hahaha");
		System.out.println(user.getId());
	}
	
}
