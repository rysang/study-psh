package springbook.learningtest.jdk;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;

public class ReflectionTest {

	@Test 
	public void invokeMethod() throws Exception {
		String name="Spring";
		
		assertEquals(name.length(), 6);
		
		Method lengthMethod = String.class.getMethod("length");
		assertEquals(lengthMethod.invoke(name), 6);
		
		assertEquals(name.charAt(0), 'S');
		
		Method charAtMethod = String.class.getMethod("charAt", int.class);
		assertEquals(charAtMethod.invoke(name, 0), 'S');
	}

	@Test
	public void simpleProxy() throws Exception {
		Hello proxiedHello = (Hello)Proxy.newProxyInstance(getClass().getClassLoader()
				, new Class[] {Hello.class}
				, new UppercaseHandler(new HelloTarget()));
		assertEquals(proxiedHello.sayHello("toby"), "HELLO TOBY");
		assertEquals(proxiedHello.sayHi("toby"), "HI TOBY");
		assertEquals(proxiedHello.sayThankYou("toby"), "THANKYOU TOBY");
	}
	
	@Test
	public void proxyFactoryBean() throws Exception {
		ProxyFactoryBean pfBean = new ProxyFactoryBean();
		pfBean.setTarget(new HelloTarget());
		pfBean.addAdvice(new UppercaseAdvice());
		
		Hello proxiedHello = (Hello) pfBean.getObject();
		assertEquals(proxiedHello.sayHello("toby"), "HELLO TOBY");
		assertEquals(proxiedHello.sayHi("toby"), "HI TOBY");
		assertEquals(proxiedHello.sayThankYou("toby"), "THANKYOU TOBY");
	}
	
	@Test
	public void classNamePointcutAdvisor() throws Exception {
		NameMatchMethodPointcut classMethodPointcut = new NameMatchMethodPointcut() {
			public ClassFilter getClassFilter() {
				return new ClassFilter() {
					@Override
					public boolean matches(Class<?> clazz) {
						return clazz.getSimpleName().startsWith("HelloT");
					}
				};
			}
		};
		classMethodPointcut.setMappedName("sayH*");
		
		checkAdviced(new HelloTarget(), classMethodPointcut, true);
		
		class HelloWorld extends HelloTarget {};
		checkAdviced(new HelloWorld(), classMethodPointcut, false);
		
		class HelloToby extends HelloTarget {};
		checkAdviced(new HelloToby(), classMethodPointcut, true);
	}
	
	private void checkAdviced(Object target,
			Pointcut pointcut, boolean adviced) {
		ProxyFactoryBean pfBean = new ProxyFactoryBean();
		pfBean.setTarget(target);
		pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
		Hello proxiedHello = (Hello) pfBean.getObject();
		if(adviced) {
			assertEquals(proxiedHello.sayHello("toby"), "HELLO TOBY");
			assertEquals(proxiedHello.sayHi("toby"), "HI TOBY");
			assertEquals(proxiedHello.sayThankYou("toby"), "ThankYou toby");
		} else {
			assertEquals(proxiedHello.sayHello("toby"), "hello toby");
			assertEquals(proxiedHello.sayHi("toby"), "hi toby");
			assertEquals(proxiedHello.sayThankYou("toby"), "ThankYou toby");
		}
	}

	@Test
	public void pointcutAdvisor() throws Exception {
		ProxyFactoryBean pfBean = new ProxyFactoryBean();
		pfBean.setTarget(new HelloTarget());
		
		NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
		pointcut.setMappedName("sayH*");
		
		pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
		
		Hello proxiedHello = (Hello) pfBean.getObject();
		assertEquals(proxiedHello.sayHello("toby"), "HELLO TOBY");
		assertEquals(proxiedHello.sayHi("toby"), "HI TOBY");
		assertEquals(proxiedHello.sayThankYou("toby"), "ThankYou toby");
	}
	
	static class UppercaseAdvice implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			String ret = (String) invocation.proceed();
			return ret.toUpperCase();
		}
		
	}
}
