package model;

import java.util.Collection;
import java.util.Iterator;

public class OrderItems {
	
	private final OrderId orderId = new OrderId();
	private final Collection<OrderItem> items;

	public OrderItems(Collection<OrderItem> items) {
		this.items = items;
	}

	public OrderId getOrderId() {
		return orderId;
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public int size() {
		return items.size();
	}

	public long[] getItemIds() {
		long[] ids = new long[items.size()];
		Iterator<OrderItem> iterator = items.iterator();
		for (int i = 0; i < ids.length; i++) {
			ids[i] = iterator.next().getItemId().asLong();
		}
		return ids;
	}

	// TODO Convert item currencies to customer currency
	public Amount getTotalAmount() {
		String currency = null;
		long amount = 0L;
		for (OrderItem item : items) {
			Amount itemAmount = item.getAmount();
			if (amount > 0) { assertSameCurrency(itemAmount.getCurrency(), currency); }
			amount += itemAmount.getFullAmount();
			currency = itemAmount.getCurrency();
		}
		if (currency == null) { currency = "CHF"; }
		return new Amount(amount, currency);
	}

	private void assertSameCurrency(String c1, String c2) {
		if (!c1.trim().equalsIgnoreCase(c2.trim())) {
			throw new RuntimeException(String.format("No matching currencies: %s vs. %s", c1, c2));
		}
	}

}
