package listener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import chunk.ChunkID;
import protocols.Protocol;

public class Listener implements Runnable {
	
	public MulticastSocket socket;
	public InetAddress address;
	public int port;
	
	private volatile HashMap<String, ArrayList<Integer>> storedNo;
	private volatile ArrayList<String> chunksToSave;
	private volatile HashMap<String, Boolean> chunksAlreadySent;
	
	public Listener(InetAddress address, int port) {
		this.address = address;
		this.port = port;
		
		storedNo = new HashMap<String, ArrayList<Integer>>();
		chunksToSave = new ArrayList<String>();
		chunksAlreadySent = new HashMap<String, Boolean>();
	}

	@Override
	public void run() {
		// open socket
		try {
			socket = new MulticastSocket(port);
			socket.setTimeToLive(1);
			socket.joinGroup(address);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] buf = new byte[65000];

		boolean done = false;
		while (!done) {
			try {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				handler(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handler(DatagramPacket packet) {
		new Thread(new Protocol(packet)).start();
	}
	
	public synchronized int getNoStoreds(ChunkID chunkID) {
		if (storedNo.get(chunkID.toString()) != null) {
			return storedNo.get(chunkID.toString()).size();
		}
		else
			return 0;
	}
	
	public synchronized void addStored(ChunkID chunkID, int peerID) {
		if (storedNo.containsKey(chunkID.toString())) {
			if (!storedNo.get(chunkID.toString()).contains(peerID))
				storedNo.get(chunkID.toString()).add(peerID);
		} else {
			ArrayList<Integer> peers = new ArrayList<Integer>();
			peers.add(peerID);
			storedNo.put(chunkID.toString(), peers);
		}
	}
	
	public synchronized void removePeerFromChunk(ChunkID chunkID, int peerID) {
		if (storedNo.containsKey(chunkID.toString())) {
			if (storedNo.get(chunkID.toString()).contains(peerID))
				storedNo.get(chunkID.toString()).removeAll(Arrays.asList(peerID));
		}
	}
	
	public synchronized void addChunksToSave(ChunkID chunkID) {
		chunksToSave.add(chunkID.toString());
	}
	
	public synchronized void removeChunksToSave(ChunkID chunkID) {
		chunksToSave.remove(chunkID.toString());
	}
	
	public synchronized boolean savingChunksOf(ChunkID chunkID) {
		return chunksToSave.contains(chunkID.toString());
	}
	
	public synchronized void saveCHUNKS(ChunkID chunkID) {
		chunksAlreadySent.put(chunkID.toString(), false);
	}
	
	public synchronized boolean stopSaveCHUNKS(ChunkID chunkID) {
		return chunksAlreadySent.remove(chunkID.toString());
	}
	
	public synchronized void addToSentChunks(ChunkID chunkID) {
		if (chunksAlreadySent.containsKey(chunkID))
			chunksAlreadySent.put(chunkID.toString(), true);
	}

}
