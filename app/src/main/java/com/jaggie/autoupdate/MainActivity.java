package com.jaggie.autoupdate;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jaggie.autoupdatelib.AutoUpdateMain;


public class MainActivity extends AppCompatActivity {

//    private String url = "https://netmobile:MobP8w@www.hkg1vl0077.p2g.netd2.hsbc.com.hk/mobile/mobileApp/native/1514g/jaggie";

    private String url="https://jaggiegor.github.io/hot-fix-artifacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AutoUpdateMain.tryToStartTheCheckingService(this, url);
    }

}
