package com.jaggie.autoupdatelib;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ren.yale.android.intremetalupdate.EasyIncrementalUpdate;

/**
 * Created by Jaggie on 2018/3/21.
 */

public class DownloadIntentService extends IntentService {

    private static final String TAG = "TestUpdate";

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    private String pathSeparator = File.separator;
    private String patchServerUrlPre;

    private static final String KEY_OF_PATCH_SERVER_URL = "urlPre";
    private static final String KEY_OF_IS_SILENT_DOWNLOAD="isSilentDownload";
    private static final int NOTIFICATION_S_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID="auto_update";

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        disableSSLCertificateChecking();
        return super.onStartCommand(intent, flags, startId);
    }

    //build the notification
    private Notification genNotification() {
        //创建NotificationCompat.Builder
        if(Build.VERSION.SDK_INT>=26) {
            createNotificationChannel();
            mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        }else{
            mBuilder = new NotificationCompat.Builder(this);

        }
        mBuilder.setSmallIcon(R.drawable.ic_download_nc_small);
//        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        mBuilder.setContentTitle("AutoUpdate");//设置标题
//        mBuilder.setContentText("详细内容:正在下载……");//设置详细内容
        mBuilder.setTicker("this is ticker");//首先弹出来的，用于提醒的一行小字
        mBuilder.setOngoing(true);//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
        mBuilder.setWhen(System.currentTimeMillis());//设置时间
        mBuilder.setProgress(0, 0, false);//设置进度条，true为不确定(持续活动)的进度条

        Notification notification = mBuilder.build();

        //创建通知栏之后通过给他添加.flags属性赋值
        notification.flags |= Notification.FLAG_NO_CLEAR;//不自动清除
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;//前台服务标记
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;//标记声音或者震动一次
        notification.flags |= Notification.FLAG_AUTO_CANCEL;//点击通知之后，取消状态栏图标
        /*这里的标识位有很多，根据情况选择*/
//        notificationManager.notify(ID_NOTIFICATION_DOWNLOAD, notification);//显示notification
        return notification;
    }

    private String sizeOfByte(int length){
        if(length<1024*1024){
            return String.format("%.2f", (float)length/1024)+"KB";
        }else{
            return String.format("%.2f", (float)length/(1024*1024))+"MB";
        }

    }

    @TargetApi(26)
    private void createNotificationChannel(){
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
            "AutoUpdate", NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableLights(true); //是否在桌面icon右上角展示小红点
        channel.setLightColor(Color.RED); //小红点颜色
//        channel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
        mNotificationManager.createNotificationChannel(channel);
        }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean isSilentDownload = intent.getBooleanExtra(KEY_OF_IS_SILENT_DOWNLOAD,false);
        if (!isSilentDownload) {
            startForeground(NOTIFICATION_S_ID, genNotification());
        }
        //  url prefix of patch storage server
        String patchServerUrl = intent.getStringExtra(KEY_OF_PATCH_SERVER_URL);

        //local storage root path of device
        String storagePath = this.getExternalFilesDir(null).getPath();

        // define path of storing the patches in device
        String patchesPathPrefix = storagePath + pathSeparator + getPackageName() + "_patches";

        //define path of storing the patches online
        String netBaseUrl = patchServerUrl + pathSeparator + getPackageName() + "_patches";

        //md5 value of current apk
        String md5OfOldApk = "";

        //patch file file_name
        String fixedPatchFileName = "patch.patch";

        //local patch file download path
        String patchDownloadPath = "";

        //local new apk storage path
        String newPackagePath = "";

        //local old apk storage path
        String oldPackagePath = "";

        //define if need to be patched for current apk
        boolean canBePatched = false;


        try {
            oldPackagePath = getPackageManager().getApplicationInfo(getPackageName(), 0).sourceDir;
            md5OfOldApk = HashUtils.getMd5OfFile(oldPackagePath);
            patchDownloadPath = patchesPathPrefix + pathSeparator + md5OfOldApk + pathSeparator + fixedPatchFileName;
            //clear tmp files
            File pendingRemoveDir = new File(patchesPathPrefix);
            if (pendingRemoveDir.exists()) {
                FileUtils.deleteFiles(pendingRemoveDir);
//                this.deleteFile(patchesPathPrefix);
            }
            //create the cache dir
            File file = new File(patchDownloadPath);
            file.getParentFile().mkdirs();
//            String newPackageFileName =getPackageName().replaceAll("\\.","_");
            String newPackageFileName = "new";
            newPackagePath = patchesPathPrefix + pathSeparator + newPackageFileName + ".apk";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //Do your downloading here.
        InputStream input = null;
        OutputStream output = null;
        HttpsURLConnection connection = null;
        try {
            String latestApkDownLoadLink = netBaseUrl + pathSeparator + getPackageName() + "_latest.apk";
            String currentTagCheckLink = netBaseUrl + pathSeparator + md5OfOldApk + ".tag";
            connection = doTheConnection(currentTagCheckLink);
            if (connection != null) {
                //no new version updated
                Log.d(TAG, "no new version updated");
            } else {
                String downloadPath = "";
                HttpsURLConnection patchConnection = doTheConnection(netBaseUrl + pathSeparator + md5OfOldApk + pathSeparator + fixedPatchFileName);
                if (patchConnection != null) {
                    //there is a patch for this version, download it and use it
                    Log.d(TAG, "there is a patch for this version, download it and use it");

                    connection = patchConnection;
                    downloadPath = patchDownloadPath;
                    canBePatched = true;
                } else {
                    // no patch found, need to download the whole package to upgrade
                    connection = doTheConnection(latestApkDownLoadLink);
                    downloadPath = newPackagePath;
                    if (connection!=null) {
                        Log.d(TAG, "no patch found, need to download the whole package to upgrade");
                    }else {
                        Log.d(TAG, "no patch and latest package found in server!");
                    }
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                if (connection != null) {
                    try {
                    int fileLength = connection.getContentLength();
                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream(downloadPath);
//                    output =this.openFileOutput(downloadPath,MODE_PRIVATE);
                    byte data[] = new byte[4096];
                    int total = 0;
                    int count;
                    int progress = 0;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) { // only if total length is known
//                    publishProgress((int) (total * 100 / fileLength));
                            //Log down the progress
                            progress = (int) (total * 100 / fileLength);
                        }
                        output.write(data, 0, count);
                        if(!isSilentDownload) {
                            mBuilder.setProgress((int) fileLength, total, false);
                            if (canBePatched) {
                                mBuilder.setContentText("Patch downloading[" + sizeOfByte(fileLength) + "]: " + progress + "%");
                            } else {
                                mBuilder.setContentText("Whole package downloading[" + sizeOfByte(fileLength) + "]: " + progress + "%");
                            }
                            mNotificationManager.notify(NOTIFICATION_S_ID, mBuilder.build());
                        }
                    }


                        //do the patching, the new package will be generated in the newPackagePath
                        if (canBePatched) {
                            EasyIncrementalUpdate.patch(oldPackagePath, newPackagePath, patchDownloadPath);
                        }
                        ApkUtils.installApk(this, newPackagePath);

                        //can cancel the notification when it exists
                        if(!isSilentDownload) {
                            mNotificationManager.cancel(NOTIFICATION_S_ID);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            if (output != null)
                                output.close();
                            if (input != null)
                                input.close();
                            if (connection != null)
                                connection.disconnect();
                        } catch (IOException ignored) {
                            // ignore this exception
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (output != null ){
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException ignored) {
                // ignore this exception
            }

        }

    }

    private HttpsURLConnection doTheConnection(String link) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(link);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(false);
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.setRequestProperty("Connection", "Keep-Alive");
//            String username = "[username]";
//            String password = "[password]";
            String userInfo = url.getUserInfo();
            if (!TextUtils.isEmpty(userInfo) && userInfo.indexOf(":") != -1) {
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

            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
//                throw new RuntimeException("Server returned HTTP " + connection.getResponseCode()
//                        + " " + connection.getResponseMessage());
                Log.d(TAG, "http statuscode:"+connection.getResponseCode()+"---["+link+"]");
                connection.getInputStream().close();
                connection.disconnect();
                connection=null;
                return null;
            } else {
                Log.d(TAG, "http statuscode:"+connection.getResponseCode()+"---["+link+"]");
                return connection;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (connection != null)
                connection.disconnect();
        } finally {

        }
        return null;
    }

    // ignore the content server ssl cert warning
    private static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    public static Intent getDownloadIntent(Context context, String patchServerUrlPre,boolean isSilentDownload) {
        Intent downloadIntent = new Intent(context, DownloadIntentService.class);
        downloadIntent.putExtra(KEY_OF_PATCH_SERVER_URL, patchServerUrlPre);
        downloadIntent.putExtra(KEY_OF_IS_SILENT_DOWNLOAD, isSilentDownload);
        return downloadIntent;
    }

}
