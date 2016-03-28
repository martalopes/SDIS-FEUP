package protocols;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Random;

import chunk.Chunk;
import chunk.ChunkID;
import file.FileManager;
import peer.Peer;

public class Protocol implements Runnable {
	
	public static final String VERSION = "1.0";
	public static final String CRLF = "\r\n";
	
	private String message;
	private String header;
	private String[] headerTokens;

	private byte[] body;
	
	Random random = new Random();
	private DatagramPacket packet;
	
	private String expectedChunk;
	private boolean receivedExpectedChunk;

	public Protocol(DatagramPacket packet) {
		expectedChunk = null;
		receivedExpectedChunk = false;
		this.packet = packet;
		message = new String(packet.getData(), 0, packet.getLength());
		getHeader();
	}

	@Override
	public void run() {
		String messageType = headerTokens[0];
		
		// ignore packets sent by self
		if (headerTokens[2].equals(Integer.toString(Peer.getId())))
			return;
		
		switch (messageType) {
		case "PUTCHUNK":
			handlePUTCHUNK();
			break;
		case "STORED":
			handleSTORED();
			break;
		case "GETCHUNK":
			handleGETCHUNK();
			break;
		case "CHUNK":
			handleCHUNK();
			break;
		case "DELETE":
			handleDELETE();
			break;
		case "REMOVED":
			handleREMOVED();
			break;
		}
		
	}

