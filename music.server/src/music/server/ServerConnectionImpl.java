package music.server;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import music.core.Storage;
import music.core.Track;
import music.core.binarytree.BinaryTree;

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
			log.debug("Awaiting command from " + getInetAddress().getHostName() + "...");
			int command = readInt();
			final Message[] vals = Message.values();
			log.debug("Recieved " + vals[command] + " from " + getInetAddress().getHostName());
			
			switch(vals[command]) {
			case DISCONNECT:
				disconnect();
				break;
			case LIBRARY:
				// Client is requesting the binary tree of Tracks
				writeObject(STORAGE.getBinaryTree());
				break;
			case DATABASE_RETRIEVE:
				// Client is requesting file from database
				//retrieve();
				
				// Testing
				writeFile("C:/mnt/ext500GB/server/test.wav");
				
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
	
	private void retrieve() {
		// Await Track object from client that identifies the track it wishes to retrieve
		Track track = (Track) readObject();
		
		// Grabbing file path from Track object
		BinaryTree tree = STORAGE.getBinaryTree();
		String filePath = tree.find(track).getTrack().getPath();
		
		// Sending file
		writeFile(filePath);
	}
}
