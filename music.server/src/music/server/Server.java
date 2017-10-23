package music.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import music.core.Storage;

/**
 * Representation of the music server. Contains all three server sockets and acts as the entire server.
 * Server handles every connection, each connection is represented by a <code>ServerChannel</code>. Each
 * <code>ServerChannel</code> is created and terminated here.
 * 
 * @author Clay Kuznia
 */
public class Server {
	private static final Logger log = LogManager.getLogger(Server.class);
	
	// Whether the server is actively listening for incoming connections
	private static boolean open;
	
	// Loads the database and binary search tree
	public static final Storage STORAGE = new Storage("/mnt/ext500GB/server", "/database", "/download");
	
	// Three ServerSockets are used in order for clients to use different functions simultaneously
	public final ServerSocket ADD, RETRIEVE, STREAM;
	
	// A list of Channels that represent all currently connected clients
	private ArrayList<Channel> channels = new ArrayList<Channel>();
	
	/**
	 * Creates a server on the specified port using the specified backlog.
	 * Each server contains three server sockets. The first socket will use the port specified
	 * and each additional socket will use the previous port + 1.
	 * 
	 * @param port the first port to use. Ports used will range from <code>port</code> to <code>port + 2</code>
	 * @param backlog maximum length of queue for incoming connections for each socket
	 * @throws IOException thrown if any socket fails to initialize
	 */
	public Server(final int port, final int backlog) throws IOException {
		// Setting up server sockets
		ADD = new ServerSocket(port, backlog);
		RETRIEVE = new ServerSocket(port + 1, backlog);
		STREAM = new ServerSocket(port + 2, backlog);
		
		log.debug("---[Server setup complete]---");
	}
	
	/**
	 * Constantly checks for any client wishing to connect.
	 * 
	 * Standard protocol is for both client and server to connect on the ADD, RETRIEVE, and STREAM
	 * sockets respectively. In order for the next socket to connect the previous socket must accept
	 * it's connection request.
	 * 
	 * @return a <code>Channel</code> that represents the full client to server connection.
	 * @throws IOException
	 */
	public Channel checkConnections() throws IOException {
		// Thread will stay at these lines (block) until all connections are successfully established.
		Socket addSocket = ADD.accept();
		Socket retrieveSocket = RETRIEVE.accept();
		Socket streamSocket = STREAM.accept();
		
		// Ensure that all three sockets are coming from the same address.
		String address1 = addSocket.getInetAddress().getHostAddress();
		String address2 = retrieveSocket.getInetAddress().getHostAddress();
		String address3 = streamSocket.getInetAddress().getHostAddress();
		if(!address1.equals(address2) || !address2.equals(address3)) {
			log.error("All sockets were accepted but not all addresses were equal. Refusing connection.");
			addSocket.close();
			retrieveSocket.close();
			streamSocket.close();
			return null;
		}
		
		// Creating a ServerChannel that represents all three sockets as one client connection
		Channel channel = new Channel(addSocket, retrieveSocket, streamSocket);
		// Adding to list of connections
		channels.add(channel);
		
		InetAddress address = addSocket.getInetAddress();
		int port1 = addSocket.getPort();
		int port2 = retrieveSocket.getPort();
		int port3 = streamSocket.getPort();
		log.debug("Connection established [" + address + " Ports: " + port1 + ", " + port2 + ", " + port3 + "]");
		
		return channel;
	}
	
	/**
	 * Disconnects server from all currently connected clients. This does NOT
	 * affect the value of <code>music.server.Server.open</code>. However setting the
	 * value of <code>music.server.Server.open</code> to <code>false</code> results
	 * in <code>disconnectAll()</code> being called.
	 */
	public void disconnectAll() {
		// Fetch a copied list of connected channels
		ArrayList<Channel> copy = getChannels();
		for(Channel channel: copy) {
			channel.disconnect();
			// Using the copy avoids a concurrency exception
			channels.remove(channel);
		}
	}
	
	/**
	 * Creates a copy of a list of currently connected clients and returns the copy.
	 * 
	 * @return an <code>ArrayList</code> of <code>Channel</code> s currently connected at the time of calling.
	 * 
	 */
	private ArrayList<Channel> getChannels() {
		ArrayList<Channel> copy = new ArrayList<Channel>();
		for(Channel channel: channels) {
			copy.add(channel);
		}
		return copy;
	}
	
	/**
	 * This method will block upon calling until <code>music.server.Server.open</code> is set to <code>false</code>.
	 * Once <code>false</code> the method then disconnects from all clients using <code>disconnectAll()</code>, 
	 * and subsequently closes all <code>ServerSocket</code>s.
	 */
	public void run() {
		try {
			log.debug("Awaiting a connection... ");
			while(open) {
				// Keep connecting to incoming clients
				checkConnections();
				Thread.sleep(1);
			}
		} catch(IOException e) {
			log.error(e);
		} catch (InterruptedException e) {
			log.error(e);
		} finally {
			// Server is no longer listening for incoming connections, disconnect from all clients
			disconnectAll();
			log.debug("Server is closed and has disconnected from all clients.");
			try {
				// Close servers
				ADD.close();
				RETRIEVE.close();
				STREAM.close();
			} catch (IOException e) {
				log.error("Error closing servers.", e);
			}	
		}
	}
}
