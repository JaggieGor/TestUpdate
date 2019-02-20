package com.jaggie.autoupdatelib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

/**
 * Created by Jaggie on 2018/4/19.
 */

public class AutoUpdateMain {

    public final static int TEST_UPDATE_PERMISSIONS_REQUEST_CODE = 99;

    /**
     * Will check the permission and ask for the permission
     *
     * @param mContext
     * @param patchServerUrlPre
     */
    public static void tryToStartTheCheckingService(final Activity mContext, final String patchServerUrlPre) {
//        if (ContextCompat.checkSelfPermission(mContext,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED
//                ) {
//
//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(mContext,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//
//                // Show an expanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//
//            } else {
//
//                // No explanation needed, we can request the permission.
//
//
//                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
//                // app-defined int constant. The callback method gets the
//                // result of the request.
//
//            }
//
//            ActivityCompat.requestPermissions(mContext,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    TEST_UPDATE_PERMISSIONS_REQUEST_CODE);
//        } else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                mContext.onRequestPermissionsResult(TEST_UPDATE_PERMISSIONS_REQUEST_CODE, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new int[]{PackageManager.PERMISSION_GRANTED});
//            } else {
//            }
//        }

        RxPermissions rxPermissions = new RxPermissions(mContext);
        rxPermissions
                .requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new io.reactivex.functions.Consumer<Permission>() {
                               @Override
                               public void accept(Permission permission) throws Exception {
                                   if (permission.granted) {
                                       // `permission.name` is granted !
                                       startTheCheckingServiceStandlone(mContext, patchServerUrlPre);
                                   } else if (permission.shouldShowRequestPermissionRationale) {
                                       // Denied permission without ask never again
                                       //exit the app
                                       Toast.makeText(mContext,"you need to allow to enable the permission to make this feature available",Toast.LENGTH_LONG).show();

                                   } else {
                                       // Denied permission with ask never again
                                       // Need to go to the settings
                                       Toast.makeText(mContext,"you need to go to the settings to enable the permission to make this feature available",Toast.LENGTH_LONG).show();

                                   }
                               }
                           }
                );


    }

    /**
     * Be sure call the method after getting the enough permissions
     *
     * @param mContext
     * @param patchServerUrlPre
     */
    public static void startTheCheckingServiceStandlone(Context mContext, String patchServerUrlPre) {
        mContext.startService(DownloadIntentService.getDownloadIntent(mContext, patchServerUrlPre, false));
    }

    public static void startTheCheckingServiceStandlone(Context mContext, String patchServerUrlPre, boolean isSilentDownload) {
        mContext.startService(DownloadIntentService.getDownloadIntent(mContext, patchServerUrlPre, isSilentDownload));
    }

}
