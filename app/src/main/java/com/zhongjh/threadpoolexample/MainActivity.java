package com.zhongjh.threadpoolexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * @author zhongjh
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private class MyThread extends Thread {

        /**
         * 线程名称
         */
        private String threadName;

        public MyThread(String name) {
            this.threadName = name;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                System.out.println(threadName + ": " + i);
            }
        }

    }

}