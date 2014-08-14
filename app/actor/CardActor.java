package actor;

import model.Amount;
import model.CardDetail;
import model.ValidationResult;
import play.libs.F.Promise;
import play.libs.WS.Response;
import service.subtask.CreditCardTask;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

/**
 * Actor interacting with the card acquirer web service to
 * - check the card validation and make a reservation
 * - confirm the reservation and commit the transaction 
 */
public class CardActor extends UntypedActor {

	private final CreditCardTask cardTask = new CreditCardTask("http://localhost:9000/acquirer/");
	private final ActorRef client;
	private final CardDetail cardDetails;
	private final Amount totalAmount;
	private boolean committedAmount = false;

	
	/** Actor props factory. */
	public static Props props(CardDetail cardDetails, Amount totalAmount) {
		return Props.create(CardActor.class, cardDetails, totalAmount);
	}

	public CardActor(CardDetail cardDetails, Amount totalAmount) {
		this.cardDetails = cardDetails;
		this.totalAmount = totalAmount;
		this.client = context().parent(); 
		Promise<Response> validation = cardTask.checkValidityAsync(cardDetails, totalAmount);
		Patterns.pipe(validation.wrapped(), context().dispatcher()).to(self());
	}

	/**
	 * TODO:
	 * - Clearify between the check and debit request (both returning a response)
	 */
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Response && !committedAmount) {
			// first response is validity check
			ValidationResult validation = cardTask.fromCheckResponse((Response) msg);
			client.tell(validation, self());
		} else if (msg instanceof Response && committedAmount) {
			// second response is debit
			ValidationResult validation = cardTask.fromDebitResponse((Response) msg);
			client.tell(validation, self());
		} else if (msg instanceof ValidationResult) {
			ValidationResult validation = (ValidationResult) msg;
			committedAmount = true;
			Promise<Response> debit = cardTask.debitAsync(validation.getReservationId(), totalAmount);
			Patterns.pipe(debit.wrapped(), context().dispatcher()).to(self());
		}
		
	}

}
