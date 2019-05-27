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
import java.util.Timer;
import java.util.TimerTask;

public class MeterWizardRPM extends AppCompatActivity {

    BluetoothSocket btSocket = null;
    TextView text_q;
    TextView text_ratio;
    Float target_rpm = (float) 0.0;
    Button but_incre;
    Button but_decre;
    Button but_cancel;
    Button but_finish;

    Handler repeatUpdateHandler = new Handler();
    boolean mAutoIncrement = false;
    boolean mAutoDecrement = false;

    static Integer targetSpeed;
    float ratio;

    HomeFragment mHomeFragment = new HomeFragment();
    MeterWizardUnit mMeterWizardUnit = new MeterWizardUnit();
    MeterWizardRatio mMeterWizardRatio = new MeterWizardRatio();

    // For formatting RPM
    DecimalFormat df = new DecimalFormat("##0.000##");

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

        // Set prompt
        if (mMeterWizardUnit.getUnit() == "kph") {
            targetSpeed = Math.min(50, mMeterWizardRatio.getMaxSpeed() / 2);
            text_q.setText("Please adjust the buttons until the speedometer reads " + String.valueOf(targetSpeed) + " KPH.");
        } else if (mMeterWizardUnit.getUnit() == "mph") {
            msg(String.valueOf(mMeterWizardRatio.getspeedometerHalf()));
            targetSpeed = Math.min(40, mMeterWizardRatio.getMaxSpeed() / 2);
            text_q.setText("Please adjust the buttons until the speedometer reads " + String.valueOf(targetSpeed) + " MPH.");
        }

        // Change target RPM value on setting fragment
        but_incre = findViewById(R.id.but_increment);
        but_incre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncreSpeed();
                //setRatioVal();
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
                //setRatioVal();
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

        text_ratio.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    try {
                        String str = text_ratio.getText().toString();
                        if (Double.valueOf(text_ratio.getText().toString()) < 0.0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MeterWizardRPM.this);
                            builder.setTitle("Error");
                            builder.setIcon(android.R.drawable.ic_dialog_alert);
                            builder.setMessage("Please enter a value greater than 0");
                            builder.setCancelable(true);

                            final AlertDialog closeDialog = builder.create();
                            closeDialog.show();

                            // Display dialog box for 2 seconds
                            final Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                public void run() {
                                    closeDialog.dismiss();
                                    timer.cancel();
                                }
                            }, 2000);
                        }
                        // Convert ratio to target rpm
                        ratio = Float.valueOf(text_ratio.getText().toString());
                        target_rpm = ratio * targetSpeed;
                        sendRatioVal();


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
                AlertDialog.Builder builder;
                if (text_ratio.getText().toString().length() == 0) {
                    builder = new AlertDialog.Builder(MeterWizardRPM.this);
                    builder.setTitle("Adjust value");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage("Please adjust the buttons");
                    builder.setPositiveButton("OK", null);
                    builder.setCancelable(true);

                    final AlertDialog closeDialog = builder.create();
                    closeDialog.show();

                    // Display dialog box for 2 seconds
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            closeDialog.dismiss();
                            timer.cancel();
                        }
                    }, 2000);

                } else if (text_ratio.getText().toString().matches("0.0")) {
                    builder = new AlertDialog.Builder(MeterWizardRPM.this);
                    builder.setTitle("Adjust value");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage("Please adjust the buttons so the ratio is not zero");
                    builder.setPositiveButton("OK", null);
                    builder.setCancelable(true);

                    final AlertDialog closeDialog = builder.create();
                    closeDialog.show();

                    // Display dialog box for 2 seconds
                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            closeDialog.dismiss();
                            timer.cancel();
                        }
                    }, 2000);
                } else {

                    //sendRatioVal();

                    // Send string to Arduino to end calibration mode
                    if (btSocket != null) {
                        try {
                            mHomeFragment.setMeterRatioText(String.valueOf(ratio));
                            final Timer timer = new Timer();

                            // Wait for one second
                            timer.schedule(new TimerTask() {
                                public void run() {
                                }
                            }, 1000);
                            String str = "P:0\0";
                            btSocket.getOutputStream().write(str.getBytes());
                            msg(str);

                        } catch (IOException e) {
                            msg("Error");
                        }

                    }

                    if (mHomeFragment.getDriveCheck()) {
                        Intent wizIntent = new Intent(MeterWizardRPM.this, MeterWizardMagnet.class);
                        finish();
                        startActivity(wizIntent);
                    } else {
                        finish();
                    }

                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent wizIntent = new Intent(MeterWizardRPM.this, MeterWizardRatio.class);
                finish();
                // Slide from right to left
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                startActivity(wizIntent);


            }
        });
    }

    public void IncreSpeed() {
        target_rpm = target_rpm + (float) 0.1;
        setRatioVal();
        sendRatioVal();
    }

    public void DecreSpeed() {
        if (target_rpm > 0.1) {
            target_rpm = target_rpm - (float) 0.1;
            setRatioVal();
            sendRatioVal();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MeterWizardRPM.this);
            builder.setTitle("Error");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage("You have hit a value too low to be possible!");
            builder.setCancelable(true);

            final AlertDialog closeDialog = builder.create();
            closeDialog.show();

            // Display dialog box for 2 seconds
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    closeDialog.dismiss();
                    timer.cancel();
                }
            }, 2000);
        }
    }

    public void setRatioVal() {
        ratio = target_rpm/targetSpeed;
        text_ratio.setText(String.valueOf(df.format(ratio)));

    }

    public void sendRatioVal(){

        // Send ratio to Arduino
        if (btSocket != null) {
            try {
                String str = "T:" + String.valueOf(target_rpm * 10) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    static public Integer getTargetSpeed() {
        return targetSpeed;
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}
