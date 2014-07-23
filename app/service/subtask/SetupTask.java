package service.subtask;

import java.util.Random;


public class SetupTask {
	
	private final DbTask db;

	public SetupTask(DbTask db) {
		this.db = db;
		try {
			dropTables();
			createTables();
		} catch (DatabaseException e) {
			throw new DatabaseException("Cannot recreate database tables", e);
		}
	}

	private void createTables() {
		db.execute("CREATE TABLE selected_items (session_id BIGINT, item_id BIGINT, order_id BIGINT)");
		db.execute("CREATE TABLE stock_items (item_id BIGINT PRIMARY KEY, amount INTEGER, currency CHAR(3), available INTEGER)");
		db.execute("CREATE TABLE users (user_id BIGINT PRIMARY KEY, email VARCHAR(100), card VARCHAR(19), expiration VARCHAR(5))");
		db.execute("CREATE TABLE logon_users (session_id BIGINT PRIMARY KEY, user_id BIGINT)");
	}

	private void dropTables() {
		db.execute("DROP TABLE IF EXISTS logon_users");
		db.execute("DROP TABLE IF EXISTS users");
		db.execute("DROP TABLE IF EXISTS stock_items");
		db.execute("DROP TABLE IF EXISTS selected_items");
	}

	public void insertTestData() {
		insertStockItems(new long[] {1, 2, 3}, new long[] {77, 22, 55});
		insertSelectedItems(new long[] {1, 99, 99, 99}, new long[] {1, 1, 2, 3});
		db.modify("INSERT INTO users (user_id, email, card, expiration) VALUES (?,?,?,?)", 1, "user@server.domain", "1234-5678", "07-15");
		db.modifyBatch("INSERt INTO logon_users (session_id, user_id) VALUES (?,?)", new long[] {-1, 99}, 1L);
	}

	private void insertStockItems(long[] ids, long[] amounts) {
		db.modifyBatch("INSERT INTO stock_items (item_id, amount, currency, available) VALUES (?,?,?,?)", ids, amounts, "CHF", 10_000);
	}

	private void insertSelectedItems(long[] sessions, long[] items) {
		db.modifyBatch("INSERT INTO selected_items (session_id, item_id, order_id) VALUES (?,?,null)", sessions, items);
	}

	public void insertRandomData(Random random, int itemsInStock, int sessions, int totalSelections) {
		long[] items = new long[itemsInStock];
		long[] amounts = new long[itemsInStock];
		for (int i = 0; i < itemsInStock; i++) {
			items[i] = i + 10; // random items start with 10
			amounts[i] = 20 + random.nextInt(180); // pricing [20..200)
		}
		insertStockItems(items, amounts);
		
		long[] itemSessionIds = new long[totalSelections];
		long[] itemIds = new long[totalSelections];
		for (int i = 0; i < totalSelections; i++) {
			itemSessionIds[i] = random.nextInt(sessions) + 1; // session ids overlap with test data above
			itemIds[i] = random.nextInt(itemsInStock) + 10; // random items start with 10
		}
		insertSelectedItems(itemSessionIds, itemIds);
		
		long[] userSessionIds = new long[sessions];
		for (int i = 0; i < sessions; i++) {
			if (i + 1 == 99) { continue; } // already included in testdata 
			userSessionIds[i] = i + 1;
		}
		db.modifyBatch("INSERT INTO logon_users (session_id, user_id) VALUES (?,?)", userSessionIds, 1L);
	}

}
