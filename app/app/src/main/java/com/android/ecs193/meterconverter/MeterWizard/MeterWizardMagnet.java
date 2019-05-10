package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

public class MeterWizardMagnet extends AppCompatActivity {

    RadioButton but_one;
    RadioButton but_two;
    RadioButton but_four;
    Button but_next;
    Button but_cancel;

    HomeFragment mHomeFragment = new HomeFragment();

    Boolean butChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard_meter_magnet);

        but_one = findViewById(R.id.radio_one);
        but_two = findViewById(R.id.radio_two);
        but_four = findViewById(R.id.radio_four);

        but_one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_two.isChecked()) {
                    but_two.setChecked(false);
                } else if (but_four.isChecked()) {
                    but_four.setChecked(false);
                }
            }
        });

        but_two.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_one.isChecked()) {
                    but_one.setChecked(false);
                } else if (but_four.isChecked()) {
                    but_four.setChecked(false);
                }
            }
        });

        but_four.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_one.isChecked()) {
                    but_one.setChecked(false);
                } else if (but_two.isChecked()) {
                    but_two.setChecked(false);
                }
            }
        });

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (but_one.isChecked()) {
                    mHomeFragment.setMagnetText(1);
                    butChecked = true;
                } else if (but_two.isChecked()) {
                    mHomeFragment.setMagnetText(2);
                    butChecked = true;
                } else if (but_four.isChecked()) {
                    mHomeFragment.setMagnetText(4);
                    butChecked = true;
                }

                if (butChecked) {
                    Intent wizIntent = new Intent(MeterWizardMagnet.this, MeterWizardTireSize.class);
                    finish();
                    startActivity(wizIntent);
                } else {
                    new AlertDialog.Builder(MeterWizardMagnet.this)
                            .setTitle("Choose Magnet")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please choose the number of magnets")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent wizIntent = new Intent(MeterWizardMagnet.this, MeterWizardDriveCheck.class);
                finish();
                // Slide from right to left
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                startActivity(wizIntent);
            }
        });

    }
}
