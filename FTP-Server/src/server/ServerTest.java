package server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerTest {
	
	private static final Logger log = LogManager.getLogger(ServerTest.class);

	public static void main(String[] args) {
		try {
			// Setting up the server with a
			// port number of 6501 and backlog of 50
			// (backlog - requested maximum length of the queue of incoming connections)
			// So 50 people can wait on that port
			new Server(6501, 50);
		} catch (IOException e) {
			log.error(e);
		}
	}
}
