package service.subtask;

import java.time.temporal.Temporal;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import model.Amount;
import model.Customer;
import model.OrderItems;

public class MailTask {

	private final Session session;
	
	public MailTask(String host, int port) {
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", host);
		properties.setProperty("mail.smtp.port", String.valueOf(port));
		session = Session.getDefaultInstance(properties);
	}

	public void confirm(OrderItems orderItems, Temporal expectedDelivery, Customer customer) {
		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress("test@zuehlke.com"));
			message.addRecipient(RecipientType.TO, new InternetAddress(customer.getEmail()));
			message.setSubject("Confirmation Limmat Shopping");
			Amount amount = orderItems.getTotalAmount();
			String msg = String.format("You ordered %d items with a total price of %d %s\n", orderItems.size(), amount.getFullAmount(), amount.getCurrency())
					   + String.format("The order will be processed at %tD\n", expectedDelivery)
					   + String.format("The amount will be booked to your card ending with ...%s", anonymize(4, customer.getCardDetails().getNumber()));
			message.setText(msg);
			Transport.send(message);
		} catch (Exception e) {
			throw new MailException(e);
		}
	}

	private String anonymize(int length, String number) {
		return number.substring(number.length() - length);
	}
	
}
