package controllers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import model.ValidationResult;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Simulates a remote credit card aquirer service.
 * Responses are delayed a bit, but will not block the server.
 */
public class CardAcquirer extends Controller {
	
	public static F.Promise<Result> reservation(String card, String exp, int amount, String currency) {
		return F.Promise.delayed(() -> {
			response().setContentType("application/json");
			ValidationResult response = new ValidationResult(true, UUID.randomUUID().toString());
			return ok(Json.toJson(response));
		}, 100, TimeUnit.MILLISECONDS);
	}
	
	public static F.Promise<Result> commit(String reservation, int amount, String currency) {
		return F.Promise.delayed(() -> ok(String.format("Committed %d %s", amount, currency)), 100, TimeUnit.MILLISECONDS);
	}

}
