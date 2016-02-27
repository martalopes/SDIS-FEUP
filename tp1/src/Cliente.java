import java.io.IOException;
import java.net.*;


public class Cliente {
	public static void main(String[] args) throws IOException{
		if (args.length != 2) {
			System.out.println("Usage: java Echo <hostname> <string to echo>");
			return;
		}
		// send request
		DatagramSocket socket = new DatagramSocket();
		byte[] sbuf = args[1].getBytes();
		InetAddress address = InetAddress.getByName(args[0]);
		DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, address, 4445);
		socket.send(packet);
		
		byte[] buf = new byte[256];
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		String response = new String(packet.getData(), 0, packet.getLength());
		System.out.println("RECEIVED: " + response);
		
		socket.close();
	}
}
