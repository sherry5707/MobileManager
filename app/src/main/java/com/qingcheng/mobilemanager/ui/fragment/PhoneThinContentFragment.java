package com.qingcheng.mobilemanager.ui.fragment;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.activity.AppUninstallActivity;
import com.qingcheng.mobilemanager.ui.activity.PhoneThinActivity;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanSetActivity;
import com.qingcheng.mobilemanager.widget.CircleView;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import android.content.pm.IPackageStatsObserver;

/**
 * Created by chunfengliu on 2/12/18.
 */

public class PhoneThinContentFragment extends Fragment implements View.OnClickListener {

    private RecyclerView recyclerView;

    private Context mContext;

    private static final String TAG = "PhoneThinContent";

    private static final long NO_USE_LONG_TIME_INTERVAL = 1000 * 60 * 60 * 24 * 30L;

    public static final int VIEW_TYPE_SUMMARY = 0;

    public static final int VIEW_TYPE_APP_UNINSTALL = 1;

    public static final int VIEW_TYPE_VIDEO = 2;

    public static final int VIEW_TYPE_APK_CLEAN = 3;

    public static final int VIEW_TYPE_PICTURE = 4;

    private List<File> mVideoFiles;

    private List<File> mPictureFiles;

    private List<ApkInfo> mApkInfos = new ArrayList<>();

    private String[] mInstalledStrings;

    private RecyclerView.Adapter mAdapter;

    private boolean isImageCompleted = false;

    private boolean isVideoCompleted = false;

    private boolean isFileCompleted = false;

    private long mAllPackageDataSize = 0L;

