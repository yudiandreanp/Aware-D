package com.yudiandreanp.aware_d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.lang.Integer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.common.collect.Lists;


/**
 * Created by yudiandrean on 10/28/2015.
 * This class does the recognition of the human voice, then convert it into
 * text, and also sends text to TextSpeaker class
 *
 */
public class MainActivity extends Activity implements RecognitionListener {
    private final int CHECK_CODE = 0x1;
    private final int MAX_INDEX = 21;
    private final int MIN_INDEX = 1;
    private final int LONG_DURATION = 5000;
    private final int SHORT_DURATION = 1200;
    private TextView textInfoText;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ProgressBar progressBar;
    private SpeechRecognizer speechListener = null;
    private Intent recognizerIntent;
    private Button buttonDriveNow;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private TextSpeaker textSpeaker;
    private Button buttonSpeak, smsButton;
    private ArrayList<Integer> questionIndex, threeQuestionIndex;
    private int count, countThree; //counter for index
    private String currentUserAnswer; //holds the spoken answer by the user
    private ArrayList <String> currentQuestionAnswer, firstThreeQuestions; //holds the current question and answer
    private GPSManager currentLocation;
    private SMSManager smsSender;
    private ToggleButton allowSpeakButton;
    private boolean isDriving;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        count = 0;
        countThree = 0;
        currentLocation= new GPSManager(MainActivity.this);
        smsSender = new SMSManager ();
        createTextSpeaker(); //create the speaker instance
        textInfoText = (TextView) findViewById(R.id.infoText);
        buttonDriveNow = (Button) findViewById(R.id.btnDrive);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        smsButton = (Button) findViewById(R.id.smsButton);
        progressBar.setVisibility(View.INVISIBLE);
        questionIndex = rangeList(MIN_INDEX, MAX_INDEX); //creates the index for iterating question arraylist
        questionIndex = shuffleIndex(questionIndex);
        threeQuestionIndex = createThreeQuestionIndex();

        // hide the action bar
        //getActionBar().hide();

        createSpeechRecognizer();
        recognizerIntent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SECURE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-UK");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        buttonSpeak = (Button) findViewById(R.id.buttonSpeak);
        buttonSpeak.setVisibility(View.INVISIBLE);
        allowSpeakButton = (ToggleButton) findViewById(R.id.allowSpeak);
        CompoundButton.OnCheckedChangeListener toggleListener;

        isDriving = false;
        startInitialQuestion();

        toggleListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked) {
                    createTextSpeaker();
                    buttonSpeak.setVisibility(View.VISIBLE);
                    textSpeaker.allow(true);
                } else {
                    buttonSpeak.setVisibility(View.INVISIBLE);
                    textSpeaker.destroy();
                }
            }
        };

        allowSpeakButton.setOnCheckedChangeListener(toggleListener);

        buttonSpeak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                checkTTS();
                speakQuestion(0);

                //used to make sure the tts has done speaking
                if ((!textSpeaker.isFinished()) && textSpeaker.isReady())
                {
                    while(!textSpeaker.isFinished())
                    {
                        if (textSpeaker.isFinished()) {
                            break;
                        }
                    }
                }

                if (textSpeaker.isFinished()) {
                    triggerUserSpeaking();
                    textSpeaker.setNotFinished();
                }

            }
        });

        smsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                double dLatitude;
                double dLongitude;

                if(currentLocation.canGetLocation())
                {
                    dLatitude = currentLocation.getLatitude();
                    dLongitude = currentLocation.getLongitude();
                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + dLatitude + "\nLong: " + dLongitude, Toast.LENGTH_LONG).show();
                    String latitude = String.valueOf(dLatitude);
                    String longitude = String.valueOf(dLongitude);
                    String phoneNumber = "+6289693959600";
                    smsSender.sendLocationSMS(phoneNumber, latitude, longitude);
                }
                else
                {
                    currentLocation.showSettingsAlert();
                }


                }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        if (speechListener == null){
            createSpeechRecognizer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub

        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                ab.setTitle("Quit Aware-D");
                ab.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    MainActivity.this.finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

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
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "OnError " + errorMessage);
        textInfoText.setText(errorMessage);
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

        textInfoText.setText(matches.get(0));
        currentUserAnswer = matches.get(0);

        if (currentUserAnswer != null) {
            if (checkTrue()) {
                textInfoText.setText("RIGHT");
            } else {
                textInfoText.setText("WRONG");
            }
        }

        speechListener.destroy();
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
    private void speakQuestion(int option) {
        //textSpeaker.speak(inputSpeak.getText().toString());
        currentQuestionAnswer = getQuestionAnswer(option);
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
    private ArrayList<String> getQuestionAnswer(int option)
    {
        int index;
        DBReader mDbHelper = new DBReader(MainActivity.this);
        mDbHelper.createDatabase();
        mDbHelper.open();
        ArrayList <String> result = new ArrayList <>();

        if(option == 0) {
            index = getCurrentIndex();
        }
        else
        {
            index = getCurrentThreeQuestionIndex();
        }

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
        speechListener = SpeechRecognizer.createSpeechRecognizer(this);
        speechListener.setRecognitionListener(this);
    }

    //creates the speaker instance
    private void createTextSpeaker()
    {
        if (textSpeaker != null)
        {textSpeaker.destroy();}
        textSpeaker = new TextSpeaker(this);
    }

    /**
     * creates an arraylist with the range of minimum index to the max index of questions in the database
     */
    public static ArrayList<Integer> rangeList(int min, int max) {
        ArrayList<Integer> list = Lists.newArrayList();
        for (int i = min; i <= max; i++) {
            list.add(i);
        }

        return list;
    }

    /**
     * Shuffles the index of questions arraylist when the arraylist
     * interator reaches the end of arraylist, or the first time the arraylist is created
     */
    private ArrayList<Integer> shuffleIndex(ArrayList<Integer> arrayList)
        {
            long seed = System.nanoTime();
            Collections.shuffle(arrayList, new Random(seed));
            return arrayList;
        }

    /**
     * Gets the current index of the when-driving questions arraylist
     * @return int index of question
     */
    private int getCurrentIndex()
    {
        int index;
        if (count == MAX_INDEX) // if the arraylist had been iterated MAX_INDEX times
        {
            questionIndex = shuffleIndex(questionIndex);
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

    /**
     * Algorithm to check if the user's answer is true or not by checking the word
     * of the user in the answer database
     * @return boolean
     */
    private boolean checkTrue()
    {

        if (currentUserAnswer.toLowerCase().matches(".*\\b" + currentQuestionAnswer.get(1).toLowerCase() + "\\b.*"))
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

    /**
     * Triggers the RecognitionListener, prompting the user to speak
     */
    private void triggerUserSpeaking() {
        createSpeechRecognizer();
        setProgressVisible();
        speechListener.startListening(recognizerIntent);
    }

    /**
     * This method starts the dialog prompting the user if he/she is
     * ready to answer the initial questions
     */
    private void createStartInitialDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Start Answering?");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startQuestionBeforeDrive();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * This method is used to start the click listener when the user wants to drive
     * This trigger the first 3 questions for the user
     */
    private void startInitialQuestion()
    {
        buttonDriveNow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!isDriving) {
                    textSpeaker.speak(getString(R.string.before_driving1));
                    textSpeaker.pause(SHORT_DURATION);
                    textSpeaker.speak(getString(R.string.before_driving2));
                    textSpeaker.pause(SHORT_DURATION);
                    textSpeaker.speak(getString(R.string.before_driving3));
                    Log.i("Blom Kelar", "Kok");
                    new CountDownTimer(14000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            //nothing to do here *flies away*
                        }

                        public void onFinish() {
                            createStartInitialDialog();
                            Log.i("Sudah Kelar", "Kok");
                        }
                    }.start();


                    //ArrayList<String> arraySpeak = new ArrayList<>();
                    //arraySpeak.add(getString(R.string.before_driving1));
                    //arraySpeak.add(getString(R.string.before_driving2));
                    //arraySpeak.add(getString(R.string.before_driving3));

                }
            }
        });
    }

    /**
     * This method is used to generate the first three questions index
     * that the driver must correctly answer
     * @return ArrayList returnArray
     */
    private ArrayList<Integer> createThreeQuestionIndex()
    {
        ArrayList<Integer> returnArray = new ArrayList<>();
        Random r = new Random();
        //adds up random question index into the threequestion array
        for (int i=returnArray.size(); i < 4; i++)
        {
            returnArray.add(r.nextInt(MAX_INDEX - MIN_INDEX) + MIN_INDEX);
        }
        returnArray = shuffleIndex(returnArray);
        return returnArray;
    }

    /**
     * Gets the current index of the when-driving questions arraylist
     * @return int index of question
     */
    private int getCurrentThreeQuestionIndex()
    {
        int index;
        if (countThree == 2) // if the arraylist had been iterated MAX_INDEX times
        {
            threeQuestionIndex = shuffleIndex(threeQuestionIndex);
            count = 0;
            index = threeQuestionIndex.get(count);
            count++;
            return index;
        }
        else
        {
            index = threeQuestionIndex.get(count);
            count++;
            return index;
        }

    }

    /**
     * The method to start the 3 random questions before the driver
     * is allowed to drive
     */
    private void startQuestionBeforeDrive()
    {
        Log.i("Ayoayo", "Mulaii");
        checkTTS();
        speakQuestion(1);

        //used to make sure the tts has done speaking
        if ((!textSpeaker.isFinished()) && textSpeaker.isReady())
        {
            while(!textSpeaker.isFinished())
            {
                if (textSpeaker.isFinished()) {
                    break;
                }
            }
        }

        if (textSpeaker.isFinished()) {
            triggerUserSpeaking();
            textSpeaker.setNotFinished();
        }

    }
}
