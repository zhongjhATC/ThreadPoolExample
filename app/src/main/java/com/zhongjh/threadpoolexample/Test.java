package com.zhongjh.threadpoolexample;

/**
 * @author zhongjh
 * @date 2021/5/18
 */
public class Test {

    public volatile int inc = 0;

    public void increase() {
        inc++;
    }

}
