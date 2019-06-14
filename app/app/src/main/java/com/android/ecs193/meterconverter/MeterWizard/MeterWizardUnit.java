package com.android.ecs193.meterconverter.MeterWizard;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.android.ecs193.meterconverter.BtConnection;
import com.android.ecs193.meterconverter.SettingsFragment;
import com.android.ecs193.meterconverter.R;

import java.io.IOException;

public class MeterWizardUnit extends AppCompatActivity {

    BluetoothSocket btSocket = null;
    RadioButton but_kph;
    RadioButton but_mph;
    Button but_next;
    Button but_cancel;
    static String which_but;

    SettingsFragment mSettingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard_meter_units);

        btSocket = BtConnection.getBtConnection();
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
                mSettingsFragment.setUnits(which_but);
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
                mSettingsFragment.setUnits(which_but);
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
                    mSettingsFragment.setUnits(which_but);
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
                if (mSettingsFragment.getDriveCheck()) {
                    Intent wizIntent = new Intent(MeterWizardUnit.this, MeterWizardDriveCheck.class);
                    finish();
                    //slide from right to left
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    startActivity(wizIntent);
                } else {
                    // Send ratio to Arduino
                    if (btSocket != null) {
                        try {
                            String str = "P:0" + '\0';
                            btSocket.getOutputStream().write(str.getBytes());
                            //msg(str);

                        } catch (IOException e) {
                            msg("Error");
                        }
                    }
                    finish();
                    //slide from right to left
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        });

    }

    static public String getUnit() { return which_but; }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}
