package org.ddd.customer;

import org.ddd.common.EntryPoint;
import org.ddd.common.Registrar;

public class Customer extends EntryPoint {

	private String customNumber;
	private String name;
	private String address;
	private long mileage;
	private Money limitPrice;
	
	public Customer(String customerNumber, String name, String address, long limitPrice) {
		super(customerNumber);
		this.customNumber = customerNumber;
		this.name = name;
		this.address = address;
		this.limitPrice = new Money(limitPrice);
	}
	
	public void purchase(long price) {
		mileage += price * 0.01;
	}
	
	public boolean isPossibleToPayWithMileage(long price) {
		return mileage > price;
	}
	
	public boolean payWithMileage(long price) {
		if(!isPossibleToPayWithMileage(price)) {
			return false;
		}
		mileage -= price;
		return true;
	}
	
	public long getMileage() {
		return mileage;
	}

	public static Customer find(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	public Order newOrder(String orderId) {
		return Order.order(orderId, this);
	}
	
	public boolean isExceedLimitPrice(Money money) {
		return money.isGreaterThan(limitPrice);
	}
	
}
