package com.qingcheng.mobilemanager.ui.activity;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.taskmanager.RunningState;
import com.qingcheng.mobilemanager.ui.fragment.MemoryCleanFinishFragment;
import com.qingcheng.mobilemanager.ui.fragment.MemoryCleanNothingFragment;
import com.qingcheng.mobilemanager.utils.ArrayUtils;
import com.qingcheng.mobilemanager.utils.LogUtil;
import com.qingcheng.mobilemanager.utils.UiUtils;

import android.provider.Settings;


public class TaskCleanActivity extends BaseSonActivity implements View.OnClickListener,
        AbsListView.RecyclerListener, RunningState.OnRefreshUiListener {
    public static String[] mFilterProcessNames = {"com.qingcheng.mobilemanager", "com.greenorange.qcpower","inputmethod",
            "com.mediatek.op01.plugin","com.mediatek.op09.plugin","com.mediatek.mtklogger","com.sohu.inputmethod.sogou",
            "com.baidu.input","com.Android.inputmethod.pinyin","com.iflytek.inputmethod","com.gphonemanager.app",
            "com.iflytek.inputmethod.assist","com.qingcheng.home"};
    private RelativeLayout mKillDispaly;
    private LinearLayout mLoadDisplay;
    private boolean isLoading;

    private ActivityManager sAm;
    private RunningState mState;
    private ServiceListAdapter mAdapter;
    private ListView mListView;
    private TextView mTitle;
    final HashMap<View, ActiveItem> mActiveItems = new HashMap<View, ActiveItem>();

    private StringBuilder mBuilder = new StringBuilder(128);
    private TextView mKillAll;

    private TextView mMemoryView;
    private TextView mMemoryToClearView;
    private RelativeLayout mNestedScrollParentLayout;
    private RelativeLayout mThresholdContainerLayout;
    private LinearLayout mToolbarLayout;
    private ImageView mSetView;
    private TextView mRocketTextView;
    private ImageView mRocketView;

    private List<String> mLockApps = new ArrayList<>();
    private List<String> mShowLauncherApps = new ArrayList<>();
    private static final String TAG = "TaskCleanActivity";
    private long mValidMemory, mTocleanMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clean_ram);
        LogUtil.i();
        initData();
        initView();
        preBack();
        setLoading(isLoading);
        UiUtils.setScreen(this);
        getShowLauncherApp();
    }

    @Override
    public void onClick(View v) {
        LogUtil.i();
        int id = v.getId();
        if (id == R.id.kill_all) {
            mKillAll.setClickable(false);
            LogUtil.d("repsonse to the kill all action.");
            ServiceListAdapter adapter = (ServiceListAdapter) (((HeaderViewListAdapter)mListView.getAdapter()).getWrappedAdapter());
            adapter.killAllProcess();
            mKillAll.setClickable(true);
        }  else if (id == R.id.toolbar_set) {
            final Intent intent = new Intent(this, TaskCleanSetActivity.class);
            startActivity(intent);
        }
    }

    private void closeMemoryCleanFragment() {
        mKillDispaly.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.app_display_out));
        mKillDispaly.setVisibility(View.GONE);
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        mActiveItems.remove(view);
    }

    @Override
    public void onResume() {

        LogUtil.i();
        super.onResume();
        getLockApp();
        if (mState != null) {
            mState.resume(this);
        }
        updateMemoryInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mState != null) {
            mState.pause();
        }
        setLockApp();

    }

    @Override
    public void onRefreshUi(int what) {
        LogUtil.d("onRefreshUi " + what);
        switch (what) {
            case REFRESH_DATA:
            case REFRESH_STRUCTURE:
                if (isLoading){
                    isLoading = false;
                    setLoading(isLoading);
                }
                refreshUi(true);
                break;
            default:
                break;
        }
    }


    @Override
    public void initView() {
        mKillDispaly = (RelativeLayout) findViewById(R.id.rl_kill_display);
//        mBackCircular = (RelativeLayout) findViewById(R.id.white_bac_circular);
        mLoadDisplay = (LinearLayout) findViewById(R.id.loading_container);

        View back = findViewById(R.id.iv_traffic_pre);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

//        mProcessIcon = (ImageSwitcher) findViewById(R.id.is_process_icon);
//        mProcessName = (TextView) findViewById(R.id.tv_process_name);
//        mProcessCount = (TextView) findViewById(R.id.tv_process_count);
//        mProcessIcon.setFactory(new ViewSwitcher.ViewFactory() {
//            @Override
//            public View makeView() {
//                ImageView img = new ImageView(getBaseContext());
//                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
//                img.setLayoutParams(new ImageSwitcher.LayoutParams(-1, -1));
//                return img;
//            }
//        });
//        mProcessIcon.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.switcher_image_right_in));
//        mProcessIcon.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.switcher_image_left_out));


        mKillAll = (TextView) findViewById(R.id.kill_all);
        mKillAll.setOnClickListener(this);

        mListView = (ListView) findViewById(R.id.list);
        addRunningTaskHeader();
        mListView.setRecyclerListener(this);
        mAdapter = new ServiceListAdapter(mState);
        mListView.setAdapter(mAdapter);

        mMemoryView = (TextView) findViewById(R.id.ram_useinfo);
        mMemoryToClearView = (TextView) findViewById(R.id.ram_to_clear);
        mNestedScrollParentLayout = (RelativeLayout) findViewById(R.id.nestedscroll_parentlayout);
        mThresholdContainerLayout = (RelativeLayout) findViewById(R.id.title_container);
        mToolbarLayout = (LinearLayout) findViewById(R.id.rl_toolbar);
        mSetView = (ImageView) findViewById(R.id.toolbar_set);
        mSetView.setOnClickListener(this);
        mRocketTextView = (TextView) findViewById(R.id.tv_rocket);
        mRocketView = (ImageView) findViewById(R.id.iv_rocket);
    }

    @Override
    public void preBack() {
    }

    @Override
    public void initData() {
        sAm = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mState = RunningState.getInstance(getBaseContext());
        isLoading = true;
    }

    //////////////////////////////  animation kill all process
