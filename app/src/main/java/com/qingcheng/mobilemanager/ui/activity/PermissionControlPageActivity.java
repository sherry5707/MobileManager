package com.qingcheng.mobilemanager.ui.activity;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import com.qingcheng.mobilemanager.R;

import com.qingcheng.mobilemanager.adapter.ActionBarAdapter;
import com.qingcheng.mobilemanager.adapter.ActionBarAdapter.TabState;
import com.qingcheng.mobilemanager.ui.fragment.AppsFragment;
import com.qingcheng.mobilemanager.utils.LogUtil;
import com.qingcheng.mobilemanager.ui.fragment.ManagePermissionsFragment;

public class PermissionControlPageActivity extends AppCompatActivity implements ActionBarAdapter.Listener,
        CompoundButton.OnCheckedChangeListener {

    private static final int PERMISSIONS_INFO = 0;
    private static final int APPS_INFO = 1;
    private static final int NUM_TABS = 2;

    public static final String PERMISSION_CONTROL_STATE = "permission_control_state";



    private ViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    private final TabPagerListener mTabPagerListener = new TabPagerListener();
    final String mPermissionsTag = "tab-pager-perms";
    final String mAppsTag = "tab-pager-apps";


    private ManagePermissionsFragment mPermissionsFragment;
    private AppsFragment mAppsFragment;

    private FrameLayout mEmptyView;
    private Bundle mSavedInstanceState;
    private Switch mSwitch;
    private ActionBarAdapter mActionBarAdapter;


    private boolean mUserCheckedFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, 1000);

        }
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.permission_control_pages);
        LogUtil.i();
        mEmptyView = (FrameLayout) findViewById(R.id.empty_view);
        mSavedInstanceState = savedInstanceState;

        // add the switch on Action bar
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSwitch = new Switch(inflater.getContext());
        final int padding = getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
        mSwitch.setPadding(0, 0, padding, 0);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(mSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
        mSwitch.setOnCheckedChangeListener(this);

        // hide fragment firstly , then update it in onResume() according to switch status
        final FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        mPermissionsFragment = (ManagePermissionsFragment) fragmentManager.findFragmentByTag(mPermissionsTag);
        mAppsFragment = (AppsFragment) fragmentManager.findFragmentByTag(mAppsTag);

        if (mPermissionsFragment == null) {
            mPermissionsFragment = new ManagePermissionsFragment();
            mAppsFragment = new AppsFragment();
            transaction.add(R.id.tab_pager, mPermissionsFragment, mPermissionsTag);
            transaction.add(R.id.tab_pager, mAppsFragment, mAppsTag);
        }

        transaction.hide(mPermissionsFragment);
        transaction.hide(mAppsFragment);
        transaction.commit();
        fragmentManager.executePendingTransactions();

        // set page adapter
        mTabPager = (ViewPager) findViewById(R.id.tab_pager);
        mTabPagerAdapter = new TabPagerAdapter();
        mTabPager.setAdapter(mTabPagerAdapter);
        mTabPager.setOnPageChangeListener(mTabPagerListener);

        // Configure action bar
        mActionBarAdapter = new ActionBarAdapter(this, this, getSupportActionBar());

        boolean isShow = isPermControlOn(this);
        LogUtil.d("oncreate(), isShow " + isShow);


    }

    @Override
    public void onBackPressed() {
        //Intent intent = new Intent(this, HomeActivity.class);
        //startActivity(intent);
        this.finish();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        LogUtil.i();
        if (ActivityManager.isUserAMonkey()) {
            LogUtil.d( "Monkey is running");
            return;
        }

        if (!mUserCheckedFlag) {
            LogUtil.d( "mUserCheckedFlag is false , return ");
            return;
        }

       LogUtil.d("onCheckedChanged(),isChecked = " + isChecked);

        if (isChecked) {
            addUI();
            enablePermissionControl(true, this);
        } else {
  /*          // get the value from provider , it's 0 by default
            boolean isShowDlg = Settings.System.getInt(getContentResolver(),
                    PermControlUtils.PERMISSION_SWITCH_OFF_DLG_STATE, 0) == 0;
            Log.d("@M_" + TAG, "onCheckedChanged(), isShow alert Dlg = " + isShowDlg);
            if (isShowDlg) {
                // show alert dialog
                Intent intent = new Intent();
                intent.setAction(UiUtils.ACTION_SWITCH_OFF_CONTROL_FROM_APP_PERM);
                startActivity(intent);
            } else {*/
                removeUI();
                enablePermissionControl(false, this);
          //  }
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
       LogUtil.d("onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        if (mActionBarAdapter != null) {
            mActionBarAdapter.onSaveInstanceState(outState);
        }

    }
    @Override
    protected void onResume() {
        super.onResume();

        // always  check firstly
       // checkUiEnabled();

        // register observer to enable/disable the switch
        // for the case: other permssion manage apk is installed or uninstalled(Tecent)
      //  mSwitchContentObserver.register(getContentResolver());

        boolean isShow = isPermControlOn(this);
        LogUtil.d("onResume() , isShow " + isShow);

        if (isShow) {
            addUI();
        } else {
            removeUI();
        }
        mUserCheckedFlag = false;
        mSwitch.setChecked(isShow);
        mUserCheckedFlag = true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister observer
        //mSwitchContentObserver.unregister(getContentResolver());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Some of variables will be null if this Activity redirects Intent.
        // See also onCreate() or other methods called during the Activity's
        // initialization.
        if (mActionBarAdapter != null) {
            mActionBarAdapter.setListener(null);
        }
    }

    @Override
    public void onSelectedTabChanged() {
        updateFragmentsVisibility();

    }
    private void updateFragmentsVisibility() {
        TabState tab = mActionBarAdapter.getCurrentTab();
        int tabIndex = tab.ordinal();
        if (mTabPager.getCurrentItem() != tabIndex) {
            LogUtil.d(
                    "mTabPager.getCurrentItem() " + mTabPager.getCurrentItem()
                            + " tabIndex " + tabIndex);
            mTabPager.setCurrentItem(tabIndex);
        }

        invalidateOptionsMenu();

    }
    ///////////////////////////////////////////////////
    protected void addUI() {
        LogUtil.d("addUI()");
        // must get a new transaction each time
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        // set empty view to gone
        mEmptyView.setVisibility(View.GONE);
        // add all the fragment
        mPermissionsFragment = (ManagePermissionsFragment) getFragmentManager()
                .findFragmentByTag(mPermissionsTag);
        mAppsFragment = (AppsFragment) getFragmentManager().findFragmentByTag(
                mAppsTag);

        if (mPermissionsFragment == null) {
            mPermissionsFragment = new ManagePermissionsFragment();
            mAppsFragment = new AppsFragment();
            transaction.add(R.id.tab_pager, mPermissionsFragment, mPermissionsTag);
            transaction.add(R.id.tab_pager, mAppsFragment, mAppsTag);
        }
        transaction.show(mPermissionsFragment);
        transaction.show(mAppsFragment);

        transaction.commit();

        getFragmentManager().executePendingTransactions();
        // firstly remove tabs ,then add tabs and update it
        mActionBarAdapter.removeAllTab();
        mActionBarAdapter.addUpdateTab(mSavedInstanceState);
    }
    protected void removeUI() {
        LogUtil.d("removeUI()");
        // must get a new transaction each time
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        // set empty view to visible
        mEmptyView.setVisibility(View.VISIBLE);
        // remove all the fragment
        mPermissionsFragment = (ManagePermissionsFragment) getFragmentManager()
                .findFragmentByTag(mPermissionsTag);

        if (mPermissionsFragment != null) {
            transaction.hide(mPermissionsFragment);
            transaction.hide(mAppsFragment);
        }
        transaction.commit();
        getFragmentManager().executePendingTransactions();
        // remove tabs on actionbar
        mActionBarAdapter.removeAllTab();
    }
    //////////////////////////////////////////////////
    private boolean isPermControlOn(PermissionControlPageActivity permissionControlPageActivity) {
        return Settings.System.getInt(getContentResolver(),PERMISSION_CONTROL_STATE,0) > 0;
    }
    public static void enablePermissionControl(boolean state, Context context) {
        Settings.System.putInt(context.getContentResolver(),
                PERMISSION_CONTROL_STATE, state ? 1 : 0);
    }



    /////////////////////////////////////////////////////////////////  TabPagerAdapter
    private class TabPagerAdapter extends PagerAdapter{
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter() {
            LogUtil.i();
            mFragmentManager = getFragmentManager();
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public int getItemPosition(Object object) {
            LogUtil.i();
            if (object == mPermissionsFragment) {
                return PERMISSIONS_INFO;
            }

            if (object == mAppsFragment) {
                return APPS_INFO;
            }
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
             return ((Fragment) object).getView() == view;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LogUtil.i();
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            return f;
        }
        @Override
        public void destroyItem(View container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
        }
        @Override
        public void finishUpdate(View container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            LogUtil.i();
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }
        private Fragment getFragment(int position) {
            if (position == PERMISSIONS_INFO) {
                return mPermissionsFragment;
            } else if (position == APPS_INFO) {
                return mAppsFragment;
            }

            throw new IllegalArgumentException("position: " + position);
        }
    }
    //////////////////////////////////////////////////////TabPagerListener
    private class TabPagerListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Make sure not in the search mode, in which case position != TabState.ordinal().
            TabState selectedTab = TabState.fromInt(position);
            mActionBarAdapter.setCurrentTab(selectedTab, false);
            PermissionControlPageActivity.this.updateFragmentsVisibility();
        }
    }



}
