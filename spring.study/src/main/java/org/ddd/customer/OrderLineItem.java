package org.ddd.customer;

public class OrderLineItem {

	private Product product;
	private int quantity;
	
	private ProductRepository productRepository = new ProductRepository();
	
	public OrderLineItem(String productName, int quantity) {
		this.product = productRepository.find(productName);
		this.quantity = quantity;
	}
	
	public Money getPrice() {
		return product.getPrice().multiply(quantity);
	}
	
	public Product getProduct() {
		return product;
	}
}
