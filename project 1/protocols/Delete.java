package protocols;
import java.io.File;
import java.io.IOException;

import file.FileManager;

public class Delete implements Runnable{
	
	private File file;

	public Delete(File file) {
		this.file = file;
	}

	@Override
	public void run() {
		String fileID = FileManager.getFileID(file);
		try {
			Protocol.sendDELETE(fileID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
