package protocols;
import java.io.IOException;

import chunk.Chunk;
import peer.Peer;

public class BackupChunk implements Runnable{
	
	public BackupChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	private Chunk chunk;

	@Override
	public void run() {
		int waitingTime = 1000;
		int attemp = 0;
		boolean done = false;
		while (!done) {
			try {
				Protocol.sendPUTCHUNK(chunk);
				Thread.sleep(waitingTime);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		
			int noStoreds = Peer.getMcListener().getNoStoreds(chunk.getId());
			if (noStoreds < chunk.getReplicationDegree()) {
				attemp++;
				if (attemp <= 5) {
					waitingTime *= 2;
				}
				else
					done = true;
			} else
				done = true;
			
		}
	}
	
	

}
