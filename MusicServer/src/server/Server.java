package server;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Server extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	// The area the user enters their text
	private JTextField userText;
	// The area to display all the messages
	private JTextArea chatWindow;
	
	// The input and output streams
	private ObjectOutputStream output;
	private ObjectInputStream input;
	
	// The actual 'public server' that everyone can connect to
	private ServerSocket server;
	// The connection to another device (Java treats sockets as connections)
	private Socket connection;
	
	public Server() {
		super("Server BETA");
		userText = new JTextField();
		// Don't let the user enter text right away
		// we want to set up a connection first.
		userText.setEditable(false);
		// Setting up what happens when they press enter
		userText.addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent event) {
					// Passing in the String or 'message' that
					// was typed in the textbox
					//sendMessage(event.getActionCommand());
					
					// Test for sending song
					File file = new File("C:/Users/kuznia/Music/Music/Artists/Aphex Twins/Aphex Twin - Delphium.mp3");
					long size = file.length();
					// Making sure the file isn't too big
					if(size > Integer.MAX_VALUE)
						showMessage("File size too large.");
					else try {
						showMessage("Preparing to send file...");
						// Used for the buffer size
						byte[] bytes = new byte[(int) size];
						
						// For loading the file into RAM
						FileInputStream fInput = new FileInputStream(file);
						BufferedInputStream bInput = new BufferedInputStream(fInput);
						
						// For sending the loaded RAM
						BufferedOutputStream output = new BufferedOutputStream(connection.getOutputStream());
						
						
						showMessage("Sending file...");
						
						int count;
						// Reading the data with read() and sending it with write()
						// -1 means the end of stream (no more bytes to read)
						while((count = bInput.read(bytes)) > 0) {
							
							// count is the number of bytes to write,
							// 0 is the offset
							// bytes is the actual data to write
							output.write(bytes, 0, count);
							System.out.println(count + " bytes sent.");
						}
						
						fInput.close();
						bInput.close();
						output.close();
						
						showMessage("File sent. Hurray!!!");
						
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					
					
					// Clear their message, since it was sent
					userText.setText("");
				}
			}
		);
		
		chatWindow = new JTextArea();
		
		add(userText, BorderLayout.NORTH);
		add(chatWindow, BorderLayout.CENTER);
		
		setSize(500, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void start() {
		try {
			// Setting up the server with a
			// port number of 6789 and backlog of 100
			// (backlog - requested maximum length of the queue of incoming connections)
			// So 100 people can wait on that port
			server = new ServerSocket(6789, 100);
			
			// Keep the server running until we are done
			while(true) {
				try {
					// First we will start up and wait for somebody
					// to connect with, then it will initialize the streams,
					// finally it will then handle the chatting
					
					waitForConnection();
					setupStreams();
					
					// Talk to them until you are done...
					chat();
				}
				catch(EOFException e) { // This 'EOFException' signals the end of a stream
					// The server is done running
					showMessage("Server ended the connection!");
				}
				finally {
					// Close the server
					disconnect();
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void waitForConnection() throws IOException {
		// Wait for connection, then display connection information
		showMessage("Awaiting a connection...");
		// Once somebody 'asks' to connect with us, it then
		// 'accepts' the connection on this socket
		// So 'connection' will only be assigned a value
		// if a connection was made
		connection = server.accept();
		// I believe this should show their IP address
		showMessage("Connection successfully established with" + connection.getInetAddress().getHostName() + "!");
	}
	
	private void setupStreams() throws IOException {
		// Get streams to send/receive data
		showMessage("Setting up streams...");
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("Streams are now setup.");
	}
	
	private void chat() throws IOException {
		// Handling the conversation
		ableToType(true);
		String message = "Connection established. Feel free to talk! :)";
		sendMessage(message);
		do {
			/*// Have the conversation
			try {
				// Get a message from the outside
				message = (String) input.readObject();
				showMessage(message);
			}
			catch(ClassNotFoundException e) {
				// An error occurred
				e.printStackTrace();
				showMessage(e.getMessage());
			}*/
		} while (!message.equals("[CLIENT] END"));
	}
	
	public void disconnect() {
		// Close streams and sockets after we are done chatting
		showMessage("Ending connection...");
		ableToType(false);
		try {
			input.close();
			output.close();
			connection.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		showMessage("Connection ended.\n==========================\n");
	}
	
	private void sendMessage(String message) {
		// Send a message to the client
		try {
			//output.writeObject("[SERVER] " + message);
			output.flush();
			// Put the message in the chat-box
			showMessage("[SERVER] " + message);
		}
		catch(IOException e) {
			chatWindow.append("Error sending message!");
		}
	}
	
	public void showMessage(final String message) {
		SwingUtilities.invokeLater(
			new Runnable() {
				
				@Override
				public void run() {
					chatWindow.append("\n" + message);
				}
			}
		);
	}
	
	private void ableToType(final boolean canType) {
		SwingUtilities.invokeLater(
			new Runnable() {
				
				@Override
				public void run() {
					userText.setEditable(canType);
				}
			}
		);
	}
}
