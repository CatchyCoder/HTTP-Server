package music.server;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import music.core.Connection;
import music.core.Track;
import music.core.binarytree.BinaryTree;
import music.core.Command;

/**
 * Represents the full client to server connection. A session may consist of multiple sockets, each of which
 * are created and destroyed depending on the clients service requests. Multiple socket connections are used
 * in order to handle multiple services simultaneously between the client and server.
 * 
 * @author Clay Kuznia
 */
public class Session implements Runnable {
	
	private static final Logger log = LogManager.getLogger(Session.class);
	
	private boolean open = true;
	
	/* A single client connection is represented by a Session. Each
	 * Session utilizes four independent socket connections, each are on different
	 * servers (using different ports) that are dedicated to a specific use:
	 * 
	 * 1 COMM: For general communication/commands between client and server
	 * 1 ADD: For uploading files to server database.
	 * 2 RETRIEVE: For downloading files from the server database, or 
	 * 	retrieving library information from server database.
	 * 3 STREAM: For streaming files from server database.
	 * 
	 * Each socket will be running on a separate Thread,
	 * allowing each client to upload, download, and stream simultaneously.
	 * 
	 * The COMM socket is always connected until the session is terminated, but the 
	 * remaining three sockets are created and destroyed dynamically to adjust to what
	 * services the client is currently using.
	 */
	private final Connection COMM;
	private Connection add, retrieve, stream;
	
	public Session(final Socket commSocket) {
		// Create the Connection object for the main COMM socket. This will configure I/O streams.
		COMM = new Connection(commSocket);
		add = retrieve = stream = null;
		
		// Notify client that server is ready to receive commands by sending
		// an ACK.
		COMM.writeCommand(Command.ACK);
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		while(open) {
			Command command = COMM.readCommand();
			log.debug("Recieved " + command + " from " + COMM.getHostAddress());
			switch(command) {
			case DATABASE_RETRIEVE:
				// Await Track object from client that identifies the track it wishes to retrieve
				Track track = (Track) COMM.readObject();
				
				// Grabbing file path from Track object
				BinaryTree tree = Server.STORAGE.getBinaryTree();
				String filePath = tree.find(track).getTrack().getPath();
				
				// Wait for retrieve socket to be non-null. This will
				// happen when the client connects to the correct ServerSocket/port
				// and the server then updates the Session's retrieve socket to a non-null
				// value.
				while(retrieve == null) {
					// Do nothing
					try {Thread.sleep(1);}
					catch(Exception e) {log.error(e);}
				}
				
				// Sending file
				retrieve.writeFile(filePath, false);
				break;
			case LIBRARY:
				// Client is requesting the binary tree of Tracks
				COMM.writeObject(Server.STORAGE.getBinaryTree());
				break;
			case DATABASE_STREAM:
				// Client is requesting file from database for real-time streaming
				
				// Testing
				stream.writeFile("C:/mnt/ext500GB/server/test.wav", true);
				break;
				
			case DATABASE_ADD:
				// Wait for add socket to be non-null. This will
				// happen when the client connects to the correct ServerSocket/port
				// and the server then updates the Session's retrieve socket to a non-null
				// value.
				while(add == null) {
					// Do nothing
					try {Thread.sleep(1);}
					catch(Exception e) {log.error(e);}
				}
				
				// Store file from client in downloads folder
				add.readFile(Server.STORAGE.getDownloadPath());
				// Add file to database and update search tree
				Server.STORAGE.update();
				break;
			default:
				log.error("INVALID COMMAND");
				break;
			}
		}
	}
	
	public void disconnect() {
		add.disconnect();
		retrieve.disconnect();
		stream.disconnect();
	}
	
	public void setOpen(boolean value) {
		open = value;
	}
	
	public boolean open() {
		return open;
	}
	
	public String getAddress() {
		return COMM.getHostAddress();
	}
	
	public void setAddSocket(Socket socket) {
		add = new Connection(socket);
	}
	
	public void setRetrieveSocket(Socket socket) {
		retrieve = new Connection(socket);
	}
	
	public void setStreamSocket(Socket socket) {
		stream = new Connection(socket);
	}
}
