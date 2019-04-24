package com.ecs193.speedometerconverter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class Wizard extends AppCompatActivity {

    private Set<BluetoothDevice> pairedDevices;
    Intent rtnIntent;
    TextView speedometerMax;
    TextView wizardStep;
    Button incrementButton;
    Button cancelButton;
    Button wizard; //next



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //((SplashScreenSleep) this.getApplication()).setspeedometerHalf(10);
         int s = ((SplashScreenSleep) this.getApplication()).getspeedometerHalf(); //final
        System.out.println(s);



        super.onCreate(savedInstanceState);
        rtnIntent = new Intent();
        setContentView(R.layout.activity_wizard);

        wizardStep=findViewById(R.id.WizardStep);

        String text=wizardStep.getText().toString();
        text=text+" "+s;
        wizardStep.setText(text);

        speedometerMax= findViewById(R.id.speedometerMax);
        speedometerMax.setText(String.valueOf(s));
        speedometerMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int s = ((SplashScreenSleep) getApplication()).getspeedometerHalf();

                msg("speedometer Max is __"+Integer.toString(s));
            }
        });

        cancelButton = findViewById(R.id.but_cancel2);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });


        wizard = findViewById(R.id.Next);
        wizard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent wizIntent = new Intent(Wizard.this, Wizard2.class);
                startActivity(wizIntent);
            }
        });

        incrementButton=findViewById(R.id.increment);
        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int s = ((SplashScreenSleep) getApplication()).getspeedometerHalf();
                s = s+1;
                ((SplashScreenSleep) getApplication()).setspeedometerHalf(s);
                speedometerMax.setText(String.valueOf(s));
            }
        });
        incrementButton=findViewById(R.id.decrement);
        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int s = ((SplashScreenSleep) getApplication()).getspeedometerHalf();
                s = s-1;
                ((SplashScreenSleep) getApplication()).setspeedometerHalf(s);
                speedometerMax.setText(String.valueOf(s));
            }
        });
/*
        while (1==1) {
            speedometerMax.setText(s, TextView.BufferType.EDITABLE);
            msg("updating EditText");
        }
*/
    }


    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}