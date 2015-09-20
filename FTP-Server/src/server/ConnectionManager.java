package server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionManager implements Runnable {
	
	private Server server;
	private ArrayList<Connection> sockets = new ArrayList<Connection>();
	
	public ConnectionManager(Server server) {
		this.server = server;
	}
	
	public Connection checkConnections() throws IOException {
		if(sockets.size() == 0) System.out.print("Awaiting a connection... ");
		// Thread will stay at this line until a connection is
		// successfully established.
		Socket socket = server.getServer().accept();
		
		// This will automatically configure the connection
		Connection connection = new Connection(socket);
		
		// Adding to the list of connections
		sockets.add(connection);
		
		System.out.println("Connection found with " + socket.getInetAddress().getHostName() + ".");
		
		return connection;
	}
	
	@Override
	public void run() {
		System.out.println("Server is up and running.\n");
		try {
			while(Server.isOpen()) {
				// Keep connecting to incoming clients
				checkConnections();
				Thread.sleep(1);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			// Server has closed, disconnect from all clients
			for(Connection connection : sockets) connection.disconnect();
		}
	}
	
	public ArrayList<Connection> getConnections() {
		return sockets;
	}
}
