package com.qingcheng.mobilemanager.ui.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.activity.PhoneThinActivity;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanSetActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.bumptech.glide.Glide;

/**
 * Created by chunfengliu on 3/6/18.
 */

public class PhoneThinCleaningFragment extends Fragment {
    private RecyclerView recyclerView;

    private TextView mDeleteView;

    private Context mContext;

    private List<File> fileList;

    private int mType;

    private List<PhoneThinContentFragment.ApkInfo> mApkInfos;

    private static final String FRAGMENT_TYPE = "fragment_type";

    public static final int FRAGMENT_IMAGE = 10;

    public static final int FRAGMENT_VIDEO = 11;

    public static final int FRAGMENT_APK = 12;

    public static final int FRAGMENT_APP = 13;

    private static final String TAG = "PhoneThinCleaning";

    private String[] mInstalledStrings;

    private List<File> mSelected;

    private boolean isAllSelected = false;

    private RecyclerView.Adapter mAdapter;

    public static PhoneThinCleaningFragment newInstance(int type) {

        Bundle args = new Bundle();
        args.putInt(FRAGMENT_TYPE, type);
        PhoneThinCleaningFragment fragment = new PhoneThinCleaningFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initData();
        mContext = getContext();
        View view = inflater.inflate(R.layout.fragment_phone_thin_cleaning, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.id_recyclerview);
        mDeleteView = (TextView) view.findViewById(R.id.tv_confirm);
        mDeleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //delete file
                for (File file : mSelected) {
                    try {
                        if (mType == FRAGMENT_IMAGE) {
                            deleteImage(file);
                        } else if (mType == FRAGMENT_VIDEO) {
                            deleteVideo(file);
                        }else {
                            file.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (mType == FRAGMENT_APK) {
                        mApkInfos.remove(fileList.indexOf(file));
                    }
                    fileList.remove(file);
                }
                Log.d(TAG, "delete before files="+fileList.size() + " mselect=" + mSelected.toString());
                mSelected.clear();
                mAdapter.notifyDataSetChanged();
                Log.d(TAG, "delete after files="+fileList.size() + " mselect=" + mSelected.toString());
            }
        });
        if (mType == FRAGMENT_IMAGE) {
            recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
            recyclerView.addItemDecoration(new PhoneThinActivity.DividerGridItemDecoration(getResources().getDrawable(R.drawable.divider_grid, null)));
            mAdapter = new MyImageAdapter();
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            recyclerView.addItemDecoration(new TaskCleanSetActivity.DividerItemDecoration(getResources().getDrawable(R.drawable.divider_black_light, null),
                    TaskCleanSetActivity.DividerItemDecoration.VERTICAL_LIST));
            mAdapter = new PhoneThinCleaningAdapter();
        }
        recyclerView.setAdapter(mAdapter);
        return view;
    }

    private void initData() {
        mSelected = new LinkedList<>();
        mType = getArguments().getInt(FRAGMENT_TYPE);
        switch (mType) {
            case FRAGMENT_IMAGE:
                fileList = ((PhoneThinActivity) getActivity()).getPictureFiles();
                break;
            case FRAGMENT_VIDEO:
                fileList = ((PhoneThinActivity) getActivity()).getVideoFiles();
                break;
            case FRAGMENT_APK:
                fileList = ((PhoneThinActivity) getActivity()).getApkFiles();
                mApkInfos = ((PhoneThinActivity) getActivity()).getApkInfos();
                mInstalledStrings = ((PhoneThinActivity) getActivity()).getInstalledStrings();
                break;
            case FRAGMENT_APP:
                break;
        }
        ((PhoneThinActivity) getActivity()).setAllSelectViewText(isAllSelected);
    }

    class PhoneThinCleaningAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            view = LayoutInflater.from(mContext).inflate(R.layout.item_phone_thin_cleaning, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            File file = fileList.get(position);
            MyViewHolder myViewHolder = (MyViewHolder) holder;
            if (mType == FRAGMENT_APK && mApkInfos != null) {
                myViewHolder.imageViewIcon.setImageDrawable(mApkInfos.get(position).icon);
                myViewHolder.textViewName.setText(mApkInfos.get(position).name);
                myViewHolder.textViewDescribe.setText(mApkInfos.get(position).isInstalled ? mInstalledStrings[0] : mInstalledStrings[1]);
            } else {
                Glide.with(mContext).load(file).into(myViewHolder.imageViewIcon);
                myViewHolder.textViewName.setText(file.getName());
                try {
                    myViewHolder.textViewDescribe.setText(getFormatTime(file.lastModified()));
                } catch (Exception e) {
                    e.printStackTrace();
                    myViewHolder.textViewDescribe.setText(file.getPath());
                }
            }

            myViewHolder.textViewSize.setText(PhoneThinContentFragment.getProperSize(file.length()));
            final int currentPosition = position;

            myViewHolder.checkBoxSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (!mSelected.contains(fileList.get(currentPosition))) {
                            mSelected.add(fileList.get(currentPosition));
                        }
                    } else {
                        if (mSelected.contains(fileList.get(currentPosition))) {
                            mSelected.remove(fileList.get(currentPosition));
                        }
                    }
                    if (mSelected.size() == fileList.size() && !isAllSelected) {
                        isAllSelected = true;
                        ((PhoneThinActivity)getActivity()).setAllSelectViewText(isAllSelected);
                    } else if (mSelected.size() != fileList.size() && isAllSelected ) {
                        isAllSelected = false;
                        ((PhoneThinActivity)getActivity()).setAllSelectViewText(isAllSelected);
                    }
                }
            });
            myViewHolder.checkBoxSelect.setChecked(mSelected.contains(fileList.get(currentPosition)));
            Log.d(TAG, " onbincviewholder files=" + fileList.toString() + " select=" + mSelected.toString());
        }

        @Override
        public int getItemCount() {
            return fileList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView textViewName, textViewDescribe, textViewSize;
        ImageView imageViewIcon;
        CheckBox checkBoxSelect;

        public MyViewHolder(View itemView) {
            super(itemView);
            textViewName = (TextView) itemView.findViewById(R.id.tv_name);
            textViewDescribe = (TextView) itemView.findViewById(R.id.tv_describe);
            textViewSize = (TextView) itemView.findViewById(R.id.tv_size);
            imageViewIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            checkBoxSelect = (CheckBox) itemView.findViewById(R.id.cb_select);
        }
    }

    class MyImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            view = LayoutInflater.from(mContext).inflate(R.layout.item_phone_thin_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            File file = fileList.get(position);
            ImageViewHolder myViewHolder = (ImageViewHolder) holder;
            Glide.with(mContext).load(file).into(myViewHolder.imageView);
            final Integer currentPosition = position;
            myViewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (!mSelected.contains(fileList.get(currentPosition))) {
                            mSelected.add(fileList.get(currentPosition));
                        }
                    } else {
                        if (mSelected.contains(fileList.get(currentPosition))) {
                            mSelected.remove(fileList.get(currentPosition));
                        }
                    }

                    if (mSelected.size() == fileList.size() && !isAllSelected) {
                        isAllSelected = true;
                        ((PhoneThinActivity)getActivity()).setAllSelectViewText(isAllSelected);
                    } else if (mSelected.size() != fileList.size() && isAllSelected ) {
                        isAllSelected = false;
                        ((PhoneThinActivity)getActivity()).setAllSelectViewText(isAllSelected);
                    }
                }
            });
            myViewHolder.checkBox.setChecked(mSelected.contains(fileList.get(currentPosition)));
            Log.d(TAG, " onbincviewholder files=" + fileList.toString() + " select=" + mSelected.toString());
            Log.d(TAG, "pos=" + currentPosition + "  " + mSelected.contains(fileList.get(currentPosition)) + "  " + mSelected.toString());
        }

        @Override
        public int getItemCount() {
            return fileList == null ? 0 : fileList.size();
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        CheckBox checkBox;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            checkBox = (CheckBox) itemView.findViewById(R.id.cb_select);
        }
    }


    public static String getFormatTime(long time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date(time);
        return simpleDateFormat.format(date);
    }

    public void setAllSelected() {
        mSelected.clear();
        if (isAllSelected) {
            isAllSelected = false;
        } else {
            isAllSelected = true;
            mSelected.clear();
            for (int i = 0; i < fileList.size(); i++) {
                mSelected.add(fileList.get(i));
                Log.d(TAG, "selected =" + i);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        return isAllSelected;
    }

    private void deleteImage(File file) {
        //delete in database
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = mContext.getContentResolver();
        String where = MediaStore.Images.Media.DATA + "='" + file.getPath() + "'";
        mContentResolver.delete(uri, where, null);

        //refresh media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] paths = new String[]{Environment.getExternalStorageDirectory().toString()};
            MediaScannerConnection.scanFile(mContext, paths, null, null);
            MediaScannerConnection.scanFile(mContext, new String[] {
                            file.getAbsolutePath()},
                    null, new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri)
                        {
                        }
                    });
        } else {
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    private void deleteVideo(File file) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = mContext.getContentResolver();
        String where = MediaStore.Video.Media.DATA + "='" + file.getPath() + "'";
        mContentResolver.delete(uri, where, null);
    }

}
