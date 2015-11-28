package com.yudiandreanp.aware_d;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.text.format.Time;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by yudiandrean on 11/14/2015.
 */
public class Session {

    private final Context mContext;
    private Calendar mStartTime, mEndTime;
    private Location mAlertedLocation;
    private int threeRight, threeTries, errorTimes, mainRight;
    private ArrayList <Integer> mQuestionAskedArray; //index of asked question
    private ArrayList <String> mUserAnswerArray; //Collection of users' answers
    private boolean statusThreeQuestions; //check if the session is in the initial questions mode or the driving mode

    public Session(Context context, Calendar startTime)
    {
        this.mContext = context;
        mStartTime = startTime;
        threeRight = 0;
        threeTries = 0;
        statusThreeQuestions = true;
    }

    public void incrementThreeTries()
    {
        threeTries++;
    }

    public void incrementThreeRight()
    {
        threeRight++;
        threeTries++;
    }

    public int getThreeRight()
    {
        return threeRight;
    }

    public int getThreeTries()
    {
        return threeTries;
    }

    public void setBooleanStatusThree()
    {
        if (statusThreeQuestions)
        {
            statusThreeQuestions = false;
        }
        else
        {
            statusThreeQuestions = true;
        }
    }

    public boolean getBooleanStatusThree(){return statusThreeQuestions;}

}
