package service.subtask;

public class DatabaseException extends RuntimeException {
	
	private static final long serialVersionUID = -411214573871681872L;

	public DatabaseException(Throwable cause) {
		super(cause);
	}
	
	public DatabaseException(String message, Throwable cause, Object... args) {
		super(String.format(message, args), cause);
	}

	public DatabaseException(String message, Object... args) {
		super(String.format(message, args));
	}


}