package music.server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerDriver {

	private static final Logger log = LogManager.getLogger(ServerDriver.class);

	public static void main(String[] args) {
		// Create white space in between server sessions for readability
		log.debug("\n\n\n");
		log.debug("============| NEW SERVER SESSION |============");
		
		try {
			final int port = 6501;
			final int backlog = 50; // Maximum queue length of incoming connections
			Server server = new Server(port, backlog);
			log.debug("RUNNING SERVER:");
			server.run();
		} catch (IOException e) {
			log.error("Failed to initialize server.", e);
		}
	}
}
