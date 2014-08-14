package controllers;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import model.SessionId;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.Future;
import service.OrderProcess;
import service.helper.Scheduler;
import service.subtask.DbTask;
import service.subtask.SetupTask;
import actor.DatabaseActor;
import actor.OrderActor;
import akka.actor.ActorRef;
import akka.japi.Util;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class LimmatShopping extends Controller {
	
	private static final int ITEMS_IN_STOCK = 1000;
	private static final int SESSIONS = 100;
	private static final int TOTAL_SELECTIONS = SESSIONS * 3;
	private static final int DB_POOL_SIZE = 4;

	private static final Scheduler<DbTask> dbTasks = new Scheduler<>(DB_POOL_SIZE, OrderProcess::createDb);
	private static final SetupTask setupTask = new SetupTask(dbTasks.get());
	private static final Random random = new Random();
	
	private static final ActorRef database = Akka.system().actorOf(DatabaseActor.props(), "databases");

	static {
		setupTask.insertTestData();
		setupTask.insertRandomData(random, ITEMS_IN_STOCK, SESSIONS, TOTAL_SELECTIONS);
		System.out.println(String.format("Created random test data for %d sessions using %d items in stock", SESSIONS, ITEMS_IN_STOCK));
	}

	public static Result order() {
		SessionId sessionId = new SessionId((long) (random.nextInt(SESSIONS) + 1));
		OrderProcess orderProcess = new OrderProcess(sessionId, dbTasks);
		String response = orderProcess.processOrder();
		return ok(response);
	}
	
	public static F.Promise<Result> actorOrder() {
		SessionId sessionId = new SessionId((long) (random.nextInt(SESSIONS) + 1));
		ActorRef orders = Akka.system().actorOf(OrderActor.props(sessionId, database));
		Future<Object> response = Patterns.ask(orders, OrderActor.PROCESS_ORDER, new Timeout(4, TimeUnit.SECONDS));
		Future<String> stringResponse = response.mapTo(Util.classTag(String.class));
		return F.Promise.wrap(stringResponse).map(content -> ok(content));
	}

}
