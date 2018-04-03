package com.qingcheng.mobilemanager.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.utils.UiUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chunfengliu on 1/31/18.
 */

public class TaskCleanSetActivity extends BaseSonActivity {

    private RecyclerView recyclerView;
    private TaskCleanSetAdapter taskCleanSetAdapter;
    private static final String TAG = "TaskCleanSetActivity";
    public static final String File_LOCK_APP = "lock_app.txt";
    private List<String>  mLockApp = new ArrayList<>();
    private ImageView mBackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taskclean_set);
        initView();
        initData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setLockApp();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.id_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(getDrawable(R.drawable.divider_black_light), DividerItemDecoration.VERTICAL_LIST));
        mBackView = (ImageView) findViewById(R.id.iv_back);
        mBackView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLockApp();
                finish();
            }
        });
        UiUtils.setScreen(this);
        getWindow().setStatusBarColor(getColor(R.color.bluedarker));
    }

    @Override
    public void preBack() {

    }

    @Override
    public void initData() {
        new QueryInstallApp().execute();
    }

    class TaskCleanSetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public LinkedList<MyAppInfo> getDatas() {
            return datas;
        }

        private LinkedList<MyAppInfo> datas;
        private int lockCount;

        private static final int VIEWTYPE_TITLE = 0;
        private static final int VIEWTYPE_CONTENT = 1;

        public TaskCleanSetAdapter(LinkedList<MyAppInfo> datas, int lockCount) {
            super();
            this.datas = datas;
            this.lockCount = lockCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEWTYPE_TITLE) {
                view = LayoutInflater.from(TaskCleanSetActivity.this).inflate(R.layout.running_task_header, parent, false);
                return new TitleViewHold(view);
            } else if (viewType == VIEWTYPE_CONTENT) {
                view = LayoutInflater.from(TaskCleanSetActivity.this).inflate(R.layout.item_taskclean_set, parent, false);
                return new ContentViewHold(view);
            }
            return null;

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (lockCount == 0) {
                if (holder instanceof ContentViewHold) {
                    final int realIndex;
                    realIndex = position - 1;
                    ((ContentViewHold) holder).appIcon.setImageDrawable(datas.get(realIndex).getImage());
                    ((ContentViewHold) holder).appName.setText(datas.get(realIndex).getAppLabel());
                    ((ContentViewHold) holder).appLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            datas.get(realIndex).setLocked(isChecked);
                        }
                    });
                    ((ContentViewHold) holder).appLock.setChecked(datas.get(realIndex).isLocked());
                } else if (holder instanceof TitleViewHold) {
                    ((TitleViewHold) holder).titleView.setText(getString(R.string.unlocked_task) + "(" + (datas.size() - lockCount) + ")");
                }
            } else {
                if (holder instanceof ContentViewHold) {
                    final int realIndex;
                    if (position < lockCount + 1 && position > 0) {
                        realIndex = position - 1;
                    } else {
                        realIndex = position - 2;
                    }
                    ((ContentViewHold) holder).appIcon.setImageDrawable(datas.get(realIndex).getImage());
                    ((ContentViewHold) holder).appName.setText(datas.get(realIndex).getAppLabel());
                    ((ContentViewHold) holder).appLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            datas.get(realIndex).setLocked(isChecked);
                        }
                    });
                    ((ContentViewHold) holder).appLock.setChecked(datas.get(realIndex).isLocked());
                } else if (holder instanceof TitleViewHold) {
                    if (position == 0) {
                        ((TitleViewHold) holder).titleView.setText(getString(R.string.locked_task) + "(" + lockCount + ")");
                    } else if (position == lockCount + 1) {
                        ((TitleViewHold) holder).titleView.setText(getString(R.string.unlocked_task) + "(" + (datas.size() - lockCount) + ")");
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            if (lockCount == 0) {
                return datas.size() + 1;
            } else {
                return datas.size() + 2;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (lockCount == 0) {
                if (position == 0) {
                    return VIEWTYPE_TITLE;
                } else {
                    return VIEWTYPE_CONTENT;
                }

            } else {
                if (position == 0 || position == lockCount + 1) {
                    return VIEWTYPE_TITLE;
                } else {
                    return VIEWTYPE_CONTENT;
                }
            }
        }

        class ContentViewHold extends RecyclerView.ViewHolder {
            ImageView appIcon;
            TextView appName;
            Switch appLock;

            public ContentViewHold(View itemView) {
                super(itemView);
                appIcon = (ImageView) itemView.findViewById(R.id.app_icon);
                appName = (TextView) itemView.findViewById(R.id.app_name);
                appLock = (Switch) itemView.findViewById(R.id.app_lock);
            }
        }

        class TitleViewHold extends RecyclerView.ViewHolder {

            TextView titleView;

            public TitleViewHold(View itemView) {
                super(itemView);
                titleView = (TextView) itemView.findViewById(R.id.running_task);
            }
        }
    }

    public class MyAppInfo {
        private Drawable image;
        private String appName;
        private boolean locked;
        private String appLabel;

        public MyAppInfo(Drawable image, String appName, boolean locked) {
            this.image = image;
            this.appName = appName;
            this.locked = locked;
        }

        public MyAppInfo() {

        }

        public Drawable getImage() {
            return image;
        }

        public void setImage(Drawable image) {
            this.image = image;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public String getAppLabel() {
            return appLabel;
        }

        public void setAppLabel(String appLabel) {
            this.appLabel = appLabel;
        }
    }


    private LinkedList<MyAppInfo> scanLocalInstallAppList() {
        PackageManager packageManager = getPackageManager();
        LinkedList<MyAppInfo> myAppInfos = new LinkedList<MyAppInfo>();
        List<MyAppInfo> lockApps = new LinkedList<>();
        List<MyAppInfo> unLockApps = new LinkedList<>();
        try {
            //get show in launcher
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setAction(Intent.ACTION_MAIN);


            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfoList) {
                MyAppInfo myAppInfo = new MyAppInfo();
                myAppInfo.setAppName(resolveInfo.activityInfo.packageName);
                myAppInfo.setImage(resolveInfo.loadIcon(packageManager));
                myAppInfo.setAppLabel(resolveInfo.loadLabel(packageManager).toString());
                if (mLockApp.contains(resolveInfo.activityInfo.packageName)) {
                    myAppInfo.setLocked(true);
                    lockApps.add(myAppInfo);
                } else {
                    myAppInfo.setLocked(false);
                    unLockApps.add(myAppInfo);
                }
            }

            //get all installed app
//            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
//            for (int i = 0; i < packageInfos.size(); i++) {
//                PackageInfo packageInfo = packageInfos.get(i);
//                //过滤掉系统app
//                if ((ApplicationInfo.FLAG_SYSTEM & packageInfo.applicationInfo.flags) != 0) {
//                    continue;
//                }
//                MyAppInfo myAppInfo = new MyAppInfo();
//                myAppInfo.setAppName(packageInfo.packageName);
////                if (packageInfo.applicationInfo.loadIcon(packageManager) == null) {
////                    continue;
////                }
//                myAppInfo.setImage(packageInfo.applicationInfo.loadIcon(packageManager));
//                myAppInfo.setAppLabel(packageInfo.applicationInfo.loadLabel(packageManager).toString());
//                if (mLockApp.contains(packageInfo.packageName)) {
//                    myAppInfo.setLocked(true);
//                    lockApps.add(myAppInfo);
//                } else {
//                    myAppInfo.setLocked(false);
//                    unLockApps.add(myAppInfo);
//                }
//            }
            myAppInfos.addAll(lockApps);
            myAppInfos.addAll(unLockApps);
            return myAppInfos;
        } catch (Exception e) {
            Log.e(TAG, "===============获取应用包信息失败");
            return null;
        }
    }

    class QueryInstallApp extends AsyncTask<Void, Void, LinkedList<MyAppInfo>> {

        @Override
        protected LinkedList<MyAppInfo> doInBackground(Void... params) {

            getLockApp();
            return scanLocalInstallAppList();
        }

        @Override
        protected void onPostExecute(LinkedList<MyAppInfo> myAppInfos) {
            if (myAppInfos == null || myAppInfos.isEmpty()) {
                taskCleanSetAdapter = new TaskCleanSetAdapter(myAppInfos, 0);
            } else {
                int lockCount = 0;
                for (MyAppInfo myAppInfo : myAppInfos) {
                    if (myAppInfo.locked) {
                        lockCount++;
                    }
                }
                taskCleanSetAdapter = new TaskCleanSetAdapter(myAppInfos, lockCount);
            }

            recyclerView.setAdapter(taskCleanSetAdapter);
        }
    }

    public static class DividerItemDecoration extends RecyclerView.ItemDecoration {

        public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

        public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

        private Drawable mDivider;

        private int mOrientation;

        public DividerItemDecoration(Drawable drawable, int orientation) {
            mDivider = drawable;
            setOrientation(orientation);
        }

        public void setOrientation(int orientation) {
            if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
                throw new IllegalArgumentException("invalid orientation");
            }
            mOrientation = orientation;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent) {
            if (mOrientation == VERTICAL_LIST) {
                drawVertical(c, parent);
            } else {
                drawHorizontal(c, parent);
            }
        }

        public void drawVertical(Canvas c, RecyclerView parent) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin +
                        Math.round(ViewCompat.getTranslationY(child));
                final int bottom = top + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        public void drawHorizontal(Canvas c, RecyclerView parent) {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getHeight() - parent.getPaddingBottom();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int left = child.getRight() + params.rightMargin +
                        Math.round(ViewCompat.getTranslationX(child));
                final int right = left + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
            if (mOrientation == VERTICAL_LIST) {
                outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
            } else {
                outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
            }
        }
    }

    private void setLockApp() {
        FileOutputStream fileOutputStream = null;
        BufferedWriter writer = null;
        try {
            fileOutputStream = TaskCleanSetActivity.this.openFileOutput(File_LOCK_APP, MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            StringBuilder stringBuilder = new StringBuilder();
            LinkedList<MyAppInfo> datas = taskCleanSetAdapter.getDatas();
            for (int i=0;i<datas.size();i++) {
                if (datas.get(i).isLocked()) {
                    stringBuilder.append(datas.get(i).getAppName() + ";");
                }
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

    private void getLockApp() {
        FileInputStream fileInputStream = null;
        BufferedReader bufferedReader = null;
        try {
            fileInputStream = TaskCleanSetActivity.this.openFileInput(File_LOCK_APP);
            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            if (stringBuilder.length() > 0) {
                String[] strings = stringBuilder.toString().split(";");
                for (String string : strings) {
                    mLockApp.add(string);
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
}
