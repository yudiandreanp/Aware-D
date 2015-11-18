package com.yudiandreanp.aware_d;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by yudiandrean on 11/3/2015.
 */
public class TextSpeaker implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;

    private boolean ready = false;

    private boolean allowed = false;

    private boolean finished;

    public TextSpeaker(Context context){
        tts = new TextToSpeech(context, this);
            }

    public boolean isAllowed(){
        return allowed;
    }

    public void allow(boolean allowed){
        this.allowed = allowed;
    }

    @Override
    public void onInit(int status) {

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

            @Override
            public void onDone(String utteranceId) {
                Log.i("Text Speaker","Done Speaking");
                finished = true;
            }

            @Override
            public void onError(String utteranceId) {
                Log.i("Text Speaker","Error Speaking");
            }

            @Override
            public void onStart(String utteranceId) {
                Log.i("Text Speaker","Started Speaking");
                finished=false;
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

     public void speak(String text){

        // Speak only if the TTS is ready
        // and the user has allowed speech

        if(ready && allowed) {
            tts.setSpeechRate(0.8f);
            HashMap<String, String> hash = new HashMap<String,String>();
            hash.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_NOTIFICATION));
            hash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    "message ID");
            tts.speak(text, TextToSpeech.QUEUE_ADD, hash);

        }
    }


    //access the status of utterance to pass to other class (MainActivity)
    public boolean isFinished()
    {
        return finished;
    }

    //access the tts readiness status
    public boolean isReady()
    {
        return ready;
    }

    //reset the utterance status to not finished
    public void setNotFinished()
    {
        finished = false;
    }

    public void pause(int duration){
        tts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
    }

    // Free up resources
    public void destroy(){
        tts.shutdown();
    }


}
