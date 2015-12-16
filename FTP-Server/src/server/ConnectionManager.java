package server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionManager {
	
	private static final Logger log = LogManager.getLogger(ConnectionManager.class);
	
	private final Server SERVER;
	private ArrayList<Connection> sockets = new ArrayList<Connection>();
	
	public ConnectionManager(final Server SERVER) {
		this.SERVER = SERVER;
	}
	
	public Connection checkConnections() throws IOException {
		if(sockets.size() == 0) log.debug("Awaiting a connection... ");
		
		// Thread will stay at this line (block) until a connection is successfully established.
		Socket socket = SERVER.getServer().accept();
		
		log.debug("Connection established with [" + socket.getInetAddress().getHostName() + "].");
		
		// This will automatically configure the connection
		Connection connection = new Connection(socket, this);
		
		// Adding to the list of connections
		sockets.add(connection);
		
		return connection;
	}
	
	public void listen() {
		log.debug("Server is up and running.\n");
		try {
			while(Server.isOpen()) {
				// Keep connecting to incoming clients
				checkConnections();
				Thread.sleep(1);
			}
		}
		catch(IOException e) {
			log.error(e);
		} catch (InterruptedException e) {
			log.error(e);
		}
		finally {
			// Disconnect from all clients
			for(Connection connection : sockets) connection.disconnect();
			try {
				// Close server
				SERVER.getServer().close();
				log.debug("Server is closed and has disconnected from all clients.");
			} catch (IOException e) {
				log.error("Error closing server.", e);
			}	
		}
	}
	
	synchronized public void remove(Connection connection) {
		sockets.remove(connection);
	}
}
