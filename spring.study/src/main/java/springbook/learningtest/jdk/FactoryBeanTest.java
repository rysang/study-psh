package springbook.learningtest.jdk;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FactoryBeanTest {

	@Autowired
	ApplicationContext context;
	
	@Test
	public void getMessageFromFactoryBean() throws Exception {
		Object message = context.getBean("message");
		assertEquals(message.getClass(), Message.class);
		assertEquals(((Message)message).getText(), "Factory Bean");
	}

	@Test
	public void getFactoryBean() throws Exception {
		Object factory = context.getBean("&message");
		assertEquals(factory.getClass(), MessageFactoryBean.class);
	}
}
