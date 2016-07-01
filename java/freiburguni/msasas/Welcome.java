package freiburguni.msasas;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class Welcome extends AppCompatActivity {

    private BluetoothDevice device;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Bundle mainmenuData = getIntent().getExtras();
        if(mainmenuData == null){
            return;
        }
        device = getIntent().getExtras().getParcelable("btdevice");
    }

    public void Challenge(View view){
        Intent i = new Intent(this,PowerChallenge.class);
        i.putExtra("btdevice",device);
        startActivity(i);
    }

    public void HighScores(View view){
        Intent i = new Intent(this,HighScores.class);
        startActivity(i);
    }

}
