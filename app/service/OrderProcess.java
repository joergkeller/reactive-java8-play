package service;

import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import model.Amount;
import model.CardDetail;
import model.Customer;
import model.OrderItems;
import model.SessionId;
import model.ValidationResult;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.WS.Response;
import scala.concurrent.ExecutionContext;
import service.helper.Scheduler;
import service.subtask.CreditCardTask;
import service.subtask.CustomerTask;
import service.subtask.DbTask;
import service.subtask.ItemTask;
import service.subtask.MailTask;
import service.subtask.OrderTask;
import akka.dispatch.ExecutionContexts;

public class OrderProcess {
	
	static final ExecutionContext runner = ExecutionContexts.fromExecutorService(Executors.newFixedThreadPool(8)); // non-blocking tasks
	static final ExecutionContext dbRunner = ExecutionContexts.fromExecutorService(Executors.newFixedThreadPool(4)); // blocking db access
	static final ExecutionContext fsRunner = ExecutionContexts.fromExecutorService(Executors.newSingleThreadExecutor()); // blocking filesystem writes
	static final ExecutionContext mailRunner = ExecutionContexts.fromExecutorService(Executors.newSingleThreadExecutor()); // blocking mail send

	public static List<String> awaitAll(Collection<Promise<String>> promises) {
		@SuppressWarnings("unchecked")
		Promise<String>[] p = promises.toArray(new Promise[promises.size()]);
		return Promise.sequence(runner, p).get(300, TimeUnit.SECONDS);
	}

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
		return asyncProcessOrder().get(60, TimeUnit.SECONDS);
	}

	public Promise<String> asyncProcessOrder() {
		return collectSessionItems();
	}

	private Promise<String> collectSessionItems() {
		Promise<Optional<Customer>> customerFuture = Promise.promise(() -> customerTask.obtain(sessionId), dbRunner);
		customerFuture.onFailure(th -> th.printStackTrace(), fsRunner);

		Promise<OrderItems> orderFuture = Promise.promise(() -> itemTask.collect(sessionId), dbRunner);
		orderFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		
		Promise<Tuple<Optional<Customer>, OrderItems>> sessionFuture = customerFuture.zip(orderFuture);
		return sessionFuture.flatMap(tuple -> checkCardValidation(tuple._1, tuple._2), runner);
	}

	private Promise<String> checkCardValidation(Optional<Customer> customerOption, OrderItems orderItems) {
		if (orderItems.isEmpty()) {
			return Promise.pure(onMissingItems());
		}
		if (!customerOption.isPresent()) {
			return Promise.pure(onMissingCustomer());
		}
		Customer customer = customerOption.get();

		CardDetail cardDetails = customer.getCardDetails();
		Amount totalAmount = orderItems.getTotalAmount();
		Promise<Response> responseFuture = cardTask.checkValidityAsync(cardDetails, totalAmount);
		responseFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		Promise<ValidationResult> validationFuture = responseFuture.map(cardTask::fromCheckResponse, runner);
		validationFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		return validationFuture.flatMap(validation -> assignOrderedItems(customer, orderItems, validation), runner);
	}

	private Promise<String> assignOrderedItems(Customer customer, OrderItems orderItems, ValidationResult validation) {
		CardDetail cardDetails = customer.getCardDetails();
		Amount totalAmount = orderItems.getTotalAmount();
		if (!validation.isValid()) {
			return Promise.pure(onInvalidCard(cardDetails, totalAmount));
		}
		
		Promise<Boolean> reservationFuture = Promise.promise(() -> itemTask.assign(sessionId, orderItems), dbRunner);
		reservationFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		return reservationFuture.flatMap(reservation -> chargeTotalAmount(customer, orderItems, validation, reservation), runner);
	}

	private Promise<String> chargeTotalAmount(Customer customer, OrderItems orderItems, ValidationResult validation, Boolean reservation) {
		if (!reservation) {
			return Promise.pure(onOutOfStock());
		}
		
		Amount totalAmount = orderItems.getTotalAmount();
		Promise<Response> responseFuture = cardTask.debitAsync(validation.getReservation(), totalAmount);
		responseFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		Promise<ValidationResult> debitFuture = responseFuture.map(cardTask::fromDebitResponse, runner);
		debitFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		return debitFuture.flatMap(debitResult -> submitOrderForProcessing(customer, orderItems, debitResult), runner);
	}

	private Promise<String> submitOrderForProcessing(Customer customer, OrderItems orderItems, ValidationResult debitResult) {
		CardDetail cardDetails = customer.getCardDetails();
		Amount totalAmount = orderItems.getTotalAmount();
		if (!debitResult.isValid()) {
			return Promise.pure(onInvalidCard(cardDetails, totalAmount));
		}
		
		Promise<Temporal> deliveryFuture = Promise.promise(() -> orderTask.submit(orderItems, customer), fsRunner);
		deliveryFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		return deliveryFuture.flatMap(expectedDelivery -> sendConfirmationMail(customer, orderItems, expectedDelivery), runner);
	}

	private Promise<String> sendConfirmationMail(Customer customer, OrderItems orderItems, Temporal expectedDelivery) {
		Promise<Boolean> mailFuture = Promise.promise(() -> mailTask.confirm(orderItems, expectedDelivery, customer), mailRunner);
		mailFuture.onFailure(th -> th.printStackTrace(), fsRunner);
		return Promise.pure(onOrderConfirmation(orderItems, expectedDelivery, customer));
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
