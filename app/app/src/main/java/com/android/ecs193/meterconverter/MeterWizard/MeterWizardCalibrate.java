package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

public class MeterWizardCalibrate extends AppCompatActivity {

    TextView text_target;
    TextView text_current;
    Button but_cancel;
    Button but_finish;

    HomeFragment mHomeFragment = new HomeFragment();
    MeterWizardRPM mMeterWizardRPM = new MeterWizardRPM();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_meter_calibrate);

        text_target = findViewById(R.id.text_target_speed);
        text_target.setText(String.valueOf(mMeterWizardRPM.getTargetSpeed()));

        text_current = findViewById(R.id.text_curr_speed);
        // TODO: add gps for current speed

        but_finish = findViewById(R.id.but_finish);
        but_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    finish();
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent wizIntent = new Intent(MeterWizardCalibrate.this, MeterWizardTireSize.class);
                finish();
                // Slide from right to left
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                startActivity(wizIntent);


            }
        });

    }
}


