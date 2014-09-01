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

public class Connection implements Runnable {
	
	private final Socket socket;
	
	// For simple communication with the client
	private ObjectInputStream input;
	private ObjectOutputStream output;
	
	public Connection(Socket socket) {
		this.socket = socket;
		
		try {
			// Establish a connection and begin communicating with the client
			setupStreams();
			System.out.println("Setup is now finished.\n");
			new Thread(this).start();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void setupStreams() throws IOException {
		// Get streams to send/receive data
		System.out.print("Setting up streams... ");
		
		input = new ObjectInputStream(socket.getInputStream());
		output = new ObjectOutputStream(socket.getOutputStream());
		output.flush();
		
		System.out.println("Done");
	}
	
	public void run () {
		// Keep contacting the client if the server is open
		while(Server.isOpen()) {
			try {
				Thread.sleep(10000);
				// Wait for the client to give the server a command
				//System.out.println("Awaiting message...");
				int message = (Integer) input.readObject();
				
				System.out.println("[" + socket.getInetAddress().getHostName() + "] " + message);
				
				switch(message) {
				case 0:
					//send(0);
					break;
				case 1:
					//send(1);
					break;
				default:
					System.out.println("Invalid action");
				}
			}
			catch(ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// Tell the client that the server has closed
		//send(-1);
	}
	
	private void send(int message) {
		try {
			// Send a message to the client
			output.writeObject(new Integer(message));
			output.flush();			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendFile(File file) {
		
		// Making sure the file isn't too big
		long size = file.length();
		
		if(size > Integer.MAX_VALUE)
			System.err.println("File size too large.");
		
		else try {
			System.out.println("Preparing to send file...");
			// Used for the buffer size
			byte[] bytes = new byte[(int) size];
			
			// For loading the file into RAM
			FileInputStream fInput = new FileInputStream(file);
			BufferedInputStream bInput = new BufferedInputStream(fInput);
			
			// For sending the loaded RAM
			BufferedOutputStream bOutput = new BufferedOutputStream(socket.getOutputStream());
			bOutput.flush();
			
			System.out.println("Sending file...");
			
			// Reading the data with read() and sending it with write()
			// -1 means the end of stream (no more bytes to read)
			for(int count; (count = bInput.read(bytes)) >= 0;) {
				// count is the number of bytes to write,
				// 0 is the offset
				// bytes is the actual data to write
				output.write(bytes, 0, count);
				System.out.println(count + " bytes sent.");
			}
			
			fInput.close();
			bInput.close();
			bOutput.close();
			
			System.out.println("File sent. Hurray!!!");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		// Close streams and sockets
		System.out.println("Ending connection...");
		try {
			input.close();
			output.close();
			socket.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		System.out.println("Connection ended.\n==========================\n");
	}
}
