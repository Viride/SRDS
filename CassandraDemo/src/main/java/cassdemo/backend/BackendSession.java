package cassdemo.backend;

import com.datastax.driver.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cassdemo.Settings;
import cassdemo.Order;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * For error handling done right see:
 * https://www.datastax.com/dev/blog/cassandra-error-handling-done-right
 *
 * Performing stress tests often results in numerous WriteTimeoutExceptions,
 * ReadTimeoutExceptions (thrown by Cassandra replicas) and
 * OpetationTimedOutExceptions (thrown by the client). Remember to retry
 * failed operations until success (it can be done through the RetryPolicy mechanism:
 * https://stackoverflow.com/questions/30329956/cassandra-datastax-driver-retry-policy )
 */


public class BackendSession {

	private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

	private Session session;

	public BackendSession(String contactPoint, String keyspace) throws BackendException {

		Cluster cluster = Cluster.builder().addContactPoint(contactPoint).withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE)).build();
		try {
			session = cluster.connect(keyspace);
		} catch (Exception e) {
			throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
		}
		prepareStatements();
	}

	public int getTicket(Settings settings, Order order) throws BackendException{
		int result = 0;



		upsertScreenings(order.hall, order.clientId, order.clientRequestId, order.numberOfSeats);
		//chcę swój bilet

		result = checkTicket(settings, order, result);

		return result;
	}

	public int checkTicket(Settings settings, Order order, int result) throws BackendException{
		int result2=0;
		List<Order> output = selectAllByHall(order.hall);
		output.sort(Comparator.comparing(Order::GetTime));
		int index = output.indexOf(order);


		int[] rows = new int[settings.hallRowNumber];
		int count2 = 0;
		List<Integer> inside2 = new ArrayList<Integer>();

		for(int i=0; i<output.size();i++){
			Order ord = output.get(i);
			for(int j=0; j<settings.hallRowNumber; j++){
				if(rows[j] + ord.numberOfSeats<=settings.hallRowSize){
					inside2.add(ord.clientId);
					rows[j] = rows[j] + ord.numberOfSeats;
					count2 = count2 + ord.numberOfSeats;
					break;
				}
			}
		}
		if(inside2.contains(order.clientId)){
			result2=1;
		}

		return result2;
	}

	private static PreparedStatement SELECT_ALL_FROM_SCREENINGS;
	private static PreparedStatement SELECT_ALL_FROM_SCREENINGS_HALL;
	private static PreparedStatement INSERT_INTO_SCREENINGS;
	private static PreparedStatement DELETE_ALL_FROM_SCREENINGS;
	//private static PreparedStatement CHECK_NICK;
	//private static PreparedStatement INSERT_NICK;
	//private static PreparedStatement DELETE_SCREENING;
	//private static PreparedStatement UPDATE_COLS;
	//private static PreparedStatement SELECT_COLS;
	//private static PreparedStatement INCREMENT;
	//private static PreparedStatement GET_COUNTER_VALUE;
	private static final String USER_FORMAT = "- %-10s  %-10s %-10s %-10s %-10s\n";
	public AtomicInteger counter = new AtomicInteger(0);
	// private static final SimpleDateFormat df = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private void prepareStatements() throws BackendException {
		try {
			SELECT_ALL_FROM_SCREENINGS = session.prepare("SELECT * FROM screenings;");
			SELECT_ALL_FROM_SCREENINGS_HALL = session.prepare("SELECT hall, clientid, clientrequestid, blobAsBigint(timestampAsBlob(time)), numberofplaces FROM screenings where hall=? ALLOW FILTERING;");
			INSERT_INTO_SCREENINGS = session
					.prepare("INSERT INTO screenings (hall, clientId, clientRequestId, numberOfPlaces, time) VALUES (?, ?, ?, ?, toUnixTimeStamp(now()));");
			DELETE_ALL_FROM_SCREENINGS = session.prepare("TRUNCATE screenings;");
			//CHECK_NICK = session.prepare("select * from nicki where nick = ?;").setConsistencyLevel(ConsistencyLevel.QUORUM);
			//INSERT_NICK = session.prepare("update nicki set user = ? where nick = ?;").setConsistencyLevel(ConsistencyLevel.QUORUM);
			//DELETE_SCREENING = session.prepare("delete user from nicki where nick = ?;").setConsistencyLevel(ConsistencyLevel.QUORUM);
			//UPDATE_COLS = session.prepare("update test set col1=?, col2=? where id = 0;").setConsistencyLevel(ConsistencyLevel.QUORUM);
			//SELECT_COLS = session.prepare("select * from test where id=0;").setConsistencyLevel(ConsistencyLevel.QUORUM);
			//INCREMENT = session.prepare("update counting set counter = counter + 1 where id = 1;").setConsistencyLevel(ConsistencyLevel.ONE);
			//GET_COUNTER_VALUE = session.prepare("select * from counting where id = 1;");
		} catch (Exception e) {
			throw new BackendException("Could not prepare statements. " + e.getMessage() + ".", e);
		}

		logger.info("Statements prepared");
	}

	public String selectAll() throws BackendException {
		StringBuilder builder = new StringBuilder();
		BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_SCREENINGS);

		ResultSet rs = null;
		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		for (Row row : rs) {
			String rhall = row.getString("hall");
			String rclientId = row.getString("clientId");
			String rclientRequestId = row.getString("clientRequestId");
			int rnumberOfPlaces = row.getInt("numberOfPlaces");
			int rtime = row.getInt("system.blobasbigint(system.timestampasblob(time))");
			builder.append(String.format(USER_FORMAT, rhall, rclientId, rclientRequestId, rnumberOfPlaces, rtime));
		}

		
		return builder.toString();
	}
	public List<Order> selectAllByHall(String hallName) throws BackendException {
		StringBuilder builder = new StringBuilder();
		BoundStatement bs = new BoundStatement(SELECT_ALL_FROM_SCREENINGS_HALL);
		bs.bind(hallName);
		ResultSet rs = null;

		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}
		List<Order> orders = new ArrayList<Order>();

		for (Row row : rs) {
			String rhall = row.getString("hall");
			int rclientId = row.getInt("clientId");
			int rclientRequestId = row.getInt("clientRequestId");
			int rnumberOfPlaces = row.getInt("numberOfPlaces");
			long rtime = row.getLong("system.blobasbigint(system.timestampasblob(time))");
			Order order = new Order(rhall, rclientId, rclientRequestId, rnumberOfPlaces, rtime);
			builder.append(String.format(USER_FORMAT, rhall, rclientId, rclientRequestId, rnumberOfPlaces, rtime));
			orders.add(order);
		}

		
		return orders;
	}
	public void upsertScreenings(String hall, int clientId, int clientRequestId, int numberOfPlaces) throws BackendException {
		BoundStatement bs = new BoundStatement(INSERT_INTO_SCREENINGS);
		bs.bind(hall, clientId, clientRequestId, numberOfPlaces);

		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform an upsert. " + e.getMessage() + ".", e);
		}

		//logger.info("User " + clientId + ":" + clientRequestId + " upserted");
	}

	public void deleteAll() throws BackendException {
		BoundStatement bs = new BoundStatement(DELETE_ALL_FROM_SCREENINGS);

		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a delete operation. " + e.getMessage() + ".", e);
		}

		logger.info("All screenings deleted");
	}



	protected void finalize() {
		try {
			if (session != null) {
				session.getCluster().close();
			}
		} catch (Exception e) {
			logger.error("Could not close existing cluster", e);
		}
	}


}