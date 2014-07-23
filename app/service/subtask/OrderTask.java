package service.subtask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

import model.Customer;
import model.OrderItems;

public class OrderTask {

	private static int orderId = 1;
	
	private final File directory;

	public OrderTask(String dirname) {
		directory = new File(dirname);
		directory.mkdirs();
	}

	public Temporal process(OrderItems items, Customer customer) {
		try {
			File file = new File(directory, String.format("order-%d.txt", orderId++));
			if (file.exists()) { file.delete(); }
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			writer.append(String.format("Total item count: %d\n", items.size()));
			for (long itemId : items.getItemIds()) {
				writer.append(String.valueOf(itemId)).append(", ");
			}
			writer.close();
		} catch (IOException e) {
			throw new FileException(e);
		}
		int numberOfOrders = directory.listFiles().length;
		return calculateExpectedDeliveryDate(numberOfOrders);
	}

	private Temporal calculateExpectedDeliveryDate(int numberOfOrders) {
		int delay = 2 + (numberOfOrders / 20);
		return ZonedDateTime.now().plus(delay, ChronoUnit.DAYS);
	}

}
