package freiburguni.msasas;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;


import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

public class Cycling extends AppCompatActivity {
    private static final UUID Power_Service = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
    private static final UUID Power_Data_Char = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb");
    private static final UUID Heart_rate_Service = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID Heart_rate_Char = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    TextView textViewTime;
    private float avgPower;
    private static int PowerInputs;
    private  int checkCounter = 0;
    private float crankCadence= 0;
    private BluetoothDevice HeartRatedevice;
    private BluetoothDevice Powerdevice;
    private boolean dialogShowing;
    private BarChart CadenceBarChart;
    private ArrayList<BarEntry> CadenceEntries;
    private BarDataSet CadenceDataset;
    private ArrayList<String> CadenceLabels;
    private BarData CadenceData;
    private Button startbtn;
    private boolean startbtnpressed;
    private boolean paused;
    private BarChart PowerBarChart;
    private ArrayList<BarEntry> PowerEntries;
    private BarDataSet PowerDataset;
    private ArrayList<String> PowerLabels;
    private BarData PowerData;
    private TextView Heart_Rate;
    long oldmillis;

    private int mLastCrankEventTime;
    private int mLastCrankRevolutions;
    private BluetoothGatt HeartRateConnectedGatt;
    private BluetoothGatt PowerConnectedGatt;
    private static final String TAG = "BluetoothGattActivity";

    private ProgressDialog mProgress;
    private AlertDialog.Builder dialogBuilder;
    long startTime = 0;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;

            textViewTime.setText(String.format("%02d:%02d:%02d",hours, minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycling);
        mLastCrankRevolutions = 0;
        mLastCrankEventTime = 0;
        avgPower = 0;
        PowerInputs = 0;
        dialogShowing = false;
        startbtnpressed = false;
        paused = false;
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        textViewTime.setText("00:00:00");
        startbtn = (Button) findViewById(R.id.startbtn);
        startbtn.setText("start");
        startbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (startbtn.getText().equals("stop")) {
                    timerHandler.removeCallbacks(timerRunnable);
                    b.setText("start");
                    final Intent i = new Intent(Cycling.this,Welcome.class);
                    if(getIntent().hasExtra("heart")){
                        i.putExtra("heart",HeartRatedevice);
                        HeartRateConnectedGatt.close();
                    }

                    if(getIntent().hasExtra("power")){
                        i.putExtra("power",Powerdevice);
                        PowerConnectedGatt.close();
                    }
                    avgPower = avgPower/PowerInputs;
                    if(PowerInputs == 0){
                        avgPower = 0;
                    }


                    Log.i(TAG,"Average Power: " + String.valueOf(avgPower));

                    // dialog
                    dialogBuilder = new AlertDialog.Builder(Cycling.this);
                    final EditText txtInput = new EditText(Cycling.this);
                    txtInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(17)});

