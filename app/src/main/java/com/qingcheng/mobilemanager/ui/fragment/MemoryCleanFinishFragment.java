package com.qingcheng.mobilemanager.ui.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.ui.activity.AppUninstallActivity;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanActivity;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by chunfengliu on 2/5/18.
 */

public class MemoryCleanFinishFragment extends Fragment {

    private static final String TAG = "MemoryCleanFinish";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final TaskCleanActivity activity = (TaskCleanActivity)getActivity();
        View view = inflater.inflate(R.layout.memory_clean_finish, container, false);
        TextView textViewSpeedBoost = (TextView) view.findViewById(R.id.tv_speed_boost);
        textViewSpeedBoost.setText("" + activity.getSpeed());
        TextView textViewClearRubbish = (TextView) view.findViewById(R.id.clear_rubbish);
        textViewClearRubbish.setText(getString(R.string.clear_rubbish, activity.getToCleanMemoryMB()));
        TextView textView = (TextView) view.findViewById(R.id.speed_useinfo);
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
