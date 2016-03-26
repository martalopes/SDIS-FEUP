import java.io.IOException;


import java.net.*;
import java.util.HashMap;

public class Servidor {
	
	private static HashMap<String, String> plates;

	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket(4445);
		
		plates = new HashMap<String, String>();
		
		// get response
		byte[] rbuf = new byte[256];
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		socket.receive(packet);
		// display response
		String received = new String(packet.getData());
		String[] request = received.split(" ");
		String plate = request[1];
		String owner;
		String response = "";
		
		switch(request[0]) {
		case "REGISTER":
			owner = request[2];
			if (plates.containsKey(plate)) {
				response = "-1";
			} else {
				plates.put(plate, owner);
				response = Integer.toString(plates.size());
			}

			break;
		case "LOOKUP":
			if (plates.containsKey(plate)) {
				owner = plates.get(plate);
				response = owner;
			} else {
				response = "NOT_FOUND";
			}

			break;
		}
		System.out.println("SENT: " + response);
		rbuf = response.getBytes();
		InetAddress address = packet.getAddress();
		int port = packet.getPort();
		packet = new DatagramPacket(rbuf, rbuf.length, address, port);
		socket.send(packet);
		socket.close();
	}
}
