package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;

import com.android.ecs193.meterconverter.R;

public class MeterWizardRatio extends AppCompatActivity {

    private int speedometer_half;

    TextView text_speed;
    Button but_incre;
    Button but_decre;
    Button but_cancel;
    Button but_next;

    Handler repeatUpdateHandler = new Handler();
    boolean mAutoIncrement = false;
    boolean mAutoDecrement = false;

    static Integer maxSpeed;
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
        setContentView(R.layout.activity_wizard_meter_ratio);

        text_speed = findViewById(R.id.speedometerMax);

        // Change max speed value on setting fragment
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
                mAutoDecrement = false;
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
                }
                return false;
            }
        });

        text_speed.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    try {
                        setspeedometerHalf(Integer.valueOf(text_speed.getText().toString()));
                        if (Integer.valueOf(text_speed.getText().toString()) < 0) {
                            new AlertDialog.Builder(MeterWizardRatio.this)
                                .setTitle("Error")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage("Please enter a value greater than 0")
                                .setPositiveButton("OK", null)
                                .show();
                        }
                    } catch (NumberFormatException nfe) {
                        new AlertDialog.Builder(MeterWizardRatio.this)
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

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (text_speed.getText().toString().matches("")) {
                    new AlertDialog.Builder(MeterWizardRatio.this)
                            .setTitle("Enter Speed")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please enter the speed of your car")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    maxSpeed = Integer.valueOf(text_speed.getText().toString());
                    Intent wizIntent = new Intent(MeterWizardRatio.this, MeterWizardRPM.class);
                    finish();
                    startActivity(wizIntent);
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent wizIntent = new Intent(MeterWizardRatio.this, MeterWizardUnit.class);
                startActivity(wizIntent);
            }
        });

    }

    public void IncreSpeed() {
        int speed = getspeedometerHalf();
        speed = speed + 1;
        setspeedometerHalf(speed);
        text_speed.setText(String.valueOf(speed));
    }

    public void DecreSpeed() {
        int speed = getspeedometerHalf();
        if (speed > 0) {
            speed = speed - 1;
        }
        setspeedometerHalf(speed);
        text_speed.setText(String.valueOf(speed));
    }

     public Integer getspeedometerHalf() {
        return speedometer_half;
    }

    public void setspeedometerHalf(int speedometer_half) {
        this.speedometer_half = speedometer_half;
    }

    static public Integer getMaxSpeed() {
        return maxSpeed;
    }

}


