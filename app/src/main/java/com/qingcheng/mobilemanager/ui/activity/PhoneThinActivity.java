package com.qingcheng.mobilemanager.ui.activity;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.mediatek.storage.StorageManagerEx;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.fragment.PhoneThinCleaningFragment;
import com.qingcheng.mobilemanager.ui.fragment.PhoneThinContentFragment;
import com.qingcheng.mobilemanager.utils.UiUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by chunfengliu on 2/12/18.
 */

public class PhoneThinActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView titleView;
    private ImageView backView;
    private TextView allSelectView;

    public static final int FRAGMENT_CONTENT = 0;

    private static final String TAG = "PhoneThinActivity";

    private static final String STORAGE_PATH_PHONE = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String STORAGE_PATH_TFCARD = StorageManagerEx.getExternalStoragePath();

    private List<File> mVideoFiles = new ArrayList<>();

    private List<File> mApkFiles = new ArrayList<>();

    private List<File> mPictureFiles = new ArrayList<>();

    private PhoneThinContentFragment phoneThinContentFragment;

    private PhoneThinCleaningFragment phoneThinCleaningFragment;

    private String[] mAllSelecteds;

    private ImageAsyncTask imageAsyncTask;

    private VideoAsyncTask videoAsyncTask;

    private FileAsyncTask fileAsyncTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_thin);
        titleView = (TextView) findViewById(R.id.toolbar_title);
        backView = (ImageView) findViewById(R.id.iv_traffic_pre);
        backView.setOnClickListener(this);
        allSelectView = (TextView) findViewById(R.id.toolbar_all_select);
        allSelectView.setOnClickListener(this);
        phoneThinContentFragment = new PhoneThinContentFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, phoneThinContentFragment).commit();
        UiUtils.setScreen(this);
        getWindow().setStatusBarColor(getColor(R.color.bluedarker));
        mAllSelecteds = new String[]{getString(R.string.check_all), getString(R.string.no_item_select)};
    }

    @Override
    protected void onStart() {
        super.onStart();
        initData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseData();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_traffic_pre:
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
                break;
            case R.id.toolbar_all_select:
                if (phoneThinCleaningFragment != null) {
                    phoneThinCleaningFragment.setAllSelected();
                    setAllSelectViewText(phoneThinCleaningFragment.isAllSelected());
                }
                break;
        }
    }

    public void changeStatus(int status) {
        if (FRAGMENT_CONTENT == status) {
            titleView.setText(getString(R.string.weight_loss));
            allSelectView.setVisibility(View.GONE);
        } else if (PhoneThinCleaningFragment.FRAGMENT_APK == status){
            titleView.setText(getString(R.string.apk_clear));
            allSelectView.setVisibility(View.VISIBLE);
        } else if (PhoneThinCleaningFragment.FRAGMENT_APP == status) {
            titleView.setText(getString(R.string.uninstall_app));
            allSelectView.setVisibility(View.VISIBLE);
        } else if (PhoneThinCleaningFragment.FRAGMENT_IMAGE == status) {
            titleView.setText(getString(R.string.photo_clean));
            allSelectView.setVisibility(View.VISIBLE);
        } else if (PhoneThinCleaningFragment.FRAGMENT_VIDEO == status) {
            titleView.setText(getString(R.string.video_manage));
            allSelectView.setVisibility(View.VISIBLE);
        }
    }

    private List<File> getFileListByType(int type) {
        List<File> fileDataList = new ArrayList<File>();
        Cursor cursor = null;
        try {
            if (type == 6) {
                File externalFile = new File(STORAGE_PATH_PHONE + "/Pictures/Screenshots");
                if (externalFile.exists()) {
                    File[] infiles = externalFile.listFiles();
                    if (infiles != null) {
                        for (int i = 0; i < infiles.length; i++) {
                            String filePath = infiles[i].getAbsolutePath();
                            File data = new File(filePath);
                            fileDataList.add(data);
                        }
                    }
                }
            } else if (type == 7) {
                File externalFile = new File(STORAGE_PATH_PHONE + "/bluetooth");
                if (externalFile.exists()) {
                    File[] infiles = externalFile.listFiles();
                    if (infiles != null) {
                        for (int i = 0; i < infiles.length; i++) {
                            String filePath = infiles[i].getAbsolutePath();
                            File data = new File(filePath);
                            fileDataList.add(data);
                        }
                    }
                }
            } else {
                if (isInternalExist() && isCanUseSdCard()) {
                    scanPathforMediaStore(STORAGE_PATH_PHONE);
                }
                boolean isTFcardExist = isTFCardExist();
                if (isTFcardExist) {
                    scanPathforMediaStore(STORAGE_PATH_TFCARD);
                }
                Uri uri = MediaStore.Files.getContentUri("external");
                // Uri uri = Uri.parse("content://media/external/file");
                    ContentResolver cr = getContentResolver();
                    String selection = null;// base on MediaStore.Files.FileColumns.MIME_TYPE "mime_type"
                    if (type == 0) {// music
                        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        // selection = "mime_type LIKE 'audio/%'";
                    } else if (type == 1) {// zip
                        selection = "mime_type = 'application/zip' OR mime_type = 'application/x-rar-compressed'";
                    } else if (type == 2) {// image
                        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        // selection = "mime_type LIKE 'image/%' AND _data LIKE '%DCIM%'";
                    } else if (type == 3) {// text
                        selection = "mime_type LIKE 'text/%' OR mime_type = 'application/pdf' OR mime_type = 'application/msword' OR mime_type = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' OR mime_type = 'application/vnd.ms-powerpoint' OR mime_type = 'application/vnd.openxmlformats-officedocument.presentationml.presentation' OR mime_type = 'application/vnd.ms-excel' OR mime_type = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'";
                    } else if (type == 4) {// video
                        // uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        selection = "mime_type LIKE 'video/%' OR _data LIKE '%.rmvb'";
                    } else if (type == 5) {// android package
                        selection = "mime_type = 'application/vnd.android.package-archive' OR _data LIKE '%.apk'";
                    } else if (type == 8) { //added recently
                        long sinceDate = (System.currentTimeMillis() - 3600L * 1000 * 24 * 30) / 1000; // one mounth ago
                        Log.d(TAG, "one mouth ago second:" + sinceDate);
                        selection = "mime_type is not null and " + MediaStore.Files.FileColumns.DATE_MODIFIED + " >= " + sinceDate;
                    }
                    String order = null;
                    if (type == 2) {
                        order = "datetaken";
                    } else if (type == 8) {
                        order = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";
                    } else {
                        order = MediaStore.Files.FileColumns.DATA;
                    }
                    cursor = cr.query(uri, null, selection, null, order);
                    android.util.Log.i(TAG, "type " + type + " cursor.getCount() == " + cursor.getCount());
                    if (cursor.moveToFirst()) {
                        do {
                            String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                            // if (filePath.contains("/.") && !SharedPreferencesUtil.isShowHidenFiles(mContext)) {
                            if (filePath.contains("/.") || filePath.startsWith("/system/")) {// never show the hide file or directory, EWWJLJ-1526
                                continue;
                            }
                            if (!TextUtils.isEmpty(STORAGE_PATH_TFCARD) && !isTFcardExist && filePath.startsWith(STORAGE_PATH_TFCARD)) {
                                continue;
                            }
                            if (!isTFcardExist && !filePath.startsWith(STORAGE_PATH_PHONE)) {
                                continue;
                            }
                            if (!new File(filePath).exists()) {// seem not needed since add scanPathforMediaStore method
                                continue;
                            }
                            File data = new File(filePath);
                            fileDataList.add(data);
                        } while (cursor.moveToNext());
                    }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileDataList;
    }

    private void scanPathforMediaStore(String path) {
        if ( !TextUtils.isEmpty(path)) {
            String[] paths = {path};
            MediaScannerConnection.scanFile(PhoneThinActivity.this, paths, null, null);
        }
    }

    private static boolean isInternalExist() {
        // //if is needed add this
        // if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        // return false;
        // }

        File root = Environment.getDataDirectory();
        try {
            StatFs statFs = new StatFs(root.getPath());
            long blockSize = statFs.getBlockSize();
            long totalBlocks = statFs.getBlockCount();
            if (totalBlocks * blockSize > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private static boolean isCanUseSdCard() {
        try {
            return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isTFCardExist() {
        // if no external SD card , StorageManagerEx.getExternalStoragePath() is "" .
        if (TextUtils.isEmpty(StorageManagerEx.getExternalStoragePath())) {
            Log.d(TAG, "isTFCardExist STORAGE_PATH_TFCARD is empty , false");
            return false;
        }
        File root = new File(StorageManagerEx.getExternalStoragePath());
        if (root.exists()) {
            File[] files = root.listFiles();
//            if (files != null && files.length > 0 && root.length() > 0) {
            if (files != null && root.length() > 0) { // bug: EWWJLJ-2215, delete condition: files.length > 0.
                try {
                    StatFs statFs = new StatFs(root.getPath());
                    long blockSize = statFs.getBlockSize();
                    long totalBlocks = statFs.getBlockCount();
                    if (totalBlocks * blockSize > 0) {
                        Log.d(TAG, "isTFCardExist true");
                        return true;
                    }
                } catch (java.lang.IllegalArgumentException e) {
                    e.printStackTrace();
                    Log.d(TAG, "isTFCardExist false");
                    return false;
                }
            }
        }
        Log.d(TAG, "isTFCardExist false");
        return false;
    }

    public static long getTotalPhoneStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            long total = 0;
            if (isInternalExist()) {
                total = getTotalStorage(STORAGE_PATH_PHONE);
            }
            return total;
        }
        return 0;
    }

    private static long getTotalStorage(String path) {
        try {
            Log.d(TAG, "getTotalStorage " + path);
            StatFs statFs = new StatFs(path);
            long blockSize = statFs.getBlockSize();
            long totalBlocks = statFs.getBlockCount();
            return totalBlocks * blockSize;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getAvailablePhoneStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            long total = 0;
            if (isInternalExist()) {
                total = getAvailableStorage(STORAGE_PATH_PHONE);
            }
            return total;
        }
        return 0;
    }

    private static long getAvailableStorage(String path) {
        try {
            Log.d(TAG, "getAvailableStorage " + path);
            StatFs statFs = new StatFs(path);
            long blockSize = statFs.getBlockSize();
            long availableBlocks = statFs.getAvailableBlocks();
            return blockSize * availableBlocks;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static long getTotalTFCardStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            long total = 0;
            if (isInternalExist()) {
                total = getTotalStorage(STORAGE_PATH_TFCARD);
            }
            return total;
        }
        return 0;
    }

    public static long getAvailableTFCardStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            long total = 0;
            if (isInternalExist()) {
                total = getAvailableStorage(STORAGE_PATH_TFCARD);
            }
            return total;
        }
        return 0;
    }

    private void initData() {
        imageAsyncTask = new ImageAsyncTask();
        imageAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        videoAsyncTask = new VideoAsyncTask();
        videoAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        fileAsyncTask = new FileAsyncTask();
        fileAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void releaseData() {
        if (imageAsyncTask != null) {
            imageAsyncTask.cancel(true);
            imageAsyncTask = null;
        }
        if (videoAsyncTask != null) {
            videoAsyncTask.cancel(true);
            videoAsyncTask = null;
        }
        if (fileAsyncTask != null) {
            fileAsyncTask.cancel(true);
            fileAsyncTask = null;
        }
    }

    class FileAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mApkFiles.clear();
            mApkFiles = getFileListByType(5);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (phoneThinContentFragment != null) {
                phoneThinContentFragment.refreshData(PhoneThinContentFragment.VIEW_TYPE_APK_CLEAN, mApkFiles.isEmpty());
            }
            Log.d(TAG, "apk file asynctast on post apk file=" + mApkFiles.size());
        }
    }

    class ImageAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mPictureFiles.clear();
            mPictureFiles = getFileListByType(2);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (phoneThinContentFragment != null) {
                phoneThinContentFragment.refreshData(PhoneThinContentFragment.VIEW_TYPE_PICTURE, mPictureFiles.isEmpty());
            }
            Log.d(TAG, "image asynctast on post picture=" + mPictureFiles.size());
        }
    }

    class VideoAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mVideoFiles.clear();
            mVideoFiles = getFileListByType(4);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (phoneThinContentFragment != null) {
                phoneThinContentFragment.refreshData(PhoneThinContentFragment.VIEW_TYPE_VIDEO, mVideoFiles.isEmpty());
            }
            Log.d(TAG, "video asynctast on post video=" + mVideoFiles.size());
        }
    }

    public List<File> getVideoFiles() {
        return mVideoFiles;
    }

    public List<File> getPictureFiles() {
        return mPictureFiles;
    }

    public List<File> getApkFiles() {
        return mApkFiles;
    }

    public void changeFragment(int type) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        phoneThinCleaningFragment = PhoneThinCleaningFragment.newInstance(type);
        fragmentTransaction.hide(phoneThinContentFragment);
        fragmentTransaction.add(R.id.fragment_container, phoneThinCleaningFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        changeStatus(type);
    }

    public List<PhoneThinContentFragment.ApkInfo> getApkInfos() {
        if (phoneThinContentFragment != null) {
            return phoneThinContentFragment.getApkInfos();
        } else {
            return null;
        }
    }

    public String[] getInstalledStrings() {
        if (phoneThinContentFragment != null) {
            return phoneThinContentFragment.getInstalledStrings();
        } else {
            return null;
        }
    }

    public static class DividerGridItemDecoration extends RecyclerView.ItemDecoration {

        private Drawable mDivider;

        public DividerGridItemDecoration(Drawable drawable) {
            mDivider = drawable;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

            drawHorizontal(c, parent);
            drawVertical(c, parent);

        }

        private int getSpanCount(RecyclerView parent) {
            // 列数
            int spanCount = -1;
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {

                spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                spanCount = ((StaggeredGridLayoutManager) layoutManager)
                        .getSpanCount();
            }
            return spanCount;
        }

        public void drawHorizontal(Canvas c, RecyclerView parent) {
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int left = child.getLeft() - params.leftMargin;
                final int right = child.getRight() + params.rightMargin
                        + mDivider.getIntrinsicWidth();
                final int top = child.getBottom() + params.bottomMargin;
                final int bottom = top + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        public void drawVertical(Canvas c, RecyclerView parent) {
            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);

                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int top = child.getTop() - params.topMargin;
                final int bottom = child.getBottom() + params.bottomMargin;
                final int left = child.getRight() + params.rightMargin;
                final int right = left + mDivider.getIntrinsicWidth();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        private boolean isLastColum(RecyclerView parent, int pos, int spanCount,
                                    int childCount) {
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                if ((pos + 1) % spanCount == 0)// 如果是最后一列，则不需要绘制右边
                {
                    return true;
                }
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int orientation = ((StaggeredGridLayoutManager) layoutManager)
                        .getOrientation();
                if (orientation == StaggeredGridLayoutManager.VERTICAL) {
                    if ((pos + 1) % spanCount == 0)// 如果是最后一列，则不需要绘制右边
                    {
                        return true;
                    }
                } else {
                    childCount = childCount - childCount % spanCount;
                    if (pos >= childCount)// 如果是最后一列，则不需要绘制右边
                        return true;
                }
            }
            return false;
        }

        private boolean isLastRaw(RecyclerView parent, int pos, int spanCount,
                                  int childCount) {
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                childCount = childCount - childCount % spanCount;
                if (pos >= childCount)// 如果是最后一行，则不需要绘制底部
                    return true;
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int orientation = ((StaggeredGridLayoutManager) layoutManager)
                        .getOrientation();
                // StaggeredGridLayoutManager 且纵向滚动
                if (orientation == StaggeredGridLayoutManager.VERTICAL) {
                    childCount = childCount - childCount % spanCount;
                    // 如果是最后一行，则不需要绘制底部
                    if (pos >= childCount)
                        return true;
                } else
                // StaggeredGridLayoutManager 且横向滚动
                {
                    // 如果是最后一行，则不需要绘制底部
                    if ((pos + 1) % spanCount == 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void getItemOffsets(Rect outRect, int itemPosition,
                                   RecyclerView parent) {
            int spanCount = getSpanCount(parent);
            int childCount = parent.getAdapter().getItemCount();
            if (isLastRaw(parent, itemPosition, spanCount, childCount))// 如果是最后一行，则不需要绘制底部
            {
                outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
            } else if (isLastColum(parent, itemPosition, spanCount, childCount))// 如果是最后一列，则不需要绘制右边
            {
                outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
            } else {
                outRect.set(0, 0, mDivider.getIntrinsicWidth(),
                        mDivider.getIntrinsicHeight());
            }
        }
    }

    public void setAllSelectViewText(boolean isAllSelected) {
        if (isAllSelected) {
            allSelectView.setText(mAllSelecteds[1]);
        } else {
            allSelectView.setText(mAllSelecteds[0]);
        }
    }

}
