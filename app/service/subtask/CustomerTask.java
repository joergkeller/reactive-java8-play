package service.subtask;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import model.CardDetail;
import model.Customer;
import model.SessionId;

public class CustomerTask {

	private final DbTask db;
	
	public CustomerTask(DbTask db) {
		this.db = db;
	}

	public Optional<Customer> obtain(SessionId id) {
		return db.read("SELECT usr.email, usr.card, usr.expiration FROM logon_users lu JOIN users usr on usr.user_id=lu.user_id WHERE lu.session_id=?", id.asLong(), CustomerTask::readCustomer);
	}
	
	private static Optional<Customer> readCustomer(ResultSet result) {
		try {
			if (!result.next()) {
				return Optional.empty();
			}
			CardDetail cardDetail = new CardDetail(result.getString("card"), result.getString("expiration"));
			Customer customer = new Customer(cardDetail, result.getString("email"));
			if (result.next()) {
				throw new DatabaseException("Duplicate logon customer");
			}
			return Optional.of(customer);
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

}
