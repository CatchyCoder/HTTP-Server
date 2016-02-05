package music.server;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import music.core.AbstractConnection;

public class ConnectionImpl extends AbstractConnection implements Runnable {
	
	private static final Logger log = LogManager.getLogger(ConnectionImpl.class);
	
	private ConnectionManager manager;
	
	public ConnectionImpl(Socket socket, ConnectionManager manager) {
		super(socket);
		this.manager = manager;
		
		// Begin session
		new Thread(this).start();
	}
	
	@Override
	public void run () {
		// Just keep checking if the socket has been closed, if not then it must be open.
		// If the client asks to disconnect (or if anything goes wrong), disconnect() will be called
		// and isClosed() should return true - terminating the loop.
		while(!isClosed()) {
			// Wait for the client to give the server a command in the form of an integer
			log.debug("Awaiting command from " + getInetAddress() + "...");
			int command = readInt();
			
			log.debug("Got command [" + command + "] from [" + getInetAddress().getHostName() + "]");
			
			switch(command) {
			case 0:
				// Client wishes to disconnect from server
				disconnect();
				break;
			case 1: // Client is requesting a test integer to ensure a solid connection
				int rand = (int)(Math.random() * 100) + 1;
				sendInt(rand);
				break;
			case 2: // Client is requesting a test file.
				sendFile("/mnt/ext500GB/server/muse-hysteria.mp3");
				break;
			case 3: // Client is adding a file to the database
				downloadFile(Server.STORAGE.getDownloadPath());
				// Updating server with downloaded file
				Server.STORAGE.update();
				break;
			case 4:
				// Client is requesting file from database
				
				// TODO: Make a call to Storage to retrieve file
				
				break;
			case 5:
				// Client is requesting a list of the contents in the database
				
				// TODO: Make a call to Storage to compile list
				
				break;
			default:
				log.error("Command [" + command + "] is invalid.");
			}
		}
	}
	
	@Override
	public synchronized void disconnect() {
		super.disconnect();
		// Removing the connection from the connection manager's list of connections
		manager.remove(this);
	}
}
