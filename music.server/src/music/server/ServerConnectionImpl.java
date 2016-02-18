package music.server;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerConnectionImpl extends ServerConnection {
	
	private static final Logger log = LogManager.getLogger(ServerConnectionImpl.class);
	
	public ServerConnectionImpl(Socket socket, ConnectionManager manager, Storage storage) {
		super(socket, manager, storage);
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
			
			final Message[] vals = Message.values();
			
			switch(vals[command]) {
			case DISCONNECT:
				disconnect();
				break;
			case LIBRARY:
				// Client is requesting the binary tree of Tracks
				writeTree(STORAGE.getBinaryTree());
				break;
			case DATABASE_RETRIEVE:
				// Client is requesting file from database
				writeFile("/mnt/ext500GB/server/muse-hysteria.mp3");
				break;
			case DATABASE_ADD:
				// Client is adding a file to the database
				readFile(Server.STORAGE.getDownloadPath());
				// Updating server with new track file
				Server.STORAGE.update();
				break;
			default:
				log.error("Command [" + command + "] is invalid.");
			}
		}
	}
}
