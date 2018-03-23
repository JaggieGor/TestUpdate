package com.jaggie.testupdate;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ren.yale.android.intremetalupdate.EasyIncrementalUpdate;

/**
 * Created by Jaggie on 2018/3/21.
 */

public class DownloadIntentService extends IntentService {

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Todo

        String sdcardPath = Environment.getExternalStorageDirectory().getPath();

        String cachePathPrefix = sdcardPath + "/" + getPackageName();

        String netBaseUrl = intent.getStringExtra("urlPrefix") + "/" + getPackageName() + "_patches";
        String md5OfBaseApk = "";
        String patchFileName = "patch.patch";
        String patchDownloadPath = "";
        String newPackagePath = "";
        String oldPackagePath = "";
        try {
            oldPackagePath = getPackageManager().getApplicationInfo(getPackageName(), 0).sourceDir;
            md5OfBaseApk = HashUtils.getMd5OfFile(oldPackagePath);
            patchDownloadPath = cachePathPrefix + "/patches" + "/" + md5OfBaseApk + "/" + patchFileName;
            //clear tmp files
            File pendingRemoveDir = new File(cachePathPrefix);
            if (pendingRemoveDir.exists()) {
                FileUtils.deleteFiles(pendingRemoveDir);
//                this.deleteFile(cachePathPrefix);
            }
            //create the cache dir
            File file = new File(patchDownloadPath);
            file.getParentFile().mkdirs();
//            String newPackageFileName =getPackageName().replaceAll("\\.","_");
            String newPackageFileName = "new";
            newPackagePath = cachePathPrefix + "/" + newPackageFileName + ".apk";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        //Do your downloading here.
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(netBaseUrl + "/" + md5OfBaseApk + "/" + patchFileName);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(false);
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.setRequestProperty("Connection", "Keep-Alive");
//            String username = "[username]";
//            String password = "[password]";
            String userInfo = url.getUserInfo();
            if (userInfo != null && !"".equals(userInfo.trim()) && userInfo.indexOf(":") != -1) {
                String[] info = userInfo.split(":");
                String username = info[0];
                String password = info[1];
                String credential = username + ":" + password;
                String encodingCredential = Base64.encodeToString(credential.getBytes(), 0);
                connection.setRequestProperty("Authorization", "Basic " + encodingCredential);
            }
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
            }
            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(patchDownloadPath);
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            int progress = 0;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
//                    publishProgress((int) (total * 100 / fileLength));
                    //Log down the progress
                    progress = (int) (total * 100 / fileLength);
                output.write(data, 0, count);
            }

            //do the patching
            try {
//            PatchUtils.patch(oldPackagePath,newPackagePath,patchDownloadPath);
                boolean patchingResult = EasyIncrementalUpdate.patch(oldPackagePath, newPackagePath, patchDownloadPath);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //ask the installing
            ApkUtils.installApk(this, newPackagePath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
                // ignore this exception
            }
            if (connection != null)
                connection.disconnect();
        }

    }
}
