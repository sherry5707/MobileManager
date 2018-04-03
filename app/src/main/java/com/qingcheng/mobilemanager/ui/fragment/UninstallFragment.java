package com.qingcheng.mobilemanager.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.adapter.AppManagerAdapter;
import com.qingcheng.mobilemanager.bean.AppInfo;
import com.qingcheng.mobilemanager.engine.AppInfoProvider;
import com.qingcheng.mobilemanager.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 18-1-29.
 */

public class UninstallFragment extends Fragment {
    private LayoutInflater mInflater;
    private Context mContext;
    private ListView lvAppDisplay;
    private ArrayList<AppInfo> mDatas;
    private View rlUninstall;
    private ProgressBar mProgressbar;
    private AppManagerAdapter mAdapter;
    private boolean checkedAll = false;
    private List<AppInfo> mCurrentCheckedAppInfo = new ArrayList<AppInfo>();  //当前被选中info的集合
    private TextView tvCheckAll;
    private List<Integer> listItemID = new ArrayList<Integer>();//被选中应用的position的集合

    @Override
    public void onAttach(Activity activity) {
        mContext = activity;
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        View view = inflater.inflate(R.layout.uninstallfragment, null);
        lvAppDisplay = (ListView) view.findViewById(R.id.lv_app_display);
        lvAppDisplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.mChecked.set(position,!mAdapter.mChecked.get(position));
                mAdapter.notifyDataSetChanged();
            }
        });
        mProgressbar = (ProgressBar)view.findViewById(R.id.progressbar);
        rlUninstall = view.findViewById(R.id.rl_uninstall);
        rlUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uninstall();
            }
        });
        initData();
        return view;
    }

    private Handler mHandler = new Handler();
    private Runnable initdataRunnable = new Runnable() {
        @Override
        public void run() {
            mAdapter = new AppManagerAdapter(mContext, mDatas);
            lvAppDisplay.setAdapter(mAdapter);
            mProgressbar.setVisibility(View.GONE);
        }
    };

    public void initData() {
        new Thread(){
            @Override
            public void run() {
                ArrayList<AppInfo> list = AppInfoProvider.getInstalledApps(mContext.getApplicationContext());
                mDatas = new ArrayList<>();

                for (AppInfo info : list){
                    if (info.isUserApp){
                        mDatas.add(info);
                    }
                }

                mHandler.post(initdataRunnable);
            }
        }.start();
    }

    public void uninstall(){
        listItemID.clear();
        mCurrentCheckedAppInfo.clear();
        try{
            for (int i = 0;i< mAdapter.mChecked.size();i++){
                if (mAdapter.mChecked.get(i)){
                    listItemID.add(i);
                    mCurrentCheckedAppInfo.add(mDatas.get(i));
                }
            }
        }catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            ToastUtils.showIntToast(mContext,R.string.without_checked_app);
        }

        if (listItemID.size() == 0){
            ToastUtils.showIntToast(mContext,R.string.without_checked_app);
        } else {
            for (int i = 0; i < mCurrentCheckedAppInfo.size(); i++){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_DELETE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + mCurrentCheckedAppInfo.get(i).packageName));
                startActivityForResult(intent,0);
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //mAdapter.notifyDataSetChanged();
        initData();
    }
}
