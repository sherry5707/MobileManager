/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qingcheng.mobilemanager.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qingcheng.mobilemanager.R;

/**
 * Lightweight implementation of ViewPager tabs. This looks similar to traditional actionBar tabs,
 * but allows for the view containing the tabs to be placed anywhere on screen. Text-related
 * attributes can also be assigned in XML - these will get propogated to the child TextViews
 * automatically.
 */
public class ViewPagerTabs extends HorizontalScrollView implements ViewPager.OnPageChangeListener {

    ViewPager mPager;
    private ViewPagerTabStrip mTabStrip;

    /**
     * Linearlayout that will contain the TextViews serving as tabs. This is the only child
     * of the parent HorizontalScrollView.
     */
    final int mTextStyle;
    final ColorStateList mTextColor;
    final int mTextSize;
    final boolean mTextAllCaps;
    int mPrevSelected = -1;
    int mSidePadding;

    // For displaying the unread count next to the tab icon.
    private int[] mUnreadCounts;

    private static final int TAB_SIDE_PADDING_IN_DPS = 2;

    // TODO: This should use <declare-styleable> in the future
    private static final int[] ATTRS = new int[] {
        android.R.attr.textSize,
        android.R.attr.textStyle,
        android.R.attr.textColor,
        android.R.attr.textAllCaps
    };

    // RGKMyUI
    private int mTabStripWidth;
    // RGKMyUI End

    /**
     * Simulates actionbar tab behavior by showing a toast with the tab title when long clicked.
     */
    private class OnTabLongClickListener implements OnLongClickListener {
        final int mPosition;

        public OnTabLongClickListener(int position) {
            mPosition = position;
        }

        @Override
        public boolean onLongClick(View v) {
            final int[] screenPos = new int[2];
            getLocationOnScreen(screenPos);

            final Context context = getContext();
            final int width = getWidth();
            final int height = getHeight();
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

            Toast toast = Toast.makeText(context, mPager.getAdapter().getPageTitle(mPosition),
                    Toast.LENGTH_SHORT);

            // Show the toast under the tab
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    (screenPos[0] + width / 2) - screenWidth / 2, screenPos[1] + height);

            toast.show();
            return true;
        }
    }

    public ViewPagerTabs(Context context) {
        this(context, null);
    }

    public ViewPagerTabs(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerTabs(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFillViewport(true);

        // RGKMyUI
        mTabStripWidth = getResources().getDimensionPixelOffset(R.dimen.view_pager_tab_width);
        // RGKMyUI End
        mSidePadding = (int) (getResources().getDisplayMetrics().density * TAB_SIDE_PADDING_IN_DPS);

        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        mTextSize = a.getDimensionPixelSize(0, 0);
        mTextStyle = a.getInt(1, 0);
        mTextColor = a.getColorStateList(2);
        mTextAllCaps = a.getBoolean(3, false);

        mTabStrip = new ViewPagerTabStrip(context);
        addView(mTabStrip,
                new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        a.recycle();
    }

    public void setViewPager(ViewPager viewPager) {
        mPager = viewPager;
        addTabs(mPager.getAdapter(), mPager.getCurrentItem()); // RGKMyUI
    }

    private void addTabs(PagerAdapter adapter, int currentIndex) { // RGKMyUI
        mTabStrip.removeAllViews();

        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            addTab(adapter.getPageTitle(i), i, currentIndex); // RGKMyUI
        }
    }

    private void addTab(CharSequence tabTitle, final int position, final int currentIndex) { // RGKMyUI
        View tabView;

        final TextView textView = new TextView(getContext());
        textView.setText(tabTitle);
        //textView.setBackgroundResource(R.drawable.rgk_view_pager_tab_background); // RGKMyUI

        // Assign various text appearance related attributes to child views.
        if (mTextStyle > 0) {
            textView.setTypeface(textView.getTypeface(), mTextStyle);
        }
        if (mTextSize > 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        }
        if (mTextColor != null) {
            textView.setTextColor(mTextColor);
        }
        textView.setAllCaps(mTextAllCaps);
        textView.setGravity(Gravity.CENTER);

        tabView = textView;

        tabView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(getRtlPosition(position));
            }
        });

        tabView.setOnLongClickListener(new OnTabLongClickListener(position));

        tabView.setPadding(mSidePadding, 0, mSidePadding, 0);

        mTabStrip.addView(tabView, position, new LinearLayout.LayoutParams(
                mTabStripWidth /* RGKMyUI LayoutParams.WRAP_CONTENT */, LayoutParams.MATCH_PARENT, 1));

        // Default to the first child being selected
        if (position == currentIndex) { // RGKMyUI
            mPrevSelected = currentIndex; // RGKMyUI
            tabView.setSelected(true);
        }
    }

    /**
     * Remove a tab at a certain index.
     *
     * @param index The index of the tab view we wish to remove.
     */
    public void removeTab(int index) {
        View view = mTabStrip.getChildAt(index);
        if (view != null) {
            mTabStrip.removeView(view);
        }
    }

    /**
     * Refresh a tab at a certain index by removing it and reconstructing it.
     *
     * @param index The index of the tab view we wish to update.
     */
    public void updateTab(int index) {
        removeTab(index);

        if (index < mPager.getAdapter().getCount()) {
            addTab(mPager.getAdapter().getPageTitle(index), index, mPager.getCurrentItem()); // RGKMyUI
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        position = getRtlPosition(position);
        int tabStripChildCount = mTabStrip.getChildCount();
        if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
            return;
        }

        mTabStrip.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        position = getRtlPosition(position);
        int tabStripChildCount = mTabStrip.getChildCount();
        if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
            return;
        }

        if (mPrevSelected >= 0 && mPrevSelected < tabStripChildCount) {
            mTabStrip.getChildAt(mPrevSelected).setSelected(false);
        }
        final View selectedChild = mTabStrip.getChildAt(position);
        selectedChild.setSelected(true);

        // Update scroll position
        final int scrollPos = selectedChild.getLeft() - (getWidth() - selectedChild.getWidth()) / 2;
        smoothScrollTo(scrollPos, 0);
        mPrevSelected = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private int getRtlPosition(int position) {
        if (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            return mTabStrip.getChildCount() - 1 - position;
        }
        return position;
    }
}

