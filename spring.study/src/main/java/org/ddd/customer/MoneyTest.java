package org.ddd.customer;

import static org.junit.Assert.*;

import org.junit.Test;

public class MoneyTest {
 
	@Test
	public void methodAliasing() throws Exception {
		Money money = new Money(2000);
		doSomethingWithMoney(money);
		assertEquals(new Money(2000), money);
	}

	private void doSomethingWithMoney(final Money money) {
		// TODO Auto-generated method stub
		money.add(new Money(2000));
	}
}
