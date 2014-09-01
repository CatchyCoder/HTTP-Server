package server;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
	
	/*
	 * Try to use ByteArrayInputStream for sending messages,
	 * might take up less memory.
	 */
	
	public static boolean isOpen = false;
	
	// The actual 'public server' that everyone can connect to
	public static ServerSocket server;
	
	public Server(int port, int backlog) throws IOException {
		// Setting up the server with a
		// port number of 6789 and backlog of 100
		// (backlog - requested maximum length of the queue of incoming connections)
		// So 100 people can wait on that port
		server = new ServerSocket(port, backlog);
		isOpen = true;
		
		ConnectionManager manager = new ConnectionManager(this);
		new Thread(manager).start();
	}
	
	public static boolean isOpen() {
		return isOpen;
	}
	
	public ServerSocket getServer() {
		return server;
	}
}
