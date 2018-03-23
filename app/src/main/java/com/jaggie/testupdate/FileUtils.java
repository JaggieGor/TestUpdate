package com.jaggie.testupdate;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;

/**
 * Created by Jaggie on 2018/3/23.
 */

public class FileUtils {

    public static void deleteFiles(Context context, File pendingRemoveFile) {
//        File pendingRemoveFile = new File(path);
        Uri pendingRemoveUri = FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID +
                        ".fileProvider", pendingRemoveFile);
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(pendingRemoveUri, null, null);
    }

    public static void deleteFiles(File pendingRemoveFile) {
        if (pendingRemoveFile.isDirectory())
            for (File child : pendingRemoveFile.listFiles())
                deleteFiles(child);
        pendingRemoveFile.delete();

    }
}
