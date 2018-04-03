package com.qingcheng.mobilemanager.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.qingcheng.mobilemanager.utils.PrefUtils;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.widget.TextLinear;
import com.qingcheng.mobilemanager.utils.CleanUtils;
import android.content.Intent;
import android.provider.Settings;

/**
 * 主页清理界面的Fragment
 */
public class CleanFragment extends BaseFragment {


    private static LinearLayout llAdd;
    private View view;

    public CleanFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_clean, container, false);
        initView();
        return view;
    }
    @Override
    public void initView() {
        llAdd = (LinearLayout) view.findViewById(R.id.ll_add);
        llAdd.removeAllViews();
    }
    @Override
    public void initData() {


    }

    public static void pretView(final Context context){
        llAdd.removeAllViews();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                	int oldCount = CleanUtils.getRunningProcess().size();
			CleanUtils.cleanMemory();
			int newCount = CleanUtils.getRunningProcess().size();
			PrefUtils.getInt(CleanUtils.getContext(),"system_process_count", newCount);
                    LinearLayout llView1 = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.item_textview,null);
                    TextLinear tlPersion = (TextLinear) llView1.findViewById(R.id.tlPersion);
					tlPersion.setTextSize(15,15);
                    tlPersion.setText("       " + context.getResources().getString(R.string.close_process),
						context.getResources().getString(
                            R.string.process_count, oldCount-newCount)+"    ");
                    llAdd.addView(llView1);
                }
            },350);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                if(!CleanUtils.isPowerSaveMode()) {
				Settings.Global.putInt(context.getContentResolver(), "qc_key_powermode", 1);
				Intent intent = new Intent("com.android.settings.POWER_SAVING_MODE_SERVICE");
				intent.putExtra("PowerSavingStatus", true);					
				context.sendBroadcast(intent);							
			}
                    LinearLayout llView2 = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.item_textview,null);
                    TextLinear tlPersion = (TextLinear) llView2.findViewById(R.id.tlPersion);
					tlPersion.setTextSize(15,15);
                    tlPersion.setText("       " + context.getResources().getString(R.string.power_save),
						context.getResources().getString(R.string.enabled)+"    ");
                    llAdd.addView(llView2);
                }
            },700);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                	if(!CleanUtils.isPermissionOpened()) {
				Settings.System.putInt(context.getContentResolver(),"permission_control_state", 1);
			}
                    LinearLayout llView3 = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.item_textview,null);
                    TextLinear tlPersion = (TextLinear) llView3.findViewById(R.id.tlPersion);
					tlPersion.setTextSize(15,15);
                    tlPersion.setText("       " + context.getResources().getString(R.string.permission_ctr),
						context.getResources().getString(R.string.enabled)+"    ");
                    llAdd.addView(llView3);
                }
            },1050);

    }
}
