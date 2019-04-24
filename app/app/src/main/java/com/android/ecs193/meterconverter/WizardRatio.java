package com.android.ecs193.meterconverter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class WizardRatio extends AppCompatActivity {

    TextView speedometerMax;
    TextView wizardStep;
    Button incrementButton;
    Button cancelButton;
    Button wizard; //next

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int s = ((SplashScreenSleep) this.getApplication()).getspeedometerHalf(); //final
        super.onCreate(savedInstanceState);
        new Intent();
        setContentView(R.layout.activity_wizard_ratio);

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
                Intent wizIntent = new Intent(WizardRatio.this, WizardRPM.class);
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
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}
