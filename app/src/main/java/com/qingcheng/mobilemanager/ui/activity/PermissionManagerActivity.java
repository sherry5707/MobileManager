
package com.qingcheng.mobilemanager.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.qingcheng.mobilemanager.ui.ViewPagerTabs;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.fragment.AppsFragment;
import com.qingcheng.mobilemanager.ui.fragment.ManagePermissionsFragment;
import com.qingcheng.mobilemanager.utils.UiUtils;


public class PermissionManagerActivity extends AppCompatActivity implements
    ViewPager.OnPageChangeListener {

    private static final String TAG = "PerMgrActivity";

    private static final int TAB_INDEX_APP_LIST = 0;
    private static final int TAB_INDEX_PERMISSION_LIST = TAB_INDEX_APP_LIST + 1;
    private static final int TAB_INDEX_COUNT = TAB_INDEX_PERMISSION_LIST + 1;

    private ViewPager mViewPager;
    private ViewPagerTabs mViewPagerTabs;
    private ViewPagerAdapter mViewPagerAdapter;
    private CharSequence[] mTabTitles;


    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == TAB_INDEX_APP_LIST) {
                return new AppsFragment();
            } else if (position == TAB_INDEX_PERMISSION_LIST) {
                return new ManagePermissionsFragment();
            }

            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container, position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.permission_manager_activity);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        UiUtils.setScreen(PermissionManagerActivity.this);

        /*final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setElevation(0);*/

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        int startingTab = TAB_INDEX_APP_LIST;

        mTabTitles = new CharSequence[TAB_INDEX_COUNT];
        mTabTitles[0] = getString(R.string.apps);
        mTabTitles[1] = getString(R.string.permissions_label);

        mViewPager = (ViewPager) findViewById(R.id.per_mgr_pager);

        mViewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(this);
        mViewPagerTabs = (ViewPagerTabs) findViewById(R.id.viewpager_header);
        mViewPagerTabs.setViewPager(mViewPager);

        mViewPager.setCurrentItem(startingTab);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mViewPagerTabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        mViewPagerTabs.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mViewPagerTabs.onPageScrollStateChanged(state);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
