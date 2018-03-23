package com.jaggie.testupdate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    public final static int  MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE=99;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }else {
            startTheCheckingService();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        try {
//            String sourceApkPath =getPackageManager().getApplicationInfo(getPackageName(), 0).sourceDir;
//            String md5OfSourceApk=  HashUtils.getMd5OfFile(sourceApkPath);
//            Toast.makeText(this, sourceApkPath+"\n"+md5OfSourceApk, Toast.LENGTH_LONG).show();
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }


    }


    private void startTheCheckingService(){
        Intent downloadIntent = new Intent(this, DownloadIntentService.class);
        downloadIntent.putExtra("urlPrefix",Constants.url);
        startService(downloadIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // storage-related task you need to do.
                    startTheCheckingService();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    //no permission ,please exit this app
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
                return;
            }
        }
    }

}
