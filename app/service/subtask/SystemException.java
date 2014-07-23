package service.subtask;

public class SystemException extends RuntimeException {

	private static final long serialVersionUID = 6094491319158528946L;

	public SystemException(String message, Throwable cause, Object... args) {
		super(String.format(message, args), cause);
	}

}
