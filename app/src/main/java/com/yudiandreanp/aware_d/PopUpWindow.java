package com.yudiandreanp.aware_d;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.PopupWindow ;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

//USELESS CLASS !!!!!!!!!!!!!!!!!!!!!!
/**
 * Created by Cécile on 02/11/2015.
 */
public class PopUpWindow extends PopupWindow {

    MainActivity mainActivity = new MainActivity();
/**
    public void popUpWindowDontDrive(Bundle savedInstanceState) {
        super.PopupWindow;

        this.setHeight((int)0.7*mainActivity.getWidthScreen()); // 1 = full screen
        this.setWidth((int)0.7*mainActivity.getWidthScreen());

        sleep(5);
        this.dismiss();

        //TO DEAL WITH THE BUTTON
    }

    public void sleep(int timeInSeconds) {
        try {
            Thread.sleep(timeInSeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void popUpWindowDriveNow(Bundle savedInstanceState) {
        super.PopupWindow(R.layout.drive_now) ;

        this.setHeight(5);
        this.setWidth(5);

        sleep(5);
        this.dismiss();
    }


    public void popUpWindowCorrect(Bundle savedInstanceState, Boolean correct) {
        if (correct) {
            super.PopupWindow(R.layout.correct_response) ; }
        else {
            super.PopupWindow(R.layout.wrong_response) ; }

        this.setHeight(5);
        this.setWidth(5);

        sleep(5);
        this.dismiss();
    } **/
}
