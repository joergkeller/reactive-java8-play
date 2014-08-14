package service;

import java.time.temporal.Temporal;
import java.util.Optional;

import model.Amount;
import model.CardDetail;
import model.Customer;
import model.OrderItems;
import model.SessionId;
import model.ValidationResult;
import service.helper.Scheduler;
import service.subtask.CreditCardTask;
import service.subtask.CustomerTask;
import service.subtask.DbTask;
import service.subtask.ItemTask;
import service.subtask.MailTask;
import service.subtask.OrderTask;

public class OrderProcess {
	
	final SessionId sessionId;
	final DbTask dbTask;
	final CustomerTask customerTask;
	final ItemTask itemTask;
	final CreditCardTask cardTask = new CreditCardTask("http://localhost:9000/acquirer/");
	final OrderTask orderTask = new OrderTask("orders");
	final MailTask mailTask = new MailTask("localhost", 1024);
	
	public static DbTask createDb() {
		return new DbTask("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/books", "books", "books");
	}

	public OrderProcess(SessionId sessionId, Scheduler<DbTask> dbTasks) {
		this.sessionId = sessionId;
		this.dbTask = dbTasks.get();
		this.customerTask =  new CustomerTask(dbTask);
		this.itemTask = new ItemTask(dbTask);
	}
	
	public String processOrder() {
		Optional<Customer> customerOption = customerTask.obtain(sessionId);
		if (!customerOption.isPresent()) {
			return onMissingCustomer();
		}
		Customer customer = customerOption.get();

		OrderItems orderItems = itemTask.collect(sessionId);
		if (orderItems.isEmpty()) {
			return onMissingItems();
		}

		CardDetail cardDetails = customer.getCardDetails();
		Amount totalAmount = orderItems.getTotalAmount();
		ValidationResult validation = cardTask.checkValidity(cardDetails, totalAmount);
		if (!validation.isValid()) {
			return onInvalidCard(cardDetails, totalAmount);
		}
		
		boolean reservation = itemTask.assign(sessionId, orderItems);
		if (!reservation) {
			return onOutOfStock();
		}
		
		ValidationResult debitResult = cardTask.debit(validation.getReservationId(), totalAmount);
		if (!debitResult.isValid()) {
			return onInvalidCard(cardDetails, totalAmount);
		}
		
		Temporal expectedDelivery = orderTask.submit(orderItems, customer);
		mailTask.confirm(orderItems, expectedDelivery, customer);
		
		return onOrderConfirmation(orderItems, expectedDelivery, customer);
	}

	protected String onMissingCustomer() {
		return "Missing customer information. You will be redirected to the login page.";
	}

	protected String onMissingItems() {
		return "No items selected. Please choose books from the catalog.";
	}

	protected String onOutOfStock() {
		return "Some items are out of stock. You will be redirected to the shopping basket again.";
	}

	protected String onInvalidCard(CardDetail cardDetails, Amount totalAmount) {
		return "Invalid credit card. Please enter valid card details.";
	}

	private String onOrderConfirmation(OrderItems orderItems, Temporal expectedDelivery, Customer customer) {
		Amount amount = orderItems.getTotalAmount();
		return String.format("Your order (%d items, %d %s) was processed successfully.\n"
						   + "A confirmation mail was sent to %s.\n"
	                       + "Delivery is expected at %tF.\n", 
	                         orderItems.size(), amount.getFullAmount(), amount.getCurrency(), customer.getEmail(), expectedDelivery);
	}

}
