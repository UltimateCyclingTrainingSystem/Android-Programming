package freiburguni.msasas;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PowerChallenge extends AppCompatActivity {

    private static final UUID Power_Service = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
    private static final UUID Power_Data_Char = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb");

    private BarChart CadenceBarChart;
    private ArrayList<BarEntry> CadenceEntries;
    private BarDataSet CadenceDataset;
    private ArrayList<String> CadenceLabels;
    private BarData CadenceData;

    private BarChart PowerBarChart;
    private ArrayList<BarEntry> PowerEntries;
    private BarDataSet PowerDataset;
    private ArrayList<String> PowerLabels;
    private BarData PowerData;

    private int mLastCrankEventTime = -1;
    private int mLastCrankRevolutions = -1;

    private BluetoothGatt mConnectedGatt;
    private static final String TAG = "BluetoothGattActivity";

    private ProgressDialog mProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_power_challenge);
        Bundle mainmenuData = getIntent().getExtras();
        if(mainmenuData == null){
            Log.i(TAG,"WHYYYYYY");
            return;
        }
        BluetoothDevice device = getIntent().getExtras().getParcelable("btdevice");
        Log.i(TAG,"POWER CHALLENGE " + device.getName().toString());
        mConnectedGatt = device.connectGatt(this,true,mGattCallback); // or true ?


        //Cadence bar chart
        CadenceBarChart = (BarChart)findViewById(R.id.cadencechart);
        CadenceEntries = new ArrayList<>(); // yentries data
        CadenceEntries.add(new BarEntry(0, 0));
        CadenceDataset = new BarDataSet(CadenceEntries,"Cadence");
        CadenceDataset.setColor(Color.WHITE);
        CadenceDataset.setValueTextColor(Color.WHITE);
        CadenceDataset.setValueTextSize(20f);
        // creating labels
        CadenceLabels = new ArrayList<String>(); //xentries
        CadenceLabels.add("Power");

        CadenceData = new BarData(CadenceLabels, CadenceDataset);
        CadenceBarChart.setData(CadenceData); // set the data and list of lables into chart
        CadenceBarChart.setDescription(null);
        CadenceBarChart.setTouchEnabled(false);
        CadenceBarChart.setNoDataTextDescription("Start Pedalling");
        CadenceBarChart.setGridBackgroundColor(Color.TRANSPARENT);

        //Y right axis
        YAxis CadencerightAxis = CadenceBarChart.getAxisRight();
        CadencerightAxis.setDrawLabels(false);
        CadencerightAxis.setDrawAxisLine(false);
        CadencerightAxis.setDrawGridLines(false);

        //Y left axis
        YAxis CadenceleftAxis = CadenceBarChart.getAxisLeft();
        CadenceleftAxis.setDrawGridLines(false);
        CadenceleftAxis.setDrawAxisLine(false);
        CadenceleftAxis.setDrawLabels(false);
        CadenceleftAxis.setAxisMaxValue(300f);
        CadenceleftAxis.setAxisMinValue(0f);

        //X axis
        XAxis CadencexAxis = CadenceBarChart.getXAxis();
        CadencexAxis.setDrawGridLines(false);
        CadencexAxis.setDrawLabels(false);
        CadencexAxis.setDrawAxisLine(false);
        // CadenceBarChart.notifyDataSetChanged()

        //Power bar chart
        PowerBarChart = (BarChart)findViewById(R.id.powerchart);
        PowerEntries = new ArrayList<>(); // yentries data
        PowerEntries.add(new BarEntry(0, 0));
        PowerDataset = new BarDataSet(CadenceEntries,"Power");
        PowerDataset.setColor(Color.WHITE);
        PowerDataset.setValueTextColor(Color.WHITE);
        PowerDataset.setValueTextSize(20f);
        // creating labels
        PowerLabels = new ArrayList<String>(); //xentries
        PowerLabels.add("Power");

        PowerData = new BarData(PowerLabels, PowerDataset);
        PowerBarChart.setData(PowerData); // set the data and list of lables into chart
        PowerBarChart.setDescription(null);
        PowerBarChart.setTouchEnabled(false);
        PowerBarChart.setNoDataTextDescription("Start Pedalling");
        PowerBarChart.setGridBackgroundColor(Color.TRANSPARENT);

        //Y right axis
        YAxis PowerrightAxis = PowerBarChart.getAxisRight();
        PowerrightAxis.setDrawGridLines(false);
        PowerrightAxis.setDrawLabels(false);
        PowerrightAxis.setDrawAxisLine(false);

        //Y left axis
        YAxis PowerleftAxis = PowerBarChart.getAxisLeft();
        PowerleftAxis.setDrawAxisLine(false);
        PowerleftAxis.setDrawLabels(false);
        PowerleftAxis.setDrawGridLines(false);
        PowerleftAxis.setAxisMaxValue(500f);
        PowerleftAxis.setAxisMinValue(0f);

        //X axis
        XAxis PowerxAxis = PowerBarChart.getXAxis();
        PowerxAxis.setDrawGridLines(false);
        PowerxAxis.setDrawLabels(false);
        PowerxAxis.setDrawAxisLine(false);
        //PowerBarChart.notifyDataSetChanged()
    }

    private void clearDisplayValues() {
        Log.i(TAG,"Clearning Display");
    }

    // Method to extract the arduino data and update the UI
    private void updatePowerValue(BluetoothGattCharacteristic characteristic){
        // characteristic.getValue() // array of bytes
        final int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,6);
        final int lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,8);
        final float Power = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,2);
        if (mLastCrankRevolutions >= 0) {
            float timeDifference;
            if (lastCrankEventTime < mLastCrankEventTime)
                timeDifference = (65535 + lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]
            else
                timeDifference = (lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]
            float crankCadence =0 ;
            if(timeDifference != 0){
                crankCadence= (crankRevolutions - mLastCrankRevolutions) * 60.0f / timeDifference;
            }
            if((crankRevolutions-mLastCrankRevolutions)>=4){
                crankCadence = 0;
            }
            CadenceEntries.remove(0);
            CadenceEntries.add(new BarEntry(crankCadence, 0));
            CadenceDataset = new BarDataSet(CadenceEntries,"Cadence");
            CadenceDataset.setColor(Color.WHITE);
            CadenceDataset.setValueTextSize(20f);
            CadenceDataset.setValueTextColor(Color.WHITE);
            // creating labels
            CadenceData = new BarData(CadenceLabels, CadenceDataset);
            CadenceBarChart.notifyDataSetChanged();
            CadenceBarChart.setData(CadenceData); // set the data and list of lables into chart
            CadenceBarChart.invalidate();

            PowerEntries.remove(0);
            PowerEntries.add(new BarEntry(Power, 0));
            PowerDataset = new BarDataSet(PowerEntries,"Power");
            PowerDataset.setColor(Color.WHITE);
            PowerDataset.setValueTextSize(20f);
            PowerDataset.setValueTextColor(Color.WHITE);
            // creating labels
            PowerData = new BarData(PowerLabels, PowerDataset);
            PowerBarChart.notifyDataSetChanged();
            PowerBarChart.setData(PowerData); // set the data and list of lables into chart
            PowerBarChart.invalidate();
        }
        mLastCrankRevolutions = crankRevolutions;
        mLastCrankEventTime = lastCrankEventTime;
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


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
                Toast.makeText(PowerChallenge.this,"Device disconnected",Toast.LENGTH_SHORT);
                Log.i(TAG," device disconnected u idiot...");
                mHandler.sendEmptyMessage(MSG_CLEAR);
            }else if (status != BluetoothGatt.GATT_SUCCESS){
                //If there i failure at any stage just disconnect
                Log.i(TAG,"Something is wrong u idiot");
                Toast.makeText(PowerChallenge.this,"Something went wrong",Toast.LENGTH_SHORT);

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

            BluetoothGattCharacteristic Power_characteristic = gatt.getService(Power_Service)
                    .getCharacteristic(Power_Data_Char);
            gatt.setCharacteristicNotification(Power_characteristic,true);
            //Enable remote notification
            for(BluetoothGattDescriptor desc: Power_characteristic.getDescriptors()) {
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }

            //  mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors... "));
            //reset();
            //enableNextSensor(gatt);
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
    private static final int MSG_LIST = 105;
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
}
