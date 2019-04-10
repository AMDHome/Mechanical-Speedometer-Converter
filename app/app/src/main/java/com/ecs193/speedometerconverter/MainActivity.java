package com.ecs193.speedometerconverter;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;
import android.graphics.Color;
import java.io.IOException;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    Button btnPaired;
    TextView tv;
    ListView mListView;
    BluetoothSocket btSocket = null;


    TextView unitsText;
    TextView maxSpeedText;
    TextView magnetsText;
    TextView finalDriveText;
    TextView meterRatioText;
    RadioButton wheelSizeText;
    RadioButton wheelCircText;
    String wheelUnit;

    public final static String EXTRA_ADDRESS = "com.example.myfirstapp.MESSAGE";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // here we set the listener
            switch (item.getItemId()) {
                case R.id.bottombar_bluetooth:
                    setContentView(R.layout.activity_main);
                    //getMeterSettings();
                    //putMeterSettings();
                    return true;
                case R.id.bottombar_settings:
                    findViewById(R.id.searchDevicesTitle).setVisibility(View.GONE);
                    findViewById(R.id.meterSettings).setVisibility(View.GONE);
                    //setTitle("Calibration Settings");
                    //findViewById(R.id.bluetoothLayout).setVisibility(ConstraintLayout.GONE);
                    return true;
                case R.id.bottombar_data:
                    findViewById(R.id.searchDevicesTitle).setVisibility(View.GONE);
                    findViewById(R.id.meterSettings).setVisibility(View.GONE);
                    //setTitle("Data");
                    //findViewById(R.id.bluetoothLayout).setVisibility(ConstraintLayout.GONE);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // change opacity of calibration layout
        //findViewById(R.id.meterSettings).setAlpha((float)0.3);

        // Define widgets
        btnPaired = findViewById(R.id.deviceArrow);
        mListView = findViewById(R.id.listSettings);
        maxSpeedText = findViewById(R.id.maxSpeedText);
        magnetsText = findViewById(R.id.magnetsText);
        finalDriveText = findViewById(R.id.finalDriveText);
        meterRatioText = findViewById(R.id.meterRatioText);
        wheelSizeText = findViewById(R.id.wheelSizeText);
        wheelCircText = findViewById(R.id.wheelCircText);
        unitsText = findViewById(R.id.unitsText);
        //btnOn = findViewById(R.id.button2);
        //btnOff = findViewById(R.id.button3);
        //btnDis = findViewById(R.id.button4);





        BottomNavigationView navigation = findViewById(R.id.navigation);
        //Integer btID = getResources().getIdentifier("@string/menu_bluetooth","layout", getPackageName());
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        btnPaired.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View view) {
                Intent btIntent = new Intent(MainActivity.this, BtConnection.class);
                startActivity(btIntent);
            }
        });
    }

    void getMeterSettings() {
        if (btSocket != null) {
            try {

                byte[] rtnBuff = null;
                String rtnStr;
                String[] splitArr;

                btSocket.getOutputStream().write("L:1\0".getBytes());
                //TimeUnit.SECONDS.sleep(1); // add delay
                btSocket.getInputStream().read(rtnBuff, 0, 50);
                rtnStr = new String(rtnBuff);

                if (!rtnStr.startsWith("D:")) {
                    msg("error reading in values");
                    return;
                } else {

                    splitArr = rtnStr.split(":");
                    if (Integer.getInteger(splitArr[1]) == 0) { // units = MPH
                        unitsText.setText("mph");
                    } else if (Integer.getInteger(splitArr[1]) == 1) { // units = KPH
                        unitsText.setText("kph");
                    } else {
                        msg("error reading units");
                        return;
                    }

                    maxSpeedText.setText(Integer.getInteger(splitArr[2]));

                    magnetsText.setText(Integer.getInteger(splitArr[3]));

                    finalDriveText.setText(Integer.getInteger(splitArr[4]));

                    meterRatioText.setText(Integer.getInteger(splitArr[5]));

                    wheelCircText.setText(Integer.getInteger(splitArr[6]));
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void putMeterSettings() {

        // Get layout for meter settings
        findViewById(R.id.meterSettings).setVisibility(View.VISIBLE);

        final ArrayAdapter mAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.settings_array));

        mListView.setAdapter(mAdapter);

        //setListViewHeightBasedOnChildren(mListView, findViewById(R.id.meterSettings), 0.8);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, final View view, int i, long l) {

                final int itemNum = i;

                // Set up alert dialog with title
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getResources().getStringArray(R.array.settings_array)[i]);

                if (itemNum == 0) { // units

                    builder.setSingleChoiceItems(getResources().getStringArray(R.array.units_array),
                            -1, new DialogInterface.OnClickListener() {


                                @Override
                                public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                                    unitsText.setText(getResources()
                                            .getStringArray(R.array.units_array)[selectedIndex]
                                            .replaceAll(".* ", "")
                                            .replaceAll("\\(", "")
                                            .replaceAll("\\)", ""));
                                }
                            });

                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //sendUnits(Integer.toString(which));
                            //msg(unitsText.getText().toString());
                            if (unitsText.getText().toString().equalsIgnoreCase("mph")) {
                                sendUnits(Integer.toString(0));
                            } else if (unitsText.getText().toString().equalsIgnoreCase("kph")) {
                                sendUnits(Integer.toString(1));
                            }

                        }
                    });

                    builder.setNegativeButton("Cancel", null);


                } else if (itemNum == 1) { // max speed

                    // Set text box input as int only
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        boolean result = false;

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            maxSpeedText.setText(input.getText().toString());

                            if (input.getText().toString().length() != 0) {
                                result = sendInfo(input.getText().toString(), "M:");
                            }

                            if (result == false) {
                                msg("error");
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                } else if (itemNum == 2) { // magnets

                    builder.setSingleChoiceItems(getResources().getStringArray(R.array.magnet_array),
                            -1, new DialogInterface.OnClickListener() {



                        @Override
                        public void onClick(DialogInterface dialogInterface, int selectedIndex) {

                            magnetsText.setText(getResources()
                                    .getStringArray(R.array.magnet_array)[selectedIndex]);
                        }
                    });

                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

                        private DialogInterface mDialog;
                        boolean result = false;

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //result = sendInfo(Integer.toString(which), "N:");
                            //msg(magnetsText.getText().toString());
                            if (magnetsText.getText().toString().equalsIgnoreCase("1")) {
                                result = sendInfo(Integer.toString(1), "N:");
                            } else if (magnetsText.getText().toString().equalsIgnoreCase("2")) {
                                result = sendInfo(Integer.toString(2), "N:");
                            } else if (magnetsText.getText().toString().equalsIgnoreCase("4")) {
                                result = sendInfo(Integer.toString(4), "N:");
                            }
                            //result = sendInfo(magnetsText.getText().toString(), "N:");
                            //msg(Boolean.toString(result));
                        }

                    });
                    builder.setNegativeButton("Cancel", null);
                } else if (itemNum == 3 || itemNum == 4) { // max speed or final drive

                    // Set text box input as decimal only
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        boolean result = false;

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (itemNum == 3) {

                                finalDriveText.setText(input.getText().toString());

                                if (input.getText().toString().length() != 0) {
                                    result = sendCalc(input.getText().toString(), "F:");
                                }

                            } else if (itemNum == 4) { // 4

                                meterRatioText.setText(input.getText().toString());

                                if (input.getText().toString().length() != 0) {
                                    result = sendCalc(input.getText().toString(), "S:");
                                }
                            }

                            if (result == false) {
                                msg("error");
                            }
                        }
                    });

                } else if (itemNum == 5) { // wheel size

                    // Set up layout for alert dialog
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
                    layout.addView(tireSize3); // Another add method

                    // Set layout for alert dialog
                    builder.setView(layout);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            boolean result = false;

                            wheelSizeText.setText(tireSize1.getText().toString() + ", " +
                                    tireSize2.getText().toString() + ", " +
                                    tireSize3.getText().toString());

                            if ((tireSize1.getText().toString().length() != 0) &&
                            (tireSize2.getText().toString().length() != 0)  &&
                                    (tireSize3.getText().toString().length() != 0)) {

                                result = sendWheelSizeCalc(tireSize1.getText().toString(),
                                        tireSize2.getText().toString(),
                                        tireSize3.getText().toString(),
                                        unitsText.getText().toString());
                            }

                            if (result == false) {
                                msg("error");
                            }
                        }
                    });

                } else if (itemNum == 6) { // wheel circumference

                    // Set up layout for alert dialog
                    LinearLayout layout = new LinearLayout(MainActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    // Set text box input as decimal only
                    final EditText wheelCirc = new EditText(MainActivity.this);
                    wheelCirc.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    wheelCirc.setHint("Wheel circumference");
                    layout.addView(wheelCirc); // Notice this is an add method

                    // Set up radio button for units
                    final RadioButton circInch = new RadioButton(MainActivity.this);
                    circInch.setText("Inch (in)");
                    layout.addView(circInch);

                    final RadioButton circCM = new RadioButton(MainActivity.this);
                    circCM.setText("Centimeter (cm)");
                    layout.addView(circCM);

                    // Set layout for alert dialog
                    builder.setView(layout);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            boolean result = false;

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
                                result = sendWheelCircCalc(wheelCirc.getText().toString(), wheelUnit);
                            }

                            if (result == false) {
                                msg("error");
                            }
                        }
                    });

                }
                builder.show();

                // Initialize a TextView for ListView each Item
                tv = view.findViewById(android.R.id.text1);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.WHITE);
            }
        });
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

    boolean sendInfo(String textStr, String extraStr) {

        if (btSocket != null) {
            try {
                String str = extraStr + textStr + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);
                return true;
            } catch (IOException e) {
                msg("Error");
            }
        }
        return false;
    }

    boolean sendCalc(String textStr, String extraStr) {

        if (btSocket!=null) {
            try {
                String str = textStr;
                float result = Float.parseFloat(str);
                result=result*1000000;
                str = extraStr + Integer.toString((int) result) + '\0';
                btSocket.getOutputStream().write(str.getBytes());
                msg(str);
                return true;
            } catch (IOException e) {
                msg("Error");
            }
        }
        return false;
    }

    boolean sendWheelSizeCalc(String size1, String size2, String size3, String unit) {

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
                return true;
            } catch (IOException e) {
                msg("Error");
            }
        }
        return false;
    }

    boolean sendWheelCircCalc(String textStr, String wheelUnit) {

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
                return true;
            } catch (IOException e) {
                msg("Error");
            }
        }
        return false;
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }

}

