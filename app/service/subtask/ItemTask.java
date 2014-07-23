package service.subtask;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import model.Amount;
import model.ItemId;
import model.OrderId;
import model.OrderItem;
import model.OrderItems;
import model.SessionId;

public class ItemTask {
	
	private final DbTask db;
	
	public ItemTask(DbTask db) {
		this.db = db;
	}

	public OrderItems collect(SessionId id) {
		Collection<OrderItem> items = db.read("SELECT stk.item_id, stk.amount, stk.currency FROM selected_items sel JOIN stock_items stk ON stk.item_id=sel.item_id WHERE sel.session_id=?", id.asLong(), ItemTask::readItems);
		return new OrderItems(items);
	}

	private static Collection<OrderItem> readItems(ResultSet rs) {
		try {
			Collection<OrderItem> items = new LinkedList<OrderItem>();
			while (rs.next()) {
				ItemId id = new ItemId(rs.getLong("item_id"));
				Amount amount = new Amount(rs.getLong("amount"), rs.getString("currency"));
				items.add(new OrderItem(id, amount));
			}
			return items;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public int countAssignedItems(OrderId orderId) {
		return db.read("SELECT COUNT(item_id) FROM selected_items WHERE order_id=?", orderId.asLong(), ItemTask::readCount);
	}
	
	private static int readCount(ResultSet rs) {
		try {
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} 
	}

	public boolean assign(SessionId sessionId, OrderItems items) {
		final OrderId orderId = items.getOrderId();
		db.withTransaction(() -> {
			long[] itemIds = items.getItemIds();
			db.modifyBatch("UPDATE selected_items SET order_id=? WHERE session_id=? AND item_id=?", orderId.asLong(), sessionId.asLong(), itemIds);
			db.modifyBatch("UPDATE stock_items SET available=available-1 WHERE item_id=?", itemIds);
		});
		return countAssignedItems(orderId) == items.size();
	}

}

