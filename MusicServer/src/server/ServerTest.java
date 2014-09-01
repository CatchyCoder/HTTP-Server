package server;

import java.io.IOException;

public class ServerTest {

	public static void main(String[] args) {
		try {
			new Server(6789, 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
