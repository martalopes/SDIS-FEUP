package peer;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

import listener.Listener;
import protocols.Backup;
import protocols.Delete;
import protocols.Protocol;
import protocols.Reclaim;
import protocols.Restore;

public class Peer {
	
	private static InetAddress mcAddress;
	private static int mcPort;
	
	private static InetAddress mdbAddress;
	private static int mdbPort;
	
	private static InetAddress mdrAddress;
	private static int mdrPort;
	
	private static volatile Listener mcListener;
	private static volatile Listener mdbListener;
	private static volatile Listener mdrListener;
	
	private static MulticastSocket socket;
	private static int id;
	
	private static Protocol protocol;
	
	private static ArrayList<String> storedChunks = new ArrayList<String>();
	private static ArrayList<Integer> chunksRepDegree = new ArrayList<Integer>();
	private static ArrayList<String> ownedFiles = new ArrayList<String>();

	public static Protocol getProtocol() {
		return protocol;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 7) {
			System.out.println("Usage: Peer <peerID> <mc address> <mc port> <mdb address> <mdb port> <mdr address> <mdr port>");
			return;
		}
		
		id = Integer.parseInt(args[0]);
		System.out.println("Opening peer with ID " + id);
		
		mcAddress = InetAddress.getByName(args[1]);
		mcPort = Integer.parseInt(args[2]);

		mdbAddress = InetAddress.getByName(args[3]);
		mdbPort = Integer.parseInt(args[4]);

		mdrAddress = InetAddress.getByName(args[5]);
		mdrPort = Integer.parseInt(args[6]);
		
		mcListener = new Listener(mcAddress, mcPort);
		mdbListener = new Listener(mdbAddress, mdbPort);
		mdrListener = new Listener(mdrAddress, mdrPort);
		
		new Thread(mcListener).start();
		new Thread(mdbListener).start();
		new Thread(mdrListener).start();
		
		socket = new MulticastSocket();
		
		@SuppressWarnings("resource")
		DatagramSocket dgSocket = new DatagramSocket(id);
		boolean done = false;
		while (!done) {
			byte[] rbuf = new byte[256];
			DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
			dgSocket.receive(packet);
			String received = new String(packet.getData(), 0, packet.getLength());
			String[] request = received.split(" ");
			String protocol = request[0];
			String fileName;
			File file;
			
			switch (protocol) {
			case "BACKUP":
				fileName = request[1];
				int repDegree = Integer.parseInt(request[2]);
				file = new File(fileName);
				Backup backupFile = new Backup(file, repDegree);
				new Thread(backupFile).start();
				break;
			case "RESTORE":
				fileName = request[1];
				file = new File(fileName);
				Restore restoreFile = new Restore(file);
				new Thread(restoreFile).start();
				break;
			case "DELETE":
				fileName = request[1];
				file = new File(fileName);
				Delete deleteFile = new Delete(file);
				new Thread(deleteFile).start();
				break;
			case "RECLAIM":
				int space = Integer.parseInt(request[1]);
				Reclaim reclaimSpace = new Reclaim(space);
				new Thread(reclaimSpace).start();
				break;
			}
			
		}
	}

	public static int getId() {
		return id;
	}
	
	public static Listener getMcListener() {
		return mcListener;
	}

	public static Listener getMdbListener() {
		return mdbListener;
	}

	public static Listener getMdrListener() {
		return mdrListener;
	}

	public static MulticastSocket getSocket() {
		return socket;
	}
	
	public static ArrayList<String> getStoredChunks() {
		return storedChunks;
	}
	
	public static void addChunk(String chunkID, int repDegree) {
		storedChunks.add(chunkID);
		chunksRepDegree.add(repDegree);
	}
	
	public static void removeChunk(String chunkID) {
		int index = storedChunks.indexOf(chunkID);
		storedChunks.remove(chunkID);
		chunksRepDegree.remove(index);
	}
	
	public static void addOwnedFile(String fileID) {
		if (!ownedFiles.contains(fileID))
			ownedFiles.add(fileID);
	}
	
	public static boolean hasFile(String fileID) {
		return ownedFiles.contains(fileID);
	}
	
	public static boolean repDegreeIsBelowDesired(String chunkID, int actualRepDegree) {
		if (!storedChunks.contains(chunkID))
			return false;
		
		int index = storedChunks.indexOf(chunkID);
		if (actualRepDegree < chunksRepDegree.get(index)) {
			return true;
		} else
			return false;
	}
	
	public static int getRepDegree(String chunkID) {
		if (!storedChunks.contains(chunkID))
			return 0;
		
		int index = storedChunks.indexOf(chunkID);
		return chunksRepDegree.get(index);
	}

	public static ArrayList<String> getChunksFromFile(String fileID) {
		ArrayList<String> chunks = new ArrayList<String>();
		for (int i = 0; i < storedChunks.size(); i++) {
			if (storedChunks.get(i).split("-")[0].equals(fileID))
				chunks.add(storedChunks.get(i));
		}
		return chunks;
	}

}
