package model;

public class Customer {

	private final CardDetail cardDetails;
	private final String email;

	public Customer(CardDetail cardDetails, String email) {
		this.cardDetails = cardDetails;
		this.email = email;
	}

	public CardDetail getCardDetails() {
		return cardDetails;
	}

	public String getEmail() {
		return email;
	}

}
