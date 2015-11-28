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
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.core.listeners.KCommListener;
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
    private Set<BluetoothDevice> pairedDevices = new HashSet<>();
    private ArrayList<BluetoothDevice> aliveDevices = new ArrayList<>();
    private ArrayList<ConnectedThread> socketList = new ArrayList<>();
    private ArrayList<BluetoothDevice> peerList = new ArrayList<>();
    private long btMACValue;
    private Handler mHandler;
    private final String CLASS_TAG = "KComm";
    private ServerThread serverT;
    private ClientThread clientT;
    private Context context;
    private boolean isSlave;
    private ConnectedThread socketT;
    private BluetoothDevice currMaster;
    private int connectedPeers = 0;
    private static final int MSG_RECEIVED_PCKT = 1;
    private static final int MSG_LOST_MASTER = 2;
    private static final int MSG_LOST_SLAVE = 3;
    private static final int MSG_SCAN_PERIOD = 4;
    private static final String KOALA_UUID = "e519c52c-81fb-11e5-8bcf-feff819cdc9f";

    private ArrayList<SensorDataPackageReceiveListener> sensorDataPackageReceiveListeners;
    private ArrayList<KCommListener> deviceFoundListeners = new ArrayList<KCommListener>();
    private boolean debugFastConnectMode = true;

    public KComm(Context context) {
        mHandler = new Handler(this);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SCAN_PERIOD), 60000);
        this.context = context;
        kManager = ((KoalaApplication) context).getKoalaManager(); //Store context to koala manager
        IntentFilter btEnabledFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        (context).registerReceiver(this, btEnabledFilter); //Register for bluetooth changes
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            //Not supported. Must throw error.
            return;
        }
        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBt);
            // should ensure that bluetooth has been enabled by registering for a bluetooth state change
        } else {
            scanForPeers(); //This also gets called when bluetooth is enabled
        }
        Log.i(CLASS_TAG, "End of constructor");

        sensorDataPackageReceiveListeners = new ArrayList<>();
    }

    public void addSensorDataPackageReceiveListener(SensorDataPackageReceiveListener sensorDataPackageReceiveListener) {
        sensorDataPackageReceiveListeners.add(sensorDataPackageReceiveListener);
    }

    public void addKCommListener(KCommListener listener) {
        deviceFoundListeners.add(listener);
    }

    public void removeKCommListener(KCommListener listener) {
        deviceFoundListeners.remove(listener);
    }

    public boolean scanForPeers() {
        // Ensures that the phone is discoverable through BT indefinitely
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(discoverableIntent);
        }
        if(mBluetoothAdapter.isDiscovering()) {
            Log.i(CLASS_TAG, "Already discovering, not initiating a new.");
            return false; //Already discovering, not going to initiate a new.
        }
        //Below code snippet enables scan for alive devices. If paired device is found, we can initiate connections
        aliveDevices.clear();
        IntentFilter btFoundDevice = new IntentFilter();
        btFoundDevice.addAction(BluetoothDevice.ACTION_FOUND);
        btFoundDevice.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btFoundDevice.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btFoundDevice.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btFoundDevice.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btFoundDevice.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btFoundDevice.addAction(BluetoothDevice.ACTION_UUID);
        (context).registerReceiver(this, btFoundDevice);
        mBluetoothAdapter.startDiscovery();
        return true;
    }

    private void defineMaster() { //Redefines the master if a new device is found or old master connection is lost.
        BluetoothDevice master = getMasterFromPairs();
        if(master != null) {
            if(currMaster == null || (currMaster != null && getMACLong(currMaster) < getMACLong(master))) {
                    connectToMaster(master);
            }
        }
    }

    private void connectToMaster(BluetoothDevice master) {
        if(isSlave && (socketT != null && socketT.isAlive())) { //If we're a slave and connected. Need to disconnect from master.
            if(master.getAddress().equals(currMaster.getAddress())) {
                Log.i(CLASS_TAG, "Current master is same as the one we wish to connect to.");
                return;
            }
            closeAllConnections(); //Close the connection.
            Log.i(CLASS_TAG, "Disconnected from existing master: "+currMaster.getName());
        }
        Log.i(CLASS_TAG, "Connecting as a slave to Master: "+master.getAddress());
        clientT = new ClientThread(master);
        clientT.start();
        currMaster = master;
        isSlave = true;
    }

    private BluetoothDevice getMasterFromPairs() { //null is returned if no master is found
        long maxMAC = btMACValue;
        BluetoothDevice master = null;
        for (BluetoothDevice pairedDevice : pairedDevices) {
            if(getMACLong(pairedDevice) > maxMAC) {
                master = pairedDevice;
                maxMAC = getMACLong(master);
            }
        }
        return master;
    }

    private void connectPeers() {
        String btMAC = mBluetoothAdapter.getAddress().replaceAll(":", "");
        btMACValue = Long.parseLong(btMAC, 16);
        Log.i("KComm", "BtMACVALUE = " + btMACValue + ". Connecting to peers.");
        //pairedDevices.addAll(mBluetoothAdapter.getBondedDevices());
        //pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice master = getMasterFromPairs();
        if(master != null) {
            connectToMaster(master);
        } else if(debugFastConnectMode) { //No Master found. We will have to be master if list of paired device does not contain master. This should be setup at end of the scan.
            for (BluetoothDevice pDevice : mBluetoothAdapter.getBondedDevices()) {
                pairedDevices.add(pDevice);
            }
            master = getMasterFromPairs();
            if(master != null) {
                connectToMaster(master);
            } else {
                setupAsMaster();
            }
        }
    }

    private void setupAsMaster() { //Only setup if not connected to anything. To
        Log.i(CLASS_TAG, "Size of socketList: "+socketList.size());
        if(serverT == null || (socketT != null && !(socketList.size() > 0)) ) {
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
        switch(what) {
            case MSG_RECEIVED_PCKT: //Receiving a protocol message
                if(obj instanceof KProtocolMessage) {
                    KProtocolMessage kProtocolMessage = (KProtocolMessage) obj;
                    if(kProtocolMessage.getMessageType() == KProtocolMessage.MT_DATA) {
                        switch(kProtocolMessage.getDataType()) {
                            case KProtocolMessage.DT_ACCELERATOR_DATA:
                                ArrayList<SensorData> sensorDataPackage = kProtocolMessage.getSensorDatas();
                                String senderDeviceName = kProtocolMessage.getSenderDeviceName();
                                for (SensorDataPackageReceiveListener sensorDataPackageReceiveListener : sensorDataPackageReceiveListeners) {
                                    sensorDataPackageReceiveListener.onSensorDataPackageReceive(sensorDataPackage, senderDeviceName);
                                }
                                break;
                            case KProtocolMessage.DT_NETWORK_GROUP_LIST:

                                break;
                        }
                    }
                }
                Log.i("KComm", "Received message: " + (obj).toString());
                break;
            case MSG_LOST_MASTER: //should give an additional obj which is an instance of the ConnectedThread. Any reference to it should be removed.
                //Lost connection to master. Try finding a replacement.
                Log.i(CLASS_TAG, "MSG_LOST_MASTER");
                socketT.cancel();
                socketT = null;
                currMaster = null;
                scanForPeers();
                connectedPeers--;
                break;
            case MSG_LOST_SLAVE:
                Log.i(CLASS_TAG, "MSG_LOST_SLAVE");
                ConnectedThread socket = (ConnectedThread) obj;
                socketList.remove(socket);
                socket.cancel();
                connectedPeers--;
                break;
            case MSG_SCAN_PERIOD:
                Log.i(CLASS_TAG, "Scan period timer run out.");
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SCAN_PERIOD), 60000);
                scanForPeers();
                break;
            case 5:

                break;
        }
        return false;
    }

    public void closeAllConnections() {
        Log.i(CLASS_TAG, "Start: Closing all connections.");
        connectedPeers = 0;
        if(clientT != null) clientT.cancel(); //Both cancels will close the connectedThread socket
        if(serverT != null) serverT.cancel();
        for (ConnectedThread aSocket : socketList) {
            aSocket.cancel();
        }


        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
        discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //context.startActivity(discoverableIntent); // Ensures that the phone is no longer discoverable
        Log.i(CLASS_TAG, "Finished closing all connections.");
    }

    /**
     * Send acceleration readings to master device
     * @param accReadings
     */
    public boolean sendAccReadings(ArrayList<SensorData> accReadings) {
        if(socketT == null) {
            return false;
        }
        KProtocolMessage kProtocolMessage = new KProtocolMessage(accReadings, KProtocolMessage.MT_DATA, KProtocolMessage.DT_ACCELERATOR_DATA, mBluetoothAdapter.getName());
        socketT.writeObject(kProtocolMessage);
        return true;
    }

    public boolean broadcastToSlaves(KProtocolMessage msg) {
        for (ConnectedThread slave : socketList) {
            slave.writeObject(msg);
        }
        return true;
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
            scanForPeers(); //Start scanning for peers as bluetooth has turned on.
            return;
        }
        if (BluetoothDevice.ACTION_UUID.equals(intent.getAction())) {
            Log.i(CLASS_TAG, "UUID fetched.");
            Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
            for (Parcelable uuid : uuidExtra) {
                Log.i(CLASS_TAG, "UUIDs fetched: "+uuid);
            }

            return;
        }
        if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) { //If new bt device found. Add to aliveDevices list.
            BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            newDevice.fetchUuidsWithSdp();
            Log.i(CLASS_TAG, "Found device: " + newDevice.getAddress() + ". Is paired? : " + newDevice.getBondState());
            aliveDevices.add(newDevice);
            for (ParcelUuid uuid : newDevice.getUuids()) {
                Log.i(CLASS_TAG, "FoundDevice UUIDs: "+uuid);
            }

            for (KCommListener aListener : deviceFoundListeners) {
                aListener.onDeviceFound(newDevice);
            }

            if(newDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                pairedDevices.add(newDevice);
                if(debugFastConnectMode) {
                    connectPeers();
                }
            }
            return;
        }
        if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            Log.i(CLASS_TAG, "Discovery finished.");
            for (KCommListener aListener : deviceFoundListeners) {
                aListener.onStopScan();
            }
            if(!isSlave) { //If a master has not been found at this point in time. Establish master socket.
                setupAsMaster();
            }
            return;
        }
        if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
            Log.i(CLASS_TAG, "Discovery initiated.");
            for (KCommListener aListener : deviceFoundListeners) {
                aListener.onStartScan();
            }
            return;
        }

        if(intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
            BluetoothDevice connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i(CLASS_TAG, "ACL connected to: "+connectedDevice.getName());
            connectedPeers++;
            for (ParcelUuid uuid : connectedDevice.getUuids()) {
                Log.i(CLASS_TAG, "UUID--- of connected device = "+uuid);
                if(uuid.getUuid().toString().compareToIgnoreCase(KOALA_UUID) == 0) { //Is an actual KOALA peer
                    Log.i(CLASS_TAG, "UUID of connected device = "+uuid);
                    peerList.add(connectedDevice);
                }

            }

            for (KCommListener aListener : deviceFoundListeners) {
                aListener.onDeviceConnected(connectedDevice);
            }
            return;
        }

        if(intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
            BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.i(CLASS_TAG, "ACL disconnected from: "+disconnectedDevice.getAddress());
            Log.i(CLASS_TAG, "Removing device ret: "+peerList.remove(disconnectedDevice));
            for (KCommListener aListener : deviceFoundListeners) {
                aListener.onDeviceDisconnected(disconnectedDevice);
            }
            return;
        }

        if(intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothDevice.BOND_BONDED) {
                Log.i(CLASS_TAG, "Acquired bond to: " + device.getName());
                defineMaster(); //Changes master if new device has higher MAC address than curr master.
                for (KCommListener aListener : deviceFoundListeners) {
                    aListener.onDevicePaired(device);
                }
            } else if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothDevice.BOND_NONE &&
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1) == BluetoothDevice.BOND_BONDED) {
                    Log.i(CLASS_TAG, "Prev bonded device now unbonded: " + device.getName());
                for (KCommListener aListener : deviceFoundListeners) {
                    aListener.onDeviceUnpaired(device);
                }
            }
            return;
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final ObjectInputStream mmObjectInputStream;
        private final ObjectOutputStream mmObjectOutputStream;
        private final int lostMsg;

        public ConnectedThread(BluetoothSocket socket, int lostMsg) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            ObjectInputStream tmpObjIn = null;
            ObjectOutputStream tmpObjOut = null;
            this.lostMsg = lostMsg;

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
                    mHandler.obtainMessage(MSG_RECEIVED_PCKT, sensorDataPackage).sendToTarget();
                } catch(IOException e) {
                    e.printStackTrace();
                    mHandler.obtainMessage(lostMsg, this).sendToTarget(); //IO exception, connection lost.
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
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
                UUID uuid = UUID.fromString(KOALA_UUID);
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
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        private void manageConnectedClient(BluetoothSocket socket) {
            Log.i(CLASS_TAG, "Now connected to: "+socket.getRemoteDevice().getName());
            socketT = new ConnectedThread(socket, MSG_LOST_SLAVE);
            socketT.start();
            socketList.add(socketT);
            Log.i(CLASS_TAG, "Size of socket list: "+socketList.size());
        }


    }

    private class ClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device ) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(KOALA_UUID));
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
                Log.i(CLASS_TAG, "Unable to create client socket. Retrying setup of network.");
                //TODO Implement the case 5 message case in handler
                mHandler.obtainMessage(5, null).sendToTarget(); //IO exception. Unable to connect.
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
            socketT = new ConnectedThread(mmSocket, MSG_LOST_MASTER);
            socketT.start(); // Should store this thread somewhere so it can be closed correctly.
        }
    }

}
