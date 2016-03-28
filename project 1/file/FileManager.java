package file;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

import chunk.Chunk;
import chunk.ChunkID;
import peer.Peer;

public class FileManager {
	
	public static void saveChunk(ChunkID chunkID, byte[] data) throws IOException {
		String folderName = "CHUNKS_PEER" + Peer.getId();
		if (!folderExists(folderName))
			createFolder(folderName);
		
		FileOutputStream out = new FileOutputStream(folderName + "/" + chunkID.toString());
		out.write(data, 0, data.length);
		out.close();
	}
	
	public static byte[] loadChunk(ChunkID chunkID) throws FileNotFoundException {
		String folderName = "CHUNKS_PEER" + Peer.getId();
		if (!folderExists(folderName))
			createFolder(folderName);
		
		File file = new File(folderName + "/" + chunkID.toString());
		FileInputStream inputStream = new FileInputStream(file);

		byte[] data = new byte[(int) file.length()];

		try {
			inputStream.read(data);
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return data;
	}
	
	public static void saveFile(String fileName, byte[] data) throws IOException {
		String folderName = "RESTORED_PEER" + Peer.getId();
		if (!folderExists(folderName))
			createFolder(folderName);
		
		FileOutputStream out = new FileOutputStream(folderName + "/" + fileName);
		out.write(data);
		out.close();
	}
	
	public static long deleteChunk(String chunkID) {
		String folderName = "CHUNKS_PEER" + Peer.getId();
		File file = new File(folderName + "/" + chunkID);
		long length = file.length();
		file.delete();
		Peer.removeChunk(chunkID);
		return length;
	}
	
	public static String getFileID(File file) {
		String name = file.getName() + Peer.getId();
		return sha256(name);
	}
	
	private static boolean folderExists(String name) {
		File file = new File(name);

		return file.exists() && file.isDirectory();
	}

	private static void createFolder(String name) {
		File file = new File(name);

		file.mkdir();
	}
	
	public static String sha256(String base) {
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
    }
	
	public static long getNumChunks(File file) {
		return file.length() / Chunk.MAX_CHUNK_SIZE + 1;
	}
}
