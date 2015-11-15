package com.yudiandreanp.aware_d;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.text.format.Time;

import java.util.ArrayList;
import java.util.GregorianCalendar;

/**
 * Created by yudiandrean on 11/14/2015.
 */
public class Session {

    private final Context mContext;
    private GregorianCalendar startTime, endTime;
    private Location alertedLocation;
    private ArrayList <Integer> questionAskedArray;
    private ArrayList <String> userAnswerArray;

    public Session(Context context, GregorianCalendar startTime)
    {
        this.mContext = context;
    }


}
