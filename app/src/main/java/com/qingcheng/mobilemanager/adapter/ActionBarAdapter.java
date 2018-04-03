package com.qingcheng.mobilemanager.adapter;



import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;

import com.qingcheng.mobilemanager.utils.LogUtil;
import com.qingcheng.mobilemanager.R;
/**
 * Created by cunhuan.liu on 17/2/9.
 */
public class ActionBarAdapter {


    private final Context mContext;
    private final ActionBar mActionBar;

    private static final String EXTRA_KEY_SELECTED_TAB = "navBar.selectedTab";
    private static final TabState DEFAULT_TAB = TabState.PERMISSIONS_INFO;
    private TabState mCurrentTab = DEFAULT_TAB;

    private Listener mListener;
    private final MyTabListener mTabListener = new MyTabListener();
    public interface Listener {
        void onSelectedTabChanged();
    }


    public ActionBarAdapter(Context context, Listener listener,
                            ActionBar actionBar) {
        mContext = context;
        mListener = listener;
        mActionBar = actionBar;
    }

    public void addUpdateTab(Bundle savedState) {
        // Set up tabs
        addTab(TabState.PERMISSIONS_INFO, R.string.permissions_label);
        addTab(TabState.APPS_INFO, R.string.apps);
        LogUtil.d("initialize() .....");
        if (savedState != null) {
            mCurrentTab = TabState.fromInt(savedState
                    .getInt(EXTRA_KEY_SELECTED_TAB));
            LogUtil.d("get saved tab  " + mCurrentTab);
        }
        update();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }
    private void addTab(TabState tabState, int description) {
        final ActionBar.Tab tab = mActionBar.newTab();
        tab.setTag(tabState);
        tab.setTabListener(mTabListener);
        tab.setText(description);
        mActionBar.addTab(tab);
    }

    public void removeAllTab() {
        mActionBar.removeAllTabs();
        // must set the navigation mode to standard ,
        // or will show the tab background
        mTabListener.mIgnoreTabSelected = true;
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mTabListener.mIgnoreTabSelected = false;
    }

    public TabState getCurrentTab() {
        return mCurrentTab;
    }
 /*
     * change the current tab
     * @param tab TabState, tab will be set to the current
     * @param notifyListener boolean , if true ,
     * will initiative update fragment visibility
     * */

    public void setCurrentTab(TabState tab, boolean notifyListener) {
        if (tab == null) {
            throw new NullPointerException();
        }
        if (tab == mCurrentTab) {
            LogUtil.d("tab == mCurrentTab ,directly return " + tab);
            return;
        }
        mCurrentTab = tab;
        LogUtil.d( "mCurrentTab = " + mCurrentTab + " notifyListner "
                + notifyListener);
        int index = mCurrentTab.ordinal();
        if ((mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS)
                && (index != mActionBar.getSelectedNavigationIndex())) {
            mActionBar.setSelectedNavigationItem(index);
        }

        if (notifyListener && mListener != null) {
            mListener.onSelectedTabChanged();
        }
    }



    private void update() {
        if (mActionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_TABS) {
          LogUtil.d("update() " + mCurrentTab.ordinal() + mCurrentTab);

            /* setNavigationMode will trigger onTabSelected() with the tab which
             was previously  selected.
             The issue is that when we're first switching to the tab
             navigation mode after
             screen orientation changes, onTabSelected() will get called with
             the first tab
             (i.e. "Apps"), which would results in mCurrentTab getting set to
             "Apps" and  we'd lose restored tab.
             So let's just disable the callback here temporarily. We'll notify
             the listener after this anyway.
             * */
            mTabListener.mIgnoreTabSelected = true;
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mActionBar.setSelectedNavigationItem(mCurrentTab.ordinal());
            mTabListener.mIgnoreTabSelected = false;
        }

    }
    public void onSaveInstanceState(Bundle outState) {
        LogUtil.d( "onSaveInstanceState() " + mCurrentTab.ordinal());
        outState.putInt(EXTRA_KEY_SELECTED_TAB, mCurrentTab.ordinal());
    }

    ////////////////////////////////////////////////////////////////////
    public enum TabState {
        PERMISSIONS_INFO, APPS_INFO;

        public static TabState fromInt(int value) {

            if (PERMISSIONS_INFO.ordinal() == value) {
                return PERMISSIONS_INFO;
            }
            if (APPS_INFO.ordinal() == value) {
                return APPS_INFO;
            }
            throw new IllegalArgumentException("Invalid value: " + value);
        }
    }

    //////////////////////////////////////
    private class MyTabListener implements ActionBar.TabListener {
        /**
         * If true, it won't call {@link #setCurrentTab} in
         * {@link #onTabSelected}. This flag is used when we want to
         * programmatically update the current tab without
         * {@link #onTabSelected} getting called.
         */
        public boolean mIgnoreTabSelected;

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            LogUtil.d("onTabSelected() " + " ignore ? " + mIgnoreTabSelected);
            if (!mIgnoreTabSelected) {
                LogUtil.d( "setCurrentTab()");
                setCurrentTab((TabState) tab.getTag(), true);
            }
        }

    }


}
