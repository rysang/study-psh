package org.ddd.customer;

import org.ddd.common.Registrar;

public class CustomerRepository {

	public void save(Customer customer) {
		Registrar.add(Customer.class, customer);
		
	}

	public Customer find(String identity) {
		return (Customer) Registrar.get(Customer.class, identity);
	}

}
