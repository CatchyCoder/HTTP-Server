package music.server;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {
	
	private static final Logger log = LogManager.getLogger(Server.class);
	
	public static boolean isOpen = false;
	
	public static ServerSocket server;
	private final ConnectionManager MANAGER;
	public static Storage STORAGE;
	
	public Server(final int port, final int backlog) throws IOException {
		// Loads the database and binary search tree
		STORAGE = new Storage();
		// Setting up server
		server = new ServerSocket(port, backlog);
		// Setting up the connection manager which handles connections
		MANAGER = new ConnectionManager(this);
		
		isOpen = true;
		log.debug("Server setup is complete.");
	}
	
	public void start() {
		MANAGER.listen();
	}
	
	public void stop() {
		MANAGER.disconnectAll();
	}
	
	public static boolean isOpen() {
		return isOpen;
	}
	
	public ServerSocket getServer() {
		return server;
	}
}
