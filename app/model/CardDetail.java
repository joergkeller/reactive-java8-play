package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CardDetail {
	
	private static final DateTimeFormatter outputPattern = DateTimeFormatter.ofPattern("MM-yy");
	private static final DateTimeFormatter inputPattern = DateTimeFormatter.ofPattern("dd-MM-yy");

	private final String number;
	private final LocalDate expiration;
	
	public CardDetail(String number, LocalDate expiration) {
		this.number = number;
		this.expiration = expiration;
	}

	public CardDetail(String number, String expiration) {
		this(number, LocalDate.parse("01-" + expiration, inputPattern));
	}

	public String getNumber() {
		return number;
	}

	public String getExpirationAsString() {
		return outputPattern.format(expiration);
	}

}
