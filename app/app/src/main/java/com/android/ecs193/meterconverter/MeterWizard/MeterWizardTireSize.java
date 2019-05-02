package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

public class MeterWizardTireSize extends AppCompatActivity {

    TextView text_size;
    Button but_cancel;
    Button but_next;

    HomeFragment mHomeFragment = new HomeFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_meter_tire_size);

        text_size = findViewById(R.id.text_tireSize);
        text_size.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    try {
                        mHomeFragment.setTireSize(text_size.getText().toString());
                    } catch (NumberFormatException nfe) {
                        new AlertDialog.Builder(MeterWizardTireSize.this)
                            .setTitle("Error")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please enter the tire size")
                            .setPositiveButton("OK", null)
                            .show();
                    }
                    return true;
                }
                return false;
            }
        });

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (text_size.getText().toString().matches("")) {
                    new AlertDialog.Builder(MeterWizardTireSize.this)
                            .setTitle("Enter Tire Size")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please enter the tire size")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    mHomeFragment.setTireSize(text_size.getText().toString());
                    Intent wizIntent = new Intent(MeterWizardTireSize.this, MeterWizardCalibrate.class);
                    finish();
                    startActivity(wizIntent);
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent wizIntent = new Intent(MeterWizardTireSize.this, MeterWizardMagnet.class);
                finish();
                // Slide from right to left
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                startActivity(wizIntent);


            }
        });

    }
}


