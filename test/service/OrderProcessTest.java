package service;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import model.SessionId;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import play.libs.F.Promise;
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
	public static void setupClass() {
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

	private String processOrderForSession(long id, Scheduler<DbTask> dbTasks) {
		SessionId sessionId = new SessionId(id);
		OrderProcess orderProcess = new OrderProcess(sessionId, dbTasks);
		return orderProcess.processOrder();
	}

	@Test
	public void singleOrder() {
		String result = processOrderForSession(99, dbTasks);
		assertThat(result, startsWith("Your order"));
	}
	
	@Test
	public void order_missingItems_failure() {
		String result = processOrderForSession(-1, dbTasks);;
		assertThat(result, startsWith("No items selected"));
	}
	
	@Test @Ignore
	public void massiveOrderRequests_sequentialExecution() {
		for (int i = 0; i < numberOfOrders; i++) {
			processOrderForSession(randomSessionId(), dbTasks);
		}
	}
	
	@Test @Ignore
	public void massiveOrderRequests_newThreads() throws InterruptedException {
		List<Thread> threads = new ArrayList<Thread>(numberOfOrders);
		for (int i = 0; i < numberOfOrders; i++) {
			Thread th = new Thread(() -> processOrderForSession(randomSessionId(), dbTasks));
			th.start();
			threads.add(th);
		}
		// wait for threads to complete
		for (Thread th : threads) {
			th.join();
		}
	}

	@Test @Ignore
	/* With 1 connection:
	 * r-threads	sync
	 *   1		290s
	 * With 4 connections:
	 * r-threads	sync	async.get	async	parallel	ws	parallel+ws
	 *   1					222s		25s		26s			8s	9s
	 *   4			81s		 65s		25s		25s			8s	8s
	 *  16			49s		 49s		25s		26s			8s	8s
	 *  64 			32s		 53s		25s		25s			8s	8s
	 * 256			30s		 64s		25s		26s			9s	8s
	 */
	public void massiveOrderRequest_threadPool() throws InterruptedException {
		Collection<Promise<String>> results = Collections.synchronizedCollection(new LinkedList<Promise<String>>());
		ExecutorService executor = Executors.newFixedThreadPool(threadPoolCount);
		for (int i = 0; i < numberOfOrders; i++) {
			executor.execute(() -> {
				SessionId sessionId = new SessionId((long) randomSessionId());
				OrderProcess orderProcess = new OrderProcess(sessionId, dbTasks);
				Promise<String> result = orderProcess.asyncProcessOrder();
				results.add(result);
			});
		}
		executor.shutdown();
		executor.awaitTermination(300, TimeUnit.SECONDS);
		OrderProcess.awaitAll(results);
		executor.shutdownNow();
	}
	
}
