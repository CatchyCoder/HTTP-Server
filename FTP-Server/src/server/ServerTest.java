package server;

import java.io.IOException;

public class ServerTest {

	public static void main(String[] args) {
		try {
			// Setting up the server with a
			// port number of 6789 and backlog of 100
			// (backlog - requested maximum length of the queue of incoming connections)
			// So 100 people can wait on that port
			new Server(6789, 20);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
