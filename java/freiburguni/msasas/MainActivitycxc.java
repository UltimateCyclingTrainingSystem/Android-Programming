package freiburguni.msasas;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.RequiresPermission;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class MainActivitycxc extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = "BluetoothGattActivity";
    private static final String DEVICE_NAME = "PowerMeterBLE";
    private BluetoothAdapter btAdapter;
    private final static int REQUEST_ENABLE_BT = 10;
    // Arduino Power serivce and characterestic
    private static final UUID Power_Service = UUID.fromString("19B10010-E8F2-537E-4F6C-D104768A1214");
    private static final UUID Power_Data_Char = UUID.fromString("19B10011-E8F2-537E-4F6C-D104768A1214");

    private BluetoothGatt mConnectedGatt;
    // for all the discovered devices during the scan
    private SparseArray<BluetoothDevice> mDevices;
    private TextView mPower;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activitycxc);
        setProgressBarIndeterminate(true);

        mPower = (TextView) findViewById(R.id.powertxt);

        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //Make sure dialog is hidden
//        mProgress.dismiss(); MADE problems !! later maybe

        //Cancel any scan in progress
        // mHandler.removeCallbacks(mStopRunnable);
        //mHandler.removeCallbacks(mStartRunnable);
        //btAdapter.stopLeScan(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Enable bluetooth in the android
        if (btAdapter == null || !btAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            //finish();
          //return;
        }
        clearDisplayValues();

    }

    private void clearDisplayValues() {

    }

    @Override
    public void onStop() {
        super.onStop();
        //Disconnect from the device
        if(mConnectedGatt !=null){
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Add the scan option to the menu
        getMenuInflater().inflate(R.menu.menu_main_activitycxc,menu);
        //Add any devices discovered
        for(int i=0;i<mDevices.size();i++){
            menu.add(0,mDevices.keyAt(i),0,mDevices.valueAt(i).getName());
        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.action_scan:
                mDevices.clear();
                Log.i(TAG,"Scanning ");
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(item.getItemId());
                Log.i(TAG,"Connecting to "+device.getName());
                // make a connection with the device using the specific LE-specific
                //connect GATT() method, passing in a callback for GAT events
                mConnectedGatt = device.connectGatt(this,true,mGattCallback);
                //Display progress UI
              //  mHandler.sendMessage(Message.obtain(null,MSG_PROGRESS,"Connecting to " + device.getName()+"..."));
                return super.onOptionsItemSelected(item);
        }
    }

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private void startScan() {
        btAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

        //mHandler.postDelayed(mStopRunnable, 5000); //stop scan after 5 seconds
    }

    private void stopScan() {
        btAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }

    /* BluetoothAdapter.LeScanCallback */

    @Override
    //rssi : receive signal strength of the device
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);
        /*
         * We are looking for SensorTag devices only, so validate the name
         * that each device reports before adding it to our collection
         */
        if (DEVICE_NAME.equals(device.getName())) {
            mDevices.put(device.hashCode(), device);
            //Update the overflow menu
            invalidateOptionsMenu();
        }
    }

    //Having a bluetooth device, next is trying to connect with it
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);
            // this will get called when a device connects or disconnects
            Log.d(TAG,"Connection State Change: "); // incomplete
            if(status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED){
                // Once successfully connected we must next discover all the services on the device
                // before we can read and write their characteristics
                gatt.discoverServices(); // if successful, u will enter the OnServicesDiscovered function
                //mHandler.sendMessage(Message.obtain(null,MSG_PROGRESS,"Discovering Services..."));
            }else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED){
                //clear the UI values
                //mHandler.sendEmptyMessage(MSG_CLEAR);
            }else if (status != BluetoothGatt.GATT_SUCCESS){
                //If there i failure at any stage just disconnect
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //super.onServicesDiscovered(gatt, status);
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            //List<BluetoothGattService> services = gatt.getServices();
          //  for (BluetoothGattService service : services) {
            //    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            //}
            BluetoothGattCharacteristic characteristic;
            Log.d(TAG,"Discovering Services ");
            characteristic = gatt.getService(Power_Service).getCharacteristic(Power_Data_Char);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // this will get called anytime you perform a read or write characteristic operation

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }
    };
}
