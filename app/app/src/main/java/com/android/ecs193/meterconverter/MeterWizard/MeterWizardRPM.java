package com.android.ecs193.meterconverter.MeterWizard;

import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
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
import android.widget.Toast;

import com.android.ecs193.meterconverter.BtConnection;
import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

import java.io.IOException;
import java.text.DecimalFormat;

public class MeterWizardRPM extends AppCompatActivity {

    BluetoothSocket btSocket = null;
    TextView text_q;
    TextView text_ratio;
    TextView text_rpm;
    Button but_incre;
    Button but_decre;
    Button but_cancel;
    Button but_finish;

    Handler repeatUpdateHandler = new Handler();
    boolean mAutoIncrement = false;
    boolean mAutoDecrement = false;

    Integer targetSpeed;

    HomeFragment mHomeFragment = new HomeFragment();
    MeterWizardUnit mMeterWizardUnit = new MeterWizardUnit();
    MeterWizardRatio mMeterWizardRatio = new MeterWizardRatio();

    // For formatting RPM
    DecimalFormat df = new DecimalFormat("###.#");

    class RptUpdater implements Runnable {
        public void run() {
            if( mAutoIncrement ){
                IncreSpeed();
                repeatUpdateHandler.postDelayed( new RptUpdater(), 50 );
            } else if( mAutoDecrement ){
                DecreSpeed();
                repeatUpdateHandler.postDelayed( new RptUpdater(), 50 );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_meter_rpm);

        btSocket = BtConnection.getBtConnection();
        text_q = findViewById(R.id.text_rpm_q);
        text_ratio = findViewById(R.id.text_ratio);
        text_rpm = findViewById(R.id.text_rpm);

        // Set prompt
        if (mMeterWizardUnit.getUnit() == "kph") {
            targetSpeed = Math.min(50, mMeterWizardRatio.getMaxSpeed() / 2);
            text_q.setText("Please adjust the target RPM until the speedometer reads " + String.valueOf(targetSpeed) + " KPH.");
        } else if (mMeterWizardUnit.getUnit() == "mph") {
            msg(String.valueOf(mMeterWizardRatio.getspeedometerHalf()));
            targetSpeed = Math.min(40, mMeterWizardRatio.getMaxSpeed() / 2);
            text_q.setText("Please adjust the target RPM until the speedometer reads " + String.valueOf(targetSpeed) + " MPH.");
        }

        // Change target RPM value on setting fragment
        but_incre = findViewById(R.id.but_increment);
        but_incre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreSpeed();
            }
        });
        but_incre.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v) {
                mAutoIncrement = true;
                repeatUpdateHandler.post( new RptUpdater() );
                return false;
            }
        });
        but_incre.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                        && mAutoIncrement ){
                    mAutoIncrement = false;
                    setRatioVal();
                }
                return false;
            }
        });


        but_decre = findViewById(R.id.but_decrement);
        but_decre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DecreSpeed();
            }
        });
        but_decre.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v) {
                mAutoDecrement = true;
                repeatUpdateHandler.post( new RptUpdater() );
                return false;
            }
        });
        but_decre.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                        && mAutoDecrement ){
                    mAutoDecrement = false;
                    setRatioVal();
                }
                return false;
            }
        });

        text_rpm.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    try {
                        String str = text_rpm.getText().toString();
                        mHomeFragment.setMeterRatioText(df.format(Double.valueOf(str)));
                        if (Double.valueOf(text_rpm.getText().toString()) < 0.0) {
                            new AlertDialog.Builder(MeterWizardRPM.this)
                                .setTitle("Error")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage("Please enter a value greater than 0")
                                .setPositiveButton("Confirm", null)
                                .show();
                        }
                        setRatioVal();
                    } catch (NumberFormatException nfe) {
                        new AlertDialog.Builder(MeterWizardRPM.this)
                            .setTitle("Error")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please enter a value")
                            .setPositiveButton("OK", null)
                            .show();
                    }
                    return true;
                }
                return false;
            }
        });

        but_finish = findViewById(R.id.but_finish);
        but_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (text_rpm.getText().toString().matches("")) {
                    new AlertDialog.Builder(MeterWizardRPM.this)
                            .setTitle("Enter target RPM")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please enter the target RPM of your car")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(MeterWizardRPM.this)
                        .setTitle("Please confirm the following is correct")
                        .setIcon(android.R.drawable.ic_menu_info_details)
                        .setMessage("Max Speed: " + mMeterWizardRatio.getMaxSpeed() + '\n'
                                + "Target RPM: " + text_rpm.getText().toString())
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Send string to Arduino to end calibration mode
                                if (btSocket != null) {
                                    try {
                                        String str = "P:0\0";
                                        btSocket.getOutputStream().write(str.getBytes());
                                        msg(str);

                                    } catch (IOException e) {
                                        msg("Error");
                                    }

                                }
                                finish();
                            }
                        })
                        .setNegativeButton("Go Back", null)
                        .show();
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent wizIntent = new Intent(MeterWizardRPM.this, MeterWizardRatio.class);
                startActivity(wizIntent);
            }
        });
    }

    public void IncreSpeed() {

        float rpm = Float.valueOf(text_rpm.getText().toString());
        rpm = rpm + (float) 0.1;
        text_rpm.setText(String.valueOf(df.format(rpm)));
    }

    public void DecreSpeed() {
        float rpm = Float.valueOf(text_rpm.getText().toString());
        if (rpm > 0.0) {
            rpm = rpm - (float) 0.1;
        }
        text_rpm.setText(String.valueOf(df.format(rpm)));
    }

    public void setRatioVal() {

        float ratio = Float.valueOf(text_rpm.getText().toString())/targetSpeed;
        text_ratio.setText("Speedometer Ratio: " + String.valueOf(ratio));
        mHomeFragment.setMeterRatioText(String.valueOf(ratio));

        // Send ratio to Arduino
        if (btSocket != null) {
            try {
                String str = "T:" + String.valueOf(ratio * 10) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}
