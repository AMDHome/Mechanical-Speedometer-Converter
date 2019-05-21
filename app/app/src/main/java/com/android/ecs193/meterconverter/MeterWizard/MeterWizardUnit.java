package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

import java.util.Timer;
import java.util.TimerTask;

public class MeterWizardUnit extends AppCompatActivity {

    RadioButton but_kph;
    RadioButton but_mph;
    Button but_next;
    Button but_cancel;
    static String which_but;

    HomeFragment mHomeFragment = new HomeFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard_meter_units);

        but_kph = findViewById(R.id.radio_kph);
        but_mph = findViewById(R.id.radio_mph);
        //unitsText = findViewById(R.id.unitsText);

        but_kph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_mph.isChecked()) {
                    but_mph.setChecked(false);
                }
                which_but = "kph";
                mHomeFragment.setUnits(which_but);
                Intent wizIntent = new Intent(MeterWizardUnit.this, MeterWizardRatio.class);
                finish();
                startActivity(wizIntent);
            }
        });

        but_mph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_kph.isChecked()) {
                    but_kph.setChecked(false);
                }
                which_but = "mph";
                mHomeFragment.setUnits(which_but);
                Intent wizIntent = new Intent(MeterWizardUnit.this, MeterWizardRatio.class);
                finish();
                startActivity(wizIntent);
            }
        });

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (but_mph.isChecked() || but_kph.isChecked()) {
                    mHomeFragment.setUnits(which_but);
                    Intent wizIntent = new Intent(MeterWizardUnit.this, MeterWizardRatio.class);
                    finish();
                    startActivity(wizIntent);
                } else {
                    new AlertDialog.Builder(MeterWizardUnit.this)
                            .setTitle("Choose Unit")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please choose one of the units")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mHomeFragment.getDriveCheck()) {
                    Intent wizIntent = new Intent(MeterWizardUnit.this, MeterWizardDriveCheck.class);
                    finish();
                    //slide from right to left
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    startActivity(wizIntent);
                } else {
                    finish();
                    //slide from right to left
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        });

    }

    static public String getUnit() { return which_but; }
}
