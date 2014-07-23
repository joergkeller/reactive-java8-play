package model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

public class CardDetailTest {
	
	@Test
	public void cardExpiration() {
		LocalDate expiration = LocalDate.of(2014, Month.JULY, 22);
		String formattedExpiration = new CardDetail("", expiration).getExpirationAsString();
		assertThat(formattedExpiration, equalTo("07-14"));
	}

}
