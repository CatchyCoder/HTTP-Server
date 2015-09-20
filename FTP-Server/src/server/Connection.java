package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Connection implements Runnable {
	
	private final Socket socket;
	
	// For simple communication with the client
	private ObjectInputStream input;
	private ObjectOutputStream output;
	
	private static final Logger log = LogManager.getLogger(Connection.class);
	
	public Connection(Socket socket) {
		this.socket = socket;
		
		try {
			// Establish a connection and begin communicating with the client
			setupStreams();
			new Thread(this).start();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setupStreams() throws IOException {
		// Get streams to send/receive data
		log.debug("Setting up streams... ");
		
		output = new ObjectOutputStream(socket.getOutputStream());
		output.flush();
		input = new ObjectInputStream(socket.getInputStream());
		
		log.debug("Done");
	}
	
	public void run () {
		// Keep contacting the client if the server is open
		while(Server.isOpen()) {
			try {
				
				/*
				 * Maybe later on detect if there is a
				 * connection problem and automatically reconnect
				 * to the server. Detect by using specific Exceptions.
				 * Might need them in multiple places.
				 */
				
				
				Server.isOpen = false;
				if(true) return;
				
				System.err.println("\nSHOULD NOT PRINT\n");
				
				int message = -1;
				try {
					// Wait for the client to give the server a command
					message = (Integer) input.readObject();
				}
				catch(Exception e) {
					e.printStackTrace();
					Thread.sleep(50);
					System.err.println("\nAn error occurred");
				}
				
				System.out.println("[" + socket.getInetAddress().getHostName() + "] " + message);
				
				switch(message) {
				case 0:
					// Send music library info to client
					String[][] songs = {
							{"id", "artist", "album", "song"},
							{"id2", "artist2", "album2", "song2"}
					};
					
					output.writeObject(songs);
					output.flush();
					break;
				case 1:
					// Receive and ID number for the file the client wants
					int songID = (Integer) input.readObject();
					System.out.println("songID:" + songID);
					
					// Now send the client the song
					sendFile(new File("C:/Users/Clay/Music/Aphex Twin - Delphium.mp3"));
					Server.isOpen = false;
					//send(1);
					break;
				default:
					System.out.println("Invalid action");
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Tell the client that the server has closed
		//send(-1);
	}
	
	private void send(Object object) {
		try {
			// Send a message to the client
			output.writeObject(object);
			output.flush();			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendFile(File file) {
		
		// Making sure the file isn't too big
		long size = file.length();
		
		if(size > Long.MAX_VALUE)
			log.error("File size too large.");
		
		else try {
			log.debug("Preparing to send file...");
			// Used for the buffer size
			byte[] bytes = new byte[(int) size];
			
			// For reading/loading the file into RAM
			FileInputStream fInput = new FileInputStream(file);
			BufferedInputStream bInput = new BufferedInputStream(fInput);
			
			// For sending the file loaded into RAM
			BufferedOutputStream bOutput = new BufferedOutputStream(socket.getOutputStream());
			
			log.debug("Sending file...");
			
			// Reading the data with read() and sending it with write()
			// -1 means the end of stream (no more bytes to read)
			for(int count; (count = bInput.read(bytes)) > -1;) {
				
				// count is the number of bytes to write,
				// 0 is the offset
				// bytes is the actual data to write
				bOutput.write(bytes, 0, count);
				log.debug(count + " bytes sent.");
			}
			
			fInput.close();
			bInput.close();
			bOutput.close();
			
			log.debug("File sent. Hurray!!!");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		// Close streams and sockets
		log.debug("Ending connection... ");
		try {
			socket.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		log.debug("Done.");
	}
}
