package org.ddd.customer;

import static org.junit.Assert.*;

import org.ddd.common.Registrar;
import org.junit.Before;
import org.junit.Test;

public class CustomerTest {
	
	@Before
	public void setUp() {
		Registrar.init();
	}

	@Test
	public void aliasing() throws Exception {
		Customer customer = new Customer("CUST-01", "홍길동", "경기도 안양시", 110000);
		Customer anotherCustomer = customer;
		
		long price = 1000;
		customer.purchase(price);
		
		assertEquals(price*0.01, anotherCustomer.getMileage(), 0.1);
		//assertEquals(0, anotherCustomer.getMileage());
	}
	
	@Test
	public void customerIdentical() throws Exception {
		CustomerRepository customerRepository = new CustomerRepository();
		
		Customer customer = new Customer("CUST-01", "홍길동", "경기도 안양시", 110000);
		customerRepository.save(customer);
		Customer anotherCustomer = customerRepository.find("CUST-01");
		assertSame(customer, anotherCustomer);
	}
}
