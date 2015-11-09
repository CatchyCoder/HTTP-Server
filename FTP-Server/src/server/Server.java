package server;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;

public class Server {
	
	private static final Logger log = LogManager.getLogger(Server.class);
	
	public static boolean isOpen = false;
	
	// The actual 'public server' that everyone can connect to
	public static ServerSocket server;
	public static Storage STORAGE;
	
	public Server(int port, int backlog) throws IOException {
		server = new ServerSocket(port, backlog);
		isOpen = true;
		
		STORAGE = new Storage();
		
		STORAGE.sortFiles();
		log.debug("done sorting");
		
		new ConnectionManager(this);
		
		// TESTING...
		try {Thread.sleep(5000);}
		catch(Exception e){}
		
	}
	
	public static boolean isOpen() {
		return isOpen;
	}
	
	public ServerSocket getServer() {
		return server;
	}
}
