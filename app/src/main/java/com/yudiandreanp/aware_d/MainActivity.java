package com.yudiandreanp.aware_d;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private final int CHECK_CODE = 0x1;
    private final int LONG_DURATION = 5000;
    private final int SHORT_DURATION = 1200;
    private TextView txtSpeechInput;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ProgressBar progressBar;
    private Intent recognizerIntent;
    private ToggleButton toggleButton;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private TextSpeaker textSpeaker;
    private ToggleButton testSpeak;
    private EditText inputSpeak;
    private CompoundButton.OnCheckedChangeListener toggleListener;
    private Button buttonSpeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        //btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        toggleButton = (ToggleButton) findViewById (R.id.btnSpeak2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.INVISIBLE);
        // hide the action bar
        //getActionBar().hide();
        inputSpeak = (EditText) findViewById(R.id.testText);
        buttonSpeak = (Button) findViewById(R.id.buttonSpeak);


        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    setProgressVisible(); //what have to be done here
                } else {
                    setProgressInvisible(); //what else have to be done
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void readDatabase()
    {
        DBTestAdapter mDbHelper = new DBTestAdapter(MainActivity.this);
        mDbHelper.createDatabase();
        mDbHelper.open();

        Cursor testdata = mDbHelper.getTestData();
        while(testdata.moveToNext()) {
            for (int i = 0; i < testdata.getColumnCount(); i++) {
                String result = testdata.getString(i);
                Log.i("test select query", result);
            }
        }


        mDbHelper.close();
    }

    private void setProgressVisible()
    {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
    }

    private void setProgressInvisible()
    {
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
    }
}