	private void handlePUTCHUNK() {
		System.out.println("Received PUTCHUNK");
		ChunkID chunkID = new ChunkID(headerTokens[3], Integer.parseInt(headerTokens[4]));
		if (expectedChunk != null && expectedChunk.equals(chunkID.toString()))
			receivedExpectedChunk = true;
		try {
			Thread.sleep(random.nextInt(400));
			if (Peer.getMcListener().getNoStoreds(chunkID) < Integer.parseInt(headerTokens[5]) && !Peer.hasFile(headerTokens[3])) {
				System.out.println("Storing chunk with ID " + chunkID);
				sendSTORED(chunkID);
				getBody();
				Peer.getMcListener().addStored(chunkID, Peer.getId());
				Peer.addChunk(chunkID.toString(), Integer.parseInt(headerTokens[5]));
				FileManager.saveChunk(chunkID, body);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void handleSTORED() {
		ChunkID chunkID = new ChunkID(headerTokens[3], Integer.parseInt(headerTokens[4]));
		System.out.println("Received STORED for chunk with ID " + chunkID + " from peer " + headerTokens[2]);
		Peer.getMcListener().addStored(chunkID, Integer.parseInt(headerTokens[2]));
	}
	
	private void handleGETCHUNK(){
		ChunkID chunkID = new ChunkID(headerTokens[3], Integer.parseInt(headerTokens[4]));
		System.out.println("Received GETCHUNK for chunk with ID " + chunkID);
		
		if (Peer.getStoredChunks().contains(chunkID.toString())) {
			Peer.getMdrListener().saveCHUNKS(chunkID);
			
			try {
				Thread.sleep(random.nextInt(400));
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			
			boolean chunkSent = Peer.getMdrListener().stopSaveCHUNKS(chunkID);
			
			if (!chunkSent) {
				try {
					byte[] data = FileManager.loadChunk(chunkID);
					Chunk chunk = new Chunk(chunkID.getFileID(), chunkID.getChunkNo(), 0, data);
					Peer.getMdrListener().addToSentChunks(chunkID);
					System.out.println("Sending chunk with ID " + chunkID);
					sendCHUNK(chunk);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	
	private void handleCHUNK() {
		ChunkID chunkID = new ChunkID(headerTokens[3], Integer.parseInt(headerTokens[4]));
		System.out.println("Received CHUNK with ID " + chunkID + " from peer " + headerTokens[2]);
		
		if (Peer.getMdrListener().savingChunksOf(chunkID)) {
			System.out.println("Saving chunk with ID " + chunkID);
			getBody();
			Chunk chunk = new Chunk(chunkID.getFileID(), chunkID.getChunkNo(), 0, body);
			Restore.addChunk(chunk);
		}
		else
			Peer.getMdrListener().addToSentChunks(chunkID);
	}
	
	private void handleDELETE() {
		String fileID = headerTokens[3];
		System.out.println("Received DELETE for file with ID " + fileID);
		ArrayList<String> chunksToDelete = Peer.getChunksFromFile(fileID);
		for (int i = 0; i < chunksToDelete.size(); i++) {
			FileManager.deleteChunk(chunksToDelete.get(i));
		}
	}
	
	private void handleREMOVED() {
		int peerID = Integer.parseInt(headerTokens[2]);
		ChunkID chunkID = new ChunkID(headerTokens[3], Integer.parseInt(headerTokens[4]));
		System.out.println("Received REMOVE from peer " + peerID + " for chunk with ID " + chunkID);
		Peer.getMcListener().removePeerFromChunk(chunkID, peerID);
		
		if (Peer.repDegreeIsBelowDesired(chunkID.toString(), Peer.getMcListener().getActualRepDegree(chunkID))) {
			expectedChunk = chunkID.toString();
			try {
				Thread.sleep(random.nextInt(400));
				if (!receivedExpectedChunk) {
					try {
						byte[] data = FileManager.loadChunk(chunkID);
						Chunk chunk = new Chunk(chunkID.getFileID(), chunkID.getChunkNo(), Peer.getRepDegree(chunkID.toString()), data);
						System.out.println("Replication degree is below desired; sending chunk with ID " + chunkID);
						sendPUTCHUNK(chunk);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			expectedChunk = null;
			receivedExpectedChunk = false;
		}
	}
	
	public static void sendPUTCHUNK(Chunk chunk) throws IOException {
		String header = "PUTCHUNK" + " " + VERSION + " " + Peer.getId() + " "
						+ chunk.getId().getFileID() + " " + chunk.getId().getChunkNo()
						+ " " + chunk.getReplicationDegree() + " " + CRLF + CRLF;
		
		byte[] message = joinBytes(header.getBytes(), chunk.getData());
		sendToMDB(message);
	}
	
	public static void sendSTORED(ChunkID chunkID) throws IOException {
		String message = "STORED" + " " + VERSION + " " + Peer.getId() + " "
						+ chunkID.getFileID() + " " + chunkID.getChunkNo() + " " + CRLF + CRLF;
		
		sendToMC(message.getBytes());
	}
	
	public static void sendGETCHUNK(ChunkID chunkID) throws IOException {
		String message = "GETCHUNK" + " " + VERSION + " " + Peer.getId() + " "
						+ chunkID.getFileID() + " " + chunkID.getChunkNo() + " " + CRLF + CRLF;
		
		sendToMC(message.getBytes());
	}
	
	public static void sendCHUNK(Chunk chunk) throws IOException {
		String header = "CHUNK" + " " + VERSION + " " + Peer.getId() + " "
						+ chunk.getId().getFileID() + " " + chunk.getId().getChunkNo() + " " + CRLF + CRLF;
		
		byte[] message = joinBytes(header.getBytes(), chunk.getData());
		
		sendToMDR(message);
	}
	
	public static void sendDELETE(String fileID) throws IOException {
		String message = "DELETE" + " " + VERSION + " " + Peer.getId() + " "
						+ fileID + " " + CRLF + CRLF;
		
		sendToMC(message.getBytes());
	}
	
	public static void sendREMOVED(String chunkID) throws IOException {
		String[] chunkTokens = chunkID.split("-");
		String fileID = chunkTokens[0];
		String chunkNo = chunkTokens[1];
		String message = "REMOVED" + " " + VERSION + " " + Peer.getId() + " "
						+ fileID + " " + chunkNo + " " + CRLF + CRLF;
		
		sendToMC(message.getBytes());
	}
	
	private synchronized static void sendToMC(byte[] buf) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, buf.length, Peer.getMcListener().address, Peer.getMcListener().port);
		Peer.getSocket().send(packet);
	}
	
	private synchronized static void sendToMDB(byte[] buf) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, buf.length, Peer.getMdbListener().address, Peer.getMcListener().port);
		Peer.getSocket().send(packet);
	}
	
	private synchronized static void sendToMDR(byte[] buf) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, buf.length, Peer.getMdrListener().address, Peer.getMcListener().port);
		Peer.getSocket().send(packet);
	}
	
	private void getBody() {
		String[] tokens = message.split("\\r\\n\\r\\n");
		int bodyOffset = tokens[0].length() + 4;
		body = getArrayFromOffset(packet.getData(), bodyOffset, packet.getLength());
	}

	private void getHeader() {
		header = message.split("\\r\\n\\r\\n")[0];
		headerTokens = header.split(" ");
	}
	
	// utilities
	
	public static byte[] joinBytes(byte[] b1, byte[] b2) {
		byte[] b = new byte[b1.length + b2.length];

		System.arraycopy(b1, 0, b, 0, b1.length);
		System.arraycopy(b2, 0, b, b1.length, b2.length);

		return b;
	}
	
	private byte[] getArrayFromOffset(byte[] data, int offsetOfBody, int length) {
        int size = length - offsetOfBody;
        byte[] toRet = new byte[size];
       
        for(int i = offsetOfBody; i < length; i++) {
            toRet[i - offsetOfBody] = data[i];
        }
       
        return toRet;
    }
}
