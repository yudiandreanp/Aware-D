package com.yudiandreanp.aware_d;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by yudiandrean on 11/16/2015.
 *
 * A class that imports android SmsManager to send sms with
 * map link to the designated phone number;
 *
 * Created to make a modular design of the app
 */
public class SMSManager {

    private final String MAP_LINK = "http://www.google.com/maps/place/";
    private final Context mContext;
    private String user;
    private SharedPreferences SP;


    public SMSManager(Context context)
    {
        this.mContext = context;
        SP = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void sendLocationSMS(String phone, String latitude, String longitude)
    {
        String comma = ",";
        String mLink = MAP_LINK + latitude + comma + longitude;
        user = SP.getString("username", "NA");
        String smsText = "We detect unaware driving by your friend " + user + " "
                + mLink;

        try
        {
            SmsManager smsSender = SmsManager.getDefault();
            smsSender.sendTextMessage(phone, null, smsText, null, null);
            Log.d("SMS", "SMS Sent!");
        }
        catch (Exception e)
        {
            Log.d("SMS", "Cannot send sms");
        }

    }

}
