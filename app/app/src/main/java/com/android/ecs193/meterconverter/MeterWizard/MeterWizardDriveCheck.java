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

public class MeterWizardDriveCheck extends AppCompatActivity {

    BluetoothSocket btSocket = null;
    RadioButton but_yes;
    RadioButton but_no;
    Button but_next;
    Button but_cancel;

    SettingsFragment mSettingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard_meter_drive_check);

        btSocket = BtConnection.getBtConnection();

        but_yes = findViewById(R.id.radio_yes);
        but_no = findViewById(R.id.radio_no);
        //unitsText = findViewById(R.id.unitsText);

        but_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_no.isChecked()) {
                    but_no.setChecked(false);
                }
                Intent wizIntent = new Intent(MeterWizardDriveCheck.this, MeterWizardMagnet.class);
                finish();
                startActivity(wizIntent);
            }
        });

        but_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_yes.isChecked()) {
                    but_yes.setChecked(false);
                }
                mSettingsFragment.setDriveCheck();
                Intent wizIntent = new Intent(MeterWizardDriveCheck.this, MeterWizardUnit.class);
                finish();
                startActivity(wizIntent);
            }
        });

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (but_no.isChecked()) { // go to ratio wizard
                    mSettingsFragment.setDriveCheck();
                    Intent wizIntent = new Intent(MeterWizardDriveCheck.this, MeterWizardUnit.class);
                    finish();
                    startActivity(wizIntent);
                } else if (but_yes.isChecked()){
                    Intent wizIntent = new Intent(MeterWizardDriveCheck.this, MeterWizardMagnet.class);
                    finish();
                    startActivity(wizIntent);
                } else {
                    new AlertDialog.Builder(MeterWizardDriveCheck.this)
                            .setTitle("Choose Yes or No")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please choose either Yes or No")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            // Send ratio to Arduino
            if (btSocket != null) {
                try {
                    String str = "D:F" + '\0';
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
        });

    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}
