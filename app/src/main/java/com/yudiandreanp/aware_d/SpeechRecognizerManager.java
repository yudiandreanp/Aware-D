package com.yudiandreanp.aware_d;

/**
 * Modified by yudiandrean on 11/25/2015.
 ** ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SpeechRecognizerManager {

    private static final String TAG = SpeechRecognizerManager.class.getSimpleName();
    protected AudioManager mAudioManager;
    protected Intent mSpeechRecognizerIntent;
    protected android.speech.SpeechRecognizer mGoogleSpeechRecognizer;
    private Context mContext;
    private OnResultListener mOnResultListener;
    private OnErrorListener mOnErrorListener;
    private int errorInt;


    public SpeechRecognizerManager(Context context) {
        this.mContext = context;
        this.mOnResultListener = (OnResultListener)context;
        this.mOnErrorListener = (OnErrorListener)context;
        initGoogleSpeechRecognizer();
    }

    public void destroy() {

        if (mGoogleSpeechRecognizer != null) {
            mGoogleSpeechRecognizer.cancel();
            mGoogleSpeechRecognizer.destroy();
        }

    }

    private void initGoogleSpeechRecognizer() {
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        mGoogleSpeechRecognizer = android.speech.SpeechRecognizer
                .createSpeechRecognizer(mContext);
        mGoogleSpeechRecognizer.setRecognitionListener(new GoogleRecognitionListener());

        mSpeechRecognizerIntent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                mContext.getPackageName());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
    }



    protected class GoogleRecognitionListener implements
            android.speech.RecognitionListener
    {

        private final String TAG = GoogleRecognitionListener.class
                .getSimpleName();

        @Override
        public void onBeginningOfSpeech() {
            Log.i("SpeechRecognizerManager", "onBeginningOfSpeech");
        }

        @Override
        public void onEndOfSpeech() {
            Log.i("SpeechRecognizerManager", "onEndOfSpeech");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.i("SpeechRecognizerManager", "onReadyForSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.i(TAG, "onBufferReceived: " + buffer);

        }

        @Override
        public void onError(int error) {
            Log.e(TAG, "onError:" + error);
            errorInt = error;
            //sends list of workds to mOnResultListener
            if (mOnErrorListener!=null){
                mOnErrorListener.OnError(error);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

            if ((partialResults != null)
                    && partialResults
                    .containsKey(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)) {
                ArrayList<String> heard = partialResults
                        .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = partialResults
                        .getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES);
                // receiveWhatWasHeard(heard, scores);

                for (int i = 0; i < heard.size(); i++) {
                    Log.d(TAG, "onPartialResultsheard:" + heard.get(i)
                            + " confidence:" + scores[i]);

                }
            }

        }

        @Override
        public void onResults(Bundle partialResults) {
            if ((partialResults != null)
                    && partialResults
                    .containsKey(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)) {
                ArrayList<String> heard = partialResults
                        .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = partialResults
                        .getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES);

                for (int i = 0; i < heard.size(); i++) {
                    Log.d(TAG, "onResultsheard:" + heard.get(i)
                            + " confidence:" + scores[i]);

                }


                //sends list of workds to mOnResultListener
                if (mOnResultListener!=null){
                    mOnResultListener.OnResult(heard);
                }

            }



        }


        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    }


    public void setOnResultListener(OnResultListener onResultListener){
        mOnResultListener=onResultListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener){
        mOnErrorListener=onErrorListener;
    }

    public interface OnResultListener
    {
        public void OnResult(ArrayList<String> commands);
    }

    public interface OnErrorListener
    {
        public void OnError(int error);
    }

    public void startRecognizing()
    {
        mGoogleSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }


}
