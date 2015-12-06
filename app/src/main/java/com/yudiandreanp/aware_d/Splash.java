package com.yudiandreanp.aware_d;

/**
 * Created by yudiandrean on 12/5/2015.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


public class Splash extends Activity {

    /** Duration of wait **/
    private final int SPLASH_DISPLAY_LENGTH = 2500;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.splash_fragment);

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent mainIntent = new Intent(Splash.this,MainActivity.class);
                Splash.this.startActivity(mainIntent);
                Splash.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}

//public class SplashFragment extends Fragment {
//
//
//
//    @Override
//    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
//
////        new Handler().postDelayed(new Runnable() {
////                                      @Override
////                                      public void run() {
////                                /* Create an Intent that will start the Menu-Activity. */
////                                          inflater.inflate(R.layout.splash_fragment, container, false);
////                                      }
////                                  },
////                SPLASH_DISPLAY_LENGHT);
//
//        return inflater.inflate(R.layout.splash_fragment, container, false);
//    }
//}