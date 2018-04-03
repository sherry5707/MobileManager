package com.qingcheng.mobilemanager.ui.activity;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.ViewPagerTabs;
import com.qingcheng.mobilemanager.ui.fragment.UninstallFragment;
import com.qingcheng.mobilemanager.ui.fragment.WhiteListFragment;
import com.qingcheng.mobilemanager.utils.UiUtils;


public class NewAppManagerActivity extends FragmentActivity implements
        ViewPager.OnPageChangeListener {

    private static final int TAB_INDEX_APP_UNINSTLL = 0;
    private static final int TAB_INDEX_WHITE_LIST = TAB_INDEX_APP_UNINSTLL + 1;
    private static final int TAB_INDEX_COUNT = TAB_INDEX_WHITE_LIST + 1;

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
            if (position == TAB_INDEX_APP_UNINSTLL) {
                return new UninstallFragment();
            } else if (position == TAB_INDEX_WHITE_LIST) {
                return WhiteListFragment.newInstance(false);
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);
        UiUtils.setScreen(NewAppManagerActivity.this);
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mTabTitles = new CharSequence[TAB_INDEX_COUNT];
        mTabTitles[0] = getString(R.string.uninstall);
        mTabTitles[1] = getString(R.string.whitelist);

        mViewPager = (ViewPager) findViewById(R.id.per_mgr_pager);

        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(this);
        mViewPagerTabs = (ViewPagerTabs) findViewById(R.id.viewpager_header);
        mViewPagerTabs.setViewPager(mViewPager);

        mViewPager.setCurrentItem(TAB_INDEX_APP_UNINSTLL);
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
}
