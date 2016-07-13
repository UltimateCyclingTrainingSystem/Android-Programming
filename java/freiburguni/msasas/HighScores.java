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
    private static ArrayList<String> arrayList;
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
        }

        if(getIntent().hasExtra("power")){
            Powerdevice = getIntent().getExtras().getParcelable("power");
        }

        clearbtn = (Button) findViewById(R.id.btnclearscores);
        list = (ListView) findViewById(R.id.highscorelist);
        arrayList = new ArrayList<String>();
        intent = new Intent(this,Welcome.class);
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
            avgPower = getIntent().getExtras().getFloat("avgPower");
            Log.i(TAG,cyclerName+ ": " + String.valueOf(avgPower));

           // SharedPreferences sharedPreferences = getSharedPreferences("MyData", Context.MODE_PRIVATE);
            //SharedPreferences.Editor  editor = sharedPreferences.edit();
          //  editor.putString("name", cyclerName+ " " + String.valueOf((avgPower)));

            arrayList.add(cyclerName + "    " +String.valueOf(avgPower));
            adapter.notifyDataSetChanged();
            //nameList = new Set(arrayList);
           // Log.i(TAG,"size " +String.valueOf(nameList.size()));
            //editor.putStringSet("name",(Set)arrayList);
           // editor.commit();
        }

       // SharedPreferences sharedPreferences = getSharedPreferences("MyData", Context.MODE_PRIVATE);
        //arrayList = sharedPreferences.getStringSet("name",(Set)arrayList);






    }
    public void ClearScores(View view) {
        // this line adds the data of your EditText and puts in your array
        arrayList.clear();
        // next thing you have to do is check if your adapter has changed
        adapter.notifyDataSetChanged();

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