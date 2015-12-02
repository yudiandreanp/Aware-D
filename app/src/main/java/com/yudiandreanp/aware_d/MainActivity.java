package com.yudiandreanp.aware_d;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.lang.Integer;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.AudioManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

/**
 * Created by yudiandrean on 10/28/2015.
 * This class does the recognition of the human voice, then convert it into
 * text, and also sends text to TextSpeaker class
 */

public class MainActivity extends ActionBarActivity implements SpeechRecognizerManager.OnResultListener, SpeechRecognizerManager.OnErrorListener {
    private final int CHECK_CODE = 0x1;
    private final int MAX_INDEX = 30;
    private final int MIN_INDEX = 1;
    private boolean ready = false;
    private boolean allowed = false;
    private TextView textInfoText;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ProgressBar progressBar;
    private Intent recognizerIntent;
    private Button buttonDriveNow;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private Button smsButton;
    private ArrayList<Integer> questionIndex, threeQuestionIndex;
    private int count, countThree, errorCount; //counter for index and right-tries of three questions
    private String currentUserAnswer, latitude, longitude, phoneNumber; //holds the spoken answer by the user
    private ArrayList <String> currentQuestionAnswer, firstThreeQuestions; //holds the current question and answer
    private GPSManager currentLocation;
    private SMSManager smsSender;
    private boolean isDriving;
    private DrivingSession currentSession;
    private TextToSpeech tts;
    private SpeechRecognizerManager mSpeechRecognizerManager;
    private TimerTask task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        count = 0;
        countThree = 0;
        currentLocation= new GPSManager(MainActivity.this);
        smsSender = new SMSManager ();
        //createTextSpeaker(); //create the speaker instance
        createTTS();
        textInfoText = (TextView) findViewById(R.id.infoText);
        buttonDriveNow = (Button) findViewById(R.id.btnDrive);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        smsButton = (Button) findViewById(R.id.smsButton);
        progressBar.setVisibility(View.INVISIBLE);
        questionIndex = rangeList(MIN_INDEX, MAX_INDEX); //creates the index for iterating question arraylist
        questionIndex = shuffleIndex(questionIndex);
        threeQuestionIndex = createThreeQuestionIndex();

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        createSpeechRecognizer();

        isDriving = false;
        createTTS();
        ttsAllow(true);
        startInitialQuestion();


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
        if (mSpeechRecognizerManager == null){
            createSpeechRecognizer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

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

//    @Override
//    public void onBeginningOfSpeech() {
//        Log.i(LOG_TAG, "onBeginningOfSpeech");
//        progressBar.setIndeterminate(false);
//        progressBar.setMax(10);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsDestroy();
    }

//    //on dB of speaker's voice changed will change the progress bar
//    @Override
//    public void onRmsChanged(float rmsdB) {
//        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
//        progressBar.setProgress((int) rmsdB);
//    }

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

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

            @Override
            public void onDone(String utteranceId) {
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textInfoText.setText("");
                        triggerUserSpeaking();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
                Log.i("Text Speaker", "Error Speaking");
            }

