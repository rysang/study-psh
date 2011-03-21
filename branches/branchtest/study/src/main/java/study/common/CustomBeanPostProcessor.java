package study.common;

import java.lang.reflect.Field;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class CustomBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		// TODO Auto-generated method stub
		System.out.println("postProcessBeforeInitialization");
		
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		System.out.println("postProcessAfterInitialization");
		Class clazz = bean.getClass();
		System.out.println("here1 : "+clazz.getSimpleName());
		if(clazz.getSimpleName().equals("User")) {
			try {
				Field field = clazz.getDeclaredField("id");
				field.setAccessible(true);
				String id = (String) field.get(bean);
				if(id.equals("want813")) {
					field.set(bean, "hahaha");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return bean;
	}

}
