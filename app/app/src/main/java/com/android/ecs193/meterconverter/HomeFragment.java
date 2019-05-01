package com.android.ecs193.meterconverter;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import com.android.ecs193.meterconverter.MeterWizard.MeterWizardRatio;
import com.android.ecs193.meterconverter.MeterWizard.MeterWizardUnit;

public class HomeFragment extends Fragment {

    Button butWizardRatio;
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
    static TextView meterRatioText;
    TextView wheelSizeText;
    TextView wheelCircText;

    TextView tv;
    ListView mListView;
    BluetoothSocket btSocket = null;

    AlertDialog alertDialog1;

    String wheelUnit;
    final static int BT_INTENT_FLAG = 0;

    private RecyclerView recyclerView;

    MeterWizardRatio mMeterWizardRatio = new MeterWizardRatio();
    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_settings, container, false);

        view.findViewById(R.id.myCarSettings).setAlpha((float)0.3);
        view.findViewById(R.id.meterSettings).setAlpha((float)0.3);
        view.findViewById(R.id.sensorSettings).setAlpha((float)0.3);

        pairDevice = view.findViewById(R.id.deviceArrow);
        pairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent btIntent = new Intent(getActivity(), com.android.ecs193.meterconverter.BtConnection.class);
                startActivityForResult(btIntent, BT_INTENT_FLAG);

            }
        });

        butUnits = view.findViewById(R.id.but_units);
        unitsText = view.findViewById(R.id.unitsText);
        butUnits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getButtonDialog("Choose Units", R.array.units_array, unitsText);
            }
        });

        butMaxSpeed= view.findViewById(R.id.but_maxSpeed);
        maxSpeedText = view.findViewById(R.id.maxSpeedText);
        butMaxSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Max Speed", maxSpeedText, "M:");
            }
        });

        butMagnets= view.findViewById(R.id.but_magnets);
        magnetsText = view.findViewById(R.id.magnetsText);
        butMagnets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getButtonDialog("Choose Number of Magnets", R.array.magnet_array, magnetsText);
            }
        });

        butFinalDrive= view.findViewById(R.id.but_finalDrive);
        finalDriveText = view.findViewById(R.id.finalDriveText);
        butFinalDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Final Drive", finalDriveText, "F:");
            }
        });

        butRatio= view.findViewById(R.id.but_ratio);
        meterRatioText = view.findViewById(R.id.meterRatioText);
        butRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextBoxDialog("Enter Speedometer Ratio", meterRatioText, "S:");
            }
        });

        butWizardRatio = view.findViewById(R.id.find_ratio);
        butWizardRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (btSocket != null) {
                    try {
                        // Write to Arduino to tell it to enter speedometer ratio calibration mode
                        btSocket.getOutputStream().write("P:1\0".getBytes());

                        Intent wizIntent = new Intent(getActivity(), MeterWizardUnit.class);
                        startActivity(wizIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        butSize= view.findViewById(R.id.but_size);
        wheelCircText = view.findViewById(R.id.wheelCircText);
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
        /*recyclerView = (RecyclerView) view.findViewById(R.id.home_rv);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        rv_list = new ArrayList<>();
        rv_list.add(new HomeItem("Home", R.drawable.ic_settings_black_24dp));
        rv_list.add(new HomeItem("Dashboard", R.drawable.ic_dashboard));
        rv_list.add(new HomeItem("Notification", R.drawable.ic_notifications));
        rv_list.add(new HomeItem("image", R.drawable.ic_image));
        rv_list.add(new HomeItem("Music video", R.drawable.ic_music_video));
        rv_list.add(new HomeItem("Settings", R.drawable.ic_settings));


        MeterWizardUnits mAdapter = new MeterWizardUnits(rv_list);

        recyclerView.setAdapter(mAdapter);

        recyclerView.setItemAnimator(new DefaultItemAnimator());*/

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
                getView().findViewById(R.id.myCarSettings).setAlpha((float)1);
                getView().findViewById(R.id.meterSettings).setAlpha((float)1);
                getView().findViewById(R.id.sensorSettings).setAlpha((float)1);

                TextView deviceText = getView().findViewById(R.id.device_text);
                deviceText.setText(BtConnection.getBtName());
                getMeterSettings();
            }
        }
    }

    void getButtonDialog(final String title, int arrID, final TextView textBox) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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
                                mMeterWizardRatio.setspeedometerHalf(Integer.parseInt(input.getText().toString()));
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
                    mMeterWizardRatio.setspeedometerHalf(Integer.parseInt(splitArr[2]));
                    magnetsText.setText(splitArr[3]);

                    getOrigData(splitArr[4], finalDriveText, 1000000, false);
                    getOrigData(splitArr[5], meterRatioText, 1000000, false);
                    getOrigData(splitArr[6], wheelCircText, 1000000000, true);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void getOrigData(String src, TextView dest, int divider, boolean wheelSize) {

        if (src != null) {

            if (wheelSize) {
                //double val = Double.valueOf(src) / divider / Math.PI;
                //if (unitsText.getText().toString().equalsIgnoreCase("mph")) {
                    dest.setText(String.valueOf(Double.valueOf(src) * 63360 / divider));
                //} else if (unitsText.getText().toString().equalsIgnoreCase("kph")) { // cm

                 //   dest.setText(String.valueOf(val * 100));
                //}
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

    /*void sendWheelSizeCalc(String size1, String size2, String size3, String unit) {

        if (btSocket != null) {
            try {

                int result1 = Integer.parseInt(size1);
                int result2 = Integer.parseInt(size2);
                int result3 = Integer.parseInt(size3);

                double diameter;
                double result;

                if (unit == "kph") {
                    diameter = (result1 * result2 / 500) + (result3 * 2.54);
                    result = (diameter / 100) * Math.PI;
                } else { // mph
                    diameter = (result1 * result2 / 1270) + result3;
                    result = (diameter * 1000 / 63360) * Math.PI;
                }
                result = result * 100000000;

                String str = "W:" + Integer.toString((int) result) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);

            } catch (IOException e) {
                msg("Error");
            }
        }
    }*/

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

    static public void setMeterRatioText(String value) {
        meterRatioText.setText(value);
    }

    private void msg(String s)
    {
        Toast.makeText(getActivity().getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }
}

