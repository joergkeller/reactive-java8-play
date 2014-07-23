package model;

public class Amount {

	private final long amount;
	private final String currency;

	public Amount(long amount, String currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public long getFullAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

}
