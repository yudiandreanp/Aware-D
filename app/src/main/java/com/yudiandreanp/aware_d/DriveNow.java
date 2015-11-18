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
public class DriveNow extends Activity {

    private static final int CODE_QUESTIONSVIEW_ACTIVITY = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drive_now);

        Button backMenu = (Button) findViewById(R.id.drive_now);
        backMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DriveNow.this, "clicked", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(DriveNow.this, MainActivity.class);
                startActivityForResult(intent, CODE_QUESTIONSVIEW_ACTIVITY);
            }
        });
    }
}
