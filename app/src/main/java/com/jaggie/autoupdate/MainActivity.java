package com.jaggie.autoupdate;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jaggie.autoupdatelib.AutoUpdateMain;

public class MainActivity extends AppCompatActivity {

    private String url ="https://netmobile:MobP8w@www.hkg1vl0077.p2g.netd2.hsbc.com.hk/mobile/mobileApp/native/1514g/jaggie";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AutoUpdateMain.tryToStartTheCheckingService(this,url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AutoUpdateMain.TEST_UPDATE_PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // storage-related task you need to do.
                    AutoUpdateMain.startTheCheckingServiceStandlone(this,url);

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
