package com.qingcheng.mobilemanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.activity.NewPermissionManagerActivity;

/**
 * Created by chunfengliu on 3/12/18.
 */

public class NewPermissionManagerFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission_manager, container, false);
        view.findViewById(R.id.ll_auto_launch).setOnClickListener(this);
        view.findViewById(R.id.ll_app_permission).setOnClickListener(this);
        ((NewPermissionManagerActivity) getActivity()).setTitle(R.string.give_permissions);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_auto_launch:
                ((NewPermissionManagerActivity) getActivity()).startWhiteList();
                break;
            case R.id.ll_app_permission:
                ((NewPermissionManagerActivity) getActivity()).startPermissionManage();
                break;
        }
    }
}
