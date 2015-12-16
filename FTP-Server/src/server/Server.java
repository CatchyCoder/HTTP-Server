package server;

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
		// Setting up server, the external hard drive it uses, and then the
		// server's connection manager which handles accepting and canceling connections.
		server = new ServerSocket(port, backlog);
		isOpen = true;
		
		STORAGE = new Storage();
		
		// TEST
		STORAGE.sortFiles();
		log.debug("Done sorting.");
		// END TEST
		
		MANAGER = new ConnectionManager(this);
	}
	
	public void start() {
		MANAGER.listen();
	}
	
	public static boolean isOpen() {
		return isOpen;
	}
	
	public ServerSocket getServer() {
		return server;
	}
}
