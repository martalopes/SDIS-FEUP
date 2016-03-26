package ui;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UI {
	public static void main(String[] args) throws IOException {
		if (args.length < 3 || args[1].equals("BACKUP") && args.length < 4) {
			System.out.println("Usage: java UI <peer_ap> <sub_protocol> <opnd_1> <opnd_2> ");
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		DatagramSocket socket = new DatagramSocket();
		String message = args[1] + " " + args[2];
		if (args.length == 4)
			message += " " + args[3];
		byte[] sbuf = message.getBytes();
		InetAddress address = InetAddress.getByName("localhost");
		DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, address, port);
		socket.send(packet);
		System.out.println("Sent: " + message);
		socket.close();
	}
}
