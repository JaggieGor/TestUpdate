package com.jaggie.diffgenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {

	public static void deleteFiles(File pendingRemoveFile) {
		if (pendingRemoveFile.isDirectory())
			for (File child : pendingRemoveFile.listFiles())
				deleteFiles(child);
		pendingRemoveFile.delete();

	}
	
	public static void copyTo(String sourcePath,String destPath) {
		try {
			File apkFile = new File(sourcePath);
			Files.copy(apkFile.toPath(), new FileOutputStream(destPath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
