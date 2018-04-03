package com.qingcheng.mobilemanager.application;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Description:
 * Copyright  : Copyright (c) 2016
 * Company    : RGK
 * Author     : qi.guan
 * Date       : 2016/12/20 16:36
 */

public class MyApplication extends Application {

    private static Context context;
    private static Handler handler;
    private static int mainThreadId;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        handler = new Handler();
        mainThreadId = android.os.Process.myTid();

        //Thread.setDefaultUncaughtExceptionHandler(new MyHandler());
    }

    public static Context getContext(){
        return context;
    }
    public static Handler getHandler(){
        return handler;
    }
    public static int getMainThreadId(){
        return mainThreadId;
    }

    /*class MyHandler implements Thread.UncaughtExceptionHandler{
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            throwable.printStackTrace();

            try {
                PrintWriter writer = new PrintWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MobileManagerError.log");
                throwable.printStackTrace(writer);
                writer.flush();
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Process.killProcess(android.os.Process.myPid());
        }
    }*/
}
