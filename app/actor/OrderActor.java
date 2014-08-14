package actor;

import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.concurrent.Callable;

import model.Amount;
import model.CardDetail;
import model.Customer;
import model.OrderItems;
import model.SessionId;
import model.ValidationResult;
import scala.concurrent.Future;
import service.subtask.MailTask;
import service.subtask.OrderTask;
import actor.DatabaseActor.DbCommit;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;


/**
 * Actor to process an order on behalf of the customer. Each instance is
 * assigned to the current session and will process the steps to
 * 1. Select customer and order items associated with the session
 * 2. Perform credit card validation and commit
 * 3. Assign selected items as bought (only if card is valid, but before amount committed)
 * 4. Send an order to the store house, receiving an expected delivery date
 * 5. Confirm the order as email and with a response to the user. 
 */
public class OrderActor extends UntypedActor {

	// Actor message -> String response
	public static final Object PROCESS_ORDER = "Start processing of an order";
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);
	private final OrderTask orderTask = new OrderTask("orders");
	private final MailTask mailTask = new MailTask("localhost", 1024);

	private final SessionId sessionId;
	private final ActorRef database;
	private ActorRef requesting;
	private ActorRef card;
	private Customer customer = null;
	private OrderItems items = null;

	private ValidationResult validation;

	
	/** Actor props factory. */
	public static Props props(SessionId sessionId, ActorRef database) {
		return Props.create(OrderActor.class, sessionId, database);
	}

	public OrderActor(SessionId sessionId, ActorRef database) {
		this.sessionId = sessionId;
		this.database = database;
		log.info("OrderActor created");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg == PROCESS_ORDER) {
			initiateOrderProcess(sessionId);
		} else if (msg instanceof Optional<?>) {
			@SuppressWarnings("unchecked")
			Optional<Customer> customer = (Optional<Customer>) msg;
			receivingCustomer(customer);
		} else if (msg instanceof OrderItems) {
			OrderItems items = (OrderItems) msg;
			receivingItems(items);
		} else if (msg instanceof ValidationResult) {
			ValidationResult validation = (ValidationResult) msg;
			receivingCardValidation(validation);
		} else if (msg instanceof DatabaseActor.DbCommit) {
			DbCommit commit = (DbCommit) msg;
			receivingDbCommit(commit);
		} else if (msg instanceof Temporal) {
			Temporal delivery = (Temporal)msg;
			receivingDeliveryDate(delivery);
		}
	}

	private void initiateOrderProcess(SessionId sessionId) {
		log.info("Initiate order process");
		requesting = sender();
		database.tell(new DatabaseActor.RequestCustomer(sessionId), self());
		database.tell(new DatabaseActor.RequestItems(sessionId), self());
	}

	private void receivingCustomer(Optional<Customer> option) {
		log.info("Receiving customer");
		if (!option.isPresent()) {
			String response = "Missing customer information. Please login.";
			requesting.tell(response, self());
		} else {
			this.customer = option.get();
			if (hasBothCustomerAndItems()) { startCardValidation(); }
		}
	}

	private void receivingItems(OrderItems items) {
		log.info("Receiving items");
		this.items = items;
		if (items.isEmpty()) {
			String response = "No items selected. Please choose books from the catalog.";
			requesting.tell(response, self());
		} else if (hasBothCustomerAndItems()) {
			startCardValidation();
		}
	}

	private boolean hasBothCustomerAndItems() {
		return customer != null && items != null && !items.isEmpty();
	}

	private void startCardValidation() {
		log.info("Starting card validation");
		CardDetail cardDetails = customer.getCardDetails();
		Amount totalAmount = items.getTotalAmount();
		card = context().actorOf(CardActor.props(cardDetails, totalAmount));
	}

	private void receivingCardValidation(ValidationResult validation) {
		log.info("Receiving card validation");
		this.validation = validation;
		if (!validation.isValid()) {
			String response = "Invalid credit card. Please enter valid card details.";
			requesting.tell(response, self());
		} else if (validation.isReservation()) {
			database.tell(new DatabaseActor.AssignItems(sessionId, items), self());
		} else {
			Callable<Temporal> submission = () -> orderTask.submit(items, customer);
			Future<Temporal> delivery = Futures.future(submission, context().dispatcher());
			Patterns.pipe(delivery, context().dispatcher()).to(self());
		}
	}

	private void receivingDbCommit(DatabaseActor.DbCommit commit) {
		log.info("receiving database commit");
		if (!commit.success) {
			String response = "Some items are out of stock. You will be redirected to the shopping basket again.";
			requesting.tell(response, self());
		} else {
			card.tell(validation, self());
		}
	}

	private void receivingDeliveryDate(Temporal delivery) {
		log.info("Receiving delivery date");
		Callable<Boolean> confirmation = () -> mailTask.confirm(items, delivery, customer);
		Futures.future(confirmation, context().dispatcher());
		Amount amount = items.getTotalAmount();
		String response = String.format("Your order (%d items, %d %s) was processed successfully.\n"
						   + "A confirmation mail was sent to %s.\n"
		                   + "Delivery is expected at %tF.\n", 
		                     items.size(), amount.getFullAmount(), amount.getCurrency(), customer.getEmail(), delivery);
		requesting.tell(response, self());
	}

}
