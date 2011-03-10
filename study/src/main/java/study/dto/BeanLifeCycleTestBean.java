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
		System.out.println("setBeanName() �����մϴ�.");
		this.beanName = beanName;
	}

	public String getBeanName(){
		return beanName;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		System.out.println("setBeanClassLoader() �����մϴ�.");
		this.classLoader = classLoader;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		System.out.println("setBeanFactory() �����մϴ�.");
		this.beanFactory = beanFactory;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		System.out.println("setResourceLoader() �����մϴ�.");
		this.resourceLoader = resourceLoader;
	}

	public ResourceLoader getResourceLoader(){
		return resourceLoader;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher arg0) {
		System.out.println("setApplicationEventPublisher() �����մϴ�.");
	}

	public void setMessageSource(MessageSource arg0) {
		System.out.println("setMessageSource() �����մϴ�.");
	}

	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		System.out.println("setApplicationContext() �����մϴ�.");
	}

	public void setServletContext(ServletContext servletContext){
		System.out.println("setServletContext() �����մϴ�.");
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("postProcessAfterInitialization() �����մϴ�.");
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("postProcessAfterInitialization() �����մϴ�.");
		return bean;
	}

	public void afterPropertiesSet() throws Exception {
		System.out.println("afterPropertiesSet() �����մϴ�.");
	}

	public void customInit(){
		System.out.println("customInit() �����մϴ�.");
	}

	@Override
	public void setResourceLoader(
			org.springframework.core.io.ResourceLoader arg0) {
		// TODO Auto-generated method stub

	}
}


