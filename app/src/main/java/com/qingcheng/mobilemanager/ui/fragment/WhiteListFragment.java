package com.qingcheng.mobilemanager.ui.fragment;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.bean.CheckedPermRecord;
import com.qingcheng.mobilemanager.taskmanager.WhiteListManager;
import com.qingcheng.mobilemanager.utils.PermControlUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 18-1-30.
 */

public class WhiteListFragment extends Fragment implements AdapterView.OnItemClickListener{
    private static final String TAG="WhiteListFragment";
    private Context mCxt;
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    private View mRootView;
    protected ListView mListView;
    protected TextView mEmptyView;
    protected AutoBootAdapter mApdater;
    private AutoBootAysncLoader mAsyncTask;
    private WhiteListManager mWhiteListManager;

    public static final String KEY_FROM_PERMISSSION_MANAGE = "from_permission_manage";

    public static WhiteListFragment newInstance(boolean isFromPermissionManage) {

        Bundle args = new Bundle();
        args.putBoolean(KEY_FROM_PERMISSSION_MANAGE, isFromPermissionManage);
        WhiteListFragment fragment = new WhiteListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCxt = activity;
    }

    @Override
    public void onStart() {
        super.onStart();
        //setactionbar click
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mInflater=inflater;
        mRootView=inflater.inflate(R.layout.white_list,container,false);
        mListView=(ListView)mRootView.findViewById(android.R.id.list);
        mListView.setSaveEnabled(true);
        mListView.setItemsCanFocus(true);
        mListView.setOnItemClickListener(this);
        mListView.setTextFilterEnabled(true);
        mEmptyView = (TextView) mRootView.findViewById(R.id.empty);
        mApdater = new AutoBootAdapter();
        mListView.setAdapter(mApdater);
        if (!getArguments().getBoolean(KEY_FROM_PERMISSSION_MANAGE, false)) {
            mRootView.findViewById(R.id.rl_toolbar).setVisibility(View.GONE);
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        Log.i(TAG,"onResume");
        super.onResume();
        mWhiteListManager = new WhiteListManager(mCxt, UserHandle.myUserId());
        load();
    }

    protected void load() {
        mAsyncTask = (AutoBootAysncLoader) new AutoBootAysncLoader().execute();
    }

    protected List<CheckedPermRecord> loadData() {
        if (mWhiteListManager != null) {
            mWhiteListManager.initAutoBootList(false);
            return mWhiteListManager.getBootList();
        } else {
            return new ArrayList<CheckedPermRecord>();
        }
    }

    protected void refreshUi(boolean dataChanged) {
        AutoBootAdapter adapter = (AutoBootAdapter) (mListView.getAdapter());
        if (dataChanged) {
            adapter.notifyDataSetChanged();
        }
        // if the data list is null , set the empty notify text
        if (adapter.mReceiverList == null || adapter.mReceiverList.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView l = (ListView) parent;
        AutoBootAdapter adapter = (AutoBootAdapter) l.getAdapter();
        CheckedPermRecord info = (CheckedPermRecord) adapter.getItem(position);
        String pkgName = info.mPackageName;
        Switch statusBox = (Switch) view.findViewById(R.id.status);
        if (!statusBox.isEnabled()) {
            Log.d("@M_" + TAG, "onItemClick , " + pkgName + " but disabled");
            return;
        }

        // get current status , change to opp
        boolean status = info.isEnable();

        // just change switch status , then will trigger switch's checkedChangeListener
        statusBox.setChecked(!status);

        handleItemClick(new CheckedPermRecord(info.mPackageName,info.mUid, info.mPermission,
                !info.isEnable()));
    }

    protected void handleItemClick(CheckedPermRecord info) {
        if (mWhiteListManager != null) {
            mWhiteListManager.setPkgWhiteListPolicy(info);
//            updateSummary();
        }
        load();
    }

    class AutoBootAdapter extends BaseAdapter {
        List<CheckedPermRecord> mReceiverList = new ArrayList<CheckedPermRecord>();

        public AutoBootAdapter() {

        }

        public void setDataAndNotify(List<CheckedPermRecord> receiverList) {
            mReceiverList = receiverList;
            refreshUi(true);
        }

        @Override
        public int getCount() {
            if (mReceiverList != null) {
                return mReceiverList.size();
            }
            return 0;
        }

        @Override
        public CheckedPermRecord getItem(int position) {
            return mReceiverList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            CheckedPermRecord infoItem = getItem(position);
            if (convertView == null) {
                v = mInflater.inflate(R.layout.white_list_app_item, parent, false);
            } else {
                v = convertView;
            }
            ImageView appIcon = (ImageView) v.findViewById(R.id.app_icon);
            TextView pkgLabel = (TextView) v.findViewById(R.id.app_name);
            String pkgName = infoItem.mPackageName;
            String label = PermControlUtils.getApplicationName(mCxt,
                    pkgName);
            if (label != null) {
                Drawable icon = PermControlUtils.getApplicationIcon(mCxt,
                        pkgName);
                appIcon.setImageDrawable(icon);
                pkgLabel.setText(label);
            } else {
                pkgLabel.setText(pkgName);
            }
            Switch sswitch = (Switch ) v.findViewById(R.id.status);
            sswitch.setChecked(infoItem.isEnable());
            sswitch.setEnabled(true);
            return v;
        }
    }

    private class AutoBootAysncLoader extends
            AsyncTask<Void, Integer, List<CheckedPermRecord>> {
        @Override
        protected List<CheckedPermRecord> doInBackground(Void... params) {
            // get the origin data
            if (isCancelled()) {
                return null;
            }

            List<CheckedPermRecord> recordList = loadData();
            if (recordList == null) {
                return null;
            }
            return recordList;
        }

        @Override
        protected void onPostExecute(List<CheckedPermRecord> receiverList) {
            mApdater.setDataAndNotify(receiverList);
        }
    }
}
