package protocols;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import chunk.Chunk;
import chunk.ChunkID;
import file.FileManager;
import peer.Peer;

public class Restore implements Runnable {
	
	private File file;
	private static ArrayList<Chunk> chunks;
	
	public Restore(File file) {
		this.file = file;
		chunks = new ArrayList<Chunk>();
	}

	@Override
	public void run() {
		String fileID = FileManager.getFileID(file);
		System.out.println("Trying to restore file with ID " + fileID);
		if (!Peer.hasFile(fileID))
			return;
		
		long numChunks = FileManager.getNumChunks(file);
		
		for (int i = 0; i < numChunks; i++) {
			ChunkID chunkID = new ChunkID(FileManager.getFileID(file), i);
			try {
				Peer.getMdrListener().addChunksToSave(chunkID);
				Protocol.sendGETCHUNK(chunkID);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		int k = 0;
		while (chunks.size() < numChunks && k < 100) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			k++;
		}
		
		byte[] fileData = new byte[0];
		for (int i = 0; i < numChunks; i++) {
			Chunk chunkI = null;

			for (Chunk chunk : chunks) {
				if (chunk.getId().getChunkNo() == i) {
					chunkI = chunk;
					Peer.getMdrListener().removeChunksToSave(chunkI.getId());
					break;
				}
			}
			
			if (chunkI == null)
				System.out.println("Missing chunk no " + i);
			else
				fileData = Protocol.joinBytes(fileData, chunkI.getData());
		}
		
		try {
			FileManager.saveFile(file.getName(), fileData);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Finished restoring file with ID " + fileID);
	}
	
	public static ArrayList<Chunk> getChunks() {
		return chunks;
	}
	
	public static void addChunk(Chunk chunk) {
		chunks.add(chunk);
	}

}