//    int Iindex = 0;
//    int IprocessCount = 0;
    private void killProcessAnimation(boolean isKill, final ArrayList<RunningState.MergedItem> Items) {
        LogUtil.i();
//        if (isKill) {
//            IprocessCount = Items.size();

//            mProcessIcon.setVisibility(View.VISIBLE);
//            mProcessCount.setVisibility(View.VISIBLE);
//            mProcessName.setTextSize(15);

        mValidMemory = getValidMemoryMB();
        mTocleanMemory = getToCleanMemoryMB(Items);

        int height = getScreenSize();
        Log.d(TAG, "screen height=" + height);
        ObjectAnimator.ofFloat(mRocketView, "translationY", 0, -height).setDuration(1500).start();
        mRocketView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRocketView.setVisibility(View.GONE);
                mRocketTextView.setVisibility(View.GONE);
                showCleaningFinished();
            }
        }, 1500);
//        mBackCircular.setVisibility(View.VISIBLE);
//            mProcessName.setText("");
//            mProcessIcon.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    if (Iindex >= IprocessCount) {
//                        Iindex = 0;
//                        IprocessCount = 0;
//                        killProcessAnimation(false, null);
//                        return;
//                    }
//                    if (Items.get(Iindex).mPackageInfo != null) {
//                        mProcessIcon.setImageDrawable(Items.get(Iindex).mPackageInfo.loadIcon(getPackageManager()));
//                    }
////                    mProcessName.setText(Items.get(Iindex).mDisplayLabel);
////                    mProcessCount.setText(String.valueOf((Iindex + 1) + "/" + IprocessCount));
//                    mProcessIcon.postDelayed(this, 500);
//                    Iindex++;
//                }
//            }, 100);

