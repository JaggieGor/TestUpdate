package com.jaggie.diffgenerator;

import java.io.File;

public class FileUtils {

	public static void deleteFiles(File pendingRemoveFile) {
		if (pendingRemoveFile.isDirectory())
			for (File child : pendingRemoveFile.listFiles())
				deleteFiles(child);
		pendingRemoveFile.delete();

	}

}
