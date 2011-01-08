package org.ddd.customer;

import org.ddd.common.Registrar;

public class OrderRepository {
	public void save(Order order) {
		Registrar.add(Order.class, order);
		
	}

	public Order find(String identity) {
		return (Order) Registrar.get(Order.class, identity);
	}
}
