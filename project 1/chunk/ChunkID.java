package chunk;

public class ChunkID {
	
	private String fileID;
	private int chunkNo;
	
	public ChunkID(String fileID, int chunkNo) {
		this.fileID = fileID;
		this.chunkNo = chunkNo;
	}
	
	@Override
	public String toString() {
		return fileID + "-" + chunkNo;
	}
	
	public String getFileID() {
		return fileID;
	}
	
	public int getChunkNo() {
		return chunkNo;
	}
}
