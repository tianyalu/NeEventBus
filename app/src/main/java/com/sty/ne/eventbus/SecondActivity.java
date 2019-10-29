package com.sty.ne.eventbus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by tian on 2019/10/29.
 */

public class SecondActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        EventBus.getDefault().post(new Bean("测试标题", "测试内容->主线程"));

//        new Thread() {
//            @Override
//            public void run() {
//                super.run();
//                EventBus.getDefault().post(new Bean("测试标题", "测试内容->子线程"));
//                Log.e("sty", "Second Thread Name: " + Thread.currentThread().getName());
//            }
//        }.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
