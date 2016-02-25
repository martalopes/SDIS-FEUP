import java.io.IOException;
import java.net.*;

public class Servidor {
	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket(4445);
		// get response
		byte[] rbuf = new byte[10];
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		socket.receive(packet);
		// display response
		String received = new String(packet.getData());
		System.out.println("Echoed Message: " + received);
		socket.close();
	}
}
