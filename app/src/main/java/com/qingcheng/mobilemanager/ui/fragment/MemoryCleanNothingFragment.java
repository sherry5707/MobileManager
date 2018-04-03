package com.qingcheng.mobilemanager.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.activity.AppUninstallActivity;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanActivity;

/**
 * Created by chunfengliu on 2/5/18.
 */

public class MemoryCleanNothingFragment extends Fragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.memory_clean_nothing, container, false);
        final TaskCleanActivity activity = (TaskCleanActivity) getActivity();
        TextView textView = (TextView) view.findViewById(R.id.tv_clean_raminfo);
        textView.setText(activity.getMemoryInfo());
        view.findViewById(R.id.tv_uninstall_app_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, AppUninstallActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }
}
