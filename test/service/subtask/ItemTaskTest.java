package service.subtask;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import model.ItemId;
import model.OrderItem;
import model.OrderItems;
import model.SessionId;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ItemTaskTest {
	
	static DbTask db = new DbTask("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/books", "books", "books");
	static SetupTask setup = new SetupTask(db);

	ItemTask task = new ItemTask(db);
	
	@BeforeClass
	public static void setup() {
		setup.insertTestData();
	}
	
	@AfterClass
	public static void teardown() {
		db.close();
	}

	@Test
	public void getItems_newSession_noItems() {
		OrderItems items = task.collect(new SessionId(0L));
		assertThat(items.isEmpty(), equalTo(true));
	}
	
	/**
	 * {@link DbTask#insertTestData}
	 */
	@Test
	public void getItems_existingSession_someItems() {
		OrderItems items = task.collect(new SessionId(99L));
		assertThat(items.size(), equalTo(3));
	}
	
	/**
	 * {@link DbTask#insertTestData}
	 */
	@Test
	public void assignItems_newOrder() {
		OrderItem item1 = new OrderItem(new ItemId(1), null);
		OrderItem item2 = new OrderItem(new ItemId(2), null);
		OrderItem item3 = new OrderItem(new ItemId(3), null);
		OrderItems items = new OrderItems(Arrays.asList(item1, item2, item3));
		assertThat(task.countAssignedItems(items.getOrderId()), equalTo(0)); // precondition
		task.assign(new SessionId(99), items);
		assertThat(task.countAssignedItems(items.getOrderId()), equalTo(3));
	}
}
