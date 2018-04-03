package com.qingcheng.mobilemanager.utils;

import java.util.ArrayList;
import java.util.List;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.provider.Settings;
import com.qingcheng.mobilemanager.application.MyApplication;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanActivity;
import java.io.File;
import android.os.Environment;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import android.os.UserHandle;
import java.lang.reflect.Method;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.content.Intent;

public class CleanUtils {
	private static long cacheSize = 0;
    public static Context getContext(){
        return MyApplication.getContext();
    }
	public static boolean isPowerSaveMode() {
		return (Settings.Global.getInt(getContext().getContentResolver(), "qc_key_superpowermode", 0) == 1
				|| Settings.Global.getInt(getContext().getContentResolver(), "qc_key_powermode", 0) == 1);
	}
	public static boolean isPermissionOpened() {
		return Settings.System.getInt(getContext().getContentResolver(), "permission_control_state", 0) > 0;
	}

    public static List<RunningAppProcessInfo> getRunningProcess(){  
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE); 
        List<RunningAppProcessInfo> run = am.getRunningAppProcesses();  
        List<RunningAppProcessInfo> list = new ArrayList<ActivityManager.RunningAppProcessInfo>();    
        for(RunningAppProcessInfo ra : run){  
            if(ra.processName.equals("system") 
			|| ra.processName.equals("com.qingcheng.mobilemanager")
			|| ra.processName.equals("com.greenorange.qcpower")){  
                continue;  
            }  
            list.add(ra);  
        }  
        return list;  
    }  
    public static String getFormatSize(double size) {  
        double kiloByte = size / 1024;  
        if (kiloByte < 1) {  
        	return "0K";
        }  
  
        double megaByte = kiloByte / 1024;  
        if (megaByte < 1) {  
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));  
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)  
                    .toPlainString() + "KB";  
        }  
  
        double gigaByte = megaByte / 1024;  
        if (gigaByte < 1) {  
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));  
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)  
                    .toPlainString() + "MB";  
        }  
  
        double teraBytes = gigaByte / 1024;  
        if (teraBytes < 1) {  
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));  
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)  
                    .toPlainString() + "GB";  
        }  
        BigDecimal result4 = new BigDecimal(teraBytes);  
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()  
                + "TB";  
    }  
    public static long getFolderSize(File file) throws Exception {  
        long size = 0;  
        try {  
            File[] fileList = file.listFiles();  
            for (int i = 0; i < fileList.length; i++) {  
                if (fileList[i].isDirectory()) {  
                    size = size + getFolderSize(fileList[i]);  
                } else {  
                    size = size + fileList[i].length();  
                }  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        return size;  
    }  
 public static long getTotalCacheSize() throws Exception {
    	cacheSize = 0l;
	PackageManager pm = getContext() .getPackageManager();
	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	List<ResolveInfo> mResolveInfo  = pm.queryIntentActivities(mainIntent, 0);
	
	for(int i=0; i< mResolveInfo.size(); i++){
		ResolveInfo info = mResolveInfo.get(i);
		if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
			invokePMGetPackageSizeInfo(pm, info.activityInfo.packageName);
		} 
	}		
    	return cacheSize;
    }
    public static void cleanAllCache() {
		PackageManager pm = getContext() .getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> mResolveInfo	= pm.queryIntentActivities(mainIntent, 0);
		
		for(int i=0; i< mResolveInfo.size(); i++){
			ResolveInfo info = mResolveInfo.get(i);
			if ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
				pm.deleteApplicationCacheFiles(info.activityInfo.packageName, null);
			} 
		}		
    }
  
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
	public static void cleanMemory() {
        long beforeCleanMemory = getAvailMemory();
        ArrayList<String> filterProcessNames = new ArrayList<>();
        for (int k = 0; k < TaskCleanActivity.mFilterProcessNames.length; k++) {
            filterProcessNames.add(TaskCleanActivity.mFilterProcessNames[k]);
        }

        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        RunningAppProcessInfo runningAppProcessInfo = null;
        List<RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
        for (int i = 0; i < runningAppProcessInfoList.size(); ++i) {
            runningAppProcessInfo = runningAppProcessInfoList.get(i);
            String processName = runningAppProcessInfo.processName;
            if (processName.equals("com.qingcheng.theme") || filterProcessNames.contains(processName))
                continue;
            killProcessByRestartPackage(processName);
        }
        long afterCleanMemory = getAvailMemory();
    }
	
	public static void killProcessByRestartPackage(String packageName) {
		ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.restartPackage(packageName);
		System.gc();
	}
	public static long getAvailMemory() {
		ActivityManager activityManager=(ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		return memoryInfo.availMem / (1024 * 1024);
	}
	public static long getTotalMemory() {
		String filePath = "/proc/meminfo";
		String lineString;
		String[] stringArray;
		long totalMemory = 0;
		try {
			FileReader fileReader = new FileReader(filePath);
			BufferedReader bufferedReader = new BufferedReader(fileReader,1024 * 8);
			lineString = bufferedReader.readLine();
			stringArray = lineString.split("\\s+");
			totalMemory = Integer.valueOf(stringArray[1]).intValue();
			bufferedReader.close();
		} catch (IOException e) {
		}
		return totalMemory / 1024;
	}
    public static long invokePMGetPackageSizeInfo(PackageManager pm, String pkgName) throws Exception {
		if (pkgName != null) {
			try {
				Method getPackageSizeInfo = pm.getClass().getMethod(
					"getPackageSizeInfo",
					String.class, IPackageStatsObserver.class );
				getPackageSizeInfo.invoke(pm,
					pkgName,  new IPackageStatsObserver.Stub() {
			            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
						cacheSize += stats.cacheSize;
			            }
			        } );
				return cacheSize;
			} catch (Exception e) {
			}
		}
		return 0l;
    }

}
