package com.yudiandreanp.aware_d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.lang.Integer;
import android.database.Cursor;
import android.os.CountDownTimer;
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

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;


/**
 * Created by yudiandrean on 10/28/2015.
 * This class does the recognition of the human voice, then convert it into
 * text, and also sends text to TextSpeaker class
 *
 */
public class MainActivity extends Activity implements RecognitionListener {

    private final int CHECK_CODE = 0x1;
    private final int MAX_INDEX = 11;
    private final int MIN_INDEX = 1;
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
    private ArrayList<Integer> questionIndex;
    private int count; //counter for index
    private String currentUserAnswer; //holds the spoken answer by the user
    private ArrayList <String> currentQuestionAnswer; //holds the current question and answer


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createTextSpeaker();
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        //btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        toggleButton = (ToggleButton) findViewById(R.id.btnSpeak2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.INVISIBLE);
        questionIndex = rangeList(MIN_INDEX, MAX_INDEX);
        shuffleIndex();
        count = 0;
        // hide the action bar
        //getActionBar().hide();
        createSpeechRecognizer();
        recognizerIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SECURE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        inputSpeak = (EditText) findViewById(R.id.testText);
        buttonSpeak = (Button) findViewById(R.id.buttonSpeak);
        buttonSpeak.setVisibility(View.INVISIBLE);

        //start speaking
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    createSpeechRecognizer();
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
                    createTextSpeaker();
                    buttonSpeak.setVisibility(View.VISIBLE);
                    textSpeaker.allow(true);
                    textSpeaker.speak(getString(R.string.start_speaking));
                } else {
                    buttonSpeak.setVisibility(View.INVISIBLE);
                    textSpeaker.speak(getString(R.string.stop_speaking));
                    textSpeaker.destroy();
                                    }
            }
        };

        testSpeak.setOnCheckedChangeListener(toggleListener);

        buttonSpeak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if ((!inputSpeak.getText().toString().equals(""))) {
                    checkTTS();
                    speakQuestion(); //speaks random questions
                    new CountDownTimer(3000, 1000) {

                        public void onTick(long millisUntilFinished) {
                                                    }

                        public void onFinish() {
                            triggerUserSpeaking();
                        }
                    }.start();

                } else {
                    checkTTS();
                    speakQuestion();
                    new CountDownTimer(3000, 1000) {

                        public void onTick(long millisUntilFinished) {
                        }

                    public void onFinish() {
                        triggerUserSpeaking();
                    }
                    }.start();
                }
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        if (speech == null){
            createSpeechRecognizer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) {
            speech.destroy();
            Log.i(LOG_TAG, "destroy speech recognizer");
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
        Log.d(LOG_TAG, "OnError " + errorMessage);
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
        currentUserAnswer = matches.get(0);

        if (currentUserAnswer != null) {
            if (checkTrue()) {
                txtSpeechInput.setText("RIGHT");
            } else {
                txtSpeechInput.setText("WRONG");
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        textSpeaker.destroy();
    }

    //on dB of speaker's voice changed will change the progress bar
    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        progressBar.setProgress((int) rmsdB);
    }

    //set visible the progress bar
    private void setProgressVisible() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
    }

    //set invisible for progress bar
    private void setProgressInvisible() {
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
    }

    //speaks random questions
    private void speakQuestion() {
        //textSpeaker.speak(inputSpeak.getText().toString());
        currentQuestionAnswer = getQuestionAnswer();
        textSpeaker.speak(currentQuestionAnswer.get(0));
    }

    //checks text-to-speech data
    private void checkTTS() {
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }


    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
//            case SpeechRecognizer.ERROR_CLIENT:
//                message = "Client side error";
//                break;
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


    //reads question on randomized value
    private ArrayList getQuestionAnswer()
    {
        DBTestAdapter mDbHelper = new DBTestAdapter(MainActivity.this);
        mDbHelper.createDatabase();
        mDbHelper.open();
        ArrayList <String> result = new ArrayList <>();
        int index = getCurrentIndex();

        Cursor data = mDbHelper.getQuestion(index);  //the query for question
        //while (testdata.moveToNext()) {
            for (int i = 0; i < data.getColumnCount(); i++) {
                result.add(data.getString(i));
                Log.i("question query", result.get(0));
          //  }
        }

        data = mDbHelper.getAnswer(index); //query for answer
        //while (testdata.moveToNext()) {
        for (int i = 0; i < data.getColumnCount(); i++) {
            result.add(data.getString(i));
            Log.i("answer query", result.get(1));
            //  }
        }


        mDbHelper.close();
        return result;
    }

    //creates the speech recognition instance
    private void createSpeechRecognizer()
    {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
    }

    //creates the speaker instance
    private void createTextSpeaker()
    {
        if (textSpeaker != null)
        {textSpeaker.destroy();}
        textSpeaker = new TextSpeaker(this);
    }

    //creates an arraylist with the range of minimum index to the max index of questions in the database
    public static ArrayList<Integer> rangeList(int min, int max) {
        ArrayList<Integer> list = Lists.newArrayList();
        for (int i = min; i <= max; i++) {
            list.add(i);
        }

        return list;
    }

    private void shuffleIndex()
        {
            long seed = System.nanoTime();
            Collections.shuffle(questionIndex, new Random(seed));
        }

    private int getCurrentIndex()
    {
        int index;
        if (count == MAX_INDEX) // if the arraylist had been iterated MAX_INDEX times
        {
            shuffleIndex();
            count = 0;
            index = questionIndex.get(count);
            count++;
            return index;
        }
        else
        {
            index = questionIndex.get(count);
            count++;
            return index;
        }

    }

    private boolean checkTrue()
    {

        if (currentUserAnswer.toLowerCase().equals(currentQuestionAnswer.get(1).toLowerCase()))
        {
            Log.i(LOG_TAG, "true");
            Log.i(LOG_TAG, currentQuestionAnswer.get(0));
            Log.i(LOG_TAG, currentQuestionAnswer.get(1));
            Log.i(LOG_TAG, currentUserAnswer);
            return true;
        }
        else
        {
            Log.i(LOG_TAG, "wrong");
            Log.i(LOG_TAG, currentQuestionAnswer.get(0));
            Log.i(LOG_TAG, currentQuestionAnswer.get(1));
            Log.i(LOG_TAG, currentUserAnswer);
            return false;
        }
    }

    private void triggerUserSpeaking() {
        createSpeechRecognizer();
        setProgressVisible();
        speech.startListening(recognizerIntent);
    }
}
