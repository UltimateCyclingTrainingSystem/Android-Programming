package freiburguni.msasas;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class Welcome extends AppCompatActivity {

    private BluetoothDevice Powerdevice;
    private BluetoothDevice HeartRatedevice;
    private static final String TAG = "BluetoothGattActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
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

    }

    public void Challenge(View view){
        Intent i = new Intent(this,PowerChallenge.class);
        if(getIntent().hasExtra("heart")){
            i.putExtra("heart",HeartRatedevice);
        }

        if(getIntent().hasExtra("power")){
            i.putExtra("power",Powerdevice);
        }
        startActivity(i);
    }

    public void HighScores(View view){
        Intent i = new Intent(this,HighScores.class);
        startActivity(i);
    }

}