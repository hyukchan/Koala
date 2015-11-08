package com.KoMark.Koala.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.data.SensorData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KComm implements Handler.Callback {

    private BluetoothAdapter mBluetoothAdapter;
    private KoalaManager kManager;
    private Set<BluetoothDevice> pairedDevices;
    private long btMACValue;
    private Handler mHandler;
    private final String CLASS_TAG = "KComm";
    private ServerThread serverT;
    private ClientThread clientT;
    private boolean isSlave;
    private ConnectedThread socketT;

    public KComm(Context context) {
        mHandler = new Handler(this);
        kManager = ((KoalaApplication) context).getKoalaManager();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            //Not supported. Must throw error.
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBt);
            // should ensure that bluetooth has been enabled by registering for a bluetooth state change
        }
        String btMAC = mBluetoothAdapter.getAddress().replaceAll(":", "");
        btMACValue = Long.parseLong(btMAC, 16);
        Log.i("KComm", "BtMACVALUE = " + btMACValue);
        System.out.println("btMAC address: " + btMAC);
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice master = null;
        boolean foundMaster = false;
        int masterIndex;
        if (pairedDevices.size() > 0) {
            int index = 0;
            long maxMAC = btMACValue;
            for(BluetoothDevice aDevice : pairedDevices) {
                if(getMACLong(aDevice) > maxMAC) {
                    maxMAC = getMACLong(aDevice);
                    master = aDevice;
                    foundMaster = true;
                }
                index++;
            }
        }
        if(foundMaster) {
            Log.i("KComm", "Connecting as a slave to Master: "+master.getAddress());
            clientT = new ClientThread(master);
            clientT.start();
            isSlave = true;
        } else {
            Log.i("KComm", "Establishing server socket as a Master.");
            serverT = new ServerThread();
            serverT.start();
            isSlave = false;
        }
        Log.i(CLASS_TAG, "End of constructor");


    }

    private long getMACLong(BluetoothDevice device) {
        return Long.parseLong(device.getAddress().replaceAll(":", ""), 16);
    }

    public void init() {


    }

    @Override
    public boolean handleMessage(Message inputMessage) {
        int what = inputMessage.what, arg1 = inputMessage.arg1, arg2 = inputMessage.arg2;
        Object obj = inputMessage.obj;
        switch(inputMessage.what) {
            case 1:
                byte[] buf = (byte[]) obj;
                Log.i("KComm", "Received message: "+(obj).toString());
                Log.i(CLASS_TAG, "Message: "+buf.toString());
                break;
            case 2:
                Log.i(CLASS_TAG, "Write/Read now possible.");
                if(isSlave) {
                    Log.i(CLASS_TAG, "Slave writing to master");
                    byte[] byteArr = new byte[10];
                    byteArr = "Hello".getBytes();
                    socketT.write(byteArr);
                }
        }
        return false;
    }

    public void closeAllConnections() {
        if(clientT != null) clientT.cancel();
        if(serverT != null) serverT.cancel();
    }

    public void sendAccReadings(ArrayList<SensorData> accReadings) {

    }

    public boolean isSlave() {
        return isSlave;
    }

    private class ServerThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public ServerThread() {
            BluetoothServerSocket tmpSocket = null;
            try {
                Log.i("KComm", "Creating server socket");
                UUID uuid = UUID.fromString("e519c52c-81fb-11e5-8bcf-feff819cdc9f");
                tmpSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Koala", uuid);
                Log.i(CLASS_TAG, "Created server socket");
            } catch(IOException e) {e.printStackTrace();}
            mmServerSocket = tmpSocket;
        }

        public void run() {
            BluetoothSocket socket = null;
            while(true) {
                try {
                    socket = mmServerSocket.accept();
                    Log.i(CLASS_TAG, "A client has connected");
                } catch(IOException e) {e.printStackTrace(); break;}

                if(socket != null) {
                    manageConnectedClient(socket);
                    try {
                        mmServerSocket.close(); //Should not be closed if looking to accept more clients.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch(IOException e) { }
        }

        private void manageConnectedClient(BluetoothSocket socket) {
            Log.i("KComm", "Now connected to: "+socket.getRemoteDevice().getName());
            socketT = new ConnectedThread(socket);
            socketT.run();
        }


    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch(IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buf = new byte[1024];
            int bytes;
            mHandler.obtainMessage(2).sendToTarget();
            while(true) {
                try {
                    bytes = mmInStream.read(buf);
                    mHandler.obtainMessage(1, bytes, -1, buf).sendToTarget();
                } catch(IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch(IOException e) { }
        }

        public void cancel() {
            try {
               mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device ) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("e519c52c-81fb-11e5-8bcf-feff819cdc9f"));
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch(IOException e) {
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch(IOException closeException) {
                    closeException.printStackTrace();
                }
                return;
            }
            Log.i(CLASS_TAG, "Client socket established");
            manageMasterConnection(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        private void manageMasterConnection(BluetoothSocket mmSocket) {
            socketT = new ConnectedThread(mmSocket);
            socketT.run(); // Should store this thread somewhere so it can be closed correctly.
        }
    }

}
