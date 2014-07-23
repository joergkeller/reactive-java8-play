package service.subtask;

public class MailException extends RuntimeException {

	private static final long serialVersionUID = -2320641910771988308L;

	public MailException(Throwable th) {
		super(th);
	}

}
