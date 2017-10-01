package com.aitorvs.android.fingerlocksample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by user on 2017-09-30.
 */

public class StartActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
        Button btn1 = (Button)findViewById(R.id.voice);
        Button btn2= (Button)findViewById(R.id.upm);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voice:
                Intent intent = new Intent(this, voice.class);
                startActivity(intent);
                break;
            case R.id.upm:
                Intent intent1 = new Intent(this, AppEntryActivity.class);
                startActivity(intent1);
                break;
        }
    }


}