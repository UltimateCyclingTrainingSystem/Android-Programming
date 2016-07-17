package freiburguni.msasas;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

public class HighScores extends AppCompatActivity {

    private BluetoothDevice Powerdevice;
    private BluetoothDevice HeartRatedevice;
    private String cyclerName;
    private float avgPower;
    private Button clearbtn;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private static ArrayList<String> arrayList = new ArrayList<String>();;
    private static final String TAG = "BluetoothGattActivity";
    private Intent intent;
    private static Set<String> nameList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_scores);
        Bundle mainmenuData = getIntent().getExtras();

        if (mainmenuData == null) {
            Log.i(TAG,"Wala3");
            return;
        }
        if(getIntent().hasExtra("heart")){
            HeartRatedevice = getIntent().getExtras().getParcelable("heart");
            intent = new Intent(this,Welcome.class);

        }

        if(getIntent().hasExtra("power")){
            Powerdevice = getIntent().getExtras().getParcelable("power");
            intent = new Intent(this,Welcome.class);
        }

        if(getIntent().hasExtra("nothing")){
            intent = new Intent(this,MainActivitycxc.class);
        }

        try {
            FileInputStream input = openFileInput("lines.txt"); // Open input stream
            DataInputStream din = new DataInputStream(input);
            int sz = din.readInt(); // Read line count
            arrayList.clear();
            for (int i = 0; i < sz; i++) { // Read lines
                String line = din.readUTF();
                arrayList.add(line);
            }
            din.close();
        }
        catch (IOException exc) { exc.printStackTrace(); }

        clearbtn = (Button) findViewById(R.id.btnclearscores);
        list = (ListView) findViewById(R.id.highscorelist);

        // Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position,convertView,parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.BLACK);
                tv.setTextSize(25f);
                return view;
            }
        };
        // Here, you set the data in your ListView
        list.setAdapter(adapter);

        if(getIntent().hasExtra("name")){
            Log.i(TAG,"Yew");
            cyclerName = getIntent().getExtras().getString("name");
            if(cyclerName.isEmpty()){
                cyclerName = "No name";
            }
            avgPower = getIntent().getExtras().getFloat("avgpower");
            Log.i(TAG,cyclerName+ ": " + String.valueOf(avgPower));
            arrayList.add(cyclerName + "    " +String.valueOf(Math.round(avgPower)));
            SharedPreferences sharedPreferences = getSharedPreferences("AveragePower", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit(); // object to edit the file
            editor.apply();
            String name = sharedPreferences.getString("fieh","");
            arrayList.add(name);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            //Modes: MODE_PRIVATE, MODE_WORLD_READABLE, MODE_WORLD_WRITABLE
            FileOutputStream output = openFileOutput("lines.txt",MODE_WORLD_READABLE);
            DataOutputStream dout = new DataOutputStream(output);
            dout.writeInt(arrayList.size()); // Save line count
            for(String line : arrayList) // Save lines
                dout.writeUTF(line);
            dout.flush(); // Flush stream ...
            dout.close(); // ... and close.
        }
        catch (IOException exc) { exc.printStackTrace(); }
    }

    public void ClearScores(View view) {
        // this line adds the data of your EditText and puts in your array
        arrayList.clear();
        // next thing you have to do is check if your adapter has changed
        adapter.notifyDataSetChanged();
        SharedPreferences sharedPreferences = getSharedPreferences("AveragePower", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

    }


    @Override
    public void onBackPressed() {

        if(getIntent().hasExtra("heart")){
            intent.putExtra("heart",HeartRatedevice);
        }

        if(getIntent().hasExtra("power")){
            intent.putExtra("power",Powerdevice);
        }
        startActivity(intent);

    }
}