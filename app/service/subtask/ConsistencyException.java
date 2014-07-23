package service.subtask;

public class ConsistencyException extends RuntimeException {

	private static final long serialVersionUID = -914849690814735751L;

	public ConsistencyException(String message, Object... args) {
		super(String.format(message, args));
	}

}
