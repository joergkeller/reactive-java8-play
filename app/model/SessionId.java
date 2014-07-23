package model;

public class SessionId {

	private final long id;

	public SessionId(long id) {
		this.id = id;
	}

	public long asLong() {
		return id;
	}

}
