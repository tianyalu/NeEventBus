package com.sty.ne.eventbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //告诉EventBus将本类的方法放到EventBus中进行管理
        EventBus.getDefault().register(this);

        Intent intent = new Intent();
        intent.setClass(this, SecondActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void test(Bean bean) {
        Log.e("sty", "MainActivity: " + bean.toString());
        Log.e("sty", "MainActivity Thread Name: " + Thread.currentThread().getName());
    }

    @Subscribe()
    public void test2(Bean bean) {
        Log.e("sty", "MainActivity 第二个相同参数的方法 ->>> " + bean.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
