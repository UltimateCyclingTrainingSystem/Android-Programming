package freiburguni.msasas;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.jar.Manifest;

public class MainActivitycxc extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {
    private ListView list;
    CheckBox checkBox;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private Intent intent;
    private TextView devicesfound;
    boolean btnIsVisible;
    private static final String TAG = "BluetoothGattActivity";
    private BluetoothAdapter btAdapter;
    private final static int REQUEST_ENABLE_BT = 10;
    String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"};
    int permsRequestCode = 200;


    // Arduino Power serivce and characterestic

    private BluetoothGatt mConnectedGatt;
    // for all the discovered devices during the scan
    private SparseArray<BluetoothDevice> mDevices;
    private ProgressDialog mProgress;
    private ImageButton workoutbtn;

    public void statusCheck()
    {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();

        }


    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();

    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                        System.exit(0);
                    }
                }).setNegativeButton("No", null).show();
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
        switch (permsRequestCode){
            case 200:
                Log.i(TAG,"hello");
                boolean fineLocationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean CoarseLocationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        statusCheck();
        requestPermissions(perms,permsRequestCode);

        setContentView(R.layout.activity_main_activitycxc);
        setProgressBarIndeterminate(true);
        btnIsVisible = false;
        workoutbtn =(ImageButton)findViewById(R.id.workoutbtn);
        workoutbtn.setVisibility(View.GONE);
        devicesfound = (TextView)findViewById(R.id.devicesfound);
        list = (ListView)findViewById(R.id.mylist);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        arrayList = new ArrayList<String>();
        // Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.select_dialog_multichoice, arrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position,convertView,parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.BLACK);
                tv.setTextSize(20f);
                checkBox = (CheckBox) view.findViewById(android.R.id.checkbox);

                return view;
            }
        };
        // Here, you set the data in your ListView

        list.setAdapter(adapter);

        intent = new Intent(MainActivitycxc.this, Welcome.class);
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

        list.setOnItemClickListener(
                new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        BluetoothDevice device = mDevices.valueAt(position);
                        if(device.getName().toString().equals("heart rate sensor")) {
                            if (intent.hasExtra("heart")) {
                                intent.removeExtra("heart");
                            } else {
                                intent.putExtra("heart", device);
                            }
                        }
                        else{
                            if (intent.hasExtra("power")) {
                                intent.removeExtra("power");
                            } else {
                                intent.putExtra("power", device);
                            }
                        }
                    }

                }
        );
    }

    public void GoToHighScores(View view){
        Intent i = new Intent(this,HighScores.class);
        i.putExtra("nothing",true);
        startActivity(i);


    }

    public void GoWorkout(View view){
        if(intent.hasExtra("heart") || intent.hasExtra("power")){
            startActivity(intent);
        }
        else
        {
            Toast.makeText(this,"Choose at least one sensor",Toast.LENGTH_SHORT).show();
        }
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
                workoutbtn.setVisibility(View.GONE);
                btnIsVisible = false;
                arrayList.clear();
                adapter.notifyDataSetChanged();
                Log.i(TAG,"Go to start scan ");
                startScan();
                return true;
            default:
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
        if(intent.hasExtra("heart")){
            intent.removeExtra("heart");
        }
        if(intent.hasExtra("power")){
            intent.removeExtra("power");
        }
        btnIsVisible = false;
        setProgressBarIndeterminateVisibility(true);
        Toast.makeText(this,"Scanning",Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(mStopRunnable, 3000); //stop scan after _ milli seconds
    }

    private void stopScan() {
        btAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
        mHandler.sendEmptyMessage(MSG_LIST);

    }

    /* BluetoothAdapter.LeScanCallback */

    @Override
    //rssi : receive signal strength of the device
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        /*
         * We are looking for Arduino device, so validate the name
         * that each device reports before adding it to our collection
         */
        mDevices.put(device.hashCode(), device);
        //Update the overflow menu
        invalidateOptionsMenu();
    }


    private static final int MSG_LIST = 105;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_LIST:
                    arrayList.clear();
                    adapter.notifyDataSetChanged();
                    for(int i=0;i<mDevices.size();i++){
                        Log.i(TAG,mDevices.valueAt(i).getName().toString() + " "+i);
                        arrayList.add(mDevices.valueAt(i).getName().toString());
                        adapter.notifyDataSetChanged();
                        if(!btnIsVisible){
                            workoutbtn.setVisibility(View.VISIBLE);
                            btnIsVisible = true;
                        }
                    }
                    devicesfound.setText("Device(s) Found: "+ mDevices.size());
                    break;
            }
        }
    };

}