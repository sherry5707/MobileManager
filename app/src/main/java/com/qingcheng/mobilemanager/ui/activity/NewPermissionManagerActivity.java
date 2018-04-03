package com.qingcheng.mobilemanager.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.fragment.AutostartFragment;
import com.qingcheng.mobilemanager.ui.fragment.NewPermissionManagerFragment;
import com.qingcheng.mobilemanager.utils.UiUtils;

/**
 * Created by chunfengliu on 3/12/18.
 */

public class NewPermissionManagerActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_permission_manager);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
        getSupportFragmentManager().beginTransaction().add(R.id.fl_container, new NewPermissionManagerFragment()).commit();
        UiUtils.setScreen(this);
        getWindow().setStatusBarColor(getColor(R.color.blue_normal));
    }

    public void startWhiteList() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_container, new AutostartFragment()).addToBackStack(null).commit();
    }

    public void startPermissionManage() {
        Intent intent = new Intent(this, PermissionManagerActivity.class);
        startActivity(intent);
    }

    public void setTitle(int res) {
        toolbar.setTitle(res);
    }
}
