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
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class MainActivitycxc extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = "BluetoothGattActivity";
  //  private static final String DEVICE_NAME = "PowerMet";
    private BluetoothAdapter btAdapter;
    private final static int REQUEST_ENABLE_BT = 10;
    // Arduino Power serivce and characterestic
    private static final UUID Power_Service = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
    private static final UUID Power_Data_Char = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb");

    private static final UUID Cadence_Service = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID Cadence_Data_Char = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb");
    private static final byte CRANK_REV_DATA_PRESENT = 0x02; // 1bit

    private BluetoothGatt mConnectedGatt;
    // for all the discovered devices during the scan
    private SparseArray<BluetoothDevice> mDevices;
    private TextView mPower;
    private ProgressDialog mProgress;

    private int mLastCrankEventTime = -1;
    private int mLastCrankRevolutions = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main_activitycxc);
        setProgressBarIndeterminate(true);

        mPower = (TextView) findViewById(R.id.editText);

        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

          /*
         * A progress dialog will be needed while the connection process is
         * taking place
         */
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //Make sure dialog is hidden
       // mProgress.dismiss(); //MADE problems !! later maybe

        //Cancel any scan in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        btAdapter.stopLeScan(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Enable bluetooth in the android
        if (btAdapter == null || !btAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
           // finish();
            //return;
        }
        clearDisplayValues();
    }

    private void clearDisplayValues() {
        Log.i(TAG,"Clearning Display");
        mPower.setText("-----------------");
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
                Log.i(TAG,"Go to start scan ");
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(item.getItemId());
                Log.i(TAG,"Connecting to "+device.getName());
                // make a connection with the device using the specific LE-specific
                //connect GATT() method, passing in a callback for GAT events
                mConnectedGatt = device.connectGatt(this,true,mGattCallback); // or true ?
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
        Log.i(TAG,"Scanning");
        mHandler.postDelayed(mStopRunnable, 1500); //stop scan after 5 seconds
    }

    private void stopScan() {
        btAdapter.stopLeScan(this);
        Log.i(TAG,"Scanning stopped");
        setProgressBarIndeterminateVisibility(false);
    }

    /* BluetoothAdapter.LeScanCallback */

    @Override
    //rssi : receive signal strength of the device
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ rssi:" + rssi);
        /*
         * We are looking for Arduino device, so validate the name
         * that each device reports before adding it to our collection
         */
        mDevices.put(device.hashCode(), device);
        //Update the overflow menu
        invalidateOptionsMenu();


    }

    //Having a bluetooth device, next is trying to connect with it
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /* State Machine Tracking*/

        private int mState =0;

        private void reset(){
            mState = 0;
        }

        private void advance(){
            mState++;
        }


       /* private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;

            switch (mState) {
                case 0:

                    Log.d(TAG, "Enabling Power");
                    characteristic = gatt.getService(Power_Service)
                            .getCharacteristic(Power_Data_Char);
                    Log.d(TAG,characteristic.getUuid().toString());
                    break;
                case 1:
                    Log.d(TAG, "Enabling Cadence");
                    characteristic = gatt.getService(Cadence_Service)
                            .getCharacteristic(Cadence_Data_Char);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            gatt.readCharacteristic(characteristic);
            //clearDisplayValues();
            for(BluetoothGattDescriptor desc: characteristic.getDescriptors()) {
                Log.i(TAG, "eh: " + desc.getUuid());
            }

        }*/


       /* private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Reading pressure cal");
                    characteristic = gatt.getService(Power_Service)
                            .getCharacteristic(Power_Data_Char);
                    break;
                case 1:
                    Log.d(TAG, "Reading pressure");
                    characteristic = gatt.getService(Cadence_Service)
                            .getCharacteristic(Cadence_Data_Char);
                    break;

                default:
                    //mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.i(TAG, "All Sensors Enabled 2");
                    return;
            }
            gatt.writeCharacteristic(characteristic);
        }*/



        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);
            // this will get called when a device connects or disconnects
            Log.d(TAG,"Connection State Change: " + status+" -> "+connectionState(newState));
            if(status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED){
                // Once successfully connected we must next discover all the services on the device
                // before we can read and write their characteristics
                Log.i(TAG,"Go discover the services...");
                gatt.discoverServices(); // if successful, u will enter the OnServicesDiscovered function
              // mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
            }else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED){
                //clear the UI values
                Log.i(TAG," device disconnected u idiot...");
                mHandler.sendEmptyMessage(MSG_CLEAR);
            }else if (status != BluetoothGatt.GATT_SUCCESS){
                //If there i failure at any stage just disconnect
                Log.i(TAG,"Something is wrong u idiot");
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //super.onServicesDiscovered(gatt, status);
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            //get the list of services available in the device
            Log.i(TAG,"Service discovered: "+ status);
            List<BluetoothGattService> services = gatt.getServices();
            for(BluetoothGattService service:services){
                Log.i(TAG,"Service UUID: " + service.getUuid());
                for(BluetoothGattCharacteristic characteristic: service.getCharacteristics()){
                    Log.i(TAG,"Characteristics UUID: " + characteristic.getUuid());
                }
            }
            BluetoothGattCharacteristic characteristic = gatt.getService(Cadence_Service)
                    .getCharacteristic(Cadence_Data_Char);
            for(BluetoothGattDescriptor desc: characteristic.getDescriptors()){
                Log.i(TAG,"Descriptor: " + desc.getUuid().toString());
            }

            gatt.setCharacteristicNotification(characteristic,true);
            //Enable remote notification
            Log.i(TAG, "Descriptors: " + characteristic.getDescriptors());
            for(BluetoothGattDescriptor desc: characteristic.getDescriptors()) {
                //here u should find descriptor UUID that matches Client Characteristic Configuration (0x____)
                // and then call setValue on that descriptor
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }

          //  mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors... "));
            //reset();
            //enableNextSensor(gatt);
            }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           // super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG,"onCharacteristicWrite");
            //readNextSensor(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(Power_Data_Char.equals(characteristic.getUuid())){
                Log.i(TAG,"Finally");
            }
            else{
                Log.i(TAG,"SAD");
            }
            //super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG,"onCharacteristicRead");
            mHandler.sendMessage(Message.obtain(null,MSG_POWER,characteristic));
            //enable notify to get any update on this sensor
            //Enable Local notification
            gatt.setCharacteristicNotification(characteristic,true);
            //Enable remote notification
            Log.i(TAG, "Descriptors: " + characteristic.getDescriptors());
            for(BluetoothGattDescriptor desc: characteristic.getDescriptors()){
                //here u should find descriptor UUID that matches Client Characteristic Configuration (0x____)
                // and then call setValue on that descriptor
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // this will get called anytime you perform a read or write characteristic operation
            // after notifications are enabled, all updates from arduino on the characteristic values
            //will be posted here. Similar to read we hand this up to UI thread to update the display
            Log.i(TAG,"onCharacteristicChanged");
            //if(Power_Data_Char == characteristic.getUuid()){
                mHandler.sendMessage(Message.obtain(null,MSG_POWER,characteristic));
            //}
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG,"onDescriptorWrite");
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        private String connectionState(int status){
            switch (status){
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };

    private static final int MSG_POWER = 101;
    private static final int MSG_CLEAR = 102;
    private static final int MSG_PROGRESS = 103;
    private static final int MSG_DISMISS = 104;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            BluetoothGattCharacteristic characteristic;
            switch (msg.what){
                case MSG_POWER:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if(characteristic.getValue() == null){
                        Log.w(TAG,"Error updating Power value");
                        return;
                    }
                    updatePowerValue(characteristic);
                    break;
                case MSG_PROGRESS:
                    mProgress.setMessage((String)msg.obj);
                    if(!mProgress.isShowing()){
                        mProgress.show();
                        Log.i(TAG,"MSG_PROGRESS in handling");
                    }
                    break;
                case MSG_CLEAR:
                    clearDisplayValues();
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
            }
        }
    };

    // Method to extract the arduino data and update the UI
    private void updatePowerValue(BluetoothGattCharacteristic characteristic){
        int value_offset =0;
        int test = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,1);
       // characteristic.getValue() // array of bytes
        int flags = characteristic.getValue()[value_offset];
        value_offset+=1;
        boolean crank_revolutions_data_present = (flags & 0x02) >0;

        if(crank_revolutions_data_present){

            Log.i(TAG,"craank rev data present");
            final int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,value_offset);
            value_offset+=2;
            final int lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,value_offset);

            if (mLastCrankRevolutions >= 0) {
                float timeDifference;
                if (lastCrankEventTime < mLastCrankEventTime)
                    timeDifference = (65535 + lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]
                else
                    timeDifference = (lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]

                final float crankCadence = (crankRevolutions - mLastCrankRevolutions) * 60.0f / timeDifference;
                mPower.setText(String.format("%d",(int)crankCadence));
            }
            mLastCrankRevolutions = crankRevolutions;
            mLastCrankEventTime = lastCrankEventTime;
        }
            int b = characteristic.getValue()[0];
            Log.i(TAG, String.format("Flag cadence:  0x%02X" , characteristic.getValue()[0]));
            Log.i(TAG, String.format("Cranck rev low:  0x%02X" , characteristic.getValue()[1]));
            Log.i(TAG, String.format("Cranck rev high: 0x%02X " , characteristic.getValue()[2]));
            Log.i(TAG, String.format("Last Cranck event time low: 0x%02X " , characteristic.getValue()[3]));
            Log.i(TAG, String.format("Last Cranck event time high: 0x%02X " , characteristic.getValue()[4]));
            Log.i(TAG,String.format("Cranck rev: %d ", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,1)));
            Log.i(TAG,String.format("Last cranck event time: %d ", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,3)));
    }

}