package server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Driver {
	
	private static final Logger log = LogManager.getLogger(Driver.class);

	public static void main(String[] args) {
		// Create some space in between server sessions
		log.debug("");
		log.debug("");
		log.debug("");
		log.debug("============| NEW SERVER SESSION |============");
		
		try {
			final int port = 6501;
			final int backlog = 50; // Maximum queue length of incoming connections
			Server server = new Server(port, backlog);
			server.start();
		} catch (IOException e) {
			log.error("Failed to initialize server.", e);
		}
	}
}
