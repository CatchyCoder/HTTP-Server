//package old;
//
//import org.apache.ftpserver.FtpServer;
//import org.apache.ftpserver.FtpServerFactory;
//import org.apache.ftpserver.ftplet.FtpException;
//import org.apache.ftpserver.listener.ListenerFactory;
//
//public class Test {
//		
//	public Test() {
//		FtpServerFactory serverFactory = new FtpServerFactory();
//		FtpServer server = serverFactory.createServer();
//		ListenerFactory lFactory = new ListenerFactory();
//		
//		try {
//			// Set the port of the listener
//			lFactory.setPort(2221);
//			// Replace the default listener
//			serverFactory.addListener("default", lFactory.createListener());
//			// Starting server
//			System.out.print("Starting server... ");
//			server.start();
//			System.out.println("Done.");
//			
//		} catch (FtpException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public static void main(String[] args) {
//		new Test();
//	}
//}
