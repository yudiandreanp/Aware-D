package com.yudiandreanp.aware_d;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by Cécile on 14/11/2015.
 */
public class DontDrive extends Activity {

    private static final int CODE_MAIN_ACTIVITY = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dont_drive);

        Button backMenu = (Button) findViewById(R.id.back_to_main_menu);
        backMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DontDrive.this, "clicked", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(DontDrive.this, MainActivity.class);
                startActivityForResult(intent, CODE_MAIN_ACTIVITY);
            }
        });
    }
}
