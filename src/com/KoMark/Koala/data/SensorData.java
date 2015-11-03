package com.KoMark.Koala.data;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class SensorData {
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
