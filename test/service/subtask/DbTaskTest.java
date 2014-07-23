package service.subtask;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.Test;


public class DbTaskTest {
	
	static DbTask db = new DbTask("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/books", "books", "books");
	
	@AfterClass
	public static void teardown() throws Exception {
		db.close();
	}

	@Test
	public void connection_exists() throws Exception {
		assertThat(db.getConnection(), notNullValue());
	}
	
	@Test
	public void connection_createTable_empty() throws Exception {
		db.execute("DROP TABLE IF EXISTS selected_items");
		db.execute("CREATE TABLE selected_items (session_id BIGINT, item_id BIGINT, order_id BIGINT)");
	}
}
