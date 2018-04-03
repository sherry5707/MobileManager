package com.qingcheng.mobilemanager.utils;

import android.util.Log;


/**
 * Created by cunhuan.liu on 17/2/8.
 */

public class LogUtil {

    //打印重要的log 使用HUAN
    //只打印流程 使用-->
    //打印所有  HUA
    private static String TAG = "HUAN";
    private static String FLOW_TAG = "-->";

    public static boolean allowD = true;
    public static boolean allowI = false;

    private static String generateTag(StackTraceElement caller) {
        String tag = "%s.%s";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);// 获取到类名
        //tag = String.format(tag, callerClazzName, caller.getMethodName(),caller.getLineNumber()); // 替换
        tag = String.format(tag, callerClazzName, caller.getMethodName());
        return tag;
    }

    private static StackTraceElement getCallerStackTraceElement() {
        // Log.i("HUAN-PermissionUtil", "" + Thread.currentThread().getStackTrace().toString());
        return Thread.currentThread().getStackTrace()[4];
    }

    public static void i() {

        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        if (allowI)
            Log.i("HUA  " + FLOW_TAG, tag);
    }

    public static void i(String content) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        if (allowI)
            Log.i(TAG + " " + tag + ":", content);
    }

    public static void d(String content) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        if (allowD)
            Log.d(TAG + " " + tag + ":", content);
    }

    public static void e(String content) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(TAG + " " + tag + ":", content);
    }
    public static void e(String content,Throwable t) {
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(TAG + " " + tag + ":", content,t);
    }
}
