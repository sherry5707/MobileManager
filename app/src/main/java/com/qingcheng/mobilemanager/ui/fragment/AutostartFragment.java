package com.qingcheng.mobilemanager.ui.fragment;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.bean.CheckedPermRecord;
import com.qingcheng.mobilemanager.taskmanager.WhiteListManager;
import com.qingcheng.mobilemanager.ui.activity.NewPermissionManagerActivity;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanSetActivity;
import com.qingcheng.mobilemanager.utils.PermControlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chunfengliu on 3/14/18.
 */

public class AutostartFragment extends Fragment {

    private RecyclerView recyclerView;

    private MyAdapter myAdapter;

    private AutoBootAysncLoader mAsyncTask;
    private WhiteListManager mWhiteListManager;

    private static final String TAG = "AutostartFragment";

    private Context mCxt;

    private boolean isShowingSystemApp = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCxt = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_autostart, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.id_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mCxt));
        recyclerView.addItemDecoration(new TaskCleanSetActivity.DividerItemDecoration(mCxt.getDrawable(R.drawable.divider_black_light),
                TaskCleanSetActivity.DividerItemDecoration.VERTICAL_LIST));

        mWhiteListManager = new WhiteListManager(mCxt, UserHandle.myUserId());
        mAsyncTask = (AutoBootAysncLoader) new AutoBootAysncLoader().execute(isShowingSystemApp);

        setHasOptionsMenu(true);
        ((NewPermissionManagerActivity) getActivity()).setTitle(R.string.whitelist);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_autostart_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.system_app:
                if (isShowingSystemApp) {
                    isShowingSystemApp = false;
                    item.setTitle(R.string.show_system_app);
                    mAsyncTask.cancel(true);
                    mAsyncTask = (AutoBootAysncLoader) new AutoBootAysncLoader().execute(isShowingSystemApp);
                } else {
                    isShowingSystemApp = true;
                    item.setTitle(R.string.hide_system_app);
                    mAsyncTask.cancel(true);
                    mAsyncTask = (AutoBootAysncLoader) new AutoBootAysncLoader().execute(isShowingSystemApp);
                }
                return true;
            case R.id.about:
                new AutostartDialogFragment().show(getFragmentManager(), "dialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAsyncTask.cancel(true);
    }

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<CheckedPermRecord> mReceiverList;

        private int mAutostartEnabledCount = 0;

        private static final int VIEWTYPE_TITLE = 0;
        private static final int VIEWTYPE_CONTENT = 1;

        public MyAdapter(List<CheckedPermRecord> checkedPermRecords, int enabledCount) {
            mReceiverList = checkedPermRecords;
            mAutostartEnabledCount = enabledCount;
        }

        public void setReceiverList(List<CheckedPermRecord> mReceiverList) {
            this.mReceiverList = mReceiverList;
        }

        public void setAutostartEnabledCount(int mAutostartEnabledCount) {
            this.mAutostartEnabledCount = mAutostartEnabledCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEWTYPE_TITLE) {
                view = LayoutInflater.from(mCxt).inflate(R.layout.running_task_header, parent, false);
                return new TitleViewHolder(view);
            } else if (viewType == VIEWTYPE_CONTENT) {
                view = LayoutInflater.from(mCxt).inflate(R.layout.item_taskclean_set, parent, false);
                return new ContentViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TitleViewHolder) {
                if (mAutostartEnabledCount == 0) {
                    ((TitleViewHolder) holder).titleView.setText(getString(R.string.no_autostart_app_count, mReceiverList.size() - mAutostartEnabledCount));
                } else {
                    if (position == 0) {
                        ((TitleViewHolder) holder).titleView.setText(getString(R.string.autostart_app_count, mAutostartEnabledCount));
                    } else {
                        ((TitleViewHolder) holder).titleView.setText(getString(R.string.no_autostart_app_count, mReceiverList.size() - mAutostartEnabledCount));
                    }
                }
            } else {
                ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
                int realIndex = position - 1;
                if (mAutostartEnabledCount != 0 && position > mAutostartEnabledCount) {
                    realIndex--;
                }
                CheckedPermRecord infoItem = mReceiverList.get(realIndex);
                String pkgName = infoItem.mPackageName;
                String label = PermControlUtils.getApplicationName(mCxt,
                        pkgName);
                if (label != null) {
                    Drawable icon = PermControlUtils.getApplicationIcon(mCxt,
                            pkgName);
                    contentViewHolder.imageView.setImageDrawable(icon);
                    contentViewHolder.textView.setText(label);
                } else {
                    contentViewHolder.textView.setText(pkgName);
                }
                final int checkIndex = realIndex;
                contentViewHolder.aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        CheckedPermRecord checkedPermRecord = new CheckedPermRecord(mReceiverList.get(checkIndex).mPackageName,
                                mReceiverList.get(checkIndex).mUid,mReceiverList.get(checkIndex).mPermission, isChecked);
                        mWhiteListManager.setPkgWhiteListPolicy(checkedPermRecord);
                    }
                });
                contentViewHolder.aSwitch.setChecked(infoItem.isEnable());
            }
        }

        @Override
        public int getItemCount() {
            if (mAutostartEnabledCount == 0) {
                return mReceiverList.size() + 1;
            } else {
                return mReceiverList.size() + 2;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (mAutostartEnabledCount == 0) {
                if (position == 0) {
                    return VIEWTYPE_TITLE;
                } else {
                    return VIEWTYPE_CONTENT;
                }
            } else {
                if (position == 0 || position == mAutostartEnabledCount + 1) {
                    return VIEWTYPE_TITLE;
                } else {
                    return VIEWTYPE_CONTENT;
                }
            }
        }

        class TitleViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;

            public TitleViewHolder(View itemView) {
                super(itemView);
                titleView = (TextView) itemView.findViewById(R.id.running_task);
            }
        }

        class ContentViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;
            Switch aSwitch;
            public ContentViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.app_icon);
                textView = (TextView) itemView.findViewById(R.id.app_name);
                aSwitch = (Switch) itemView.findViewById(R.id.app_lock);
            }
        }


    }

    private class AutoBootAysncLoader extends
            AsyncTask<Boolean, Integer, List<CheckedPermRecord>> {
        @Override
        protected List<CheckedPermRecord> doInBackground(Boolean... params) {
            // get the origin data
            if (isCancelled()) {
                return null;
            }

            List<CheckedPermRecord> recordList = loadData(params[0]);
            if (recordList == null) {
                return null;
            }
            return recordList;
        }

        @Override
        protected void onPostExecute(List<CheckedPermRecord> receiverList) {
            if (receiverList == null || receiverList.isEmpty()) {
                return;
            }
            int count = 0;
            for (CheckedPermRecord checkedPermRecord : receiverList) {
                if (checkedPermRecord.isEnable()) {
                    count++;
                }
            }
            if (myAdapter == null) {
                myAdapter = new MyAdapter(receiverList, count);
                recyclerView.setAdapter(myAdapter);
            } else {
                myAdapter.setAutostartEnabledCount(count);
                myAdapter.setReceiverList(receiverList);
                myAdapter.notifyDataSetChanged();
            }
        }
    }

    private List<CheckedPermRecord> loadData(boolean isShowingSystemApp) {
        if (mWhiteListManager != null) {
            mWhiteListManager.initAutoBootList(isShowingSystemApp);
            return mWhiteListManager.getBootList();
        } else {
            return new ArrayList<CheckedPermRecord>();
        }
    }


}
