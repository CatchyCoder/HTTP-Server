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
