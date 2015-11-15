package com.KoMark.Koala.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.core.listeners.SensorDataPackageReceiveListener;
import com.KoMark.Koala.data.KProtocolMessage;
import com.KoMark.Koala.data.SensorData;

import java.io.*;
import java.util.*;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KComm extends BroadcastReceiver implements Handler.Callback {

    private BluetoothAdapter mBluetoothAdapter;
    private KoalaManager kManager;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> aliveDevices = new ArrayList<>();
    private long btMACValue;
    private Handler mHandler;
    private final String CLASS_TAG = "KComm";
    private ServerThread serverT;
    private ClientThread clientT;
    private Context context;
    private boolean isSlave;
    private ConnectedThread socketT;

    private ArrayList<SensorDataPackageReceiveListener> sensorDataPackageReceiveListeners;

    public KComm(Context context) {
        mHandler = new Handler(this);
        this.context = context;
        kManager = ((KoalaApplication) context).getKoalaManager(); //Store context to koala manager
        IntentFilter btEnabledFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        (context).registerReceiver(this, btEnabledFilter); //Register for bluetooth changes
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            //Not supported. Must throw error.
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBt);
            // should ensure that bluetooth has been enabled by registering for a bluetooth state change
        } else {
            setupBtNetwork();
        }
        //setupBtNetwork();
        Log.i(CLASS_TAG, "End of constructor");

        sensorDataPackageReceiveListeners = new ArrayList<SensorDataPackageReceiveListener>();
    }

    public void addSensorDataPackageReceiveListener(SensorDataPackageReceiveListener sensorDataPackageReceiveListener) {
        sensorDataPackageReceiveListeners.add(sensorDataPackageReceiveListener);
    }

    private void setupBtNetwork() {
        // Ensures that the phone is discoverable through BT indefinitely
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(discoverableIntent);

        //Below code snippet enables scan for alive devices. If paired device is found, we can initiate connections
        aliveDevices.clear();
        IntentFilter btFoundDevice = new IntentFilter();
        btFoundDevice.addAction(BluetoothDevice.ACTION_FOUND);
        btFoundDevice.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btFoundDevice.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        (context).registerReceiver(this, btFoundDevice);
        mBluetoothAdapter.startDiscovery();
        /*




         */
        String btMAC = mBluetoothAdapter.getAddress().replaceAll(":", "");
        btMACValue = Long.parseLong(btMAC, 16);
        Log.i("KComm", "BtMACVALUE = " + btMACValue);
        System.out.println("btMAC address: " + btMAC);
        //pairedDevices.addAll(mBluetoothAdapter.getBondedDevices());
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice master = null;
        boolean foundMaster = false;
        if (pairedDevices.size() > 0) {
            long maxMAC = btMACValue;
            for(BluetoothDevice aDevice : pairedDevices) {
                if(getMACLong(aDevice) > maxMAC) {
                    maxMAC = getMACLong(aDevice);
                    master = aDevice;
                    foundMaster = true;
                }
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
                if(obj instanceof KProtocolMessage) {
                    KProtocolMessage kProtocolMessage = (KProtocolMessage) obj;
                    ArrayList<SensorData> sensorDataPackage = kProtocolMessage.getSensorDatas();
                    String senderDeviceName = kProtocolMessage.getSenderDeviceName();
                    for (SensorDataPackageReceiveListener sensorDataPackageReceiveListener : sensorDataPackageReceiveListeners) {
                        sensorDataPackageReceiveListener.onSensorDataPackageReceive(sensorDataPackage, senderDeviceName);
                    }
                }
                Log.i("KComm", "Received message: " + (obj).toString());
                break;
        }
        return false;
    }

    public void closeAllConnections() {
        Log.i(CLASS_TAG, "Start: Closing all connections.");
        if(clientT != null) clientT.cancel();
        if(serverT != null) serverT.cancel();

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
        discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //context.startActivity(discoverableIntent); // Ensures that the phone is no longer discoverable
        Log.i(CLASS_TAG, "Finish: Closing all connections.");
    }

    /**
     * Send acceleration readings to master device
     * @param accReadings
     */
    public void sendAccReadings(ArrayList<SensorData> accReadings) {
        KProtocolMessage kProtocolMessage = new KProtocolMessage(accReadings, KProtocolMessage.MT_DATA, mBluetoothAdapter.getName());
        socketT.writeObject(kProtocolMessage);
    }

    public boolean isSlave() {
        return isSlave;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(CLASS_TAG, "Broadcast received.");
        if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED) && (int) intent.getExtras().get(BluetoothAdapter.EXTRA_STATE) == BluetoothAdapter.STATE_ON) {
            int extraState = (int) intent.getExtras().get(BluetoothAdapter.EXTRA_STATE);
            int previousState = (int) intent.getExtras().get(BluetoothAdapter.EXTRA_PREVIOUS_STATE);
            Log.i(CLASS_TAG, "Extras: "+extraState + " , "+previousState);
            return;
        }
        if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
            BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i(CLASS_TAG, "Found device: "+newDevice.getAddress()+". Is paired? : "+newDevice.getBondState());
            aliveDevices.add(newDevice);
            return;
        }
        if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            Log.i(CLASS_TAG, "Discovery finished.");
            return;
        }
        if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
            Log.i(CLASS_TAG, "Discovery initiated.");
        }



    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final ObjectInputStream mmObjectInputStream;
        private final ObjectOutputStream mmObjectOutputStream;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            ObjectInputStream tmpObjIn = null;
            ObjectOutputStream tmpObjOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                tmpObjOut = new ObjectOutputStream(tmpOut);
                tmpObjOut.flush();
                tmpObjIn = new ObjectInputStream(tmpIn);
            } catch(IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mmObjectInputStream = tmpObjIn;
            mmObjectOutputStream = tmpObjOut;
        }

        public void run() {
            Object sensorDataPackage;
            //mHandler.obtainMessage(2).sendToTarget();
            while(true) {
                try {
                    sensorDataPackage = mmObjectInputStream.readUnshared();
                    mHandler.obtainMessage(1, sensorDataPackage).sendToTarget();
                } catch(IOException e) {
                    e.printStackTrace();
                    mHandler.obtainMessage(3, null).sendToTarget(); //IO exception, connection lost.
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        //TODO: delete me
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch(IOException e) { }
        }

        public void writeObject(Object o) {
            try {
                mmObjectOutputStream.reset();
                mmObjectOutputStream.writeUnshared(o);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
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
/*                    try {
                        mmServerSocket.close(); //Should not be closed if looking to accept more clients.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;*/
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
            socketT.start();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            socketT.start(); // Should store this thread somewhere so it can be closed correctly.
        }
    }

}
