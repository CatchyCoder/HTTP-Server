package server;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.FieldKey;

public class Server {
	
	private static final Logger log = LogManager.getLogger(Server.class);
	
	public static boolean isOpen = false;
	
	// The actual 'public server' that everyone can connect to
	public static ServerSocket server;
	public static Storage STORAGE;
	
	public Server(int port, int backlog) throws IOException {
		server = new ServerSocket(port, backlog);
		isOpen = true;
		
		STORAGE = new Storage();
		log.debug("Album name: " + STORAGE.getField(FieldKey.ALBUM, "/MusicServer/aphex_twin/selected_ambient_works_85–92/Aphex Twin - Delphium.mp3"));
		log.debug("Albums from Aphex Twin name: ");
		
		String[] albums = STORAGE.getAlbums("apHex TwIN");
		for(String album: albums) log.debug("Album: " + album);
		
		new ConnectionManager(this);
	}
	
	public static boolean isOpen() {
		return isOpen;
	}
	
	public ServerSocket getServer() {
		return server;
	}
}
