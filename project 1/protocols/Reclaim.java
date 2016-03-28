package protocols;
import java.io.IOException;
import java.util.ArrayList;

import chunk.ChunkID;
import file.FileManager;
import peer.Peer;

public class Reclaim implements Runnable {
	
	long spaceToReclaim;
	
	public Reclaim(long space) {
		spaceToReclaim = space;
	}

	@Override
	public void run() {
		ArrayList<String> storedChunks = Peer.getStoredChunks();
		long reclaimedSpace = 0;
		int i = 0;
		System.out.println("Trying to reclaim " + spaceToReclaim + " bytes");
		while (reclaimedSpace < spaceToReclaim && i < storedChunks.size()) {
			String chunk = storedChunks.get(i);
			reclaimedSpace += FileManager.deleteChunk(chunk);
			String[] chunkTokens = chunk.split("-");
			String fileID = chunkTokens[0];
			String chunkNo = chunkTokens[1];
			Peer.getMcListener().removePeerFromChunk(new ChunkID(fileID, Integer.parseInt(chunkNo)), Peer.getId());
			try {
				Protocol.sendREMOVED(chunk);
			} catch (IOException e) {
				e.printStackTrace();
			}
			i++;
		}
		System.out.println("Reclaimed " + reclaimedSpace + " bytes");
	}

}
