package model;

/**
 * Result of a validation request of the cc service.
 * The confirmation tag can be used for final confirmation of the payment.
 */
public class ValidationResult {

	private final boolean valid;
	private final String reservationId;

	public ValidationResult(boolean valid, String reservationId) {
		this.valid = valid;
		this.reservationId = reservationId;
	}

	/** Helper constructor if no confirmation tag is present. */
	public ValidationResult(boolean valid) {
		this(valid, null);
	}

	public boolean isValid() {
		return valid;
	}

	public boolean isReservation() {
		return valid && reservationId != null;
	}

	public String getReservationId() {
		return reservationId;
	}

}