                    // dialog process
                    dialogBuilder.setTitle("Average Power: " + String.valueOf(Math.round(avgPower)));
                    dialogBuilder.setPositiveButton("OK",new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(i);
                            finish();
                        }
                    });
                    dialogShowing = true;
                    AlertDialog dialogHighScore = dialogBuilder.create();
                    dialogHighScore.setCanceledOnTouchOutside(false);
                    dialogHighScore.setCancelable(false);
                    dialogHighScore.show();
                } else if(startbtn.getText().equals("start")){
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    b.setText("stop");
                    startbtnpressed = true;
                }
            }
        });

        Bundle mainmenuData = getIntent().getExtras();
        if(mainmenuData == null){
            Log.i(TAG,"No extras found in the cycling");
            return;
        }

        if(getIntent().hasExtra("heart")) {
            HeartRatedevice = getIntent().getExtras().getParcelable("heart");
            Log.i(TAG, "Heart CHALLENGE " + HeartRatedevice.getName().toString());
            HeartRateConnectedGatt = HeartRatedevice.connectGatt(this, true, mGattCallback); // or true ?

        }
        if(getIntent().hasExtra("power")){
            Powerdevice = getIntent().getExtras().getParcelable("power");
            Log.i(TAG, "POWER CHALLENGE " + Powerdevice.getName().toString());
            PowerConnectedGatt = Powerdevice.connectGatt(this, true, mGattCallback); // or true ?

        }
        Heart_Rate = (TextView)findViewById(R.id.hearttxt);
        Heart_Rate.setTextColor(Color.BLACK);
        Heart_Rate.setTextSize(20f);
        //Cadence bar chart
        CadenceBarChart = (BarChart)findViewById(R.id.cadencechart);
        CadenceEntries = new ArrayList<>(); // yentries data
        CadenceEntries.add(new BarEntry(0,0));
        CadenceDataset = new BarDataSet(CadenceEntries,"Cadence bpm");
        CadenceDataset.setColor(Color.BLACK);
        CadenceDataset.setValueTextColor(Color.BLACK);
        CadenceDataset.setValueTextSize(20f);
        // creating labels
        CadenceLabels = new ArrayList<String>(); //xentries
        CadenceLabels.add("Cadence");

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
        CadenceleftAxis.setAxisMaxValue(400f);
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
        PowerEntries.add(new BarEntry(0,0));
        PowerDataset = new BarDataSet(PowerEntries,"Power W");
        PowerDataset.setColor(Color.BLACK);
        PowerDataset.setValueTextColor(Color.BLACK);
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
        PowerleftAxis.setAxisMaxValue(1000f);
        PowerleftAxis.setAxisMinValue(0f);

        //X axis
        XAxis PowerxAxis = PowerBarChart.getXAxis();
        PowerxAxis.setDrawGridLines(false);
        PowerxAxis.setDrawLabels(false);
        PowerxAxis.setDrawAxisLine(false);
        //PowerBarChart.notifyDataSetChanged()
    }

    // Method to extract the arduino data and update the UI
    private void updateHeartRateValue(BluetoothGattCharacteristic characteristic){
        final int heart_rate_value =  characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,1);
        Heart_Rate.setText(String.valueOf(heart_rate_value)+ " bpm");
    }

    private void updatePowerValue(BluetoothGattCharacteristic characteristic){
        // characteristic.getValue() // array of bytes

        final int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,6);
        final int lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,8);
        final float Power = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,2);
        if(startbtnpressed && !paused) {
            avgPower += Power;
            PowerInputs++;
        }
        Log.i(TAG,"==================================\nEntry number: " + String.valueOf(PowerInputs) + ", absolute Power: "+ String.valueOf(Power));
        if(mLastCrankEventTime == lastCrankEventTime) {
            Log.i(TAG,"HI");
            checkCounter++;
            if(checkCounter >= 3){
                checkCounter = 0;
                crankCadence = 0;
            }
            CadenceEntries.add(new BarEntry(crankCadence, 0));
        }
        else if (mLastCrankRevolutions >= 0) {
            float timeDifference;
            Log.i(TAG,"Hallo");
            if (lastCrankEventTime < mLastCrankEventTime)
                timeDifference = (65535 + lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]
            else
                timeDifference = (lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]
            crankCadence = Math.round((crankRevolutions - mLastCrankRevolutions) * 60.0f / timeDifference);
            CadenceEntries.add(new BarEntry(crankCadence, 0));
            checkCounter = 0;
        }
        if((crankRevolutions-mLastCrankRevolutions)>=4){
            Log.i(TAG,"no");

            crankCadence = 0;
            CadenceEntries.add(new BarEntry(crankCadence, 0));
            checkCounter = 0;
        }
        mLastCrankRevolutions = crankRevolutions;
        mLastCrankEventTime = lastCrankEventTime;
        CadenceEntries.remove(0);

        CadenceDataset = new BarDataSet(CadenceEntries,"Cadence rpm");
        CadenceDataset.setColor(Color.BLACK);
        CadenceDataset.setValueTextSize(20f);
        CadenceDataset.setValueTextColor(Color.BLACK);
        // creating labels
        CadenceData = new BarData(CadenceLabels, CadenceDataset);
        CadenceBarChart.notifyDataSetChanged();
        CadenceBarChart.setData(CadenceData); // set the data and list of lables into chart
        CadenceBarChart.invalidate();

        PowerEntries.remove(0);
        PowerEntries.add(new BarEntry(Power, 0));
        PowerDataset = new BarDataSet(PowerEntries,"Power W");
        PowerDataset.setColor(Color.BLACK);
        PowerDataset.setValueTextSize(20f);
        PowerDataset.setValueTextColor(Color.BLACK);
        // creating labels
        PowerData = new BarData(PowerLabels, PowerDataset);
        PowerBarChart.notifyDataSetChanged();
        PowerBarChart.setData(PowerData); // set the data and list of lables into chart
        PowerBarChart.invalidate();
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
                Toast.makeText(Cycling.this,"Device disconnected",Toast.LENGTH_SHORT);
                Log.i(TAG," device disconnected u idiot...");
                mHandler.sendEmptyMessage(MSG_CLEAR);
            }else if (status != BluetoothGatt.GATT_SUCCESS){
                //If there i failure at any stage just disconnect
                Log.i(TAG,"Something is wrong u idiot");
                Toast.makeText(Cycling.this,"Something went wrong",Toast.LENGTH_SHORT);

                gatt.disconnect();
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //super.onServicesDiscovered(gatt, status);
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            //get the list of services available in the device
            boolean heartsensor = false;
            boolean powersensor = false;
            Log.i(TAG,"Service discovered: "+ status);
            List<BluetoothGattService> services = gatt.getServices();
            for(BluetoothGattService service:services){
                //   Log.i(TAG,"Service UUID: " + service.getUuid());
                for(BluetoothGattCharacteristic characteristic: service.getCharacteristics()){
                    //     Log.i(TAG,"Characteristics UUID: " + characteristic.getUuid());
                    if(Power_Data_Char.equals(characteristic.getUuid())){
                        powersensor = true;
                    }
                    if(Heart_rate_Char.equals(characteristic.getUuid())){
                        heartsensor = true;
                    }
                }
            }

            if(powersensor){
                BluetoothGattCharacteristic Power_characteristic = gatt.getService(Power_Service)
                        .getCharacteristic(Power_Data_Char);
                gatt.setCharacteristicNotification(Power_characteristic,true);
                for(BluetoothGattDescriptor desc: Power_characteristic.getDescriptors()) {
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(desc);
                }
            }
            if(heartsensor){
                BluetoothGattCharacteristic Heart_rate_characteristic = gatt.getService(Heart_rate_Service)
                        .getCharacteristic(Heart_rate_Char);
                gatt.setCharacteristicNotification(Heart_rate_characteristic,true);
                for(BluetoothGattDescriptor desc: Heart_rate_characteristic.getDescriptors()) {
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(desc);
                }

            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // this will get called anytime you perform a read or write characteristic operation
            // after notifications are enabled, all updates from arduino on the characteristic values
            //will be posted here. Similar to read we hand this up to UI thread to update the display
            Log.i(TAG,"onCharacteristicChanged UUID: " + characteristic.getUuid());
            if(Heart_rate_Char.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null,MSG_HEART_RATE,characteristic));
            }
            if(Power_Data_Char.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null,MSG_POWER,characteristic));
            }

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
    private static final int MSG_HEART_RATE = 102;
    private static final int MSG_CLEAR = 103;
    private static final int MSG_PROGRESS = 104;
    private static final int MSG_DISMISS = 105;
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

                case MSG_HEART_RATE:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if(characteristic.getValue() == null){
                        Log.w(TAG,"Error updating Heart rate value");
                        return;
                    }
                    updateHeartRateValue(characteristic);
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
    private void clearDisplayValues() {
        Log.i(TAG,"Clearning Display");
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Are you a sure you want to quit ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Cycling.this,Welcome.class);
                        if(getIntent().hasExtra("heart")){
                            i.putExtra("heart",HeartRatedevice);
                            HeartRateConnectedGatt.close();

                        }

                        if(getIntent().hasExtra("power")){
                            i.putExtra("power",Powerdevice);
                            PowerConnectedGatt.close();
                        }
                        startActivity(i);
                        finish();
                    }
                }).setNegativeButton("No", null).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        if(startbtn.getText() == "stop"){
            paused = true;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(paused){
            final Intent i = new Intent(Cycling.this,Welcome.class);
            if(getIntent().hasExtra("heart")){
                i.putExtra("heart",HeartRatedevice);
                HeartRateConnectedGatt.close();
            }

            if(getIntent().hasExtra("power")){
                i.putExtra("power",Powerdevice);
                PowerConnectedGatt.close();
            }
            avgPower = avgPower/PowerInputs;
            if(PowerInputs == 0){
                avgPower = 0;
            }


            Log.i(TAG,"Average Power: " + String.valueOf(avgPower));

            // dialog
            dialogBuilder = new AlertDialog.Builder(Cycling.this);
            final EditText txtInput = new EditText(Cycling.this);
            txtInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(17)});

            // dialog process
            dialogBuilder.setTitle("Average Power: " + String.valueOf(Math.round(avgPower)));
            dialogBuilder.setPositiveButton("OK",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(i);
                    finish();
                }
            });
            dialogShowing = true;
            AlertDialog dialogHighScore = dialogBuilder.create();
            dialogHighScore.setCanceledOnTouchOutside(false);
            dialogHighScore.setCancelable(false);
            dialogHighScore.show();
        }

    }
}