    private int mClickType = -1;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, " on create view");
        mContext = getContext();
        mInstalledStrings = new String[]{getString(R.string.installed), getString(R.string.uninstalled)};
        View view = inflater.inflate(R.layout.fragment_phone_thin_content, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.id_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.addItemDecoration(new TaskCleanSetActivity.DividerItemDecoration(getResources().getDrawable(R.drawable.divider_big, null), TaskCleanSetActivity.DividerItemDecoration.VERTICAL_LIST));
        mAdapter = new PhoneThinContentAdapter();
        recyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((PhoneThinActivity) getActivity()).changeStatus(PhoneThinActivity.FRAGMENT_CONTENT);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            ((PhoneThinActivity) getActivity()).changeStatus(PhoneThinActivity.FRAGMENT_CONTENT);
        }

        ((PhoneThinContentAdapter) mAdapter).refreshViewForType(mClickType, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_video_go:
                ((PhoneThinActivity)getActivity()).changeFragment(PhoneThinCleaningFragment.FRAGMENT_VIDEO);
                mClickType = VIEW_TYPE_VIDEO;
                break;
            case R.id.tv_image_go:
                ((PhoneThinActivity)getActivity()).changeFragment(PhoneThinCleaningFragment.FRAGMENT_IMAGE);
                mClickType = VIEW_TYPE_PICTURE;
                break;
            case R.id.tv_apk_go:
                ((PhoneThinActivity)getActivity()).changeFragment(PhoneThinCleaningFragment.FRAGMENT_APK);
                mClickType = VIEW_TYPE_APK_CLEAN;
                break;
            case R.id.tv_uninstall_app_go:
                Intent intent = new Intent(mContext, AppUninstallActivity.class);
                mClickType = VIEW_TYPE_APP_UNINSTALL;
                startActivity(intent);
                break;
        }
    }

    class PhoneThinContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Integer> mContentViewTypes = new ArrayList<>();

        public PhoneThinContentAdapter() {
            mContentViewTypes.add(VIEW_TYPE_SUMMARY);
            mContentViewTypes.add(VIEW_TYPE_APP_UNINSTALL);
            mContentViewTypes.add(VIEW_TYPE_VIDEO);
            mContentViewTypes.add(VIEW_TYPE_APK_CLEAN);
            mContentViewTypes.add(VIEW_TYPE_PICTURE);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_SUMMARY) {
                view = LayoutInflater.from(mContext).inflate(R.layout.phone_thin_summary, parent, false);
                return new SummaryViewHolder(view);
            } else if (viewType == VIEW_TYPE_APP_UNINSTALL) {
                view = LayoutInflater.from(mContext).inflate(R.layout.phone_thin_app_uninstall, parent, false);
                return new AppUninstallViewHolder(view);
            } else if (viewType == VIEW_TYPE_VIDEO) {
                view = LayoutInflater.from(mContext).inflate(R.layout.phone_thin_video, parent, false);
                return new VideoViewHolder(view);
            } else if (viewType == VIEW_TYPE_APK_CLEAN) {
                view = LayoutInflater.from(mContext).inflate(R.layout.phone_thin_apk_clean, parent, false);
                return new ApkCleanViewHolder(view);
            } else if (viewType == VIEW_TYPE_PICTURE){
                view = LayoutInflater.from(mContext).inflate(R.layout.phone_thin_image, parent, false);
                return new PictureViewHolder(view);
            } else {
                return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SummaryViewHolder) {
                ((SummaryViewHolder) holder).textView.setText(getSummary());
                ((SummaryViewHolder) holder).circleView.setProgress(mStorageRate);
            } else if (holder instanceof AppUninstallViewHolder) {
                AppUninstallViewHolder appUninstallViewHolder =  ((AppUninstallViewHolder) holder);
                int count = getNoUseLongTimeAppCount();
                if (count == 0) {
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshViewForType(VIEW_TYPE_APP_UNINSTALL, true);
                        }
                    });
                } else {
                    appUninstallViewHolder.textViewCount.setText(getString(R.string.uninstall_app_count, count));
                    appUninstallViewHolder.textViewSize.setText(getProperSize(mAllPackageDataSize));
                    appUninstallViewHolder.textViewGo.setOnClickListener(PhoneThinContentFragment.this);
                }
            } else if (holder instanceof VideoViewHolder) {
                if (isVideoCompleted) {
                    VideoViewHolder videoViewHolder = (VideoViewHolder) holder;
                    videoViewHolder.textViewSize.setText(getVideo());
                    videoViewHolder.textViewGo.setOnClickListener(PhoneThinContentFragment.this);
                    int size = mVideoFiles.size();
                    if (size <= 0) {
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshViewForType(VIEW_TYPE_VIDEO, true);
                            }
                        });
                    }
                    if (size > 0) {
                        videoViewHolder.imageView0.setImageBitmap(getVideoThumb(mVideoFiles.get(0).getPath()));
                    }
                    if (size > 1) {
                        videoViewHolder.imageView1.setImageBitmap(getVideoThumb(mVideoFiles.get(1).getPath()));
                    }
                    if (size > 2) {
                        videoViewHolder.imageView2.setImageBitmap(getVideoThumb(mVideoFiles.get(2).getPath()));
                    }
                    if (size > 3) {
                        videoViewHolder.imageView3.setImageBitmap(getVideoThumb(mVideoFiles.get(3).getPath()));
                    }
                    Log.d(TAG, "video size=" + size);
                } else {
                    Log.d(TAG, "video is loading");
                }

            } else if (holder instanceof ApkCleanViewHolder) {
                if (isFileCompleted) {
                    ApkCleanViewHolder apkCleanViewHolder = (ApkCleanViewHolder) holder;
                    apkCleanViewHolder.textViewSize.setText(getApkCount());
                    apkCleanViewHolder.textViewGo.setOnClickListener(PhoneThinContentFragment.this);
                    int size = mApkInfos.size();
                    apkCleanViewHolder.relativeLayout0.setVisibility(size > 0 ? View.VISIBLE: View.GONE);
                    apkCleanViewHolder.relativeLayout1.setVisibility(size > 1 ? View.VISIBLE: View.GONE);
                    apkCleanViewHolder.relativeLayout2.setVisibility(size > 2 ? View.VISIBLE: View.GONE);
                    apkCleanViewHolder.relativeLayout3.setVisibility(size > 3 ? View.VISIBLE: View.GONE);
                    if (size <= 0) {
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshViewForType(VIEW_TYPE_APK_CLEAN, true);
                            }
                        });
                    }
                    if(size > 0){

                        apkCleanViewHolder.imageViewIcon0.setImageDrawable(mApkInfos.get(0).icon);
                        apkCleanViewHolder.textViewName0.setText(mApkInfos.get(0).name);
                        apkCleanViewHolder.textViewTag0.setText(mApkInfos.get(0).isInstalled ? mInstalledStrings[0] : mInstalledStrings[1]);
                        apkCleanViewHolder.textViewSize0.setText(mApkInfos.get(0).size);
                    }
                    if (size > 1) {
                        apkCleanViewHolder.imageViewIcon1.setImageDrawable(mApkInfos.get(1).icon);
                        apkCleanViewHolder.textViewName1.setText(mApkInfos.get(1).name);
                        apkCleanViewHolder.textViewTag1.setText(mApkInfos.get(1).isInstalled ? mInstalledStrings[0] : mInstalledStrings[1]);
                        apkCleanViewHolder.textViewSize1.setText(mApkInfos.get(1).size);
                    }
                    if (size > 2) {

                        apkCleanViewHolder.imageViewIcon2.setImageDrawable(mApkInfos.get(2).icon);
                        apkCleanViewHolder.textViewName2.setText(mApkInfos.get(2).name);
                        apkCleanViewHolder.textViewTag2.setText(mApkInfos.get(2).isInstalled ? mInstalledStrings[0] : mInstalledStrings[1]);
                        apkCleanViewHolder.textViewSize2.setText(mApkInfos.get(2).size);
                    }
                    if (size > 3) {

                        apkCleanViewHolder.imageViewIcon3.setImageDrawable(mApkInfos.get(3).icon);
                        apkCleanViewHolder.textViewName3.setText(mApkInfos.get(3).name);
                        apkCleanViewHolder.textViewTag3.setText(mApkInfos.get(3).isInstalled ? mInstalledStrings[0] : mInstalledStrings[1]);
                        apkCleanViewHolder.textViewSize3.setText(mApkInfos.get(3).size);
                    }
                    Log.d(TAG, "apk file is loading + size =" + mApkInfos.size() + " apk=" + mApkInfos.toString());
                } else {
                    Log.d(TAG, "apk file is loading + size =" + mApkInfos.size() + " apk=" + mApkInfos.toString());
                }

            } else if (holder instanceof PictureViewHolder){
                if (isImageCompleted) {
                    PictureViewHolder pictureViewHolder = (PictureViewHolder) holder;
                    pictureViewHolder.textViewSize.setText(getPicture());
                    pictureViewHolder.textViewGo.setOnClickListener(PhoneThinContentFragment.this);
                    int size = mPictureFiles.size();
                    if (size <= 0) {
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                refreshViewForType(VIEW_TYPE_PICTURE, true);
                            }
                        });
                    }
                    if (size > 0) {
                        pictureViewHolder.imageView0.setImageBitmap(getImageThumb(mPictureFiles.get(0).getPath()));
                    }
                    if (size > 1) {
                        pictureViewHolder.imageView1.setImageBitmap(getImageThumb(mPictureFiles.get(1).getPath()));
                    }
                    if (size > 2) {
                        pictureViewHolder.imageView2.setImageBitmap(getImageThumb(mPictureFiles.get(2).getPath()));
                    }
                    if (size > 3) {
                        pictureViewHolder.imageView3.setImageBitmap(getImageThumb(mPictureFiles.get(3).getPath()));
                    }
                    Log.d(TAG, "picture size=" + size);
                } else {
                    Log.d(TAG, "iamge is loading");
                }

            } else {
                Log.e(TAG, "onBindViewHolder error: invalid status");
            }
        }

        @Override
        public int getItemCount() {
            return mContentViewTypes.size();
        }

        public void refreshAppSize() {
            AppUninstallViewHolder appUninstallViewHolder =  (AppUninstallViewHolder)recyclerView.getChildViewHolder(recyclerView.getChildAt(VIEW_TYPE_APP_UNINSTALL));
            appUninstallViewHolder.textViewSize.setText(getProperSize(mAllPackageDataSize));
        }

        public void refreshViewForType(int type, boolean isEmpty) {
            int index = mContentViewTypes.indexOf(Integer.valueOf(type));
            Log.d(TAG, "refresh " + index + " isempty="+ isEmpty + "  type=" + type + "  mcontent=" + mContentViewTypes.toString());
            if (index == -1) {
                Log.w(TAG, "removeViewForType type" + type + " not in contentviewtypes");
            } else {
                if (isEmpty) {
                    mContentViewTypes.remove(index);
                    notifyItemRemoved(index);
                } else {
                    notifyItemChanged(index);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
           return mContentViewTypes.get(position);
        }


        class SummaryViewHolder extends RecyclerView.ViewHolder {

            TextView textView;
            CircleView circleView;

            public SummaryViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.avalid_space);
                circleView = (CircleView) itemView.findViewById(R.id.circle);
            }
        }

        class AppUninstallViewHolder extends RecyclerView.ViewHolder {

            TextView textViewCount;
            TextView textViewSize, textViewGo;

            public AppUninstallViewHolder(View itemView) {
                super(itemView);
                textViewCount = (TextView) itemView.findViewById(R.id.tv_uninstall_title);
                textViewSize = (TextView) itemView.findViewById(R.id.tv_size);
                textViewGo = (TextView) itemView.findViewById(R.id.tv_uninstall_app_go);
            }
        }

        class VideoViewHolder extends RecyclerView.ViewHolder {

            TextView textViewSize, textViewGo;
            ImageView imageView0, imageView1, imageView2, imageView3;

            public VideoViewHolder(View itemView) {
                super(itemView);
                textViewSize = (TextView) itemView.findViewById(R.id.tv_size);
                imageView0 = (ImageView) itemView.findViewById(R.id.iv0);
                imageView1 = (ImageView) itemView.findViewById(R.id.iv1);
                imageView2 = (ImageView) itemView.findViewById(R.id.iv2);
                imageView3 = (ImageView) itemView.findViewById(R.id.iv3);
                textViewGo = (TextView) itemView.findViewById(R.id.tv_video_go);
            }
        }

        class ApkCleanViewHolder extends RecyclerView.ViewHolder {

            TextView textViewSize, textViewGo;
            TextView textViewName0, textViewName1, textViewName2, textViewName3;
            TextView textViewSize0, textViewSize1, textViewSize2, textViewSize3;
            TextView textViewTag0, textViewTag1, textViewTag2, textViewTag3;
            ImageView imageViewIcon0, imageViewIcon1, imageViewIcon2, imageViewIcon3;
            RelativeLayout relativeLayout0, relativeLayout1, relativeLayout2, relativeLayout3;

            public ApkCleanViewHolder(View itemView) {
                super(itemView);
                textViewSize = (TextView) itemView.findViewById(R.id.tv_size);
                textViewGo = (TextView) itemView.findViewById(R.id.tv_apk_go);

                relativeLayout0 = (RelativeLayout) itemView.findViewById(R.id.rl_apk0);
                relativeLayout1 = (RelativeLayout) itemView.findViewById(R.id.rl_apk1);
                relativeLayout2 = (RelativeLayout) itemView.findViewById(R.id.rl_apk2);
                relativeLayout3 = (RelativeLayout) itemView.findViewById(R.id.rl_apk3);

                textViewName0 = (TextView) itemView.findViewById(R.id.tv_name0);
                textViewName1 = (TextView) itemView.findViewById(R.id.tv_name1);
                textViewName2 = (TextView) itemView.findViewById(R.id.tv_name2);
                textViewName3 = (TextView) itemView.findViewById(R.id.tv_name3);

                textViewSize0 = (TextView) itemView.findViewById(R.id.tv_size0);
                textViewSize1 = (TextView) itemView.findViewById(R.id.tv_size1);
                textViewSize2 = (TextView) itemView.findViewById(R.id.tv_size2);
                textViewSize3 = (TextView) itemView.findViewById(R.id.tv_size3);

                textViewTag0 = (TextView) itemView.findViewById(R.id.tv_tag0);
                textViewTag1 = (TextView) itemView.findViewById(R.id.tv_tag1);
                textViewTag2 = (TextView) itemView.findViewById(R.id.tv_tag2);
                textViewTag3 = (TextView) itemView.findViewById(R.id.tv_tag3);

                imageViewIcon0 = (ImageView) itemView.findViewById(R.id.iv_icon0);
                imageViewIcon1 = (ImageView) itemView.findViewById(R.id.iv_icon1);
                imageViewIcon2 = (ImageView) itemView.findViewById(R.id.iv_icon2);
                imageViewIcon3 = (ImageView) itemView.findViewById(R.id.iv_icon3);
            }
        }

        class PictureViewHolder extends RecyclerView.ViewHolder {

            TextView textViewSize, textViewGo;
            ImageView imageView0, imageView1, imageView2, imageView3;

            public PictureViewHolder(View itemView) {
                super(itemView);
                textViewSize = (TextView) itemView.findViewById(R.id.tv_size);
                textViewGo = (TextView) itemView.findViewById(R.id.tv_image_go);
                imageView0 = (ImageView) itemView.findViewById(R.id.iv0);
                imageView1 = (ImageView) itemView.findViewById(R.id.iv1);
                imageView2 = (ImageView) itemView.findViewById(R.id.iv2);
                imageView3 = (ImageView) itemView.findViewById(R.id.iv3);
            }
        }
    }

    private int mStorageRate;

    private String getSummary() {
        long total, avalid;
        total = PhoneThinActivity.getTotalPhoneStorage();
        avalid = PhoneThinActivity.getAvailablePhoneStorage();
        if (PhoneThinActivity.isTFCardExist()) {
            total += PhoneThinActivity.getTotalTFCardStorage();
            avalid += PhoneThinActivity.getAvailableTFCardStorage();
        }
        mStorageRate = (int) ((total - avalid) * 100 / total);
        Log.d(TAG, "total=" + total + "  avalid=" + avalid);

        return mContext.getString(R.string.space_total) + ":" + getProperSize(total)
                + "\n" + mContext.getString(R.string.space_available) + ":" + getProperSize(avalid);
    }


    private int getNoUseLongTimeAppCount() {
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);
        if (list.isEmpty()) {
            Log.d(TAG, "no action usage access settings");
            return 0;
        } else {
            int count = 0;
            AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(),
                    mContext.getPackageName());
            boolean granted = mode == AppOpsManager.MODE_ALLOWED;
            if (granted || hasSystemAuthority()) {
                Log.d(TAG, "has permission to access");
                UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService("usagestats");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -5);
                List<UsageStats> tempList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, cal.getTimeInMillis(), System.currentTimeMillis());
                if (tempList == null || tempList.isEmpty()) {
                    Log.d(TAG, "query usage stats nothing");
                    return count;
                } else {
                    ArrayMap<String, UsageStats> map = new ArrayMap<>();
                    ArrayList<UsageStats> packageStats = new ArrayList<>();
                    List<String> thirdPackage = getThirdPackageName();
                    final int statCount = tempList.size();
                    for (int i = 0; i < statCount; i++) {
                        final UsageStats pkgStats = tempList.get(i);

                        UsageStats existingStats = map.get(pkgStats.getPackageName());
                        if (existingStats == null) {
                            if (thirdPackage.contains(pkgStats.getPackageName())) {
                                map.put(pkgStats.getPackageName(), pkgStats);
                            }
                        } else {
                            existingStats.add(pkgStats);
                        }
                    }
                    packageStats.addAll(map.values());

                    long now = System.currentTimeMillis();
                    for (UsageStats usageStats : packageStats) {
                        if (now - usageStats.getLastTimeUsed() > NO_USE_LONG_TIME_INTERVAL) {
                            count++;
                            getPkgSize(packageManager, usageStats.getPackageName());
                            Log.d(TAG, "no use for a long time=" + usageStats.getPackageName() + " " + usageStats.getLastTimeUsed());
                        }
                        Log.d(TAG, "all =" + usageStats.getPackageName() + " " + usageStats.getLastTimeUsed());
                    }
                    Log.d(TAG, "success to query and return");
                    return count;
                }
            }
            Log.d(TAG, "no permission to access");
            return count;
        }
    }


    private boolean hasSystemAuthority() {
        try {
            PackageInfo mPackageInfo = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0);
            if ((mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                Log.d(TAG, "has system permission");
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getVideo() {
        mVideoFiles = ((PhoneThinActivity)getActivity()).getVideoFiles();
        long size = 0L;
        for (File file : mVideoFiles) {
            size += file.length();
        }
        return getProperSize(size);
    }

    private String getPicture() {
        Log.d(TAG, "get picture");
        mPictureFiles = ((PhoneThinActivity)getActivity()).getPictureFiles();
        long size = 0L;
        for (File file : mPictureFiles) {
            size += file.length();
        }
        return getProperSize(size);
    }

    private String getApkCount() {
        List<File> files = ((PhoneThinActivity)getActivity()).getApkFiles();
        mApkInfos.clear();
        long size = 0L;
        for (File file : files) {
            ApkInfo apkInfo = new ApkInfo();
            try {
                apkInfo.size = getProperSize(file.length());
                size += file.length();
            } catch (Exception e) {
                e.printStackTrace();
            }
            PackageManager pm = mContext.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(file.getPath(), PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
            if (info != null) {
                ApplicationInfo appInfo = info.applicationInfo;
                if (Build.VERSION.SDK_INT >= 8) {
                    appInfo.sourceDir = file.getPath();
                    appInfo.publicSourceDir = file.getPath();
                }
                apkInfo.icon = pm.getApplicationIcon(appInfo);
                apkInfo.name = pm.getApplicationLabel(appInfo).toString();
                apkInfo.isInstalled = isApkInstalled(appInfo.packageName);
            } else {
                apkInfo.icon = mContext.getDrawable(R.drawable.ic_unintall_app);
                apkInfo.name = file.getName();
                String name = file.getName().toLowerCase(Locale.ENGLISH).trim();
                apkInfo.isInstalled = isApkInstalled(name.substring(0, name.lastIndexOf('.')));
            }
            Log.d(TAG, "info=" + info + "  name=" + apkInfo.name + " path=" + file.getPath());
            mApkInfos.add(apkInfo);
        }
        return getProperSize(size);
    }

    public List<ApkInfo> getApkInfos() {
        return mApkInfos;
    }



    private List<String> getThirdPackageName() {
        List<String> stringList = new ArrayList<>();
        //get all installed app
        List<PackageInfo> packageInfos = mContext.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            //过滤掉系统app
            if ((ApplicationInfo.FLAG_SYSTEM & packageInfo.applicationInfo.flags) != 0) {
                continue;
            }
            stringList.add(packageInfo.packageName);
            Log.d(TAG, "third package=" + packageInfo.packageName);
        }
        return stringList;
    }

    private static Bitmap getVideoThumb(String path) {
        return ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
    }

    private static Bitmap getImageThumb(String path) {
        return ThumbnailUtils.createImageThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
    }

    public static class ApkInfo {
        String name;
        boolean isInstalled;
        Drawable icon;
        String size;
    }

    private boolean isApkInstalled(String packagename) {
        Log.d(TAG, "is apk installed pakname=" + packagename);
        PackageManager localPackageManager = mContext.getPackageManager();
        try {
            PackageInfo localPackageInfo = localPackageManager.getPackageInfo(packagename, PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
            return false;
        }

    }

    public static String getProperSize(long sizeByte) {
        if (sizeByte < 1024L) {
            return sizeByte + "B";
        } else {
            float size = sizeByte / 1024f;
            if (size < 1024) {
                return String.format("%.1f",size) + "KB";
            } else {
                size /= 1024f;
                if (size < 1024) {
                    return String.format("%.1f",size) + "MB";
                } else {
                    size /= 1024f;
                    return String.format("%.1f",size) + "GB";
                }
            }
        }
    }

    public String[] getInstalledStrings() {
        return mInstalledStrings;
    }

    public void refreshData(int type, boolean isEmpty) {
        switch (type) {
            case VIEW_TYPE_PICTURE:
                isImageCompleted = true;
                break;
            case VIEW_TYPE_APK_CLEAN:
                isFileCompleted = true;
                break;
            case VIEW_TYPE_VIDEO:
                isVideoCompleted = true;
                break;
        }
        ((PhoneThinContentAdapter) mAdapter).refreshViewForType(type, isEmpty);
        Log.d(TAG, "refresh data type=" + type);
    }

    private void getPkgSize(PackageManager packageManager, String pkgName) {
        try {
            // getPackageSizeInfo是PackageManager中的一个private方法，所以需要通过反射的机制来调用
            Method method = PackageManager.class.getMethod("getPackageSizeInfo",
                    new Class[]{String.class, IPackageStatsObserver.class});
            // 调用 getPackageSizeInfo 方法，需要两个参数：1、需要检测的应用包名；2、回调
            method.invoke(packageManager, new Object[]{
                    pkgName,
                    new IPackageStatsObserver.Stub() {
                        @Override
                        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
                            try {
                                Log.d(TAG, "缓存大小=" + pStats.cacheSize +
                                        "\n数据大小=" + pStats.dataSize +
                                        "\n程序大小=" + pStats.codeSize);
                                long size = pStats.cacheSize + pStats.dataSize + pStats.cacheSize;
                                mAllPackageDataSize += size;
                                ((PhoneThinContentAdapter) mAdapter).refreshAppSize();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    get app size for android o
     */
  /*  public static long getAllAppTotalsizeO(Context context, String pkgName){
        StorageStatsManager storageStatsManager  = (StorageStatsManager)context.getSystemService(Context.STORAGE_STATS_SERVICE);
        StorageManager storageManager  = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        //获取所有应用的StorageVolume列表
        List<StorageVolume> storageVolumes  = storageManager.getStorageVolumes();
        long appSizeL = 0L;
        for (StorageVolume item : storageVolumes) {
            String uuidStr = item.getUuid();
            UUID uuid  = (uuidStr == null) ? StorageManager.UUID_DEFAULT : UUID.fromString(uuidStr);
            //通过包名获取uid
            int uid = getUid(context, pkgName);
            val storageStats = storageStatsManager.queryStatsForUid(uuid, uid);
            //获取到App的总大小
            appSizeL = storageStats.appBytes + storageStats.cacheBytes + storageStats.dataBytes;
        }
        return appSizeL;
    }

    *//**
     * 根据应用包名获取对应uid
     *//*
    public static int getUid(Context context , String pakName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pakName, PackageManager.GET_META_DATA);
            return ai.uid;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public long  getAppTotalsizeO(Context context, File path){
        try {
            StorageStatsManager storageStatsManager = (StorageStatsManager)context.getSystemService(Context.STORAGE_STATS_SERVICE);
            StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
            UUID uuid  = storageManager.getUuidForPath(path);
            //通过包名获取uid
            int uid = getUid(context, context.getPackageName());
            val storageStats = storageStatsManager.queryStatsForUid(uuid, uid);
            //获取到App的总大小
            return storageStats.appBytes + storageStats.cacheBytes + storageStats.dataBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }*/





}
