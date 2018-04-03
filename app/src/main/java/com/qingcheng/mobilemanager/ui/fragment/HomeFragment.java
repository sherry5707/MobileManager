package com.qingcheng.mobilemanager.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qingcheng.mobilemanager.Listeners.CleanFragmentTouchListener;
import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.global.GlobalConstant;
import com.qingcheng.mobilemanager.ui.activity.AppManagerActivity;
import com.qingcheng.mobilemanager.ui.activity.PermissionManagerActivity;
import com.qingcheng.mobilemanager.ui.activity.TaskCleanActivity;
import com.qingcheng.mobilemanager.ui.activity.HomeActivity;
import com.qingcheng.mobilemanager.ui.activity.PermissionControlPageActivity;
import com.qingcheng.mobilemanager.ui.activity.SaveEleActivity;
import com.qingcheng.mobilemanager.utils.EventUtil;
import com.qingcheng.mobilemanager.utils.PrefUtils;
import com.qingcheng.mobilemanager.utils.RotatingUtil;
import com.qingcheng.mobilemanager.utils.CleanUtils;
import com.qingcheng.mobilemanager.widget.RiseNumberTextView;

import java.io.Serializable;

/**
 * 主页fragment
 */
public class HomeFragment extends BaseFragment {
    private Handler mHandler;
   private final int DEF_SYSTEM_PROCESS_COUNT = 8;
    private Handler fragHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            EventUtil event = (EventUtil)data.getSerializable(GlobalConstant.KEY);
            int intMsg = event.getIntMsg();
            switch (intMsg){
                case GlobalConstant.FIRST_OPEN_INT:

                            firstOpenEvent();
                    break;
                case GlobalConstant.CHECK_IS_END:
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkEnd();
                        }
                    });
                    break;
            }
        }
    };

    private CleanFragmentTouchListener touchListener;
    private View shortCircular;
    private View view;
    private TextView tvOpt;
    private View longCircular;
    private RiseNumberTextView tvHomeCount;
    private RelativeLayout rlTransDes;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
	PrefUtils.getInt(CleanUtils.getContext(),"system_process_count", DEF_SYSTEM_PROCESS_COUNT);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);
        initView();
        return view;
    }

    @Override
    public void initView() {

        shortCircular = view.findViewById(R.id.short_circular);
        longCircular = view.findViewById(R.id.long_circular);
        tvOpt = (TextView) view.findViewById(R.id.tv_opt);
        tvHomeCount = (RiseNumberTextView) view.findViewById(R.id.tv_home_count);
        rlTransDes = (RelativeLayout) view.findViewById(R.id.rl_trans_des);
        tvOpt.setText(R.string.on_examination);
        Animation rotatingSelf = RotatingUtil.RotatingSelf(mActivity);
        if (rotatingSelf != null) {
            shortCircular.setAnimation(rotatingSelf);
        }
        rlTransDes.setClickable(false);
        checkOpened();

    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        final RelativeLayout rlFragmentHomeOpt = (RelativeLayout) view.findViewById(R.id.rl_fragment_home_opt);
        final RelativeLayout rlTransDes = (RelativeLayout) view.findViewById(R.id.rl_trans_des);
        rlTransDes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                touchListener.onCleanFragmentTouch(getView(), rlFragmentHomeOpt, true);
                CleanFragment.pretView(mActivity);
            }
        });
        final RelativeLayout homeRam = (RelativeLayout) view.findViewById(R.id.home_ram);
        homeRam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, TaskCleanActivity.class);
                startActivity(intent);
            }
        });
        final RelativeLayout homeSave = (RelativeLayout) view.findViewById(R.id.home_save);
        homeSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent().setClassName("com.greenorange.qcpower",
                    "com.greenorange.qcpower.MainActivity");
				intent.putExtra("from_mobile_manager", true);
                startActivity(intent);
            }
        });
        final RelativeLayout homeTouch = (RelativeLayout) view.findViewById(R.id.home_touch);
        homeTouch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(mActivity, AppManagerActivity.class);
                //startActivity(intent);
				Intent intent = new Intent().setClassName("com.rgk.fpfeature",
                    "com.rgk.fpfeature.FPRecognizeActivity");
				intent.putExtra("main_item_id",3);
                startActivity(intent);
            }
        });
        final RelativeLayout homeRights = (RelativeLayout) view.findViewById(R.id.home_rights);
        homeRights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, AppManagerActivity.class);
                startActivity(intent);
            }
        });
        final RelativeLayout duraSpeed = (RelativeLayout) view.findViewById(R.id.dura_speed);
        duraSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent().setClassName("com.mediatek.duraspeed",
//                    "com.mediatek.duraspeed.view.RunningBoosterMainActivity");
                Intent intent = new Intent(mActivity, PermissionManagerActivity.class);
                startActivity(intent);
            }
        });
        final RelativeLayout freezeApp = (RelativeLayout) view.findViewById(R.id.freeze_app);
        freezeApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent().setClassName("com.rgk.fpfeature",
                    "com.rgk.fpfeature.FPRecognizeActivity");
				intent.putExtra("main_item_id",2);
                startActivity(intent);
            }
        });

    }

    @Override
    public void initData() {

    }


    public void setCleanFragmentTouchListener(CleanFragmentTouchListener touchListener) {
        this.touchListener = touchListener;
    }

    /**
     * 第一次打开时应该做的事情
     */
    public void firstOpenEvent() {
        //圈圈开始转
        //显示检测中
        Log.e("-----HomeFragment----", "开始执行firstOpenEvent方法");
        new Thread(){
            @Override
            public void run() {

                long startTime = System.currentTimeMillis();

                //检测智能省电是否打开
                //检测权限管理是否开启
                //检测后台进程打开了多少个
                //数字开始增长至相应分数
                GlobalConstant.LAST_NUMBER = 0;
                //记录时间

		try{
			if(CleanUtils.isPowerSaveMode()) {
				GlobalConstant.LAST_NUMBER += 33;
			}
			int processSize = CleanUtils.getRunningProcess().size();
			int systemCount = PrefUtils.getInt(CleanUtils.getContext(),"system_process_count", DEF_SYSTEM_PROCESS_COUNT);
			if((processSize-systemCount) >33) {
				GlobalConstant.LAST_NUMBER += 0;
			}  else  {
				GlobalConstant.LAST_NUMBER += (33 - processSize+systemCount);
			}
			/*
			int avaiMemoryRate = (int)(CleanUtils.getAvailMemory()*100/CleanUtils.getTotalMemory());
			if(avaiMemoryRate > 80) {
				GlobalConstant.LAST_NUMBER += 25;
			} else if(avaiMemoryRate > 70) {
				GlobalConstant.LAST_NUMBER += 15;
			} else if(avaiMemoryRate > 60) {
				GlobalConstant.LAST_NUMBER += 5;
			} else {
				GlobalConstant.LAST_NUMBER += 0;
			}*/
			if(CleanUtils.isPermissionOpened()) {
				GlobalConstant.LAST_NUMBER += 34;
			} 	
		} catch(Exception e){
			
		}
                long endTime = System.currentTimeMillis();
                final long useTime = endTime - startTime;

                if (useTime < GlobalConstant.MIN_CHECK_TIME) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvHomeCount.setRiseInterval(GlobalConstant.FIRST_NUMBER,GlobalConstant.LAST_NUMBER)
                                    .setDuration((int)(GlobalConstant.MIN_CHECK_TIME - useTime))
                                    .runInt(true)
                                    .start();
                        }
                    });
                    SystemClock.sleep(GlobalConstant.MIN_CHECK_TIME - useTime);
                } else if (useTime > GlobalConstant.MAX_CHECK_TIME) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvHomeCount.setRiseInterval(GlobalConstant.FIRST_NUMBER,GlobalConstant.LAST_NUMBER)
                                    .setDuration((int) GlobalConstant.MAX_CHECK_TIME)
                                    .runInt(true)
                                    .start();
                        }
                    });
                    SystemClock.sleep(GlobalConstant.MAX_CHECK_TIME);
                }
                Bundle bundle = new Bundle();
                bundle.putSerializable(GlobalConstant.KEY,new EventUtil(GlobalConstant.CHECK_IS_END));

                Message message = new Message();
                message.setData(bundle);
                fragHandler.sendMessage(message);

                Message mMessage = new Message();
                mMessage.setData(bundle);
		if(mHandler != null) {
                mHandler.sendMessage(mMessage);
		} 
            }
        }.start();

    }

    /**
     * 检查完毕做的事情
     */
    public void checkEnd(){
        tvOpt.setText(R.string.click_optimization);
        longCircular.setVisibility(View.VISIBLE);
        //停止动画
        shortCircular.clearAnimation();
        //可以点击
        rlTransDes.setClickable(true);
    }

    /**
     * 检测是否打开过App
     */
    public void checkOpened(){
        PrefUtils.putBoolean(mActivity,GlobalConstant.FIRST_OPEN_STRING,false);
        boolean isOpened = PrefUtils.getBoolean(mActivity, GlobalConstant.FIRST_OPEN_STRING, false);
        Log.e("------HomeFragment-----","isOpened : "+ isOpened);
        if (!isOpened){

            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putSerializable(GlobalConstant.KEY,new EventUtil(GlobalConstant.FIRST_OPEN_INT));
            message.setData(bundle);
            fragHandler.sendMessage(message);

            Log.e("-----HomeFragment------","发送了 "+GlobalConstant.FIRST_OPEN_INT+" 消息");
            //标记已打开过App
            PrefUtils.putBoolean(mActivity,GlobalConstant.FIRST_OPEN_STRING,true);
        }
    }

}


