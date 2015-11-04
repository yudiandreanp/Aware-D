package com.yudiandreanp.aware_d;

import java.util.ArrayList;
import java.util.Locale;

import android.database.Cursor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


/**
 * Created by yudiandrean on 10/28/2015.
 * This class does the recognition of the human voice, then convert it into
 * text, and also sends text to TextSpeaker class
 *
 */
public class RecognizerManager extends Activity implements RecognitionListener {

    private final int CHECK_CODE = 0x1;
    private final int LONG_DURATION = 5000;
    private final int SHORT_DURATION = 1200;
    private TextView txtSpeechInput;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
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

        textSpeaker = new TextSpeaker(this);

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        //btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        toggleButton = (ToggleButton) findViewById(R.id.btnSpeak2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.INVISIBLE);
        // hide the action bar
        //getActionBar().hide();
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        inputSpeak = (EditText) findViewById(R.id.testText);
        buttonSpeak = (Button) findViewById(R.id.buttonSpeak);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SECURE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    setProgressVisible();
                    speech.startListening(recognizerIntent);
                } else {
                    setProgressInvisible();
                    speech.stopListening();
                }
            }
        });

        testSpeak = (ToggleButton) findViewById(R.id.toggleButton);

        toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked) {
                    textSpeaker.allow(true);
                    textSpeaker.speak(getString(R.string.start_speaking));
                } else {
                    textSpeaker.speak(getString(R.string.stop_speaking));
                    textSpeaker.allow(false);
                }
            }
        };

        testSpeak.setOnCheckedChangeListener(toggleListener);

        buttonSpeak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if ((!inputSpeak.getText().toString().equals(""))) {
                    checkTTS();
                    speakText();
                } else {
                    checkTTS();
                    speakText();
                }


            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) {
            speech.destroy();
            Log.i(LOG_TAG, "destroy");
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        progressBar.setIndeterminate(true);
        toggleButton.setChecked(false);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        txtSpeechInput.setText(errorMessage);
        toggleButton.setChecked(false);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches)
            text += result + "\n";

        txtSpeechInput.setText(matches.get(0));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        textSpeaker.destroy();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        progressBar.setProgress((int) rmsdB);
    }

    private void setProgressVisible() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
    }

    private void setProgressInvisible() {
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void speakText() {
        //textSpeaker.speak(inputSpeak.getText().toString());
        textSpeaker.speak(readQuestion());
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void checkTTS() {
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    private String readQuestion() {
        DBTestAdapter mDbHelper = new DBTestAdapter(RecognizerManager.this);
        mDbHelper.createDatabase();
        mDbHelper.open();
        String result ="";

        Cursor testdata = mDbHelper.getTestData();
        //while (testdata.moveToNext()) {
            for (int i = 0; i < testdata.getColumnCount(); i++) {
                result = testdata.getString(i);
                Log.i("test select query", result);
          //  }
        }


        mDbHelper.close();
        return result;
    }

}
