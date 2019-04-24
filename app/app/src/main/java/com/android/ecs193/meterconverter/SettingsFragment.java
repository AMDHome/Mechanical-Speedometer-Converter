package com.android.ecs193.meterconverter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class SettingsFragment extends Fragment {

    public TextView countTv;
    public Button countBtn;

    Button pairDevice;
    //TextView butUnits;
    Button butUnits;
    Button butMaxSpeed;
    Button butMagnets;
    Button butFinalDrive;
    Button butRatio;
    Button butSize;

    TextView unitsText;
    TextView maxSpeedText;
    TextView magnetsText;
    TextView finalDriveText;
    TextView meterRatioText;
    TextView wheelSizeText;
    TextView wheelCircText;

    TextView tv;
    ListView mListView;
    BluetoothSocket btSocket = null;

    AlertDialog alertDialog1;

    String wheelUnit;
    final static int BT_INTENT_FLAG = 0;

    public void SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings, container, false);

        // change opacity of calibration layout
        getView().findViewById(R.id.meterSettings).setAlpha((float)0.3);

        // change opacity of calibration layout
        //findViewById(R.id.meterSettings).setAlpha((float)0.3);
        getView().findViewById(R.id.meterSettings).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.meterSettings).setAlpha((float)0.3);
        // Define widgets

        //mListView = findViewById(R.id.listSettings);

        //wheelSizeText = findViewById(R.id.wheelSizeText);



        pairDevice = getView().findViewById(R.id.deviceArrow);
        pairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent btIntent = new Intent(getActivity(), BtConnection.class);
                getActivity().startActivityForResult(btIntent, BT_INTENT_FLAG);

            }
        });

        butUnits = getView().findViewById(R.id.but_units);
        unitsText = getView().findViewById(R.id.unitsText);
        butUnits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getButtonDialog("Choose Units", R.array.units_array, unitsText);
            }
        });

        butMaxSpeed= getView().findViewById(R.id.but_maxSpeed);
        maxSpeedText = getView().findViewById(R.id.maxSpeedText);
        butMaxSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Max Speed", maxSpeedText, "M:");
            }
        });

        butMagnets= getView().findViewById(R.id.but_magnets);
        magnetsText = getView().findViewById(R.id.magnetsText);
        butMagnets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getButtonDialog("Choose Number of Magnets", R.array.magnet_array, magnetsText);
            }
        });

        butFinalDrive= getView().findViewById(R.id.but_finalDrive);
        finalDriveText = getView().findViewById(R.id.finalDriveText);
        butFinalDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Final Drive", finalDriveText, "F:");
            }
        });

        butRatio= getView().findViewById(R.id.but_ratio);
        meterRatioText = getView().findViewById(R.id.meterRatioText);
        butRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Speedometer Ratio", meterRatioText, "S:");
            }
        });

        butSize= getView().findViewById(R.id.but_size);
        wheelCircText = getView().findViewById(R.id.wheelCircText);
        butSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getMultiTextBoxDialog(view,"Enter Tire Circumference", wheelCircText);
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);

                // Set text box input as decimal only
                final EditText wheelCirc = new EditText(getActivity());
                wheelCirc.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                wheelCirc.setHint("Wheel circumference");
                layout.addView(wheelCirc); // Notice this is an add method

                // Set up radio button for units
                final RadioButton circInch = new RadioButton(getActivity());
                circInch.setText("Inch (in)");
                layout.addView(circInch);
                final RadioButton circCM = new RadioButton(getActivity());
                circCM.setText("Centimeter (cm)");
                layout.addView(circCM);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle("Enter Tire Circumference")
                        //.setIcon(R.drawable.ic_baseline_create_24px)
                        .setView(layout)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Check which radio button was clicked
                                if (circInch.isChecked()) {
                                    wheelUnit = "inch";
                                    circCM.setChecked(false); // Make sure the other option is unchecked
                                } else {
                                    wheelUnit = "cm";
                                    circInch.setChecked(false);
                                }

                                wheelCircText.setText(wheelCirc.getText().toString() + " " + wheelUnit);

                                if (wheelCirc.getText().toString().length() != 0) {
                                    sendWheelCircCalc(wheelCirc.getText().toString(), wheelUnit);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null);

                alertDialog1 = builder.create();
                alertDialog1.show();

                // Initialize a TextView for ListView each Item
                //tv = view.findViewById(android.R.id.text1);

                // Set the text color of TextView (ListView Item)
                //tv.setTextColor(Color.WHITE);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check which request we're responding to
        if (requestCode == BT_INTENT_FLAG) {

            // Make sure the request was successful
            if (resultCode == getActivity().RESULT_OK) {
                btSocket = BtConnection.getBtConnection();
                getView().findViewById(R.id.meterSettings).setAlpha((float)1);
                getMeterSettings();
            }
        }
    }

    void getButtonDialog(final String title, int arrID, final TextView textBox) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity().getApplicationContext())
                .setTitle(title)
                .setIcon(R.drawable.ic_baseline_create_24px)
                .setSingleChoiceItems(getResources().getStringArray(arrID),
                        -1, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {

                                if (title == "Choose Units") {
                                    switch(item) {
                                        case 0:
                                            textBox.setText("mph");
                                            break;
                                        case 1:
                                            textBox.setText("kph");
                                            break;
                                    }
                                } else if (title == "Choose Number of Magnets") {
                                    switch(item) {
                                        case 0:
                                            textBox.setText("1");
                                            break;
                                        case 1:
                                            textBox.setText("2");
                                            break;
                                        case 2:
                                            textBox.setText("4");
                                            break;
                                    }
                                }
                            }
                        })
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (title == "Choose Units") {

                            if (textBox.getText().toString().equalsIgnoreCase("mph")) {
                                sendUnits(Integer.toString(0));
                            } else if (textBox.getText().toString().equalsIgnoreCase("kph")) {
                                sendUnits(Integer.toString(1));
                            }

                        } else if (title == "Choose Number of Magnets") {

                            if (textBox.getText().toString().equalsIgnoreCase("1")) {
                                sendInfo(Integer.toString(1), "N:");
                            } else if (textBox.getText().toString().equalsIgnoreCase("2")) {
                                sendInfo(Integer.toString(2), "N:");
                            } else if (textBox.getText().toString().equalsIgnoreCase("4")) {
                                sendInfo(Integer.toString(4), "N:");
                            }

                        }
                    }
                })
                .setNegativeButton("Cancel", null);

        alertDialog1 = builder.create();
        alertDialog1.show();
    }

    void getTextBoxDialog(final String title, final TextView textBox, final String arduinoStr) {

        // Set text box input as int only
        final EditText input = new EditText(getActivity());
        if (arduinoStr == "M:") { // magnet
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if ((arduinoStr == "F:") || (arduinoStr == "S:")) {
            input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setIcon(R.drawable.ic_baseline_create_24px)
                .setView(input)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        textBox.setText(input.getText().toString());

                        if (input.getText().toString().length() != 0) {
                            if (arduinoStr == "M:") {
                                sendInfo(input.getText().toString(), arduinoStr);
                            } else if ((arduinoStr == "F:") || (arduinoStr == "S:")) {
                                sendCalc(input.getText().toString(), arduinoStr);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", null);

        alertDialog1 = builder.create();
        alertDialog1.show();
    }


    void getMultiTextBoxDialog (View view, final String title, final TextView textBox) {

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set text box input as decimal only
        final EditText wheelCirc = new EditText(getActivity());
        wheelCirc.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        wheelCirc.setHint("Wheel circumference");
        layout.addView(wheelCirc); // Notice this is an add method

        // Set up radio button for units
        final RadioButton circInch = new RadioButton(getActivity());
        circInch.setText("Inch (in)");
        layout.addView(circInch);

        final RadioButton circCM = new RadioButton(getActivity());
        circCM.setText("Centimeter (cm)");
        layout.addView(circCM);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                //.setIcon(R.drawable.ic_baseline_create_24px)
                .setView(layout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Check which radio button was clicked
                        if (circInch.isChecked()) {
                            wheelUnit = "inch";
                            circCM.setChecked(false); // Make sure the other option is unchecked
                        } else {
                            wheelUnit = "cm";
                            circInch.setChecked(false);
                        }

                        textBox.setText(wheelCirc.getText().toString() + " " + wheelUnit);

                        if (wheelCirc.getText().toString().length() != 0) {
                            sendWheelCircCalc(wheelCirc.getText().toString(), wheelUnit);
                        }
                    }
                })
                .setNegativeButton("Cancel", null);

        alertDialog1 = builder.create();
        alertDialog1.show();

        // Initialize a TextView for ListView each Item
        tv = view.findViewById(android.R.id.text1);

        // Set the text color of TextView (ListView Item)
        tv.setTextColor(Color.WHITE);

        /*// Set up layout for alert dialog
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set text box input as int only
        final EditText tireSize1 = new EditText(MainActivity.this);
        tireSize1.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        tireSize1.setHint("Wheel size 1");
        layout.addView(tireSize1); // Notice this is an add method

        final EditText tireSize2 = new EditText(MainActivity.this);
        tireSize2.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        tireSize2.setHint("Wheel size 2");
        layout.addView(tireSize2);

        final EditText tireSize3 = new EditText(MainActivity.this);
        tireSize3.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        tireSize3.setHint("Wheel size 3");
        layout.addView(tireSize3);

        // Set layout for alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setIcon(R.drawable.ic_baseline_create_24px)
            .setView(layout)
            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    boolean result = false;

                    textBox.setText(tireSize1.getText().toString() + ", " +
                            tireSize2.getText().toString() + ", " +
                            tireSize3.getText().toString());

                    if ((tireSize1.getText().toString().length() != 0) &&
                            (tireSize2.getText().toString().length() != 0)  &&
                            (tireSize3.getText().toString().length() != 0)) {

                         sendWheelSizeCalc(tireSize1.getText().toString(),
                                tireSize2.getText().toString(),
                                tireSize3.getText().toString(),
                                unitsText.getText().toString());
                    }
                }
            })
            .setNegativeButton("Cancel", null);

        alertDialog1 = builder.create();
        alertDialog1.show();*/
    }

    void getMeterSettings() {

        getView().findViewById(R.id.meterSettings).setVisibility(View.VISIBLE);
        if (btSocket != null) {
            try {

                String rtnStr = null;
                String[] splitArr;

                btSocket.getOutputStream().write("L:1\0".getBytes());
                for (int rtnBuff = 0; ((rtnBuff = btSocket.getInputStream().read()) >= 0) && rtnBuff != 13;) { // 13 == '\0'
                    //Log.d("Test", b + " " + (char) b);
                    if (rtnStr == null) {
                        rtnStr = Character.toString((char) rtnBuff);
                    } else {
                        rtnStr += Character.toString((char) rtnBuff);
                    }
                }

                if (!rtnStr.startsWith("D:")) {
                    msg("error reading in values");
                    return;
                } else {

                    splitArr = rtnStr.split(":");

                    if (Integer.valueOf(splitArr[1]) == 0) { // units = MPH
                        unitsText.setText("mph");
                    } else if (Integer.valueOf(splitArr[1]) == 1) { // units = KPH
                        unitsText.setText("kph");
                    } else {
                        msg("error reading units");
                        return;
                    }

                    maxSpeedText.setText(splitArr[2]);
                    magnetsText.setText(splitArr[3]);

                    getOrigData(splitArr[4], finalDriveText, 1000000, false);
                    getOrigData(splitArr[5], meterRatioText, 1000000, false);
                    getOrigData(splitArr[6], wheelCircText, 100000000, true);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void getOrigData(String src, TextView dest, int divider, boolean wheelSize) {

        if (src != null) {

            if (wheelSize) {
                double val = Double.valueOf(src) / divider / Math.PI;
                if (unitsText.getText().toString().equalsIgnoreCase("mph")) {
                    dest.setText(String.valueOf(val * 63360 / 1000 / 3)); // divided by 3
                } else if (unitsText.getText().toString().equalsIgnoreCase("kph")) {

                    dest.setText(String.valueOf(val * 100));
                }
            } else {
                dest.setText(String.valueOf((Double.valueOf(src)) / divider));
            }
        }
    }

    void sendUnits(String indexStr) {
        //System.out.println(indexStr);
        //System.out.println("string length is: "+indexStr.length());

        if (btSocket != null) {
            try {
                String str = "U:";
                //System.out.println(indexStr);
                str = str + indexStr + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

                //System.out.println("3");
            } catch (IOException e) {
                msg("Error");
            }
        }

    }

    void sendInfo(String textStr, String extraStr) {

        if (btSocket != null) {
            try {
                String str = extraStr + textStr + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    void sendCalc(String textStr, String extraStr) {

        if (btSocket!=null) {
            try {
                String str = textStr;
                float result = Float.parseFloat(str);
                result=result*1000000;
                str = extraStr + Integer.toString((int) result) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    void sendWheelCircCalc(String textStr, String wheelUnit) {

        if (btSocket != null) {
            try {

                String str = textStr;
                float result = Float.parseFloat(str);

                if (wheelUnit == "inch") {
                    result = result * 1000000000;
                    result = result / 63360;
                } else { // cm
                    result = result * 10000;
                }

                str = "W:" + Integer.toString((int) result) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void msg(String s)
    {
        Toast.makeText(getActivity().getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }

}
