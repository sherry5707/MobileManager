package com.qingcheng.mobilemanager.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qingcheng.mobilemanager.R;
import com.qingcheng.mobilemanager.global.GlobalConstant;
import com.qingcheng.mobilemanager.utils.CleanUtils;
import com.qingcheng.mobilemanager.utils.EventUtil;
import com.qingcheng.mobilemanager.utils.PrefUtils;
import com.qingcheng.mobilemanager.utils.UiUtils;
import com.qingcheng.mobilemanager.widget.RiseNumberTextView;

/**
 * Created by root on 18-1-25.
 */

public class NewHomeActivity extends AppCompatActivity{
    private final int DEF_SYSTEM_PROCESS_COUNT = 8;
    private RiseNumberTextView mScoretextView;
    private RippleIntroView mRippleIntroView;
    private RelativeLayout mOptimizationLayout;
    private ImageButton mSettingButton;
    private GridView mGrid,mMiddleGrid;
    private int[] mFucntionDrawablearray = new int[]{R.drawable.ic_power_save_normal,R.drawable.ic_freeze_app_normal,R.drawable.ic_app_lock_normal
            ,R.drawable.ic_mem_clean_normal,R.drawable.ic_permission_manager_normal,R.drawable.ic_app_manager_normal};
    private int[] mFunctionStringArray = new int[]{R.string.power_save,R.string.freeze_app,R.string.app_lock,R.string.clean_ram,R.string.give_permissions,R.string.app_manager};
    private int[] mMiddleFunctionStringArray = new int[]{R.string.weight_loss,R.string.game_acceleration,R.string.network_scanning,
            R.string.harassment_interception, R.string.red_packet_assistant,R.string.parallelSpace};
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newhomeactivity);
        mScoretextView = (RiseNumberTextView)findViewById(R.id.score);

        UiUtils.setScreen(NewHomeActivity.this);
        mGrid = (GridView)findViewById(R.id.topgridview);
        mGrid.setAdapter(new GridviewAdapter(this,1));
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent;
                switch (position){
                    case 0:
                        intent = new Intent().setClassName("com.greenorange.qcpower",
                                "com.greenorange.qcpower.MainActivity");
                        intent.putExtra("from_mobile_manager", true);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent().setClassName("com.rgk.fpfeature",
                                "com.rgk.fpfeature.view.FreezeAppActivity");
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent().setClassName("com.rgk.fpfeature",
                                "com.rgk.fpfeature.view.AppLockActivity");
                        startActivity(intent);
                        break;
                    case 3:
                        intent = new Intent(NewHomeActivity.this, TaskCleanActivity.class);
                        startActivity(intent);
                        break;
                    case 4:
                        intent = new Intent(NewHomeActivity.this, NewPermissionManagerActivity.class);
                        startActivity(intent);
                        break;
                    case 5:
                        intent = new Intent(NewHomeActivity.this, NewAppManagerActivity.class);
                        startActivity(intent);
                        break;
                }
            }
        });

        mMiddleGrid = (GridView)findViewById(R.id.middlegridview);
        mMiddleGrid.setAdapter(new GridviewAdapter(this ,2));
        mMiddleGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position) {
                    case 0:
                        intent = new Intent(NewHomeActivity.this, PhoneThinActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                }
            }
        });

        mSettingButton = (ImageButton)findViewById(R.id.setting);
        mSettingButton.setVisibility(View.INVISIBLE);
        mSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startActivity(new Intent(NewHomeActivity.this,HomeActivity.class));
            }
        });

        mRippleIntroView = (RippleIntroView)findViewById(R.id.rippleintroview);
        mOptimizationLayout = (RelativeLayout) findViewById(R.id.optimization);
        mOptimizationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NewHomeActivity.this,OptimizeActivity.class);
                intent.putExtra("score",mScoretextView.getText().toString());
                startActivityForResult(intent,3);
            }
        });

        firstOpenEvent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("onActivityResult","onActivityResult  "+resultCode);
        if(resultCode == 3){
            mScoretextView.setText("100");
        }
    }

    public void firstOpenEvent() {
        mOptimizationLayout.setVisibility(View.INVISIBLE);
        mRippleIntroView.setValue(80,10);
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

                    if(CleanUtils.isPermissionOpened()) {
                        GlobalConstant.LAST_NUMBER += 34;
                    }
                } catch(Exception e){

                }
                long endTime = System.currentTimeMillis();
                final long useTime = endTime - startTime;

                if(GlobalConstant.LAST_NUMBER == 0){
                    GlobalConstant.LAST_NUMBER = 33;
                }
                if (useTime < GlobalConstant.MIN_CHECK_TIME) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScoretextView.setRiseInterval(GlobalConstant.FIRST_NUMBER,GlobalConstant.LAST_NUMBER)
                                    .setDuration((int)(GlobalConstant.MIN_CHECK_TIME - useTime))
                                    .runInt(true)
                                    .start();
                        }
                    });
                    SystemClock.sleep(GlobalConstant.MIN_CHECK_TIME - useTime);
                } else if (useTime > GlobalConstant.MAX_CHECK_TIME) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mScoretextView.setRiseInterval(GlobalConstant.FIRST_NUMBER,GlobalConstant.LAST_NUMBER)
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

               /* Message mMessage = new Message();
                mMessage.setData(bundle);
                if(mHandler != null) {
                    mHandler.sendMessage(mMessage);
                }*/
            }
        }.start();
    }


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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkEnd();
                        }
                    });
                    break;
            }
        }
    };

    public void checkEnd(){
        mRippleIntroView.setValue(220,30);
        mOptimizationLayout.setVisibility(View.VISIBLE);
    }

    public class GridviewAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater = LayoutInflater.from(NewHomeActivity.this);
        private Context mContext;
        private int type;

        public GridviewAdapter(Context context ,int type){
            this.mContext = context;
            this.type = type;
        }

        @Override
        public int getCount() {
            return mFucntionDrawablearray.length;
        }

        @Override
        public Object getItem(int i) {
            return mFucntionDrawablearray[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.gridview_item, null);
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.imageView.setImageDrawable(mContext.getResources().getDrawable(mFucntionDrawablearray[i]));
            if(type == 1){
                viewHolder.textView.setText(mContext.getResources().getString(mFunctionStringArray[i]));
            }else {
                viewHolder.textView.setText(mContext.getResources().getString(mMiddleFunctionStringArray[i]));
            }

            return convertView;
        }

        private class ViewHolder{
            public ImageView imageView;
            public TextView textView;
        }
    }
}
