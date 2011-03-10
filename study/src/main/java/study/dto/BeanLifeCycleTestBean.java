package study.dto;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.web.context.ServletContextAware;

import com.sun.xml.internal.ws.api.ResourceLoader;

public class BeanLifeCycleTestBean implements BeanNameAware,
BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware,
ApplicationEventPublisherAware, MessageSourceAware,
ApplicationContextAware, ServletContextAware, BeanPostProcessor,
InitializingBean{

	String beanName;

	ClassLoader classLoader;

	BeanFactory beanFactory;

	ResourceLoader resourceLoader;

	public void setBeanName(String beanName) {
		System.out.println("setBeanName() 실행합니다.");
		this.beanName = beanName;
	}

	public String getBeanName(){
		return beanName;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("setBeanClassLoader() 실행합니다.");
		this.classLoader = classLoader;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("setBeanFactory() 실행합니다.");
		this.beanFactory = beanFactory;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		System.out.println("setResourceLoader() 실행합니다.");
		this.resourceLoader = resourceLoader;
	}

	public ResourceLoader getResourceLoader(){
		return resourceLoader;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher arg0) {
		System.out.println("setApplicationEventPublisher() 실행합니다.");
	}

	public void setMessageSource(MessageSource arg0) {
		System.out.println("setMessageSource() 실행합니다.");
	}

	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		System.out.println("setApplicationContext() 실행합니다.");
	}

	public void setServletContext(ServletContext servletContext){
		System.out.println("setServletContext() 실행합니다.");
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("postProcessAfterInitialization() 실행합니다.");
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("postProcessAfterInitialization() 실행합니다.");
		return bean;
	}

	public void afterPropertiesSet() throws Exception {
		System.out.println("afterPropertiesSet() 실행합니다.");
	}

	public void customInit(){
		System.out.println("customInit() 실행합니다.");
	}

	@Override
	public void setResourceLoader(
			org.springframework.core.io.ResourceLoader arg0) {
		// TODO Auto-generated method stub

	}
}


