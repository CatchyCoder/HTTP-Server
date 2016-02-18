package music.server;

import java.net.Socket;

import music.core.AbstractConnection;

public abstract class ServerConnection extends AbstractConnection implements Runnable {

	protected ConnectionManager manager;
	protected final Storage STORAGE;
	
	public ServerConnection(Socket socket, ConnectionManager manager, Storage storage) {
		super(socket);
		this.manager = manager;
		this.STORAGE = storage;
		
		// Notify client that that server is ready for initial message
		writeInt(Message.ACK.ordinal());
		
		// Begin session
		new Thread(this).start();
	}
	
	@Override
	public synchronized void disconnect() {
		super.disconnect();
		// Removing the connection from the connection manager's list of connections
		manager.remove(this);
	}
}
