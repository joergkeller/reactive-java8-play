package model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class OrderItemsTest {
	
	@Test
	public void getTotalAmount_noItem_zeroAmount() {
		OrderItems items = new OrderItems(Collections.emptyList());
		Amount amount = items.getTotalAmount();
		assertThat(amount.getFullAmount(), equalTo(0L));
		assertThat(amount.getCurrency(), equalTo("CHF"));
	}	
	
	@Test
	public void getTotalAmount_singleItem_sameAmount() {
		OrderItem item = new OrderItem(new ItemId(1), new Amount(77, "CHF"));
		OrderItems items = new OrderItems(Arrays.asList(item));
		Amount amount = items.getTotalAmount();
		assertThat(amount.getFullAmount(), equalTo(77L));
		assertThat(amount.getCurrency(), equalTo("CHF"));
	}

	@Test
	public void getTotalAmount_multipleItems_sumAmount() {
		OrderItem item = new OrderItem(new ItemId(1), new Amount(77, "CHF"));
		OrderItems items = new OrderItems(Arrays.asList(item, item, item));
		Amount amount = items.getTotalAmount();
		assertThat(amount.getFullAmount(), equalTo(231L));
		assertThat(amount.getCurrency(), equalTo("CHF"));
	}
	
	@Test(expected = RuntimeException.class)
	public void getTotalAmount_mixedCurrencies_fail() {
		OrderItem item1 = new OrderItem(new ItemId(2), new Amount(20, "CHF"));
		OrderItem item2 = new OrderItem(new ItemId(3), new Amount(30, "USD"));
		OrderItems items = new OrderItems(Arrays.asList(item1, item2));
		items.getTotalAmount(); // should fail because of mixed currencies
	}

}
