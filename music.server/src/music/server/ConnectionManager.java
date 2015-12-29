package music.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionManager {
	
	private static final Logger log = LogManager.getLogger(ConnectionManager.class);
	
	private final Server SERVER;
	private ArrayList<ConnectionImpl> sockets = new ArrayList<ConnectionImpl>();
	
	public ConnectionManager(final Server SERVER) {
		this.SERVER = SERVER;
	}
	
	public ConnectionImpl checkConnections() throws IOException {
		if(sockets.size() == 0) log.debug("Awaiting a connection... ");
		
		// Thread will stay at this line (block) until a connection is successfully established.
		Socket socket = SERVER.getServer().accept();
		
		log.debug("Connection established with [" + socket.getInetAddress().getHostName() + "].");
		
		// This will automatically configure the connection
		ConnectionImpl connection = new ConnectionImpl(socket, this);
		
		// Adding to the list of connections
		sockets.add(connection);
		
		return connection;
	}
	
	public void listen() {
		log.debug("Server is up and running.");
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
			for(ConnectionImpl connection : sockets) connection.disconnect();
			try {
				// Close server
				SERVER.getServer().close();
				log.debug("Server is closed and has disconnected from all clients.");
			} catch (IOException e) {
				log.error("Error closing server.", e);
			}	
		}
	}
	
	public void disconnectAll() {
		for(ConnectionImpl connection: sockets) {
			connection.disconnect();
		}
	}
	
	synchronized public void remove(ConnectionImpl connection) {
		sockets.remove(connection);
	}
}
