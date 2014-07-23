package model;

/**
 * Result of a validation request of the cc service.
 * The confirmation tag can be used for final confirmation of the payment.
 */
public class ValidationResult {

	private final boolean valid;
	private final String reservation;

	public ValidationResult(boolean valid, String reservation) {
		this.valid = valid;
		this.reservation = reservation;
	}

	/** Helper constructor if no confirmation tag is present. */
	public ValidationResult(boolean valid) {
		this(valid, null);
	}

	public boolean isValid() {
		return valid;
	}

	public String getReservation() {
		return reservation;
	}

}
