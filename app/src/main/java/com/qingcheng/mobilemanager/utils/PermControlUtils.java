package com.qingcheng.mobilemanager.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

/**
 * Created by root on 18-1-30.
 */

public class PermControlUtils {
    private static final String TAG = "PmUtils";
    public static final String UID = "uid";
    public static final String EXTRA_STATE = "state";

    /**
     * Get application name by passing the  package name
     * @param context Context
     * @param pkgName package name
     * @return the application name
     */
    public static String getApplicationName(Context context, String pkgName) {
        if (pkgName == null) {
            return null;
        }
        String appName = null;
        try {
            PackageManager pkgManager = context.getPackageManager();
            ApplicationInfo info = pkgManager.getApplicationInfo(pkgName, 0);
            appName = pkgManager.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }

    /**
     * Get application icon by passing the  package name
     * @param context Context
     * @param pkgName package name
     * @return the application icon
     */
    public static Drawable getApplicationIcon(Context context, String pkgName) {
        Drawable appIcon = null;
        try {
            PackageManager pkgManager = context.getPackageManager();
            ApplicationInfo info = pkgManager.getApplicationInfo(pkgName, 0);
            appIcon = pkgManager.getApplicationIcon(info);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w("@M_" + TAG, "get icon is null");
        }
        return appIcon;
    }

    public static boolean isSystemApp(ApplicationInfo applicationInfo) {
        if (applicationInfo != null) {
            int appId = UserHandle.getAppId(applicationInfo.uid);
            return ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ||
                    (appId == Process.SYSTEM_UID);
        } else {
            Log.d(TAG, "isSystemApp() return false with null packageName");
            return false;
        }
    }
}
