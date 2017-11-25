package music.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import music.core.Storage;

/**
 * Representation of the music server. Contains all server sockets and acts as the entire server.
 * Server handles every connection, each connection is represented by a <code>Session</code>. Each
 * <code>Session</code> is created and may be terminated here.
 * 
 * @author Clay Kuznia
 */
public class Server {
	private static final Logger log = LogManager.getLogger(Server.class);
	
	// Whether the server is actively listening for incoming connections
	private static boolean open;
	
	// Loads the database and binary search tree
	public static final Storage STORAGE = new Storage("/mnt/ext500GB/server", "/database", "/download");
	
	// Multiple ServerSockets are used in order for clients to use different functions simultaneously
	public final ServerSocket COMM, ADD, RETRIEVE, STREAM;
	
	// A list of Sessions that represent all currently connected clients
	private ArrayList<Session> sessions = new ArrayList<Session>();
	
	/**
	 * Creates a server containing multiple ServerSockets using the specified ports.
	 * Each port is used for the COMM, ADD, RETRIEVE, and STREAM sockets respectively.
	 * 
	 * @param commPort port used for COMM ServerSocket
	 * @param addPort port used for ADD ServerSocket
	 * @param retrievePort port used for RETRIEVE ServerSocket
	 * @param streamPort port used for STREAM ServerSocket
	 * @param backlog maximum length of queue for incoming connections for each socket
	 * @throws IOException thrown if any socket failed to initialize
	 */
	public Server(int commPort, int addPort, int retrievePort, int streamPort, int backlog) throws IOException {
		// Setting up server sockets
		COMM = new ServerSocket(commPort, backlog);
		ADD = new ServerSocket(addPort, backlog);
		RETRIEVE = new ServerSocket(retrievePort, backlog);
		STREAM = new ServerSocket(streamPort, backlog);
		log.debug("---[Server setup complete]---");
	}
	
	/**
	 * Disconnects server from all currently connected clients. This does NOT
	 * affect the value of <code>music.server.Server.open</code>. However setting the
	 * value of <code>music.server.Server.open</code> to <code>false</code> results
	 * in <code>disconnectAll()</code> being called.
	 */
	public void disconnectAll() {
		// Fetch a copied list of connected sessions
		ArrayList<Session> copy = getSessions();
		for(Session session: copy) {
			session.disconnect();
			// Using the copy avoids a concurrency exception
			sessions.remove(session);
		}
	}
	
	/**
	 * Creates a copy of a list of currently connected clients (in the form of Sessions) and returns the copy.
	 * 
	 * @return an <code>ArrayList</code> of <code>Session</code> s currently connected at the time of calling.
	 * 
	 */
	private ArrayList<Session> getSessions() {
		ArrayList<Session> copy = new ArrayList<Session>();
		for(Session session: sessions) {
			copy.add(session);
		}
		return copy;
	}
	
	/**
	 * This method creates multiple threads that will run until <code>music.server.Server.open</code> is set to <code>false</code>.
	 * Once <code>false</code> the method then disconnects from all clients using <code>disconnectAll()</code>, 
	 * and subsequently closes all <code>ServerSocket</code>s.
	 */
	public void run() {
		// Start COMM ServerSocket thread
		new Thread(new Runnable() {
			public void run() {
				try {
					while(open) {
						// Thread will stay at this line (block) until the connection is successfully established.
						Socket commSocket = COMM.accept();
						// Creating a Session that represents the full client to server connection
						Session session = new Session(commSocket);
						// Adding to list of sessions
						sessions.add(session);
						log.debug("Connection established with " + commSocket.getInetAddress() + " on " + commSocket.getPort());
					}
				} catch (IOException e) {
					log.error(e);
				} finally {
					// Server is no longer listening for incoming connections, disconnect from all clients
					disconnectAll();
					try {
						// Close servers
						ADD.close();
						RETRIEVE.close();
						STREAM.close();
					} catch (IOException e) {
						log.error("Error closing servers.", e);
					}
					log.debug("Server is closed and has disconnected from all clients.");
				}
			}
		}).start();
		
		// Start ADD ServerSocket thread
		new Thread(new Runnable() {
			public void run() {
				try {
					while(open) {
						// Thread will stay at this line (block) until the connection is successfully established.
						Socket addSocket = ADD.accept();
						
						// Connection is part of an existing session, find that session and add
						// the connection to the session.
						String address = addSocket.getInetAddress().getHostAddress();
						for(Session session : sessions) {
							if(session.getAddress().equals(address)) {
								session.setAddSocket(addSocket);
							}
						}
						log.debug("Connection established with " + addSocket.getInetAddress() + " on " + addSocket.getPort());
					}
				} catch(IOException e) {
					log.error(e);
				}
			}
		}).start();
		
		// Start RETRIEVE ServerSocket thread
		new Thread(new Runnable() {
			public void run() {
				try {
					while(open) {
						// Thread will stay at this line (block) until the connection is successfully established.
						Socket retrieveSocket = RETRIEVE.accept();
						
						// Connection is part of an existing session, find that session and add
						// the connection to the session.
						String address = retrieveSocket.getInetAddress().getHostAddress();
						for(Session session : sessions) {
							if(session.getAddress().equals(address)) {
								session.setRetrieveSocket(retrieveSocket);
							}
						}
						log.debug("Connection established with " + retrieveSocket.getInetAddress() + " on " + retrieveSocket.getPort());
					}
				} catch(IOException e) {
					log.error(e);
				}
			}
		}).start();
		
		// Start STREAM ServerSocket thread
		new Thread(new Runnable() {
			public void run() {
				try {
					while(open) {
						// Thread will stay at this line (block) until the connection is successfully established.
						Socket streamSocket = STREAM.accept();
						
						// Connection is part of an existing session, find that session and add
						// the connection to the session.
						String address = streamSocket.getInetAddress().getHostAddress();
						for(Session session : sessions) {
							if(session.getAddress().equals(address)) {
								session.setStreamSocket(streamSocket);
							}
						}
						log.debug("Connection established with " + streamSocket.getInetAddress() + " on " + streamSocket.getPort());
					}
				} catch(IOException e) {
					log.error(e);
				}
			}
		}).start();
	}
}
