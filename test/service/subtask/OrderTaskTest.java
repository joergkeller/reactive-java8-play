package service.subtask;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.ZonedDateTime;
import java.util.Collections;

import model.CardDetail;
import model.Customer;
import model.OrderItems;

import org.junit.Test;

public class OrderTaskTest {

	@Test
	public void processOrder_writeFile() {
		OrderTask task = new OrderTask("orders");
		Customer customer = new Customer(new CardDetail("1234", "08-15"), "mail@server.domain");
		OrderItems items = new OrderItems(Collections.emptyList());
		ZonedDateTime delivery = (ZonedDateTime) task.process(items, customer);
		assertThat(delivery.isAfter(ZonedDateTime.now()), equalTo(true));
	}
	
}
