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
		
		// Notify client that that server is ready for initial message
		writeInt(Message.ACK.ordinal());
		
		while(!isClosed()) {
			// Wait for the client to give the server a command in the form of an integer
			log.debug("Awaiting command from " + getInetAddress() + "...");
			int command = readInt();
			log.debug("Got command [" + command + "] from [" + getInetAddress().getHostName() + "]");
			
			// Client wishes to disconnect from server
			if(command == Message.DISCONNECT.ordinal()) disconnect();
			
			// Client is adding a file to the database
			else if(command == Message.CLIENT_UPLOAD_FILE.ordinal()) {
				readFile(Server.STORAGE.getDownloadPath());
				// Updating server with downloaded file
				Server.STORAGE.update();
			}
			
			// Client is requesting file from database
			else if(command == Message.SERVER_UPLOAD_FILE.ordinal()) {
				writeFile("/mnt/ext500GB/server/muse-hysteria.mp3");
			}
			
			else log.error("Command [" + command + "] is invalid.");
		}
	}
	
	@Override
	public synchronized void disconnect() {
		super.disconnect();
		// Removing the connection from the connection manager's list of connections
		manager.remove(this);
	}
}
