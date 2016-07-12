package freiburguni.msasas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class HighScores extends AppCompatActivity {

    private Button clearbtn;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_scores);
        clearbtn = (Button) findViewById(R.id.btnclearscores);
        list = (ListView) findViewById(R.id.highscorelist);
        arrayList = new ArrayList<String>();

        // Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);

        // Here, you set the data in your ListView
        list.setAdapter(adapter);

    }
    public void ClearScores(View view) {
        // this line adds the data of your EditText and puts in your array
        arrayList.add("Hello");
        // next thing you have to do is check if your adapter has changed
        adapter.notifyDataSetChanged();

    }

}