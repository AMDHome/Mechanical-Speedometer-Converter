package com.android.ecs193.meterconverter.MeterWizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.ecs193.meterconverter.R;

public class MeterWizardUnit extends AppCompatActivity {

    TextView unitsText;
    RadioButton but_kph;
    RadioButton but_mph;
    Button but_next;
    Button but_cancel;
    static String which_but;

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
                //unitsText.setText("kph");
                which_but = "kph";
            }
        });

        but_mph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (but_kph.isChecked()) {
                    but_kph.setChecked(false);
                }
                //unitsText.setText("mph");
                which_but = "mph";
            }
        });

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (but_mph.isChecked() || but_kph.isChecked()) {
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
                finish();
            }
        });

    }

    static public String getUnit() { return which_but; }
}
