package com.jaggie.autoupdatelib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Jaggie on 2018/4/19.
 */

public class AutoUpdateMain  {

    public final static int TEST_UPDATE_PERMISSIONS_REQUEST_CODE = 99;

    /**
     * Will check the permission and ask for the permission
     *
     * @param mContext
     * @param patchServerUrlPre
     */
    public static void tryToStartTheCheckingService(Activity mContext, String patchServerUrlPre) {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(mContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.



                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.

            }

            ActivityCompat.requestPermissions(mContext,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TEST_UPDATE_PERMISSIONS_REQUEST_CODE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mContext.onRequestPermissionsResult(TEST_UPDATE_PERMISSIONS_REQUEST_CODE, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},new int[]{PackageManager.PERMISSION_GRANTED});
            }else{
               startTheCheckingServiceStandlone(mContext, patchServerUrlPre);
            }
        }
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


    //dynamics proxy method researching
    private static AppCompatActivity genProxy(AppCompatActivity activity) {
        InvocationHandler handler =new RequestPermissionsResultCallbackProxy(activity);
        ActivityCompat.OnRequestPermissionsResultCallback proxied = (ActivityCompat.OnRequestPermissionsResultCallback) java.lang.reflect.Proxy.newProxyInstance(
                activity.getClass().getClassLoader(),
                new Class[]{ActivityCompat.OnRequestPermissionsResultCallback.class},
                handler);

        return (AppCompatActivity) proxied;
    }

    static class RequestPermissionsResultCallbackProxy implements InvocationHandler {

        final Object realObject;

        public RequestPermissionsResultCallbackProxy(Object real) {
            realObject = real;
        }

        @Override
        public Object invoke(Object target, Method m, Object[] args) throws Throwable {
            try {
                if (m.getName().equals("onRequestPermissionsResult")) {
                    intercept();
                    return m.invoke(realObject, args);
                } else {
                    return m.invoke(realObject, args);
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        public void intercept() {
            System.out.println("wrapper onRequestPermissionsResult");
        }

    }

}
