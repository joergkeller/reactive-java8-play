package service;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import service.helper.Scheduler;
import service.subtask.DbTask;
import service.subtask.SetupTask;
	
public class OrderProcessTest {
	
	private static final int ITEMS_IN_STOCK = 1000;
	private static final int SESSIONS = 100;
	private static final int TOTAL_SELECTIONS = SESSIONS * 3;
	
	private static DbTask dbTask = OrderProcess.createDb();
	private static SetupTask setupTask = new SetupTask(dbTask);
	private static Random random = new Random();
	
	private int numberOfOrders = 1_000;
	private int threadPoolCount = 4;
	private int dbPoolSize = 4;

	private Scheduler<DbTask> dbTasks = new Scheduler<>(dbPoolSize, OrderProcess::createDb);
	
	@BeforeClass
	public static void setup() {
		setupTask.insertTestData();
		setupTask.insertRandomData(random, ITEMS_IN_STOCK, SESSIONS, TOTAL_SELECTIONS);
		dbTask.close();
		System.out.println("DB initialized.");
	}
	
	@After
	public void teardown() {
		dbTasks.forEach(db -> db.close());
	}


	private int randomSessionId() {
		return random.nextInt(SESSIONS)+1;
	}

	@Test
	public void singleOrder() {
		String result = OrderProcess.processOrderForSession(99, dbTasks);
		assertThat(result, startsWith("Your order"));
	}
	
	@Test
	public void order_missingItems_failure() {
		String result = OrderProcess.processOrderForSession(-1, dbTasks);;
		assertThat(result, startsWith("No items selected"));
	}
	
	@Test @Ignore("takes about 200s")
	public void massiveOrderRequests_sequentialExecution() {
		for (int i = 0; i < numberOfOrders; i++) {
			OrderProcess.processOrderForSession(randomSessionId(), dbTasks);
		}
	}
	
	@Test @Ignore("takes about 8s with 1/4 connections")
	public void massiveOrderRequests_newThreads() throws InterruptedException {
		List<Thread> threads = new ArrayList<Thread>(numberOfOrders);
		for (int i = 0; i < numberOfOrders; i++) {
			Thread th = new Thread(() -> OrderProcess.processOrderForSession(randomSessionId(), dbTasks));
			th.start();
			threads.add(th);
		}
		// wait for threads to complete
		for (Thread th : threads) {
			th.join();
		}
	}

	@Test //@Ignore
	/* With 1 connection:
	 * threads	randomdata
	 *   1		
	 * With 4 connections:
	 * threads	randomdata
	 *   4		81s
	 *  16		49s
	 *  64 		32s	
	 * 256		30s
	 */
	public void massiveOrderRequest_threadPool() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(threadPoolCount);
		for (int i = 0; i < numberOfOrders; i++) {
			executor.execute(() -> OrderProcess.processOrderForSession(randomSessionId(), dbTasks));
		}
		executor.shutdown();
		executor.awaitTermination(240, TimeUnit.SECONDS);
		executor.shutdownNow();
	}
}
