package com.yudiandreanp.aware_d;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.lang.Integer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ParseException;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

/**
 * Created by yudiandrean on 10/28/2015.
 * This class does the recognition of the human voice, then convert it into
 * text, and also sends text to TextSpeaker class
 */

public class MainActivity extends AppCompatActivity implements SpeechRecognizerManager.OnResultListener, SpeechRecognizerManager.OnErrorListener, SpeechRecognizerManager.OnRmsChangedListener {
    private final int CHECK_CODE = 0x1;
    private final int MAX_INDEX = 30;
    private int currentIndex, count_total, count_wrong;
    private final int MIN_INDEX = 1;
    private boolean ready = false;
    private boolean allowed = false;
    private TextView textInfoText, textButtonDriveText;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ProgressBar progressBar;
    private Intent recognizerIntent;
    private DBReader mDbHelper;
    private Button buttonDriveNow;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private Button smsButton;
    private ArrayList<Integer> questionIndex, threeQuestionIndex;
    private int count, countThree, errorCount; //counter for index and right-tries of three questions
    private String currentUserAnswer, latitude, longitude, phoneNumber, m_text; //holds the spoken answer by the user
    private ArrayList <String> currentQuestionAnswer; //holds the current question and answer
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

        task = new TimerTask() {
            public void run() {

            }
        };

        count = 0;
        countThree = 0;
        currentLocation= new GPSManager(MainActivity.this);
        smsSender = new SMSManager ();
        //createTextSpeaker(); //create the speaker instance
        createTTS();
        textInfoText = (TextView) findViewById(R.id.infoText);
        textButtonDriveText = (TextView) findViewById(R.id.buttonDriveText);
        textButtonDriveText.setText("Tap to Start Driving");
        buttonDriveNow = (Button) findViewById(R.id.btnDrive);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
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
        setDriveButtonListenerInitial();

        if(!currentLocation.canGetLocation())
        {
            currentLocation.showSettingsAlert();
        }

