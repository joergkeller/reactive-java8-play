package actor;

import java.util.Optional;

import model.Customer;
import model.OrderItems;
import model.SessionId;
import service.OrderProcess;
import service.subtask.CustomerTask;
import service.subtask.DbTask;
import service.subtask.ItemTask;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.FromConfig;


/**
 * Actor to perform database tasks like
 * - select the logged-in customer
 * - select the items selected by the customer
 * - assign the selected items as bought 
 */
public class DatabaseActor extends UntypedActor {

	// Actor message -> Optional<Customer>
	public static class RequestCustomer {
		final SessionId sessionId;
		public RequestCustomer(SessionId sessionId) {
			this.sessionId = sessionId;
		}
	}

	// Actor message -> OrderItems
	public static class RequestItems {
		final SessionId sessionId;
		public RequestItems(SessionId sessionId) {
			this.sessionId = sessionId;
		}
	}
	
	// Actor message -> DbCommit
	public static class AssignItems {
		final SessionId sessionId;
		final OrderItems items;
		public AssignItems(SessionId sessionId, OrderItems items) {
			this.sessionId = sessionId;
			this.items = items;
		}
	}
	
	// Actor response
	public static class DbCommit {
		final boolean success;
		public DbCommit(boolean success) {
			this.success = success;
		}
	}
		

	private DbTask dbTask = OrderProcess.createDb();
	private ItemTask itemTask = new ItemTask(dbTask);
	private CustomerTask customerTask = new CustomerTask(dbTask);

	
	/** Actor props factory. */
	public static Props props() {
		return Props.create(DatabaseActor.class)
				    .withRouter(new FromConfig());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof RequestCustomer) {
			requestCustomer((RequestCustomer) msg);
		} else if (msg instanceof RequestItems) {
			requestItems((RequestItems) msg);
		} else if (msg instanceof AssignItems) {
			assignItems((AssignItems) msg);
		}
	}

	private void requestCustomer(RequestCustomer request) {
		Optional<Customer> customer = customerTask.obtain(request.sessionId); // blocking request!
		sender().tell(customer, self());
	}

	private void requestItems(RequestItems request) {
		OrderItems order = itemTask.collect(request.sessionId); // blocking request!
		sender().tell(order, self());
	}

	private void assignItems(AssignItems assignment) {
		boolean success = itemTask.assign(assignment.sessionId, assignment.items); // blocking request!
		sender().tell(new DbCommit(success), self());
	}

}
