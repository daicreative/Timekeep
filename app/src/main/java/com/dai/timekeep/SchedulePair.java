package com.dai.timekeep;

import java.io.Serializable;

public class SchedulePair implements Serializable {
    private long begin;
    private long end;

    public SchedulePair(long begin, long end){
        this.begin = begin;
        this.end = end;
    }

    public long getBegin(){
        return begin;
    }

    public long getEnd(){
        return end;
    }
}