        mDbHelper = new DBReader(MainActivity.this);
        mDbHelper.createDatabase();
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsDestroy();
    }



    //set visible the progress bar
    private void setProgressVisible() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textInfoText.setText(currentQuestionAnswer.get(0));
            }
        });

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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.set_users_name:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Username");
                // Set up the input
                final EditText input = new EditText(this);
                // Specify the type of input expected;
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_text = input.getText().toString();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
                return true;

            case R.id.set_contacts:
                // showdelete();
                final AlertDialog.Builder alert = new AlertDialog.Builder(this);

                LinearLayout lila1= new LinearLayout(this);
                lila1.setOrientation(LinearLayout.VERTICAL);
                final EditText input_name = new EditText(this);
                final EditText input_number = new EditText(this);
                input_name.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                input_number.setInputType(InputType.TYPE_CLASS_PHONE);
                input_name.setHint("Contact Name...");
                input_number.setHint("Phone Number...");
                lila1.addView(input_name);
                lila1.addView(input_number);
                alert.setView(lila1);
                alert.setTitle("New Contact");

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name_contact = input_name.getText().toString().trim();
                        String number_contact = input_number.getText().toString().trim();
                        Toast.makeText(getApplicationContext(), name_contact + " " + number_contact, Toast.LENGTH_SHORT).show();
                    }
                });
                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        });
                alert.show();
                return true;

            case R.id.stats:
                Intent mainIntent = new Intent(MainActivity.this,StatisticsActivity.class);
                MainActivity.this.startActivity(mainIntent);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Calendar updateTime()
    {
        return Calendar.getInstance();
    }


    //reads question on randomized value
    private ArrayList<String> getQuestionAnswer(int option)
    {
        DBReader mDbHelper = new DBReader(MainActivity.this);
        mDbHelper.createDatabase();
        mDbHelper.open();
        ArrayList <String> result = new ArrayList <>();

        if(option == 0) {
            currentIndex = getCurrentIndex();
        }
        else
        {
            currentIndex = getCurrentThreeQuestionIndex();
        }

        Cursor data = mDbHelper.getQuestion(currentIndex);  //the query for question
        //while (testdata.moveToNext()) {
            for (int i = 0; i < data.getColumnCount(); i++) {
                result.add(data.getString(i));
                Log.i("question query", result.get(0));
          //  }
        }

        data = mDbHelper.getAnswer(currentIndex); //query for answer
        //while (testdata.moveToNext()) {
        for (int i = 0; i < data.getColumnCount(); i++) {
            result.add(data.getString(i));
            Log.i("answer query", result.get(1));
            //  }
        }

        //mDbHelper.close();
        return result;
    }

    //creates the speech recognition instance
    private void createSpeechRecognizer()
    {
        mSpeechRecognizerManager = new SpeechRecognizerManager(this);
        mSpeechRecognizerManager.setOnResultListener(this);
        mSpeechRecognizerManager.setOnErrorListener(this);
        mSpeechRecognizerManager.setOnRmsChangedListener(this);
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
     * This method is used to generate the first three questions index
     * that the driver must correctly answer
     * @return ArrayList returnArray
     */
    private ArrayList<Integer> createThreeQuestionIndex()
    {
        ArrayList<Integer> returnArray = new ArrayList<>();
        Random r = new Random();
        //adds up random question index into the threequestion array
        for (int i=0; i < 3; i++)
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
        int indexThree;
        if (countThree >= 3) // if the arraylist had been iterated 3 times
        {
            threeQuestionIndex = createThreeQuestionIndex();
            countThree = 0;
            indexThree = threeQuestionIndex.get(countThree);
            countThree++;
            return indexThree;
        }
        else
        {
            indexThree = threeQuestionIndex.get(countThree);
            countThree++;
            return indexThree;
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
        //set the button to stop button
        setDriveButtonListenerStopQuestions();
        textButtonDriveText.setText("Tap to Stop Questions");
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

    public boolean ttsIsReady()
    {
        return ready;
    }

    public void ttsPause(int duration) {
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
        setProgressInvisible();
        ArrayList<String> matches = results;
        String text = "";
        for (String result : matches)
            text += result + "\n";

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
                        ttsSpeak(getString(R.string.right));
                        textInfoText.setText(getString(R.string.right));
                    }
                    else if  (currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2)
                    {
                        //do nothing
                    }

                }
                else {
                    currentSession.incrementThreeTries();
                    ttsSpeak("Wrong, please answer carefully. Now the next question");
                    textInfoText.setText("Please answer carefully");
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
            setDriveButtonListenerInitial();
            currentSession.setEndTime(updateTime());
            currentSession.resetThree(); //reset the three questions statistics
            mDbHelper.insertStats(0, String.valueOf(currentSession.getStartTimeMillis()), 0, 0, String.valueOf(System.currentTimeMillis()));
            //TODO
            //TODO
        }

        //driving state here
        if (!currentSession.getBooleanStatusThree() && currentSession.getThreeTries() > 2 && currentSession.getThreeRight() > 2)
        {
            setDriveButtonListenerStopDriving();
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
                    ttsSpeak(getString(R.string.right));
                    textInfoText.setText(getString(R.string.right));
                    currentSession.addUserResults(true);
                    currentSession.addUserProgress(true);
                    currentSession.addUserStats(currentIndex, currentUserAnswer);
                } else {
                    ttsSpeak(getString(R.string.wrong));
                    textInfoText.setText(getString(R.string.wrong));
                    currentSession.addUserResults(false);
                    currentSession.addUserProgress(false);
                    currentSession.addUserStats(currentIndex, currentUserAnswer);
                    //TODO put the currentSession into database
                }
            }
        }

        //check the user's progress every 6 questions
        if (currentSession.getUserProgress().size() > 1)
        {
            if (currentSession.getUserResults().size() % 6 == 0) {
                int occurrences = Collections.frequency(currentSession.getUserProgress(), false);
                if (occurrences >= 3) //the user fails
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
                                        latitude = String.valueOf(dLatitude);
                                        longitude = String.valueOf(dLongitude);
                                        phoneNumber = "+6289693959600";
                                        currentSession.setAlertedLocation(currentLocation.getLocation());
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
                    ttsSpeak(getString(R.string.string_exceeded));
                    textInfoText.setText(getString(R.string.string_exceeded));
                    currentSession.setEndTime(updateTime());
                    count_total = currentSession.getUserResults().size();
                    count_wrong = 0;

                    for (boolean b : currentSession.getUserResults())
                    {
                        if (!b)
                        {
                            count_wrong++;
                        }
                    }
                    mDbHelper.insertStats(1, String.valueOf(currentSession.getStartTimeMillis()), count_total, count_wrong, String.valueOf(System.currentTimeMillis()));

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
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            setDriveButtonListenerStopDriving();
                        }
                    });

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

            ttsSpeak("You have correctly answered 3 questions to drive, now you can drive safely." +
                    " I will ask you questions every 5 minutes while you are driving. Please drive carefully and keep" +
                    "focus on the road. Thank you for your cooperation");

        }

    }

    @Override
    public void OnError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "OnError " + errorMessage);

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

        //on dB of speaker's voice changed will change the progress bar
    @Override
    public void OnRmsChanged(float rmsdB) {
        progressBar.setProgress((int) rmsdB);
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

    public void setDriveButtonListenerStopQuestions()
    {
        task.cancel();
        buttonDriveNow.setBackground(this.getResources().getDrawable(R.drawable.ico_mic_on));
        buttonDriveNow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Stop Questions?");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        mSpeechRecognizerManager.destroy();
                        createSpeechRecognizer();
                        ttsDestroy();
                        createTTS();
                        setDriveButtonListenerInitial();
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
        });
    }

    /**
     * This method is used to start the click listener when the user wants to drive
     * This trigger the first 3 questions for the user
     */
    private void setDriveButtonListenerInitial()
    {
        task.cancel();
        textButtonDriveText.setText("Tap to Drive");
        buttonDriveNow.setBackground(this.getResources().getDrawable(R.drawable.ico_mic));
        buttonDriveNow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!isDriving) {
                    buttonDriveNow.setClickable(false);
                    ttsSpeak(getString(R.string.before_driving1) + ", " +
                            getString(R.string.before_driving2) + ", " +
                            getString(R.string.before_driving3));
                    textInfoText.setText((getString(R.string.before_driving1) + ", " +
                            getString(R.string.before_driving2) + ", " +
                            getString(R.string.before_driving3)));

                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                        @Override
                        public void onDone(String utteranceId) {
                            MainActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    buttonDriveNow.setClickable(true);
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

    public void setDriveButtonListenerStopDriving()
    {

        textButtonDriveText.setText("Driving...(Tap to Stop Driving)");
        buttonDriveNow.setBackground(this.getResources().getDrawable(R.drawable.ico_mic_on));
        buttonDriveNow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Stop Driving?");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        task.cancel();
                        //on done speaking resets the tts and recognizer
                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onDone(String utteranceId) {

                                task = new TimerTask() {
                                    public void run() {
                                        MainActivity.this.runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                mSpeechRecognizerManager.destroy();
                                                createSpeechRecognizer();
                                                ttsDestroy();
                                                createTTS();
                                                setDriveButtonListenerInitial();
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

                        ttsSpeak("You have stopped driving. You can see your driving statistics by choosing statistics menu in the menu button");
                        textInfoText.setText("You have stopped driving. You can see your driving statistics by choosing statistics menu in the menu button");
                        currentSession.setEndTime(updateTime());
                        count_total = currentSession.getUserResults().size();
                        count_wrong = 0;

                        for (boolean b : currentSession.getUserResults()) {
                            if (!b) {
                                count_wrong++;
                            }
                        }
                        mDbHelper.insertStats(1, String.valueOf(currentSession.getStartTimeMillis()), count_total, count_wrong, String.valueOf(System.currentTimeMillis()));
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
        });
    }


}
