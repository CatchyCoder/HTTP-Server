package music.server;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import music.core.Connection;
import music.core.Track;
import music.core.binarytree.BinaryTree;
import music.core.Command;

/**
 * Manages and consists of three sockets. Contains three inner classes that each utilize one socket.
 * Each inner class <code>implements Runnable</code> and therefore performs operations independent
 * of any other socket stream.
 * @author Clay-
 *
 */
public class Channel {
	
	private static final Logger log = LogManager.getLogger(Channel.class);
	
	private boolean open = true;
	
	/* A single client connection is represented by a Channel. Each
	 * Channel utilizes three independent socket connections, each are on different
	 * servers (using different ports) that are dedicated to a specific use:
	 * 
	 * 1 ADD: client wishes to upload files to server database.
	 * 2 RETRIEVE: client wishes to download files from server database, or 
	 * 	retrieve library information from server database.
	 * 3 STREAM: client wishes to stream files from server database.
	 * 
	 * Each socket will be running on a separate Thread (located in ServerChannel),
	 * allowing each client to upload, download, and stream simultaneously.
	 */
	private final Connection add, retrieve, stream;
	
	public Channel(Socket addSocket, Socket retrieveSocket, Socket streamSocket) {
		// Create the Connection object for each socket. This will configure I/O streams.
		add = new Connection(addSocket);
		retrieve = new Connection(retrieveSocket);
		stream = new Connection(streamSocket);
		
		/*
		 *  Instantiate the inner class and begin it's server code and
		 *  Notify the client it is ready for commands.
		 *  
		 *  This is done with each socket.
		 */
		AddSocketThread thread = new AddSocketThread();
		new Thread(thread).start();
		add.writeCommand(Command.ACK);
		
		RetrieveSocketThread thread2 = new RetrieveSocketThread();
		new Thread(thread2).start();
		retrieve.writeCommand(Command.ACK);
		
		StreamSocketThread thread3 = new StreamSocketThread();
		new Thread(thread3).start();
		stream.writeCommand(Command.ACK);
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
	
	private class AddSocketThread implements Runnable {
		@Override
		public void run() {
			while(open) {
				Command command = add.readCommand();
				log.debug("Recieved " + command + " from " + add.getHostAddress());
				if(command.equals(Command.DATABASE_ADD)) {
					// Store file from client in downloads folder
					add.readFile(Server.STORAGE.getDownloadPath());
					// Add file to database and update search tree
					Server.STORAGE.update();
				}
			}
		}
	}
	
	private class RetrieveSocketThread implements Runnable {
		@Override
		public void run() {
			while(open) {
				Command command = retrieve.readCommand();
				log.debug("Recieved " + command + " from " + retrieve.getHostAddress());
				switch(command) {
				case DATABASE_RETRIEVE:
					// Await Track object from client that identifies the track it wishes to retrieve
					Track track = (Track) retrieve.readObject();
					
					// Grabbing file path from Track object
					BinaryTree tree = Server.STORAGE.getBinaryTree();
					String filePath = tree.find(track).getTrack().getPath();
					
					// Sending file
					retrieve.writeFile(filePath, false);
					break;
				case LIBRARY:
					// Client is requesting the binary tree of Tracks
					retrieve.writeObject(Server.STORAGE.getBinaryTree());
					break;
				default:
					break;
				}
			}
		}
	}

	private class StreamSocketThread implements Runnable {
		@Override
		public void run() {
			while(open) {
				Command command = stream.readCommand();
				log.debug("Recieved " + command + " from " + stream.getHostAddress());
				if(command.equals(Command.DATABASE_STREAM)) {
					// Client is requesting file from database for real-time streaming
					
					// Testing
					stream.writeFile("C:/mnt/ext500GB/server/test.wav", true);
				}
			}
			
		}
	}
	
	/* CLIENT CODE
	public void retrieveFile(String file) {
		// Create a new Thread that will 
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	*/
	
	
}
