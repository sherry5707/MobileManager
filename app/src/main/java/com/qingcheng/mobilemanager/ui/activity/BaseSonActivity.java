package com.qingcheng.mobilemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.qingcheng.mobilemanager.R;

/**
 * Description:
 * Copyright  : Copyright (c) 2016
 * Company    : RGK
 * Author     : qi.guan
 * Date       : 2016/11/23 18:55
 */

public abstract class BaseSonActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_left_in, R.anim.from_right_out);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.from_right_in, R.anim.from_left_out);
    }
    @Override
    public void onBackPressed() {
        /*Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);*/
        this.finish();
    }

    /**
     * 初始化布局
     */
    public abstract void initView();

    /**
     *初始化返回布局
     */
    public abstract void preBack();

    /**
     * 初始化数据
     */
    public abstract void initData();
}
