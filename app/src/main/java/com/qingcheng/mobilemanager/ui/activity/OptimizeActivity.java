package com.qingcheng.mobilemanager.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.global.GlobalConstant;
import com.qingcheng.mobilemanager.ui.SearchDevicesView;
import com.qingcheng.mobilemanager.ui.tickview.TickView;
import com.qingcheng.mobilemanager.utils.CleanUtils;
import com.qingcheng.mobilemanager.utils.PrefUtils;
import com.qingcheng.mobilemanager.utils.UiUtils;
import com.qingcheng.mobilemanager.widget.RiseNumberTextView;
import com.qingcheng.mobilemanager.widget.TextLinear;

/**
 * Created by root on 18-1-26.
 */

public class OptimizeActivity extends AppCompatActivity {
    private ImageButton mBackButton;
    private SearchDevicesView mSearchView;
    private RippleIntroView mRippleView;
    private RiseNumberTextView mScoretextView;
    private RelativeLayout mOverlayout;
    private LinearLayout llAdd;
    private boolean isOver = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optimizeactivity);

        UiUtils.setScreen(OptimizeActivity.this);

        llAdd = (LinearLayout)findViewById(R.id.ll_add);

        mOverlayout = (RelativeLayout) findViewById(R.id.overlayout);
        mOverlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOver){
                    setResult(3);
                }
                finish();
            }
        });

        mRippleView = (RippleIntroView)findViewById(R.id.rippleintroview);
        //mRippleView.cancel();


        mSearchView = (SearchDevicesView)findViewById(R.id.searchview);
        mSearchView.setWillNotDraw(false);
        mSearchView.startsearch();

        mScoretextView = (RiseNumberTextView)findViewById(R.id.score);
        //mScoretextView.setText("60");
        mScoretextView.setText(getIntent().getStringExtra("score"));

        mBackButton = (ImageButton)findViewById(R.id.back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOver){
                    setResult(3);
                }
                finish();
            }
        });

        startClean(this);

        mScoretextView.setRiseInterval(GlobalConstant.LAST_NUMBER,100)
                .setDuration(2350)
                .runInt(true)
                .start();
    }

    private void startClean(final Context context){
        llAdd.removeAllViews();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int oldCount = CleanUtils.getRunningProcess().size();
                CleanUtils.cleanMemory();
                int newCount = CleanUtils.getRunningProcess().size();
                PrefUtils.getInt(CleanUtils.getContext(),"system_process_count", newCount);

                RelativeLayout llView1 = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.scanresult_item,null);
                TextView tlPersion = (TextView) llView1.findViewById(R.id.title);
                tlPersion.setText(context.getResources().getString(R.string.close_process));
                TextView result = (TextView) llView1.findViewById(R.id.result);
                result.setText(context.getResources().getString(
                        R.string.process_count, oldCount-newCount));
                TickView tickView = (TickView) llView1.findViewById(R.id.tickview);
                tickView.setChecked(true);
                llAdd.addView(llView1);

            }
        },550);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!CleanUtils.isPowerSaveMode()) {
                    Settings.Global.putInt(context.getContentResolver(), "qc_key_powermode", 1);
                    Intent intent = new Intent("com.android.settings.POWER_SAVING_MODE_SERVICE");
                    intent.putExtra("PowerSavingStatus", true);
                    context.sendBroadcast(intent);
                }
                RelativeLayout llView2 = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.scanresult_item,null);
                TextView tlPersion = (TextView) llView2.findViewById(R.id.title);
                tlPersion.setText(context.getResources().getString(R.string.power_save));
                TextView result = (TextView) llView2.findViewById(R.id.result);
                result.setText(context.getResources().getString(R.string.enabled));
                TickView tickView = (TickView) llView2.findViewById(R.id.tickview);
                llAdd.addView(llView2);
                tickView.setChecked(true);
            }
        },1600);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!CleanUtils.isPermissionOpened()) {
                    Settings.System.putInt(context.getContentResolver(),"permission_control_state", 1);
                }

                RelativeLayout llView3 = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.scanresult_item,null);
                TextView tlPersion = (TextView) llView3.findViewById(R.id.title);
                tlPersion.setText(context.getResources().getString(R.string.permission_ctr));
                TextView result = (TextView) llView3.findViewById(R.id.result);
                result.setText(context.getResources().getString(R.string.enabled));
                TickView tickView = (TickView) llView3.findViewById(R.id.tickview);
                llAdd.addView(llView3);
                tickView.setChecked(true);

            }
        },2750);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSearchView.setVisibility(View.GONE);
                mRippleView.setValue(220,30);
                mOverlayout.setVisibility(View.VISIBLE);
                isOver = true;
            }
        },3250);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(isOver){
            setResult(3);
        }
    }
}
