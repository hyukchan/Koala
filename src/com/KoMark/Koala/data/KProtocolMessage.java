package com.KoMark.Koala.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by TeraByte on 08-11-2015.
 */
public class KProtocolMessage implements Serializable {

    public final static int DT_ACCELERATOR_DATA = 0;
    public final static int DT_NETWORK_GROUP_LIST = 1;

    public final static int MT_COMMAND = 10;
    public final static int MT_DATA = 11;

    private int messageType;
    private ArrayList<SensorData> sensorDatas;
    private ArrayList<KGroupData> groupData;


    public KProtocolMessage() {

    }

    public KProtocolMessage(ArrayList<SensorData> sensorDatas, int messageType) {
        this.sensorDatas = sensorDatas;
        this.messageType = messageType;
    }

    public KProtocolMessage setMessageType(int messageDataType) {
        this.messageType = messageDataType;
        return this;
    }

    public KProtocolMessage setSensorData(ArrayList<SensorData> sensorDatas) {
        this.sensorDatas = sensorDatas;
        return this;
    }

    public int getMessageType() {
        return messageType;
    }

    public ArrayList<SensorData> getSensorDatas() {
        return sensorDatas;
    }

    public ArrayList<KGroupData> getGroupData() {
        return groupData;
    }

    public KProtocolMessage setGroupData(ArrayList<KGroupData> groupData) {
        this.groupData = groupData;
        return this;
    }
}
