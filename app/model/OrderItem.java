package model;

public class OrderItem {

	private final ItemId id;
	private final Amount amount;

	public OrderItem(ItemId id, Amount amount) {
		this.id = id;
		this.amount = amount;
	}

	public ItemId getItemId() {
		return id;
	}

	public Amount getAmount() {
		return amount;
	}

}
