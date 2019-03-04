package cassdemo;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import cassdemo.backend.BackendException;
import cassdemo.backend.BackendSession;

class MyThread implements Runnable {
	BackendSession session;
	int id;
	Settings settings;


	MyThread(BackendSession sess, int pId, Settings order) {
		session = sess;
		id = pId;
		settings=order;
	}

	@Override
	public void run() {
						//jakaś pętla z biletami lub jeden wątek, jedno żądanie
		try {
			Random rand = new Random();
			Order order = new Order();
			order.hall=settings.hall;
			order.clientId=id;
			order.clientRequestId=0; 
			order.numberOfSeats=rand.nextInt(10) + 1;
			int result = session.getTicket(settings, order);
			Thread.sleep(1000);

			int finalResult = session.checkTicket(settings, order, result);

			if(result==1 && finalResult==1) {System.out.println(order.clientId + "\t is happy \t" + order.numberOfSeats);}
			if(result==0 && finalResult==0) {System.out.println(order.clientId + "\t is sad \t" + order.numberOfSeats);}
			if(result==1 && finalResult==0) {System.out.println(order.clientId + "\t is unlucky \t" + order.numberOfSeats);}
			if(result==0 && finalResult==1) {System.out.println(order.clientId + "\t is lucky \t" + order.numberOfSeats );}
			//na podstawie resultów dodaje do odpowiedniej grupy wynikowej


		} catch (BackendException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

public class Main {

	private static final String PROPERTIES_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, BackendException {
		String contactPoint = null;
		String keyspace = null;

		Properties properties = new Properties();
		try {
			properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));

			contactPoint = properties.getProperty("contact_point");
			keyspace = properties.getProperty("keyspace");
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		BackendSession session = new BackendSession(contactPoint, keyspace);

		String hallName = "Hall31";
		int hallSize = 200;
		int hallRowSize = 20;
		int hallRowNumber = 10;
		int guests = 100;

		Thread[] ts = new Thread[guests];
		for (int i = 0; i < guests; i++) {
			Settings settings = new Settings();
			settings.hall=hallName;
			settings.clientId=i;
			settings.hallSize=hallSize;
			settings.hallRowSize=hallRowSize;
			settings.hallRowNumber=hallRowNumber;

			(ts[i] = new Thread(new MyThread(session, i, settings))).start();
		}
		for (int i = 0; i < guests; i++) {
			try {
				ts[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println(session.counter.get());
		//System.exit(0);
	}
}