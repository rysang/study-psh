package org.ddd.customer;

import java.util.HashSet;
import java.util.Set;

import org.ddd.common.EntryPoint;

public class Order extends EntryPoint {

	private Set<OrderLineItem> lineItems = new HashSet<OrderLineItem>();
	private Customer customer;

	public Order(String orderId, Customer customer) {
		super(orderId);
		this.customer = customer;
	}
	
	public Order with(String productName, int quantity) throws OrderLimitExceededException {
		return with(new OrderLineItem(productName, quantity));
	}

	private Order with(OrderLineItem lineItem) throws OrderLimitExceededException {
		if(isExceedLimit(customer, lineItem)) {
			throw new OrderLimitExceededException();
		}
		
		lineItems.add(lineItem);
		return this;
	}
	
	private boolean isExceedLimit(Customer customer, OrderLineItem lineItem) {
		return customer.isExceedLimitPrice(getPrice().add(lineItem.getPrice()));
	}

	public Money getPrice() {
		Money result = new Money(0);
		for(OrderLineItem item : lineItems) {
			result = result.add(item.getPrice());
		}
		return result;
	}

	public static Order order(String orderId, Customer customer2) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
