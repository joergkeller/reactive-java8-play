package model;

public class ItemId {

	private final long itemid;

	public ItemId(long id) {
		this.itemid = id;
	}

	public long asLong() {
		return itemid;
	}

}
