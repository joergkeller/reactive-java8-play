package model;

public class OrderId {

	private static long nextOrderId = 42;

	private final long orderId = nextOrderId++;

	public long asLong() {
		return orderId;
	}
	
}
