package com.qingcheng.mobilemanager.taskmanager;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.qingcheng.mobilemanager.bean.CheckedPermRecord;
import com.qingcheng.mobilemanager.utils.PermControlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 18-1-30.
 */

public class WhiteListManager {
    private static final String TAG = "WhiteListManager";
    private static final String PKG_TAG = "pkg";
    private List<CheckedPermRecord> mAutoBootList = new ArrayList<CheckedPermRecord>();
    private List<ApplicationInfo> packages=new ArrayList<ApplicationInfo>();
    private Context mContext;
    private int mUid;
    private AppOpsManager mAppOpsManager;

    public WhiteListManager(Context context, int uid) {
        mContext = context;
        mUid = uid;
    }

    public List<CheckedPermRecord> getBootList() {
        return mAutoBootList;
    }

    public void setPkgWhiteListPolicy(CheckedPermRecord info) {
        for (CheckedPermRecord record : mAutoBootList) {
            if (info.mPackageName.equals(record.mPackageName)) {
                if (info.getStatus() == record.getStatus()) {
                    Log.e(TAG, "setPgkWhiteListPolicy, policy:same");
                    return;
                }
                record.setStatus(info.getStatus());
                mAppOpsManager.setMode(AppOpsManager.OP_RUN_IN_BACKGROUND,record.mUid,record.mPackageName,info.getStatus());
                break;
            }
        }
    }

    public void initAutoBootList(boolean isShowingSystemApp) {
        Log.w(TAG,"initAutoBootList");
        mAutoBootList.clear();
        List<CheckedPermRecord> allowList = new ArrayList<>();
        List<CheckedPermRecord> ignoreList = new ArrayList<>();
        mAppOpsManager = mContext.getSystemService(AppOpsManager.class);
        packages = getAllPackages(mContext, isShowingSystemApp);
        Log.w(TAG,"initAutoBootList,listsize:"+packages.size());
        int x=0;
        for(ApplicationInfo pkg:packages){
            x++;
            boolean enable = true;
            List<AppOpsManager.PackageOps> pkgOps=new ArrayList<>();
            pkgOps=mAppOpsManager.getOpsForPackage(pkg.uid, pkg.packageName, new int[]{AppOpsManager.OP_RUN_IN_BACKGROUND});
            if(pkgOps!=null) {
                for (AppOpsManager.PackageOps i : pkgOps) {
                    List<AppOpsManager.OpEntry> ops=i.getOps();
                    if(ops!=null){
                        for(AppOpsManager.OpEntry entry:ops){
                            Log.w(TAG, "x:"+x+",mode:"+entry.getMode());
                            if (entry.getMode() == AppOpsManager.MODE_ALLOWED) {
                                allowList.add(new CheckedPermRecord(pkg.packageName,pkg.uid,"",entry.getMode()));
                            } else {
                                ignoreList.add(new CheckedPermRecord(pkg.packageName,pkg.uid,"",entry.getMode()));
                            }
                        }
                    }
                }
            }else {
                Log.w(TAG, "x:"+x+",pkgOps:"+pkgOps);
                //0: allowed 1:ignored
                ignoreList.add(new CheckedPermRecord(pkg.packageName,pkg.uid,"",1));

            }
        }
        mAutoBootList.addAll(allowList);
        mAutoBootList.addAll(ignoreList);
    }

    private List<ApplicationInfo> getAllPackages(Context context, boolean isShowingSystemApp) {
        Log.w(TAG,"getAllPackages");
        List<String> allPackages = new ArrayList<String>();
        List<ApplicationInfo> allPkgInfo=new ArrayList<ApplicationInfo>();
        try {
            //filter condition
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager pm=context.getPackageManager();
            List<ResolveInfo> parlist=pm.queryIntentActivities(mainIntent,0);
            if (parlist != null) {
                if (parlist != null) {
                    for (int j = 0; j < parlist.size(); j++) {
                        ResolveInfo info = parlist.get(j);
                        String packageName =
                                (info.activityInfo != null) ? info.activityInfo.packageName : null;
                        ApplicationInfo appinfo= null;
                        try {
                            appinfo = pm.getApplicationInfo(packageName,0);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        if (PermControlUtils.isSystemApp(appinfo) && !isShowingSystemApp) {
                            continue;
                        }
                        if (packageName != null
                                && !allPackages.contains(packageName)) {

                            Log.d(TAG, "getAllPackages, packageName:"
                                    + packageName);
                            allPackages.add(packageName);
                            allPkgInfo.add(appinfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Should never happend
            Log.i(TAG,e.toString());
        }
        return allPkgInfo;
    }
}
