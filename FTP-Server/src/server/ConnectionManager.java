package server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionManager implements Runnable {
	
	private Server server;
	private ArrayList<Connection> sockets = new ArrayList<Connection>();
	
	private static final Logger log = LogManager.getLogger(ConnectionManager.class);
	
	public ConnectionManager(Server server) {
		this.server = server;
	}
	
	public Connection checkConnections() throws IOException {
		if(sockets.size() == 0) log.debug("Awaiting a connection... ");
		
		// Thread will stay at this line until a connection is successfully established.
		Socket socket = server.getServer().accept();
		
		// This will automatically configure the connection
		Connection connection = new Connection(socket);
		
		// Adding to the list of connections
		sockets.add(connection);
		
		log.debug("Connection established with [" + socket.getInetAddress().getHostName() + "].");
		
		return connection;
	}
	
	@Override
	public void run() {
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
				server.getServer().close();
			} catch (IOException e) {
				log.error(e);
			}
			log.debug("Server is closed and has disconnected from all clients.");
		}
	}
	
	public ArrayList<Connection> getConnections() {
		return sockets;
	}
}