            @Override
            public void onStart(String utteranceId) {

            }


        });

        currentQuestionAnswer = getQuestionAnswer(option);
        ttsSpeak(currentQuestionAnswer.get(0));
    }

    //checks text-to-speech data
    private void checkTTS() {
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private Calendar updateTime()
    {
        Calendar c = Calendar.getInstance();
        return c;
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
        mSpeechRecognizerManager = new SpeechRecognizerManager(this);
        mSpeechRecognizerManager.setOnResultListener(this);
        mSpeechRecognizerManager.setOnErrorListener(this);
    }



    //creates the speaker instance
    private void createTTS()
    {
        if (tts != null)
        {
            ttsDestroy();
        }

        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    @Override
                    public void onDone(String utteranceId) {
                        Log.i("Text Speaker","Done Speaking");

                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.i("Text Speaker","Error Speaking");
                    }

                    @Override
                    public void onStart(String utteranceId) {
                        Log.i("Text Speaker","Started Speaking");

                    }


                });

                if(status == TextToSpeech.SUCCESS) {
                    // Change this to match your
                    // locale
                    tts.setLanguage(Locale.US);
                    ready = true;

                }

                else
                {
                    ready = false;

                }
            }
        });
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
        //createSpeechRecognizer();
        setProgressVisible();
        //speechListener.startListening(recognizerIntent);
        mSpeechRecognizerManager.startRecognizing();
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
                currentSession = new DrivingSession(MainActivity.this, updateTime());
                dialog.dismiss();
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
                    ttsSpeak(getString(R.string.before_driving1) + "," +
                             getString(R.string.before_driving2) + "," +
                             getString(R.string.before_driving3));

                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                        @Override
                        public void onDone(String utteranceId) {
                            MainActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    createStartInitialDialog();
                                }
                            });

                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.i("Text Speaker", "Error Speaking");
                        }

                        @Override
                        public void onStart(String utteranceId) {

                        }


                    });


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
        if (countThree >= 2) // if the arraylist had been iterated MAX_INDEX times
        {
            threeQuestionIndex = shuffleIndex(threeQuestionIndex);
            countThree = 0;
            index = threeQuestionIndex.get(countThree);
            countThree++;
            return index;
        }
        else
        {
            index = threeQuestionIndex.get(countThree);
            countThree++;
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
    }

    //TTS Methods

    public boolean ttsIsAllowed(){
        return allowed;
    }

    public void ttsAllow(boolean allowed){
        this.allowed = allowed;
    }

    public void ttsSpeak(String text){

        // Speak only if the TTS is ready
        // and the user has allowed speech

        if(ready && allowed) {
            tts.setSpeechRate(0.7f);
            HashMap<String, String> hash = new HashMap<String,String>();
            hash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_NOTIFICATION));
            hash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    "message ID");
            tts.speak(text, TextToSpeech.QUEUE_ADD, hash);

        }
    }

    //access the status of utterance to pass to other class (MainActivity)
    //public boolean ttsIsFinished()
    //{
    //    return finished;
    //}

    //access the tts readiness status
    public boolean ttsIsReady()
    {
        return ready;
    }

    //reset the utterance status to not finished
    //public void ttsSetNotFinished()
    //{
    //    finished = false;
    //}

    public void ttsPause(int duration){
        tts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
    }

    // Free up resources
    public void ttsDestroy(){
        tts.shutdown();
    }

    //Decoupled for SpeechRecognizerManager
    @Override
    public void OnResult(ArrayList<String> results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results;
        String text = "";
        for (String result : matches)
            text += result + "\n";

        textInfoText.setText(matches.get(0));
        currentUserAnswer = matches.get(0);

        Log.i("ThreeTries Tries",Integer.toString(currentSession.getThreeTries()));
        Log.i("ThreeTries Right",Integer.toString(currentSession.getThreeRight()));


        //In initial questions mode
        if (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() < 3)
        {

            if (currentUserAnswer != null) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        speakQuestion(1);
                    }
                    @Override
                    public void onError(String utteranceId) {
                        Log.i("Text Speaker", "Error Speaking");
                    }
                    @Override
                    public void onStart(String utteranceId) {
                    }
                });

                if (checkTrue()){
                    currentSession.incrementThreeRight();
                    if (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() <= 2) {
                        ttsSpeak("You are right, now the next question");
                        textInfoText.setText("RIGHT");
                    }
                    else if  (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2)
                    {
                        //do nothing
                    }

                }
                else {
                    currentSession.incrementThreeTries();
                    ttsSpeak("Wrong, please answer carefully. Now the next question");
                    textInfoText.setText("WRONG");
                }
            }
        }

        //checking if the user can't answer all three
        if (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() >= 3 && currentSession.getThreeRight() <= 2)
        {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    //do nothing
                }

                @Override
                public void onError(String utteranceId) {
                    Log.i("Text Speaker", "Error Speaking");
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });

            ttsSpeak("You cannot answer all three questions, please try again by tapping the 'Drive Now' Button");
            currentSession.resetThree(); //reset the three questions statistics

        }

        //driving state here
        if (!currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2 && currentSession.getThreeRight() > 2)
        {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {


                    task = new TimerTask() {
                        public void run() {
                            speakQuestion(0);
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(task, 9000);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.i("Text Speaker", "Error Speaking");
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });

            if (currentUserAnswer != null) {
                if (checkTrue()) {
                    ttsSpeak("You are right, now wait for next question");
                    currentSession.addUserResults(true);
                    currentSession.addUserProgress(true);
                } else {
                    ttsSpeak("Wrong, please answer carefully. Now wait for the next question");
                    currentSession.addUserResults(false);
                    currentSession.addUserProgress(false);
                }
            }
        }

        //check the user's progress every 6 questions
        if (currentSession.getUserProgress().size() > 1)
        {
            if (currentSession.getUserResults().size() % 6 == 0) {
                int occurrences = Collections.frequency(currentSession.getUserProgress(), false);
                if (occurrences >= 3)
                {
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onDone(String utteranceId) {
                            task = new TimerTask() {
                                public void run() {
                                    double dLatitude;
                                    double dLongitude;

                                    if(currentLocation.canGetLocation())
                                    {
                                        dLatitude = currentLocation.getLatitude();
                                        dLongitude = currentLocation.getLongitude();
                                        Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + dLatitude + "\nLong: " + dLongitude, Toast.LENGTH_LONG).show();
                                        latitude = String.valueOf(dLatitude);
                                        longitude = String.valueOf(dLongitude);
                                        phoneNumber = "+6289693959600";

                                    }
                                    else
                                    {
                                        currentLocation.showSettingsAlert();
                                    }

                                    MainActivity.this.runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            task = new TimerTask() {
                                                public void run() {
                                                    smsSender.sendLocationSMS(phoneNumber, latitude, longitude);
                                                }
                                            };
                                            Timer timer2 = new Timer();
                                            timer2.schedule(task, 5000);

                                        }
                                    });
                                }
                            };
                            Timer timer = new Timer();
                            timer.schedule(task, 10);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.i("Text Speaker", "Error Speaking");
                        }

                        @Override
                        public void onStart(String utteranceId) {
                        }
                    });
                    ttsSpeak("You exceeded the wrong answer limit. Please stop driving now. I am going message " +
                            "your chosen contact, containing your current location");

                }
            }
        }


        //if user answer all 3 correctly
        if (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2 && currentSession.getThreeRight() > 2)
        {
            currentSession.setBooleanStatusThree();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {

                    task = new TimerTask(){
                        public void run() {
                            speakQuestion(0);
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(task, 9000);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.i("Text Speaker", "Error Speaking");
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });

            ttsSpeak("You have correctly answered 3 questions to drive, now you can drive safely." +
                    "I will ask you questions every 5 minutes while you are driving. Please drive carefully and keep" +
                    "focus on the road. Thank you for your cooperation");
        }

    }

    @Override
    public void OnError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "OnError " + errorMessage);
        textInfoText.setText(errorMessage);

        if (errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            //in the initial three questions error

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        errorCount++;
                        task = new TimerTask() {
                            public void run() {
                                MainActivity.this.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        mSpeechRecognizerManager.destroy();
                                        createSpeechRecognizer();
                                        ttsDestroy();
                                        createTTS();
                                        task = new TimerTask() {
                                            public void run() {
                                                if (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() < 3)
                                                {speakQuestion(1);}
                                                if (!currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2 && currentSession.getThreeRight() > 2)
                                                {speakQuestion(0);}
                                            }
                                        };
                                        Timer timer2 = new Timer();
                                        timer2.schedule(task, 5000);

                                    }
                                });
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(task, 10);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.i("First", "Error Speaking");
                    }

                    @Override
                    public void onStart(String utteranceId) {
                    }
                });

                ttsSpeak("We encountered an error, please make sure that your network connection if fine. I will ask you another question in 5 seconds");




        }

        if (errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            //in the initial three questions error

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    errorCount++;
                    task = new TimerTask() {
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    mSpeechRecognizerManager.destroy();
                                    createSpeechRecognizer();
                                    ttsDestroy();
                                    createTTS();
                                    task = new TimerTask() {
                                        public void run() {
                                            if (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() < 3)
                                            {speakQuestion(1);}
                                            if (!currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2 && currentSession.getThreeRight() > 2)
                                            {speakQuestion(0);}
                                        }
                                    };
                                    Timer timer2 = new Timer();
                                    timer2.schedule(task, 5000);

                                }
                            });
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(task, 10);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.i("First", "Error Speaking");
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });

            ttsSpeak("I didn't get what you said, please wait for the next question");

        }

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

}
