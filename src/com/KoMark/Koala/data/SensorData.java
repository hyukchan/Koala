package com.KoMark.Koala.data;

import java.io.Serializable;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class SensorData implements Serializable{

    private static final long serialVersionUID = 643623572591148629L;

    private float mAcc;
    private long mTimestamp;

    public SensorData(float acc, long timestamp) {
        mAcc = acc;
        mTimestamp = timestamp;
    }

    public float getAcc() {
        return mAcc;
    }

    public void setAcc(float mAcc) {
        this.mAcc = mAcc;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long mTimestamp) {
        this.mTimestamp = mTimestamp;
    }
}
