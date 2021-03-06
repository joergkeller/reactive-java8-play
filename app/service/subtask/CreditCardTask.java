package service.subtask;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;
import model.Amount;
import model.CardDetail;
import model.ValidationResult;

public class CreditCardTask {

	private final String acquirerUrl;

	public CreditCardTask(String acquirerUrl) {
		this.acquirerUrl = acquirerUrl;
	}

	public ValidationResult checkValidity(CardDetail details, Amount amount) {
		Promise<Response> promise = checkValidityAsync(details, amount);
		Response response = promise.get(10, TimeUnit.SECONDS);
		return fromCheckResponse(response);
	}

	public Promise<Response> checkValidityAsync(CardDetail details, Amount amount) {
		WSRequestHolder ws = WS.url(acquirerUrl + "reservation");
		ws.setContentType("application/json");
		ws.setQueryParameter("card", details.getNumber());
		ws.setQueryParameter("exp", details.getExpirationAsString());
		ws.setQueryParameter("amount", String.valueOf(amount.getFullAmount()));
		ws.setQueryParameter("currency", amount.getCurrency());
		Promise<Response> promise = ws.get();
		return promise;
	}

	public ValidationResult fromCheckResponse(Response response) {
		JsonNode result = response.asJson();
//		System.out.println(result.toString());
		return new ValidationResult(result.get("valid").asBoolean(), result.get("reservation").asText());
	}

	public ValidationResult debit(String reservationTag, Amount finalAmount) {
		Promise<Response> promise = debitAsync(reservationTag, finalAmount);
		Response response = promise.get(10, TimeUnit.SECONDS);
		return fromDebitResponse(response);
	}

	public Promise<Response> debitAsync(String reservationTag, Amount finalAmount) {
		WSRequestHolder ws = WS.url(acquirerUrl + "commit");
		ws.setQueryParameter("reservation", reservationTag);
		ws.setQueryParameter("amount", String.valueOf(finalAmount.getFullAmount()));
		ws.setQueryParameter("currency", finalAmount.getCurrency());
		Promise<Response> promise = ws.get();
		return promise;
	}

	public ValidationResult fromDebitResponse(Response response) {
		String body = response.getBody();
//		System.out.println(body);
		return new ValidationResult(body.startsWith("Committed"));
	}

}
