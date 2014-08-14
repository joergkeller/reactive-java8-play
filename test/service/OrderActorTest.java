package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import model.SessionId;

import org.junit.BeforeClass;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Future;
import service.subtask.DbTask;
import service.subtask.SetupTask;
import actor.DatabaseActor;
import actor.OrderActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class OrderActorTest {

	private static final int ITEMS_IN_STOCK = 1000;
	private static final int SESSIONS = 100;
	private static final int TOTAL_SELECTIONS = SESSIONS * 3;
	
	private static DbTask dbTask = OrderProcess.createDb();
	private static SetupTask setupTask = new SetupTask(dbTask);
	private static Random random = new Random();
	
	private int numberOfOrders = 1_000;
	
	private ActorSystem system = ActorSystem.create();

	@BeforeClass
	public static void setupClass() {
		setupTask.insertTestData();
		setupTask.insertRandomData(random, ITEMS_IN_STOCK, SESSIONS, TOTAL_SELECTIONS);
		dbTask.close();
		System.out.println("DB initialized.");
	}
	
	private int randomSessionId() {
		return random.nextInt(SESSIONS)+1;
	}

	@Test //@Ignore 
	public void massiveOrderRequest_actors() throws Exception {
		ActorRef database = system.actorOf(DatabaseActor.props(), "databases");
		List<Future<Object>> responses = new ArrayList<Future<Object>>(numberOfOrders);
		for (int i = 0; i < numberOfOrders; i++) {
			SessionId sessionId = new SessionId(randomSessionId());
			ActorRef orders = system.actorOf(OrderActor.props(sessionId, database));
			Future<Object> response = Patterns.ask(orders, OrderActor.PROCESS_ORDER, new Timeout(30, TimeUnit.SECONDS));
			responses.add(response);
		}
		System.out.println("Requests sent");
		Future<Iterable<Object>> completion = Futures.sequence(responses, system.dispatcher());
		Await.result(completion, new Timeout(60, TimeUnit.SECONDS).duration());
		System.out.println(String.format("%d requests processed", numberOfOrders));
	}
	
}
