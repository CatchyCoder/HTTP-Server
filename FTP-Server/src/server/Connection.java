package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import command.Command;

public class Connection implements Runnable {
	
	private final Socket socket;
	
	// List for additional input and output streams that are used (besides the basic InputStream/OutputStream).
	// Storing them in a list and closing them in a loop ensures all streams have been closed.
	private final ArrayList<InputStream> inStreams = new ArrayList<InputStream>();
	private final ArrayList<OutputStream> outStreams = new ArrayList<OutputStream>();
	
	private static final Logger log = LogManager.getLogger(Connection.class);
	
	public Connection(Socket socket) {
		this.socket = socket;
		
		try {
			/*
			 * Input streams block until their corresponding output streams have "written and flushed the header".
			 * Therefore output streams are setup first, and then the input streams
			 */
			// Output streams setup
			log.debug("Setting up output streams...");
			OutputStream out = socket.getOutputStream();
			out.flush();
			ObjectOutputStream oOutput = new ObjectOutputStream(out);
			oOutput.flush();
			BufferedOutputStream bOutput = new BufferedOutputStream(out);
			bOutput.flush();
			DataOutputStream dOutput = new DataOutputStream(out);
			dOutput.flush();
			outStreams.add(oOutput);
			outStreams.add(bOutput);
			outStreams.add(dOutput);
			log.debug("Done.");
			
			// Input streams setup
			log.debug("Setting up input streams...");
			InputStream in = socket.getInputStream();
			inStreams.add(new ObjectInputStream(in));
			log.debug("Done.");
			
		} catch (IOException e) {
			log.error("Failed to set up streams with [" + socket.getInetAddress() + "]. Disconnecting.", (Object) e.getStackTrace());
			disconnect();
			return;
		}
		
		new Thread(this).start();
	}
	
	private InputStream getInput(Class<?> classType) {
		for(InputStream stream: inStreams) {
			if(stream.getClass() == classType) return stream;
		}
		log.error("Input stream " + classType + " could not be found");
		return null;
	}
	
	private OutputStream getOutput(Class<?> classType) {
		for(OutputStream stream: outStreams) {
			if(stream.getClass() == classType) return stream;
		}
		log.error("Output stream " + classType + " could not be found");
		return null;
	}
	
	@Override
	public void run () {
		// Keep contacting the client if the server is open
		while(Server.isOpen()) {
			log.debug("test");
			// Wait for the client to give the server a command
			Command command;
			try {
				log.debug("reading command...");
				Object obj = ((ObjectInputStream) getInput(ObjectInputStream.class)).readObject();
				log.debug("got obj now for command.");
				command = (Command) obj;
				log.debug("done.");
			}catch(Exception e) {
				log.error(e);
				// The client didn't send a message, so start the loop
				// and check for the message once again
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					log.error(e1.getStackTrace());
				}
				continue;
			}
			
			log.debug("[" + socket.getInetAddress().getHostName() + "] " + command);
			
			switch(command) {
			case TEST_OBJECT:
				// Client is requesting a test object to ensure a solid connection
				int rand = (int)(Math.random() * 100);
				sendCommand(Command.TEST_OBJECT);
				break;
			case FILE:
				// Client is requesting a test file (song).
				sendFile(new File("/home/clay/git/FTP-Server/FTP-Server/src/server/Aphex Twin - Delphium.mp3"));
				break;
			default:
				log.error("Invalid action: " + command);
			}
		}
	}
	
	private void sendCommand(Command command) {
		final ObjectOutputStream out = (ObjectOutputStream) getOutput(ObjectOutputStream.class);
		try {
			out.flush();
			
			// Send a message in the form of an object to the client
			log.debug("Sending " + command);
			out.writeObject(command);
			out.flush();
			log.debug("Done.");
		} catch (IOException e) {
			log.error("Error sending object [" + command + "] to [" + socket.getInetAddress() + "].", (Object) e.getStackTrace());
		}
	}
	
	public void sendFile(File file) {
		// Making sure the file isn't too big
		long size = file.length();
		
		if(size > Long.MAX_VALUE) log.error("File size too large. " + size + " bytes.");
		else try (
				// For reading/loading the file into RAM
				FileInputStream fInput = new FileInputStream(file);
				BufferedInputStream bInput = new BufferedInputStream(fInput)
			) {
			log.debug("Preparing to send file...");
			// Used for the buffer size
			int bufferSize = 1024 * 8;
			byte[] bytes = new byte[bufferSize];
			
			// For sending the file
			final BufferedOutputStream bOutput = (BufferedOutputStream) getOutput(BufferedOutputStream.class);
			bOutput.flush();
			
			// Sending file size to client
			((DataOutputStream) (getOutput(DataOutputStream.class))).writeLong(size);
			
			log.debug("Sending " + (size / 1000.0) + "kb file...");
			log.debug("==============================================");
			
			// Reading the data with read() and sending it with write()
			// -1 from read() means the end of stream (no more bytes to read)
			for(int count; (count = bInput.read(bytes)) >= 0;) {
				// count is the number of bytes to write,
				// 0 is the offset
				// bytes is the actual data to write
				bOutput.write(bytes, 0, count);
				log.debug("Sent " + count + " bytes.");
			}
			bOutput.flush();
			
			log.debug("==============================================");
			log.debug("Done.");
			
		} catch (FileNotFoundException e) {
			log.error(e.getStackTrace());
		} catch (IOException e) {
			log.error(e.getStackTrace());
		}
	}
	
	public void disconnect() {
		// Storing address before ending connection
		InetAddress address = socket.getInetAddress();
		
		log.debug("Closing input streams...");
		for(InputStream stream: inStreams) {
			try {
				stream.close();
			} catch (IOException e) {
				log.error(e.getStackTrace());
			}
		}
		log.debug("Done.");
		
		log.debug("Closing output streams...");
		for(OutputStream stream: outStreams) {
			try {
				stream.close();
			} catch (IOException e) {
				log.error(e.getStackTrace());
			}
		}
		log.debug("Done.");
		
		log.debug("Ending connection with [" + address + "]...");
		try {
			// Close the socket and its associated input/output streams
			socket.close();
		}
		catch(IOException e) {
			log.error(e.getStackTrace());
		}
		log.debug("Done.");
	}
}
