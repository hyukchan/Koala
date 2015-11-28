package com.KoMark.Koala.ui;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.KoMark.Koala.R;
import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by Hyukchan on 28/11/2015.
 */
public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private final Context context;
    private ArrayList<BluetoothDevice> bluetoothDevices;

    public BluetoothDeviceAdapter(Context context, ArrayList<BluetoothDevice> bluetoothDevices) {
        super(context, R.layout.listitem_bluetoothdevice);
        this.context = context;
        this.bluetoothDevices = bluetoothDevices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolderItem viewHolder;

        if(convertView==null){

            // inflate the layout
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.listitem_bluetoothdevice, parent, false);

            // well set up the ViewHolder
            viewHolder = new ViewHolderItem();
            viewHolder.bluetoothDeviceName = (TextView) convertView.findViewById(R.id.bluetoothdevice_name);

            // store the holder with the view.
            convertView.setTag(viewHolder);

        }else{
            // we've just avoided calling findViewById() on resource everytime
            // just use the viewHolder
            viewHolder = (ViewHolderItem) convertView.getTag();
        }

        // object item based on the position
        BluetoothDevice bluetoothDevice = bluetoothDevices.get(position);

        // assign values if the object is not null
        if(bluetoothDevice != null) {
            // get the TextView from the ViewHolder and then set the text (item name) and tag (item ID) values
            viewHolder.bluetoothDeviceName.setText(bluetoothDevice.getName());
            viewHolder.bluetoothDeviceName.setTag(bluetoothDevice.getAddress());
        }

        return convertView;
    }

    public void setList(ArrayList<BluetoothDevice> bluetoothDevices) {
        this.bluetoothDevices = bluetoothDevices;
    }

    static class ViewHolderItem {
        TextView bluetoothDeviceName;
    }
}
