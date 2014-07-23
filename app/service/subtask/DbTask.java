package service.subtask;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

public class DbTask implements Closeable {
	
	private final Connection connection;
	
	public DbTask(String driver, String url, String user, String password) {
		try {
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException cnfe) {
			throw new SystemException("DB driver %s not found", cnfe, driver);
		} catch (SQLException se) {
			throw new SystemException("Cannot open connection to %s for %s", se, url, user);
		}
	}

	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new DatabaseException("Cannot close connection", e);
		}
	}

	public Connection getConnection() {
		return connection;
	}

	/**
	 * Run the given operations within a transaction. 
	 * Otherwise, each statement is auto-committed.  
	 */
	public synchronized void withTransaction(Runnable operations) {
		try {
			connection.setAutoCommit(false);
			operations.run();
			connection.commit();
			connection.setAutoCommit(true);
		} catch (Exception e) {
			try { connection.rollback(); } catch (SQLException se) { /* ignored */ }
			throw new DatabaseException(e);
		}
	}

	/**
	 * Execute the command without requesting a result.
	 */
	public void execute(String command) {
		try (Statement stmt = connection.createStatement()) {
			stmt.execute(command);
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Execute the sql query. The reader will consume the result set and provide the result. 
	 * Note that the reader must not declare exceptions (i.e. wrapping SQLExceptions).
	 */
	public <T> T read(String sql, long id, Function<ResultSet, T> reader) {
		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, id);
			ResultSet rs = stmt.executeQuery();
			return reader.apply(rs);
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Execute the dml command. Arguments will be set in order of appearance.
	 * Note that the default mapping from Java to sql types will be used.
	 */
	public void modify(String dml, Object... values) {
		try (PreparedStatement stmt = connection.prepareStatement(dml)) {
			for (int i = 0; i < values.length; i++) {
				stmt.setObject(i+1, values[i]);
			}
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Execute the dml command multiple times. Arguments will be set in order of appearance.
	 * Arrays of long are allowed to cover multiple rows. These arrays define the number of batches
	 * and therefore must be of the same length. The first batch will have the array values of index 0, 
	 * the next batch of index 1 and so on...
	 */
	public void modifyBatch(String dml, Object... values) {
		int count = getNumberOfRuns(values);
		try (PreparedStatement stmt = connection.prepareStatement(dml)) {
			for (int run = 0; run < count; run++) {
				for (int i = 0; i < values.length; i++) {
					Object value = values[i];
					if (value.getClass().isAssignableFrom(long[].class)) {
						long[] array = (long[]) value;
						value = array[run];
					}
					stmt.setObject(i+1, value);
				}
				stmt.addBatch();
			}
			stmt.executeBatch();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	private int getNumberOfRuns(Object... values) {
		int count = 1;
		for (Object value : values) {
			if (value.getClass().isAssignableFrom(long[].class)) {
				long[] array = (long[]) value;
				if (count == 1) { count = array.length; }
				if (count != array.length) { throw new ConsistencyException("Inconsistent array length %d vs. %d", count, array.length); }
			}
		}
		return count;
	}

}
