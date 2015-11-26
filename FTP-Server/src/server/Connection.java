package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public class Connection implements Runnable {
	
	private static final Logger log = LogManager.getLogger(Connection.class);
	
	private final Socket socket;
	
	// List for additional input and output streams that are used (besides the basic InputStream/OutputStream).
	// Storing them in a list and closing them in a loop ensures all streams have been closed.
	private final ArrayList<InputStream> inStreams = new ArrayList<InputStream>();
	private final ArrayList<OutputStream> outStreams = new ArrayList<OutputStream>();
	
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
			inStreams.add(new DataInputStream(in));
			log.debug("Done.");
			
		} catch (IOException e) {
			log.error("Failed to set up streams with [" + socket.getInetAddress() + "]. Disconnecting.", e);
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
			// Wait for the client to give the server a command in the form of an integer
			log.debug("Awaiting command from a client...");
			int clientCommand = -1;
			try {
				clientCommand = ((DataInputStream) getInput(DataInputStream.class)).readInt();
			} catch (IOException e) {
				log.error("IO error when awaiting client's message.", e);
			}
			
			log.debug("[" + socket.getInetAddress().getHostName() + "]: " + clientCommand);
			
			switch(clientCommand) {
			case 0: // Client is requesting a test integer to ensure a solid connection
				int rand = (int)(Math.random() * 100) + 1;
				sendInt(rand);
				break;
			case 1: // Client is requesting a test file (song).
				if(sendFile("/home/pi/muse-hysteria.mp3")) {
					log.debug("File sent successfully.");
				}
				else {
					log.error("Error sending file.");
				}
				break;
			default:
				log.error("Invalid command: " + clientCommand);
			}
		}
	}
	
	private void sendInt(int n) {
		final DataOutputStream out = (DataOutputStream) getOutput(DataOutputStream.class);
		try {
			out.flush();
			
			// Send a message in the form of an integer to the client
			log.debug("Sending integer: " + n);
			out.writeInt(n);
			out.flush();
			log.debug("Done.");
		} catch (IOException e) {
			log.error("Error sending integer [" + n + "] to [" + socket.getInetAddress() + "].", e);
		}
	}
	
	private boolean downloadFile() {
		log.debug("Setting up file streams (for storing file)...");
		try(
			// For storing the incoming file (saving)
			FileOutputStream fOutput = new FileOutputStream(Server.STORAGE.getDownloadPath());
			BufferedOutputStream bOutput = new BufferedOutputStream(fOutput)
		) {
			fOutput.flush();
			bOutput.flush();
			
			log.debug("Done.");
			
			// For reading the incoming file
			InputStream input = socket.getInputStream();
			
			// Declaring buffer size
			int bufferSize = 1024 * 8;
			byte[] bytes = new byte[bufferSize];
			
			// Getting file size from server
			long fileSize;
			DataInputStream dInput = (DataInputStream) getInput(DataInputStream.class);
			if((fileSize = dInput.readLong()) == -1) {
				log.error("Error getting file size from client.");
				return false;
			}
			
			log.debug("Downloading " + (fileSize / 1000.0) + "kb file...");
			log.debug("==============================================");
			
			int bytesReceived = 0;
			// Reading from the input stream and saving to a file	
			for(int bytesRead; bytesReceived < fileSize && (bytesRead = input.read(bytes)) >= 0;) {
				bOutput.write(bytes, 0, bytesRead);
				bytesReceived += bytesRead;
				log.debug("Got " + bytesRead + " bytes [" + bytesReceived + " of " + fileSize + " bytes received].");
			}
			bOutput.flush();
			fOutput.flush();
			
			log.debug("==============================================");
			log.debug("Done.");
		} catch (IOException e) {
			log.error("IO error.", e);
			return false;
		}
		log.debug("try-with-resources block executed. File streams should be closed.");
		
		// Placing downloaded file in correct artist folder
		Server.STORAGE.sortFiles();
		return true;
	}
	
	public boolean sendFile(String filePath) {
		File file = new File(filePath);
		
		// Making sure the file exists
		if(!file.exists()) {
			log.error("File \"" + filePath + "\" does not exist.");
			return false;
		}
		
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
			log.error("Could not find \"" + file.getAbsolutePath() + "\".", e);
			return false;
		} catch (IOException e) {
			log.error("IO Error.", e);
			return false;
		}
		return true;
	}
	
	public void disconnect() {
		// Storing address before ending connection
		InetAddress address = socket.getInetAddress();
		
		log.debug("Closing input streams...");
		for(InputStream stream: inStreams) {
			try {
				stream.close();
				log.debug("Done.");
			} catch (IOException e) {
				log.error("Error closing input stream with [" + address + "].", e);
			}
		}
		
		log.debug("Closing output streams...");
		for(OutputStream stream: outStreams) {
			try {
				stream.close();
				log.debug("Done.");
			} catch (IOException e) {
				log.error("Error closing output stream with [" + address + "].", e);
			}
		}
		
		log.debug("Ending connection with [" + address + "]...");
		try {
			// Close the socket and its associated input/output streams
			socket.close();
			log.debug("Done.");
		}
		catch(IOException e) {
			log.error("Error closing socket with [" + address + "].", e);
		}
	}
}
