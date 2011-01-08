package org.ddd.customer;

import static org.junit.Assert.*;

import org.ddd.common.Registrar;
import org.junit.Before;
import org.junit.Test;

public class OrderTest {

	private Customer customer;
	private OrderRepository orderRepository;
	private ProductRepository productRepository;
	
	@Before
	public void setUp() {
		Registrar.init();
		orderRepository = new OrderRepository();
		productRepository = new ProductRepository();
		productRepository.save(new Product("��ǰ1", 1000));
		productRepository.save(new Product("��ǰ2", 5000));
		
		customer = new Customer("CUST-01", "ȫ�浿", "��⵵ �Ⱦ��", 110000);
	}
	
	@Test
	public void orderPrice() throws Exception {
		Order order = customer.newOrder("CUST-01-ORDER-01").with("��ǰ1", 10).with("��ǰ2", 20);
		orderRepository.save(order);
		assertEquals(new Money(110000), order.getPrice());
	}
	
	@Test
	public void orderLimitExceed() throws Exception {
		try {
			customer.newOrder("CUST-01-ORDER-01").with("��ǰ1", 20).with("��ǰ2", 50);
			fail();
		} catch(OrderLimitExceededException ex) {
			assertTrue(true);
		}
	}
}
