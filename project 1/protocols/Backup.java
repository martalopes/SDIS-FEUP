package protocols;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import chunk.Chunk;
import file.FileManager;
import peer.Peer;

public class Backup implements Runnable {
	
	File file;
	int replicationDegree;
	
	public Backup(File file, int replicationDegree) {
		this.file = file;
		this.replicationDegree = replicationDegree;
	}

	@Override
	public void run() {
		try {
			@SuppressWarnings("resource")
			FileInputStream inputStream = new FileInputStream(file);
		
			long numChunks = FileManager.getNumChunks(file);
			String fileID = FileManager.getFileID(file);
			Peer.addOwnedFile(fileID);
			System.out.println("Backing up file with ID " + fileID);
			byte[] data = new byte[Chunk.MAX_CHUNK_SIZE];
			
			for (int i = 0; i < numChunks; i++) {
				byte[] chunkData;
				
				if (i == numChunks - 1 && file.length() % Chunk.MAX_CHUNK_SIZE == 0)
					chunkData = new byte[0];
				else {
					int numBytesRead = inputStream.read(data, 0, data.length);
					chunkData = new byte[numBytesRead];
					chunkData = Arrays.copyOfRange(data, 0, numBytesRead);
				}
				
				// send chunk
				Chunk chunk = new Chunk(fileID, i, replicationDegree, chunkData);
				Thread t = new Thread(new BackupChunk(chunk));
				t.start();
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			
			System.out.println("Finished backing up file with ID " + fileID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
