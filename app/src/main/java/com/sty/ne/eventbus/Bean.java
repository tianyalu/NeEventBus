package com.sty.ne.eventbus;

/**
 * Created by tian on 2019/10/29.
 */

public class Bean {
    private String one;
    private String two;

    public Bean(String one, String two) {

        this.one = one;
        this.two = two;
    }

    public String getOne() {
        return one;
    }

    public void setOne(String one) {
        this.one = one;
    }

    public String getTwo() {
        return two;
    }

    public void setTwo(String two) {
        this.two = two;
    }

    @Override
    public String toString() {
        return "Bean{" +
                "one='" + one + '\'' +
                ", two='" + two + '\'' +
                '}';
    }
}
