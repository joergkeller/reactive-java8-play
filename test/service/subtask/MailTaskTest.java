package service.subtask;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Collections;

import model.CardDetail;
import model.Customer;
import model.OrderItems;

import org.junit.Test;

public class MailTaskTest {

	@Test
	public void confirmMail() {
		OrderItems orderItems = new OrderItems(Collections.emptyList());
		Temporal expectedDelivery = ZonedDateTime.now().plus(5, ChronoUnit.DAYS);
		CardDetail card = new CardDetail("1234 5678", "08-15");
		Customer customer = new Customer(card, "user@server.domain");
		new MailTask("localhost", 1024).confirm(orderItems, expectedDelivery, customer);
	}
}
