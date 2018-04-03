package com.qingcheng.mobilemanager.ui.fragment;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.icu.text.ListFormatter;
import android.content.pm.permission.RuntimePermissionPresentationInfo;
import android.content.pm.permission.RuntimePermissionPresenter;

import com.qingcheng.mobilemanager.utils.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import com.qingcheng.mobilemanager.R;
/**
 * Created by cunhuan.liu on 17/2/9.
 */
public class AppsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG="AppsFragment";
    private LayoutInflater mInflater;
    private ListView mListView;
    private DataAsyncLoader mAsyncTask;
    private AppMatchPermAdapter mApdater;
    private List<AppInfo> mPkgList = new ArrayList<AppInfo>();
    private int changePosition = -1;
    private int INTENT_REQUSET=10000;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        return inflater.inflate(R.layout.app_match_permission, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView lv = (ListView) view.findViewById(R.id.list);
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setTextFilterEnabled(true);
        LogUtil.i();
        mListView = lv;
        mApdater = new AppMatchPermAdapter();
        mListView.setAdapter(mApdater);
        load();
    }

    @Override
    public void onResume() {
        LogUtil.i();
        super.onResume();
       // load();
    }

    private void load() {
        mAsyncTask = (DataAsyncLoader) new DataAsyncLoader().execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
            LogUtil.d("cancel task in onDestory()");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // start new activity to manage app permissions
        changePosition = position;
        Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mPkgList.get(position).packageName);
        try {
            startActivityForResult(intent,INTENT_REQUSET);
        } catch (ActivityNotFoundException e) {
            LogUtil.d("No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }


    // View Holder used when displaying views
    static class AppViewHolder {
        TextView mAppName;
        ImageView mAppIcon;
        TextView mAppSize;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==  INTENT_REQUSET){

            final RuntimePermissionPresenter presenter = RuntimePermissionPresenter.getInstance(getActivity());
            presenter.getAppPermissions(mPkgList.get(changePosition).packageName, new RuntimePermissionPresenter.OnResultCallback() {
                @Override
                public void onGetAppPermissions(@NonNull List<RuntimePermissionPresentationInfo> permissions) {
                    int permissionCount = permissions.size();
                    int count = 0;
                    LogUtil.d("PKG=" + mPkgList.get(changePosition).appName+ "---permissionCount=" + permissionCount);

                    for (int i = 0; i < permissionCount; i++) {
                        RuntimePermissionPresentationInfo permission = permissions.get(i);

                        LogUtil.d("permission=" + permission.getLabel() + "---IS=" + permission.isGranted()
                                + "STANDARD=" + permission.isStandard()
                        );

                        if (permission.isGranted()) {
                            if (permission.isStandard()) {
                                count++;
                            }
                        }

                    }

                    mPkgList.get(changePosition).summary =  getString(R.string.permission_count, count);

                    mApdater.notifyDataSetChanged();
                }
            }, null);
        }
    }

    class AppMatchPermAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return mPkgList.size();
        }

        @Override
        public Object getItem(int position) {
            return mPkgList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.item_apilications_permission, null);

                holder = new AppViewHolder();
                holder.mAppName = (TextView) convertView
                        .findViewById(R.id.app_name);
                holder.mAppIcon = (ImageView) convertView
                        .findViewById(R.id.app_icon);
                holder.mAppSize = (TextView) convertView
                        .findViewById(R.id.app_size);
                convertView.setTag(holder);
            } else {
                holder = (AppViewHolder) convertView.getTag();
            }
            AppInfo appItem = mPkgList.get(position);
            holder.mAppName.setText(appItem.appName);
            holder.mAppIcon.setImageDrawable(appItem.appIcon);
            holder.mAppSize.setText(appItem.summary);

            return convertView;
        }
    }

    class AppInfo {
        public String appName;
        public Drawable appIcon;
        public String packageName;
        public String summary;

    }

    private class DataAsyncLoader extends AsyncTask {
        private String finalPackageName = null;

        @Override
        protected Void doInBackground(Object... params) {
            LogUtil.i();
            final List<AppInfo> appList = new ArrayList<>();
            final PackageManager pm = getActivity().getPackageManager();
            appList.clear();
            finalPackageName = null;
            /**
             * 获取已安装的包
             */
            List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);

            Collections.sort(installedPackages, new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo o1, PackageInfo o2) {
                    if (o1.packageName != null && o2.packageName != null) {
                        return o1.packageName.compareTo(o2.packageName);
                    }
                    return 0;
                }
            });
            final Iterator iterator = installedPackages.iterator();

            while (iterator.hasNext()) {
                final PackageInfo pkgInfo = (PackageInfo) iterator.next();

                if (pkgInfo.packageName == null) {
                    continue;
                }
                ApplicationInfo appInfo=pkgInfo.applicationInfo;
                if(isSystemAPP(appInfo)){
                    continue;
                }
                if (isCancelled()) {
                    return null;
                }

                final RuntimePermissionPresenter presenter = RuntimePermissionPresenter.getInstance(getActivity());
                presenter.getAppPermissions(pkgInfo.packageName, new RuntimePermissionPresenter.OnResultCallback() {
                    @Override
                    public void onGetAppPermissions(@NonNull List<RuntimePermissionPresentationInfo> permissions) {
                        final int permissionCount = permissions.size();
                        if (permissionCount == 0) {
                            //notifyDataSetChanged(null, pkgInfo.packageName);
                            return;
                        }
                        if (isCancelled()) {
                            return ;
                        }
                        int count = 0;
                        LogUtil.d("PKG=" + pkgInfo.applicationInfo.loadLabel(pm) + "---permissionCount=" + permissionCount);

                        for (int i = 0; i < permissionCount; i++) {
                            RuntimePermissionPresentationInfo permission = permissions.get(i);

                            LogUtil.d("permission=" + permission.getLabel() + "---IS=" + permission.isGranted()
                                    + "STANDARD=" + permission.isStandard()
                            );

                            if (permission.isGranted()) {
                                if (permission.isStandard()) {
                                    count++;
                                }
                            }
                        }

                        if (permissionCount > 0) {
                            AppInfo appInfo = new AppInfo();
                            appInfo.appName = pkgInfo.applicationInfo.loadLabel(pm).toString();
                            appInfo.appIcon = pkgInfo.applicationInfo.loadIcon(pm);
                            appInfo.packageName = pkgInfo.packageName;
                            appInfo.summary = getString(R.string.permission_count, count);
                            appList.add(appInfo);
                            notifyDataSetChanged(appList, pkgInfo.packageName);
                            // onPermissionSummaryResult(grantedStandardCount, requestedCount,
                            //        grantedAdditionalCount, grantedStandardLabels);
                        }
                    }
                }, null);
                if (!iterator.hasNext()) {
                    finalPackageName = pkgInfo.packageName;
                }
            }
            return null;
        }

        private void notifyDataSetChanged(List<AppInfo> appList,String packageName) {
            if(appList!=null){
                mPkgList = appList;
                finalPackageName = null;
                mApdater.notifyDataSetChanged();
            }
            /*if (finalPackageName != null && finalPackageName.equals(packageName) && appList != null) {
                Log.e(TAG,"notifyDataSetChanged,the last mpkgList.size():"+mPkgList.size());
                mPkgList = appList;
                finalPackageName = null;
                mApdater.notifyDataSetChanged();
            }*/
        }

        private boolean isSystemAPP(ApplicationInfo info){
            if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                return false;
            } else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                return false;
            }
            return true;
        }
    }
}