//        } else {
//            mProcessIcon.setVisibility(View.GONE);
//            mProcessCount.setVisibility(View.GONE);
//            mBackCircular.setClickable(true);
//            mProcessName.setTextSize(50);
//            mProcessName.setText("完成");
//            mProcessName.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in));
//            ObjectAnimator.ofFloat(mBackCircular, "translationY", -100f, 0f).setDuration(500).start();
//            mBackCircular.setOnClickListener(this);
//        }
    }


    void refreshUi(boolean dataChanged) {
        LogUtil.i();
        ServiceListAdapter adapter = (ServiceListAdapter) (((HeaderViewListAdapter)mListView.getAdapter()).getWrappedAdapter());
        if (dataChanged) {
            adapter.refreshItems();
            adapter.notifyDataSetChanged();
        }
        int count = adapter.mItems.size();
        mTitle.setText(getString(R.string.running_app, count));
        updateMemoryToClear(getToCleanMemoryMB(adapter.mItems));
    }

    private void setLoading(boolean loading) {
        LogUtil.d("loading "+loading);
        if (loading) {
            mLoadDisplay.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mKillDispaly.setVisibility(View.GONE);
            mNestedScrollParentLayout.setVisibility(View.GONE);

        } else {
            mLoadDisplay.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            mNestedScrollParentLayout.setVisibility(View.VISIBLE);
        }


    }

    ////////////////////////////////////////////////////////////
    public class ActiveItem {
        View mRootView;
        RunningState.BaseItem mItem;
        ViewHolder mHolder;
        long mFirstRunTime;

        void updateSize(Context context, StringBuilder builder) {
            TextView upSizeView = null;
            if (mItem instanceof RunningState.ServiceItem) {
                // If we are displaying a service, then the service
                // uptime goes at the top.
                upSizeView = mHolder.size;

            } else {
                String size = mItem.mSizeStr != null ? mItem.mSizeStr : "";
                if (!size.equals(mItem.mCurSizeStr)) {
                    mItem.mCurSizeStr = size;
                    mHolder.size.setText(size);
                }
            }
        }
    }

    public class ViewHolder {
        public View rootView;
        public ImageView icon;
        public TextView name;
        public TextView size;
        public ImageView killIcon;

        public ViewHolder(View v) {
            rootView = v;
            icon = (ImageView) v.findViewById(R.id.icon);
            name = (TextView) v.findViewById(R.id.name);
            size = (TextView) v.findViewById(R.id.size);
            killIcon = (ImageView) v.findViewById(R.id.kill_icon);
            v.setTag(this);
        }

        public ActiveItem bind(RunningState state, RunningState.BaseItem item,
                               StringBuilder builder, ArrayList<RunningState.MergedItem> mItems) {
            synchronized (state.mLock) {
                LogUtil.i();
                PackageManager pm = rootView.getContext().getPackageManager();
                if (item.mPackageInfo == null && item instanceof RunningState.MergedItem) {
                    // Items for background processes don't normally load
                    // their labels for performance reasons.  Do it now.
                    ((RunningState.MergedItem) item).mProcess.ensureLabel(pm);
                    item.mPackageInfo = ((RunningState.MergedItem) item).mProcess.mPackageInfo;
                    item.mDisplayLabel = ((RunningState.MergedItem) item).mProcess.mDisplayLabel;
                }
                name.setText(item.mDisplayLabel);
                ActiveItem ai = new ActiveItem();
                ai.mRootView = rootView;
                ai.mItem = item;
                ai.mHolder = this;
                ai.mFirstRunTime = item.mActiveSince;
                item.mCurSizeStr = null;
                if (item.mPackageInfo != null) {
                    icon.setImageDrawable(item.mPackageInfo.loadIcon(pm));
                }
                icon.setVisibility(View.VISIBLE);
                ai.updateSize(rootView.getContext(), builder);

                final RunningState.MergedItem mi = (RunningState.MergedItem) item;
                final String processName = mi.mProcess.mProcessName;
                final ArrayList<RunningState.MergedItem> mNewItems = mItems;
                final String packageName;
                if (mi.mPackageInfo != null) {
                    packageName = mi.mPackageInfo.packageName;
                } else {
                    packageName = processName;
                }
                killIcon.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // kill it
                        LogUtil.d("******onClick kill icon**********");

//                        killProcess(mi.mBackground, processName, packageName);
                        // remove this item from adapter ,let the user thinks it's killed at right.
                        if (mLockApps.contains(packageName)) {
                            killIcon.setImageAlpha(127);
                            killIcon.setImageDrawable(getDrawable(R.drawable.ic_app_open));
                            mLockApps.remove(packageName);
                        } else {
                            killIcon.setImageAlpha(0xff);
                            killIcon.setImageDrawable(getDrawable(R.drawable.ic_app_close));
                            mLockApps.add(packageName);
                        }
//                        mNewItems.remove(mi);
                        mAdapter.notifyDataSetChanged();
                        refreshUi(false);
                    }
                });
                if (mLockApps.contains(packageName)) {
                    killIcon.setImageAlpha(0xff);
                    killIcon.setImageDrawable(getDrawable(R.drawable.ic_app_close));
                } else {
                    killIcon.setImageAlpha(127);
                    killIcon.setImageDrawable(getDrawable(R.drawable.ic_app_open));
                }
                return ai;
            }
        }
    }

    class ServiceListAdapter extends BaseAdapter {

        final RunningState mState;
        final LayoutInflater mInflater;
        ArrayList<RunningState.MergedItem> mItems;
        ServiceListAdapter(RunningState state) {
            mState = state;
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //com.sohu.inputmethod.sogou/.SogouIME
            String inputMethod=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
            if (inputMethod != null && inputMethod.contains("/")) {
                mFilterProcessNames[2] = inputMethod.substring(0,inputMethod.indexOf("/"));
            }
            refreshItems();
        }

        // kill all the processes , set the current process at last.
        // because if the current process is killed , the function will
        // not execute continully , so the other processes will not be killed.
        void killAllProcess() {
            mKillDispaly.setVisibility(View.VISIBLE);
            LogUtil.i();
            Log.d(TAG, "kill process");
            if (mItems == null) {
                LogUtil.d("items is null , when kill the all processes!");
                return;
            }

            ArrayList<RunningState.MergedItem> unlockItems = new ArrayList<>();
            for (RunningState.MergedItem item : mItems) {
                if (item.mPackageInfo != null && mLockApps.contains(item.mPackageInfo.packageName)) {
                    continue;
                } else {
                    unlockItems.add(item);
                }
            }
            int size = unlockItems.size();
            if (unlockItems.size() != 0) {
                killProcessAnimation(true, unlockItems);
            } else {
                showNothingCleaning();
            }
            for (int i = 0; i < size; i++) {
                RunningState.MergedItem mi = unlockItems.get(i);
                String processName = mi.mProcess.mProcessName;
                String packageName;
                if (mi.mPackageInfo != null) {
                    packageName = mi.mPackageInfo.packageName;
                } else {
                    packageName = processName;
                }
                LogUtil.d("i = " + i + " , current kill process: " + processName);
                killProcess(mi.mBackground, processName, packageName);
            }

        }
        void refreshItems() {
            LogUtil.i();

            ArrayList<RunningState.MergedItem> newItems = mState.getCurrentMergedItems();
            ArrayList<String> newProcessName = new ArrayList<String>();

            Iterator it = newItems.iterator();
            if (LogUtil.allowI){
                for (int i = 0; i<mFilterProcessNames.length;i++){
                LogUtil.i("mFilterProcessNames "+i+"="+mFilterProcessNames[i]);
                }
            }

            LogUtil.i(" filter newItems befor: " + newItems.size());
            while (it.hasNext()) {
                RunningState.MergedItem item = (RunningState.MergedItem) it.next();
                String pName = item.mProcess.mProcessName;
                newProcessName.add(pName);
                LogUtil.i("newItems = "+pName);
                if (ArrayUtils.contains(mFilterProcessNames, pName)) {
                    LogUtil.i("remove newItems = "+pName);
                    it.remove();
                }
            }
            LogUtil.d(" filter newItems after: " + newItems.size());

            ArrayList<RunningState.MergedItem> newBackgroundItems = mState.getCurrentBackgroundItems();
            for (int i = 0; i < newBackgroundItems.size(); i++) {
                String name = newBackgroundItems.get(i).mProcess.mProcessName;
                if (!newProcessName.contains(name) && !ArrayUtils.contains(mFilterProcessNames, name)) {

                    newItems.add(newBackgroundItems.get(i));
                    LogUtil.d("i: " + i + " newBackgroundItems.get(i).packageName:"
                            + newBackgroundItems.get(i).mProcess.mProcessName);
                }
            }
            LogUtil.d(" filter newItems background: " + newItems.size());

            //filter items not showing  in the home
            ArrayList<RunningState.MergedItem> showItems = new ArrayList<>();
            List<String> showPackageName = new ArrayList<>();
            for (RunningState.MergedItem item : newItems) {
                String packageName = item.mPackageInfo == null ? item.mProcess.mProcessName : item.mPackageInfo.packageName;
                if (mShowLauncherApps.contains(packageName) && !showPackageName.contains(packageName)) {
                    showItems.add(item);
                    showPackageName.add(packageName);
                    Log.d(TAG, "show packager=" + packageName);
                }
            }
            mItems = showItems;
//            if (mItems != newItems) {
//                mItems = newItems;
            LogUtil.d("mItems: " + mItems.size());
//            }
//            if (mItems == null) {
//                mItems = new ArrayList<RunningState.MergedItem>();
//            }
        }


        /// For ALPS00934194 @{
        private boolean isVaidPosition(int position) {
            LogUtil.i();
            LogUtil.d("ServiceListAdapter::isVaidPosition position = " + position);
            if (mItems == null) {
                LogUtil.d("ServiceListAdapter::isVaidPosition return false, mItem == null");
                return false;
            } else if (mItems.size() <= position) {
                LogUtil.d("ServiceListAdapter::isVaidPosition return false, size = " + mItems.size());
                return false;
            } else {
                LogUtil.d("ServiceListAdapter::isVaidPosition return true");
                return true;
            }
        }
        /// @}


        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            /// For ALPS00934194 @{
            LogUtil.i("ServiceListAdapter::getItem position = " + position);
            if (!isVaidPosition(position)) {
                LogUtil.d("ServiceListAdapter::getItem return null");
                return null;
            }
            /// @}
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            /// For ALPS00934194 @{
            LogUtil.i("ServiceListAdapter::getItemId position = " + position);
            if (!isVaidPosition(position)) {
                LogUtil.d("ServiceListAdapter::getItemId return 0");
                return 0;
            }
            /// @}
            return mItems.get(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent, position);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent, int pos) {
            View v = mInflater.inflate(R.layout.running_processes_item, parent, false);
            new ViewHolder(v);
            return v;
        }

        public void bindView(View view, int position) {
            synchronized (mState.mLock) {
                if (position >= mItems.size()) {
                    // List must have changed since we last reported its
                    // size...  ignore here, we will be doing a data changed
                    // to refresh the entire list.
                    return;
                }
                ViewHolder vh = (ViewHolder) view.getTag();
                RunningState.MergedItem item = mItems.get(position);
                ActiveItem ai = vh.bind(mState, item, mBuilder, mItems);
                mActiveItems.put(view, ai);
            }
        }
    }

    public void killProcess(boolean background, String processName, String packageName) {
        // CR: ALPS00246495
        if (processName == null) {
            LogUtil.d("to kill processName is null.");
        }
       /* if (processName.contains("launcher")) {
            LogUtil.d("not kill the process" + processName + " to avoid some trouble.");
            return;
        }*/
        if (background) {
            // Background process.  Just kill it.
            LogUtil.d("kill the background processes" + processName);
            sAm.killBackgroundProcesses(processName);
        } else {
            // make sure the proccess will be killed , We'll do a force-stop on it.
            // CR:ALPS00244890
            LogUtil.d(" always forceStopPackage" + packageName);
            sAm.forceStopPackage(packageName);
        }
    }

    private void addRunningTaskHeader() {
        View view  = getLayoutInflater().inflate(R.layout.running_task_header, null, false);
        mTitle = (TextView) view.findViewById(R.id.running_task);
        FrameLayout mNewContactDividerHeaderContainer = new FrameLayout(this);
        mNewContactDividerHeaderContainer.addView(view);
        mListView.addHeaderView(mNewContactDividerHeaderContainer, null, false);
    }

    //获取内存大小
    private void updateMemoryInfo() {
        setThresholdColor(getValidMemoryMB());
        mMemoryView.setText(getMemoryInfo());
    }

    private long getValidMemoryMB() {
        long memory = 0;
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        memory = mi.availMem / (1024 * 1024);
        return memory;
    }

    public String getToCleanMemoryMB() {
        return String.valueOf(mTocleanMemory);
    }

    public String getMemoryInfo() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long avalidMemory = mi.availMem / (1024 * 1024);
        return String.format("%.1f",avalidMemory / 1024f) + "GB/" + (mi.totalMem / (1024 * 1024 * 1024)) + "GB";
    }

    private long getToCleanMemoryMB(List<RunningState.MergedItem> datas) {
        long size = 0;
        if (datas == null || datas.isEmpty()) {
            size = 0;
        } else {
            for (RunningState.MergedItem item : datas) {
                size += item.mSize;
            }
        }
        size /= 1024 * 1024;
        return size;
    }

    private void updateMemoryToClear(long size) {
        mMemoryToClearView.setText("" + size);
    }

    private void setThresholdColor(long validMemory) {
        int color;
        if (validMemory < 500) {
            color = getColor(R.color.red);
        } else if (validMemory < 1024) {
            color = getColor(R.color.ORANGE);
        } else {
            color = getColor(R.color.bluedarker);
        }
        getWindow().setStatusBarColor(color);
        mThresholdContainerLayout.setBackgroundColor(color);
        mToolbarLayout.setBackgroundColor(color);
    }

    private void getLockApp() {
        mLockApps.clear();
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = TaskCleanActivity.this.openFileInput(TaskCleanSetActivity.File_LOCK_APP);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            if (stringBuilder.length() > 0) {
                String[] strings = stringBuilder.toString().split(";");
                for (String string : strings) {
                    mLockApps.add(string);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setLockApp() {
        FileOutputStream fileOutputStream = null;
        BufferedWriter writer = null;
        try {
            fileOutputStream = TaskCleanActivity.this.openFileOutput(TaskCleanSetActivity.File_LOCK_APP, MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < mLockApps.size(); i++) {

                stringBuilder.append(mLockApps.get(i) + ";");

            }
            writer.write(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void getShowLauncherApp() {
        PackageManager packageManager = getPackageManager();
        try {
            //get show in launcher
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfoList) {
                mShowLauncherApps.add(resolveInfo.activityInfo.packageName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getScreenSize() {
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();//屏幕分辨率容器
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        int height = mDisplayMetrics.heightPixels;
        return height;
    }

    private void showCleaningFinished() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MemoryCleanFinishFragment()).commit();

    }

    private void showNothingCleaning() {
        mRocketView.setVisibility(View.GONE);
        mRocketTextView.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MemoryCleanNothingFragment()).commit();

    }

    public long getSpeed() {
        long valid = mValidMemory;
        long toClean = mTocleanMemory;
        Log.d(TAG, "getSpeed valid=" + valid + "  toClean=" + toClean);
        if (valid == 0) {
            return 50;
        } else {
            long speed = toClean * 100 / valid;
            if (speed > 50) {
                return 50;
            } else if (speed < 1) {
                return 1;
            } else {
                return speed;
            }
        }
    }
}
