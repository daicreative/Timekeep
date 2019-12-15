package com.dai.timekeep;

public class MutableInteger
{
    public Integer value;

    MutableInteger(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}