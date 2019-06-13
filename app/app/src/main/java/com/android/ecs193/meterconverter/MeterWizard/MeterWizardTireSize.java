package com.android.ecs193.meterconverter.MeterWizard;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.TextWatcher;
import android.widget.Toast;

import com.android.ecs193.meterconverter.HomeFragment;
import com.android.ecs193.meterconverter.R;

import java.io.IOException;
import java.util.regex.Pattern;

public class MeterWizardTireSize extends AppCompatActivity {

    EditText text_size1;
    EditText text_size2;
    EditText text_size3;
    EditText text_size4;
    EditText text_size5;
    EditText text_size6;
    EditText text_size7;

    String tireSize;
    Button but_cancel;
    Button but_next;

    HomeFragment mHomeFragment = new HomeFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_meter_tire_size);

        text_size1 = findViewById(R.id.text_tireSize1);
        text_size2 = findViewById(R.id.text_tireSize2);
        text_size3 = findViewById(R.id.text_tireSize3);
        text_size4 = findViewById(R.id.text_tireSize4);
        text_size5 = findViewById(R.id.text_tireSize5);
        text_size6 = findViewById(R.id.text_tireSize6);
        text_size7 = findViewById(R.id.text_tireSize7);

        //text_size1.requestFocus();
        //text_size1.setCursorVisible(true);
        nextText(text_size1, text_size2);
        nextText(text_size2, text_size3);
        nextText(text_size3, text_size4);
        nextText(text_size4, text_size5);
        nextText(text_size5, text_size6);
        nextText(text_size6, text_size7);
        nextText(text_size7, text_size7);

        but_next = findViewById(R.id.but_next);
        but_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                tireSize = 'P' + text_size1.getText().toString() + text_size2.getText().toString() +
                        text_size3.getText().toString() + '/' + text_size4.getText().toString() +
                        text_size5.getText().toString() + 'R' + text_size6.getText().toString() +
                        text_size7.getText().toString();

                if (!Pattern.matches("P[0-9][0-9][0-9]\\/[0-9][0-9]R[0-9][0-9]", tireSize)) {
                    new AlertDialog.Builder(MeterWizardTireSize.this)
                            .setTitle("Enter Tire Size")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("Please enter only numbers")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    mHomeFragment.setTireSize(tireSize);
                    Intent wizIntent = new Intent(MeterWizardTireSize.this, MeterWizardCalibrate.class);
                    finish();
                    startActivity(wizIntent);
                }
            }
        });

        but_cancel = findViewById(R.id.but_cancel);
        but_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent wizIntent = new Intent(MeterWizardTireSize.this, MeterWizardMagnet.class);
                finish();
                // Slide from right to left
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                startActivity(wizIntent);


            }
        });

    }


    protected void nextText(final EditText text1, final EditText text2) {

        final StringBuilder sb = new StringBuilder();

        text1.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(sb.length() == 0 & text1.length()==1) {
                    sb.append(s);
                    text1.clearFocus();
                    text2.requestFocus();
                    text2.setCursorVisible(true);

                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(sb.length() == 1) {
                    sb.deleteCharAt(0);
                }
            }

            public void afterTextChanged(Editable s) {
                if(sb.length() == 0)
                {
                    text1.requestFocus();
                }

            }
        });

        // check if empty
        text1.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (text1.getText().toString().length() == 0) {
                        new AlertDialog.Builder(MeterWizardTireSize.this)
                                .setTitle("Error")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage("Please enter a number")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                    return true;
                }
                return false;
            }
        });
    }
}


