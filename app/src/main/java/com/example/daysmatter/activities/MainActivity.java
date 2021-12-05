package com.example.daysmatter.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daysmatter.R;
import com.example.daysmatter.adapters.MattersRVAdapter;
import com.example.daysmatter.models.Matter;
import com.example.daysmatter.models.MatterList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hanks.htextview.evaporate.EvaporateTextView;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private LinearLayout main_LL1;
    private NestedScrollView main_NSV;
    private LinearLayout main_LL2;
    private LinearLayout mainOwner_LL;
    private LinearLayout mainTime_LL;
    private TextClock mainDateToday_textView;
    private EvaporateTextView mainTimeHour_customTextView;
    private TextView mainTimeColon1_textView;
    private EvaporateTextView mainTimeMinute_customTextView;
    private TextView mainTimeColon2_textView;
    private EvaporateTextView mainTimeSecond_customTextView;
    private TextView mainPoweredBy_textView;
    private TextView mainOwnerName_textView;
    private CharSequence sysTimeHourStr;
    private CharSequence sysTimeMinuteStr;
    private CharSequence sysTimeSecondStr;
    private ImageView changeConfig_imageView;
    private FloatingActionButton addMatter_fab;
    private LinearLayout mainNoMatter_LL;
    private RecyclerView mainMatters_recyclerView;
    private MattersRVAdapter mattersRVAdapter;
    private TimeThread timeThread;

    //在主线程里面处理消息并更新UI界面
    private Handler mHandler;

    private ArrayList<Matter> dbMatterList;
    private MatterList matterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);

        baseInit();
    }

    private void baseInit(){
        //deal with database
        SQLiteDatabase db = LitePal.getDatabase();
        dbMatterList = (ArrayList<Matter>) LitePal.findAll(Matter.class);

        main_LL1 = findViewById(R.id.main_LL1);
        main_NSV = findViewById(R.id.main_NSV);
        main_LL2 = findViewById(R.id.main_LL2);
        mainOwner_LL = findViewById(R.id.mainOwner_LL);
        mainTime_LL = findViewById(R.id.mainTime_LL);
        mainDateToday_textView = findViewById(R.id.mainDateToday_textView);
        mainTimeHour_customTextView = findViewById(R.id.mainTimeHour_customTextView);
        mainTimeColon1_textView = findViewById(R.id.mainTimeColon1_textView);
        mainTimeMinute_customTextView = findViewById(R.id.mainTimeMinute_customTextView);
        mainTimeColon2_textView = findViewById(R.id.mainTimeColon2_textView);
        mainTimeSecond_customTextView = findViewById(R.id.mainTimeSecond_customTextView);
        mainPoweredBy_textView = findViewById(R.id.mainPoweredBy_textView);
        mainOwnerName_textView = findViewById(R.id.mainOwnerName_textView);
        changeConfig_imageView = findViewById(R.id.changeConfig_imageView);
        addMatter_fab = findViewById(R.id.addMatter_fab);
        mainNoMatter_LL = findViewById(R.id.mainNoMatter_LL);
        mainMatters_recyclerView = findViewById(R.id.mainMatters_recyclerView);

        adjustHeaderTexts();
        setMattersView();

        addMatter_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, AddNewEventActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        changeConfig_imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    timeThread.pauseThread();
                    hideSystemUi();
                    setLandscapeAttr();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    timeThread.resumeThread();
                }else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    timeThread.pauseThread();
                    setPortraitAttr();
                    getWindow().getDecorView().setSystemUiVisibility(View.VISIBLE);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    timeThread.resumeThread();
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private void adjustHeaderTexts(){
        mainTimeHour_customTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Black.ttf"));
        mainTimeMinute_customTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Black.ttf"));
        mainTimeSecond_customTextView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Black.ttf"));
        mainPoweredBy_textView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Black.ttf"));
        mainOwnerName_textView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Black.ttf"));

        // avoid null pointer exception during animating text
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        long sysTime = System.currentTimeMillis();//获取系统时间
                        sysTimeHourStr = DateFormat.format("hh", sysTime);//时间显示格式
                        sysTimeMinuteStr = DateFormat.format("mm", sysTime);//时间显示格式
                        sysTimeSecondStr = DateFormat.format("ss", sysTime);//时间显示格式

                        mainTimeHour_customTextView.animateText(sysTimeHourStr); //更新时间
                        mainTimeMinute_customTextView.animateText(sysTimeMinuteStr); //更新时间
                        mainTimeSecond_customTextView.animateText(sysTimeSecondStr); //更新时间
                        break;
                    default:
                        break;

                }
            }
        };
        timeThread = new TimeThread();
        timeThread.start(); //启动新的线程
    }

    private class TimeThread extends Thread {

        private final Object lock = new Object();

        private boolean pause = false;

        void pauseThread(){
            pause = true;
        }

        void resumeThread(){
            pause =false;
            synchronized (lock){
                lock.notify();
            }
        }

        void onPause() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            super.run();
            do {
                while (pause){
                    onPause();
                }
                try {
                    Message msg = new Message();
                    msg.what = 1;  //消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }

    public void setLandscapeAttr(){
        // place the time information in the middle of the screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) main_NSV.getLayoutParams();
        p.setMargins(0, 0, 0, 0);
        main_NSV.requestLayout();

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        main_LL1.setLayoutParams(layoutParams);
        main_LL2.setVisibility(View.GONE);
        mainOwner_LL.setVisibility(View.GONE);
        mainTime_LL.setGravity(Gravity.CENTER);

        int width = main_LL1.getWidth();
        addMatter_fab.setVisibility(View.GONE);
        mainDateToday_textView.setVisibility(View.GONE);
        mainTimeHour_customTextView.setTextSize(width / 7 + 0.0F);
        mainTimeMinute_customTextView.setTextSize(width / 7 + 0.0F);
        mainTimeSecond_customTextView.setTextSize(width / 7 + 0.0F);
        mainTimeColon1_textView.setTextSize(width / 7 + 0.0F);
        mainTimeColon2_textView.setTextSize(width / 7 + 0.0F);

        int imageResource = getResources().getIdentifier("@drawable/ic_baseline_fullscreen_exit_24", null, getPackageName());
        @SuppressLint("UseCompatLoadingForDrawables") Drawable res = getResources().getDrawable(imageResource);
        changeConfig_imageView.setImageDrawable(res);
    }

    public void setPortraitAttr(){
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) main_NSV.getLayoutParams();
        p.setMargins(0, 0, 0, 200);
        main_NSV.requestLayout();

        FrameLayout.LayoutParams frameLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        frameLayoutParams.gravity = Gravity.TOP;
        main_LL1.setLayoutParams(frameLayoutParams);
        main_LL2.setVisibility(View.VISIBLE);
        mainOwner_LL.setVisibility(View.VISIBLE);
        mainTime_LL.setGravity(Gravity.START);

        addMatter_fab.setVisibility(View.VISIBLE);
        mainDateToday_textView.setVisibility(View.VISIBLE);
        mainTimeHour_customTextView.setTextSize(36f);
        mainTimeMinute_customTextView.setTextSize(36f);
        mainTimeSecond_customTextView.setTextSize(36f);
        mainTimeColon1_textView.setTextSize(36f);
        mainTimeColon2_textView.setTextSize(36f);

        int imageResource = getResources().getIdentifier("@drawable/ic_baseline_fullscreen_24", null, getPackageName());
        @SuppressLint("UseCompatLoadingForDrawables") Drawable res = getResources().getDrawable(imageResource);
        changeConfig_imageView.setImageDrawable(res);
    }

    private void hideSystemUi(){
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onStop() {
        super.onStop();
        timeThread.pauseThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        timeThread.resumeThread();

        // TODO: Make the Adapter notify more reasonable
        matterList = new MatterList((ArrayList<Matter>) LitePal.findAll(Matter.class));
        mattersRVAdapter = new MattersRVAdapter(matterList);
        mainMatters_recyclerView.setAdapter(mattersRVAdapter);
    }

    public void setMattersView(){
        matterList = new MatterList(dbMatterList);
        if (dbMatterList.size() > 0){
            mainNoMatter_LL.setVisibility(View.GONE);
        }else {
            mainNoMatter_LL.setVisibility(View.VISIBLE);
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false){
            @Override
            public boolean canScrollVertically() {
                //Similarly you can customize "canScrollHorizontally()" for managing horizontal scroll
                return false;
            }
        };
        mainMatters_recyclerView.setLayoutManager(linearLayoutManager);
        mattersRVAdapter = new MattersRVAdapter(matterList);
        mainMatters_recyclerView.setAdapter(mattersRVAdapter);
    }
}